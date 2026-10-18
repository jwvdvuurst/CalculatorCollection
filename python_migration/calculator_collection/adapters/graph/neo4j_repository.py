from __future__ import annotations

import json
import os
from datetime import datetime, timezone
from decimal import Decimal
from itertools import islice
from typing import Any, Iterable

from neo4j import Driver, GraphDatabase
from neo4j.exceptions import ConstraintError

from ...domain import (
    Calculator,
    Manufacturer,
    Proposal,
    UserAccount,
    UserCalculatorCollection,
)
from ...repositories.protocols import CalculatorCollectionRepository


class Neo4jGraphRepository(CalculatorCollectionRepository):
    """Neo4j openCypher adapter for the application repository contract."""

    _CONSTRAINTS = (
        "CREATE CONSTRAINT calculator_id IF NOT EXISTS "
        "FOR (n:Calculator) REQUIRE n.id IS UNIQUE",
        "CREATE CONSTRAINT manufacturer_id IF NOT EXISTS "
        "FOR (n:Manufacturer) REQUIRE n.id IS UNIQUE",
        "CREATE CONSTRAINT user_username IF NOT EXISTS "
        "FOR (n:User) REQUIRE n.username IS UNIQUE",
        "CREATE CONSTRAINT user_email IF NOT EXISTS "
        "FOR (n:User) REQUIRE n.email IS UNIQUE",
        "CREATE CONSTRAINT collection_item_key IF NOT EXISTS "
        "FOR (n:CollectionItem) REQUIRE n.key IS UNIQUE",
        "CREATE CONSTRAINT wishlist_item_key IF NOT EXISTS "
        "FOR (n:WishlistItem) REQUIRE n.key IS UNIQUE",
        "CREATE CONSTRAINT proposal_id IF NOT EXISTS "
        "FOR (n:Proposal) REQUIRE n.id IS UNIQUE",
        "CREATE CONSTRAINT sequence_name IF NOT EXISTS "
        "FOR (n:Sequence) REQUIRE n.name IS UNIQUE",
        "CREATE CONSTRAINT migration_key IF NOT EXISTS "
        "FOR (n:Migration) REQUIRE n.key IS UNIQUE",
    )

    def __init__(self, driver: Driver, database: str = "neo4j") -> None:
        self._driver = driver
        self._database = database

    @classmethod
    def from_environment(cls) -> Neo4jGraphRepository:
        """Create a repository using the local Neo4j connection environment."""
        password = os.getenv("CALCOL_NEO4J_PASSWORD")
        if not password:
            raise RuntimeError("CALCOL_NEO4J_PASSWORD must be set for Neo4j.")

        driver = GraphDatabase.driver(
            os.getenv("CALCOL_NEO4J_URI", "bolt://127.0.0.1:7687"),
            auth=(os.getenv("CALCOL_NEO4J_USERNAME", "neo4j"), password),
        )
        return cls(driver, os.getenv("CALCOL_NEO4J_DATABASE", "neo4j"))

    def initialize(self) -> None:
        """Verify connectivity and create idempotent graph constraints."""
        self._driver.verify_connectivity()
        with self._driver.session(database=self._database) as session:
            for statement in self._CONSTRAINTS:
                session.run(statement).consume()
            migration = session.run(
                """
                MATCH (legacy:Migration {key: $legacy_key})
                OPTIONAL MATCH (current:Migration {key: $current_key})
                RETURN legacy.record_count AS record_count, current.key AS current_key
                """,
                legacy_key="calculators-json-v1",
                current_key="calculators-json-v2",
            ).single()
            if migration and migration["current_key"] is None:
                session.execute_write(
                    self._migrate_v1_catalogue,
                    int(migration["record_count"] or 0),
                )
        self._synchronize_sequences()

    def close(self) -> None:
        self._driver.close()

    def get_all_calculators(self) -> list[Calculator]:
        query = """
        MATCH (c:Calculator)
        OPTIONAL MATCH (m:Manufacturer)-[:MANUFACTURES]->(c)
        RETURN c AS calculator, m AS manufacturer
        """
        with self._driver.session(database=self._database) as session:
            return [
                self._calculator_from_record(record)
                for record in session.run(query)
            ]

    def get_calculator_count(self) -> int:
        with self._driver.session(database=self._database) as session:
            record = session.run("MATCH (c:Calculator) RETURN count(c) AS count").single()
            return int(record["count"]) if record else 0

    def get_manufacturer_count(self) -> int:
        with self._driver.session(database=self._database) as session:
            record = session.run(
                "MATCH (m:Manufacturer) RETURN count(m) AS count"
            ).single()
            return int(record["count"]) if record else 0

    def is_catalogue_seeded(self) -> bool:
        query = "MATCH (m:Migration {key: 'calculators-json-v2'}) RETURN count(m) > 0 AS seeded"
        with self._driver.session(database=self._database) as session:
            record = session.run(query).single()
        return bool(record and record["seeded"])

    def get_calculator_by_id(self, calculator_id: int) -> Calculator | None:
        query = """
        MATCH (c:Calculator {id: $calculator_id})
        OPTIONAL MATCH (m:Manufacturer)-[:MANUFACTURES]->(c)
        RETURN c AS calculator, m AS manufacturer
        """
        with self._driver.session(database=self._database) as session:
            record = session.run(query, calculator_id=calculator_id).single()
        return self._calculator_from_record(record) if record else None

    def search_calculators(
        self,
        search: str | None,
        min_price: Decimal | None = None,
        max_price: Decimal | None = None,
    ) -> list[Calculator]:
        query = """
        MATCH (c:Calculator)
        OPTIONAL MATCH (m:Manufacturer)-[:MANUFACTURES]->(c)
        WHERE ($search IS NULL
               OR toLower(coalesce(c.model, '')) CONTAINS toLower($search)
               OR toLower(coalesce(m.name, '')) CONTAINS toLower($search))
          AND ($min_price IS NULL OR toFloat(c.current_price) >= $min_price)
          AND ($max_price IS NULL OR toFloat(c.current_price) <= $max_price)
        RETURN c AS calculator, m AS manufacturer
        """
        parameters = {
            "search": search.strip() if search and search.strip() else None,
            "min_price": float(min_price) if min_price is not None else None,
            "max_price": float(max_price) if max_price is not None else None,
        }
        with self._driver.session(database=self._database) as session:
            return [
                self._calculator_from_record(record)
                for record in session.run(query, **parameters)
            ]

    def get_calculators_by_manufacturer(
        self,
        manufacturer_id: int,
        min_price: Decimal | None = None,
        max_price: Decimal | None = None,
    ) -> list[Calculator]:
        query = """
        MATCH (m:Manufacturer {id: $manufacturer_id})-[:MANUFACTURES]->(c:Calculator)
        WHERE ($min_price IS NULL OR toFloat(c.current_price) >= $min_price)
          AND ($max_price IS NULL OR toFloat(c.current_price) <= $max_price)
        RETURN c AS calculator, m AS manufacturer
        """
        parameters = {
            "manufacturer_id": manufacturer_id,
            "min_price": float(min_price) if min_price is not None else None,
            "max_price": float(max_price) if max_price is not None else None,
        }
        with self._driver.session(database=self._database) as session:
            return [
                self._calculator_from_record(record)
                for record in session.run(query, **parameters)
            ]

    def get_all_manufacturers(self) -> list[Manufacturer]:
        query = """
        MATCH (m:Manufacturer)
        OPTIONAL MATCH (m)-[:MANUFACTURES]->(c:Calculator)
        RETURN m AS manufacturer, collect(c) AS calculators
        """
        with self._driver.session(database=self._database) as session:
            return [
                self._manufacturer_from_record(record)
                for record in session.run(query)
            ]

    def search_manufacturers(self, search: str | None = None) -> list[Manufacturer]:
        query = """
        MATCH (m:Manufacturer)
        OPTIONAL MATCH (m)-[:MANUFACTURES]->(c:Calculator)
        WHERE $search IS NULL OR toLower(m.name) CONTAINS toLower($search)
        RETURN m AS manufacturer, collect(c) AS calculators
        """
        parameters = {
            "search": search.strip() if search and search.strip() else None,
        }
        with self._driver.session(database=self._database) as session:
            return [
                self._manufacturer_from_record(record)
                for record in session.run(query, **parameters)
            ]

    def get_manufacturer_by_id(self, manufacturer_id: int) -> Manufacturer | None:
        query = """
        MATCH (m:Manufacturer {id: $manufacturer_id})
        OPTIONAL MATCH (m)-[:MANUFACTURES]->(c:Calculator)
        RETURN m AS manufacturer, collect(c) AS calculators
        """
        with self._driver.session(database=self._database) as session:
            record = session.run(query, manufacturer_id=manufacturer_id).single()
        return self._manufacturer_from_record(record) if record else None

    def add_manufacturer(self, manufacturer: Manufacturer) -> Manufacturer:
        if manufacturer.id is None:
            manufacturer.id = self._next_id("manufacturer")
        query = """
        MERGE (m:Manufacturer {id: $id})
        SET m.name = $name
        RETURN m AS manufacturer
        """
        with self._driver.session(database=self._database) as session:
            record = session.run(
                query, id=manufacturer.id, name=manufacturer.name
            ).single()
        if record is None:
            raise RuntimeError("Neo4j did not return the saved manufacturer.")
        return self._manufacturer_from_record(record)

    def add_calculator(self, calculator: Calculator) -> Calculator:
        if calculator.id is None:
            calculator.id = self._next_id("calculator")
        manufacturer_id = None
        manufacturer_name = None
        if calculator.manufacturer is not None:
            saved_manufacturer = self.add_manufacturer(calculator.manufacturer)
            manufacturer_id = saved_manufacturer.id
            manufacturer_name = saved_manufacturer.name
        self.seed_catalogue([calculator])
        saved = self.get_calculator_by_id(calculator.id)
        if saved is None:
            raise RuntimeError("Neo4j did not return the saved calculator.")
        if saved.manufacturer is None and manufacturer_id is not None:
            saved.manufacturer = Manufacturer(id=manufacturer_id, name=manufacturer_name or "")
        return saved

    def update_manufacturer(
        self, manufacturer_id: int, new_name: str
    ) -> Manufacturer | None:
        query = """
        MATCH (m:Manufacturer {id: $manufacturer_id})
        SET m.name = $name
        OPTIONAL MATCH (m)-[:MANUFACTURES]->(c:Calculator)
        RETURN m AS manufacturer, collect(c) AS calculators
        """
        with self._driver.session(database=self._database) as session:
            record = session.run(
                query, manufacturer_id=manufacturer_id, name=new_name.strip()
            ).single()
        return self._manufacturer_from_record(record) if record else None

    def merge_manufacturers(
        self, target_id: int, source_id: int, new_name: str | None = None
    ) -> bool:
        if target_id == source_id:
            return False
        query = """
        MATCH (target:Manufacturer {id: $target_id})
        MATCH (source:Manufacturer {id: $source_id})
        WITH target, source
        FOREACH (_ IN CASE WHEN $new_name IS NULL THEN [] ELSE [1] END |
            SET target.name = $new_name)
        WITH target, source
        OPTIONAL MATCH (source)-[old:MANUFACTURES]->(c:Calculator)
        FOREACH (_ IN CASE WHEN c IS NULL THEN [] ELSE [1] END |
            MERGE (target)-[:MANUFACTURES]->(c))
        FOREACH (_ IN CASE WHEN old IS NULL THEN [] ELSE [1] END |
            DELETE old)
        WITH target, source
        DETACH DELETE source
        RETURN count(target) > 0 AS merged
        """
        with self._driver.session(database=self._database) as session:
            record = session.execute_write(
                lambda tx: tx.run(
                    query,
                    target_id=target_id,
                    source_id=source_id,
                    new_name=new_name.strip() if new_name and new_name.strip() else None,
                ).single()
            )
        return bool(record and record["merged"])

    def get_user_collection(self, username: str) -> list[UserCalculatorCollection]:
        return self._get_items(username, "CollectionItem", "HAS_COLLECTION_ITEM")

    def get_user_collection_count(self, username: str) -> int:
        return self._count_items(username, "CollectionItem", "HAS_COLLECTION_ITEM")

    def add_to_collection(
        self, username: str, calculator_id: int, notes: str | None = None
    ) -> bool:
        return self._add_item(
            username, calculator_id, notes, "CollectionItem", "HAS_COLLECTION_ITEM"
        )

    def remove_from_collection(self, username: str, calculator_id: int) -> bool:
        return self._remove_item(
            username, calculator_id, "CollectionItem", "HAS_COLLECTION_ITEM"
        )

    def is_in_collection(self, username: str, calculator_id: int) -> bool:
        return self._has_item(
            username, calculator_id, "CollectionItem", "HAS_COLLECTION_ITEM"
        )

    def update_collection_notes(
        self, username: str, calculator_id: int, notes: str | None
    ) -> bool:
        return self._update_item_notes(
            username, calculator_id, notes, "CollectionItem", "HAS_COLLECTION_ITEM"
        )

    def get_wishlist(self, username: str) -> list[UserCalculatorCollection]:
        return self._get_items(username, "WishlistItem", "HAS_WISHLIST_ITEM")

    def get_wishlist_count(self, username: str) -> int:
        return self._count_items(username, "WishlistItem", "HAS_WISHLIST_ITEM")

    def add_to_wishlist(
        self, username: str, calculator_id: int, notes: str | None = None
    ) -> bool:
        return self._add_item(
            username, calculator_id, notes, "WishlistItem", "HAS_WISHLIST_ITEM"
        )

    def remove_from_wishlist(self, username: str, calculator_id: int) -> bool:
        return self._remove_item(
            username, calculator_id, "WishlistItem", "HAS_WISHLIST_ITEM"
        )

    def is_in_wishlist(self, username: str, calculator_id: int) -> bool:
        return self._has_item(
            username, calculator_id, "WishlistItem", "HAS_WISHLIST_ITEM"
        )

    def get_user_by_username(self, username: str) -> UserAccount | None:
        query = "MATCH (user:User {username: $username}) RETURN user"
        with self._driver.session(database=self._database) as session:
            record = session.run(query, username=username).single()
        if record is None:
            return None
        properties = self._properties(record["user"])
        if not properties.get("password_hash"):
            return None
        return UserAccount(
            username=properties["username"],
            email=properties["email"],
            password_hash=properties["password_hash"],
            role=properties.get("role", "USER"),
            enabled=properties.get("enabled", True),
        )

    def create_user(self, user: UserAccount) -> bool:
        query = """
        CREATE (account:User {
            username: $username,
            email: $email,
            password_hash: $password_hash,
            role: $role,
            enabled: $enabled,
            created_at: datetime()
        })
        RETURN account.username AS username
        """
        try:
            with self._driver.session(database=self._database) as session:
                record = session.execute_write(
                    lambda tx: tx.run(
                        query,
                        username=user.username,
                        email=user.email,
                        password_hash=user.password_hash,
                        role=user.role,
                        enabled=user.enabled,
                    ).single()
                )
        except ConstraintError:
            return False
        return record is not None

    def update_wishlist_notes(
        self, username: str, calculator_id: int, notes: str | None
    ) -> bool:
        return self._update_item_notes(
            username, calculator_id, notes, "WishlistItem", "HAS_WISHLIST_ITEM"
        )

    def submit_proposal(
        self,
        manufacturer_name: str,
        model: str,
        source_url: str = "",
        raw_row_text: str = "",
        sold_from: int | None = None,
        sold_to: int | None = None,
        created_by: str = "system",
    ) -> Proposal:
        proposal_id = self._next_id("proposal")
        query = """
        CREATE (p:Proposal {
            id: $id,
            manufacturer_name: $manufacturer_name,
            model: $model,
            source_url: $source_url,
            raw_row_text: $raw_row_text,
            sold_from: $sold_from,
            sold_to: $sold_to,
            created_by: $created_by,
            status: 'pending',
            created_at: datetime()
        })
        RETURN p AS proposal
        """
        with self._driver.session(database=self._database) as session:
            record = session.run(
                query,
                id=proposal_id,
                manufacturer_name=manufacturer_name,
                model=model,
                source_url=source_url,
                raw_row_text=raw_row_text,
                sold_from=sold_from,
                sold_to=sold_to,
                created_by=created_by,
            ).single()
        if record is None:
            raise RuntimeError("Neo4j did not return the saved proposal.")
        return self._proposal_from_node(record["proposal"])

    def list_proposals(self) -> list[Proposal]:
        query = "MATCH (p:Proposal) RETURN p AS proposal ORDER BY p.created_at DESC"
        with self._driver.session(database=self._database) as session:
            return [
                self._proposal_from_node(record["proposal"])
                for record in session.run(query)
            ]

    def set_proposal_status(self, proposal_id: int, status: str) -> Proposal | None:
        query = """
        MATCH (p:Proposal {id: $proposal_id})
        SET p.status = $status
        RETURN p AS proposal
        """
        with self._driver.session(database=self._database) as session:
            record = session.run(
                query, proposal_id=proposal_id, status=status
            ).single()
        return self._proposal_from_node(record["proposal"]) if record else None

    def seed_catalogue(self, calculators: Iterable[Calculator]) -> int:
        seeded = 0
        iterator = iter(calculators)
        with self._driver.session(database=self._database) as session:
            while batch := list(islice(iterator, 500)):
                rows = []
                for calculator in batch:
                    if calculator.id is None:
                        calculator.id = self._next_id("calculator")
                    manufacturer_id = None
                    manufacturer_name = None
                    if calculator.manufacturer is not None:
                        if calculator.manufacturer.id is None:
                            calculator.manufacturer.id = self._next_id("manufacturer")
                        manufacturer_id = calculator.manufacturer.id
                        manufacturer_name = calculator.manufacturer.name
                    rows.append(
                        {
                            "id": calculator.id,
                            "model": calculator.model,
                            "sold_from": calculator.sold_from,
                            "sold_to": calculator.sold_to,
                            "source_url": calculator.source_url,
                            "raw_row_text": calculator.raw_row_text,
                            "description": calculator.description,
                            "enriched_data": json.dumps(calculator.enriched_data),
                            "current_price": str(calculator.current_price),
                            "price_currency": calculator.price_currency,
                            "price_last_updated": (
                                calculator.price_last_updated.isoformat()
                                if calculator.price_last_updated
                                else None
                            ),
                            "manufacturer_id": manufacturer_id,
                            "manufacturer_name": manufacturer_name,
                        }
                    )
                session.execute_write(self._seed_catalogue_batch, rows)
                seeded += len(rows)
        if seeded:
            self._synchronize_sequences()
            with self._driver.session(database=self._database) as session:
                session.run(
                    """
                    MERGE (migration:Migration {key: 'calculators-json-v2'})
                    SET migration.record_count = $record_count,
                        migration.completed_at = datetime()
                    """,
                    record_count=seeded,
                ).consume()
        return seeded

    @staticmethod
    def _migrate_v1_catalogue(tx: Any, record_count: int) -> None:
        tx.run(
            """
            MATCH (manufacturer:Manufacturer)
            WITH manufacturer, trim(split(manufacturer.name, ':')[0]) AS normalized_name
            WITH normalized_name, collect(manufacturer) AS group
            WITH normalized_name, group,
                 reduce(canonical = head(group), candidate IN group |
                     CASE
                         WHEN candidate.id < canonical.id THEN candidate
                         ELSE canonical
                     END
                 ) AS canonical
            SET canonical.name = normalized_name
            WITH canonical, group
            UNWIND group AS old
            WITH canonical, old
            WHERE old <> canonical
            OPTIONAL MATCH (old)-[old_relationship:MANUFACTURES]->(calculator:Calculator)
            FOREACH (_ IN CASE WHEN old_relationship IS NULL THEN [] ELSE [1] END |
                MERGE (canonical)-[:MANUFACTURES]->(calculator)
                DELETE old_relationship
            )
            WITH DISTINCT canonical, old
            DETACH DELETE old
            """,
        ).consume()
        tx.run(
            """
            MERGE (migration:Migration {key: 'calculators-json-v2'})
            SET migration.record_count = $record_count,
                migration.completed_at = datetime()
            """,
            record_count=record_count,
        ).consume()

    def _synchronize_sequences(self) -> None:
        with self._driver.session(database=self._database) as session:
            for sequence_name, label in (
                ("calculator", "Calculator"),
                ("manufacturer", "Manufacturer"),
                ("proposal", "Proposal"),
            ):
                session.run(
                    f"""
                    OPTIONAL MATCH (entity:{label})
                    WITH coalesce(max(entity.id), 0) AS maximum
                    MERGE (sequence:Sequence {{name: $name}})
                    ON CREATE SET sequence.value = maximum
                    ON MATCH SET sequence.value = CASE
                        WHEN sequence.value < maximum THEN maximum
                        ELSE sequence.value
                    END
                    """,
                    name=sequence_name,
                ).consume()

    @staticmethod
    def _seed_catalogue_batch(tx: Any, rows: list[dict[str, Any]]) -> None:
        query = """
        UNWIND $rows AS row
        FOREACH (_ IN CASE WHEN row.manufacturer_id IS NULL THEN [] ELSE [1] END |
            MERGE (m:Manufacturer {id: row.manufacturer_id})
            ON CREATE SET m.name = row.manufacturer_name
            MERGE (c:Calculator {id: row.id})
            ON CREATE SET c.model = row.model,
                          c.sold_from = row.sold_from,
                          c.sold_to = row.sold_to,
                          c.source_url = row.source_url,
                          c.raw_row_text = row.raw_row_text,
                          c.description = row.description,
                          c.enriched_data = row.enriched_data,
                          c.current_price = row.current_price,
                          c.price_currency = row.price_currency,
                          c.price_last_updated = row.price_last_updated
            MERGE (m)-[:MANUFACTURES]->(c))
        FOREACH (_ IN CASE WHEN row.manufacturer_id IS NOT NULL THEN [] ELSE [1] END |
            MERGE (c:Calculator {id: row.id})
            ON CREATE SET c.model = row.model,
                          c.sold_from = row.sold_from,
                          c.sold_to = row.sold_to,
                          c.source_url = row.source_url,
                          c.raw_row_text = row.raw_row_text,
                          c.description = row.description,
                          c.enriched_data = row.enriched_data,
                          c.current_price = row.current_price,
                          c.price_currency = row.price_currency,
                          c.price_last_updated = row.price_last_updated)
        """
        tx.run(query, rows=rows).consume()

    def _next_id(self, name: str) -> int:
        query = """
        MERGE (sequence:Sequence {name: $name})
        ON CREATE SET sequence.value = 0
        SET sequence.value = sequence.value + 1
        RETURN sequence.value AS value
        """
        with self._driver.session(database=self._database) as session:
            record = session.execute_write(
                lambda tx: tx.run(query, name=name).single()
            )
        if record is None:
            raise RuntimeError(f"Neo4j failed to allocate an ID for {name}.")
        return int(record["value"])

    def _get_items(
        self, username: str, item_label: str, user_relationship: str
    ) -> list[UserCalculatorCollection]:
        query = f"""
        MATCH (u:User {{username: $username}})-[:{user_relationship}]->(item:{item_label})
        MATCH (item)-[:FOR_CALCULATOR]->(c:Calculator)
        OPTIONAL MATCH (m:Manufacturer)-[:MANUFACTURES]->(c)
        RETURN item, c AS calculator, m AS manufacturer
        ORDER BY item.added_at DESC
        """
        with self._driver.session(database=self._database) as session:
            return [
                self._collection_item_from_record(record, username)
                for record in session.run(query, username=username)
            ]

    def _count_items(
        self, username: str, item_label: str, user_relationship: str
    ) -> int:
        query = f"""
        MATCH (u:User {{username: $username}})-[:{user_relationship}]->(item:{item_label})
        RETURN count(item) AS count
        """
        with self._driver.session(database=self._database) as session:
            record = session.run(query, username=username).single()
        return int(record["count"]) if record else 0

    def _add_item(
        self,
        username: str,
        calculator_id: int,
        notes: str | None,
        item_label: str,
        user_relationship: str,
    ) -> bool:
        query = f"""
        MATCH (c:Calculator {{id: $calculator_id}})
        MERGE (u:User {{username: $username}})
        MERGE (item:{item_label} {{key: $item_key}})
        ON CREATE SET item.notes = $notes,
                      item.added_at = datetime(),
                      item.created = true
        WITH u, c, item, coalesce(item.created, false) AS created
        REMOVE item.created
        MERGE (u)-[:{user_relationship}]->(item)
        MERGE (item)-[:FOR_CALCULATOR]->(c)
        RETURN created
        """
        parameters = {
            "username": username,
            "calculator_id": calculator_id,
            "item_key": f"{username}:{calculator_id}",
            "notes": notes.strip() if notes and notes.strip() else "",
        }
        with self._driver.session(database=self._database) as session:
            record = session.execute_write(
                lambda tx: tx.run(query, **parameters).single()
            )
        return bool(record and record["created"])

    def _remove_item(
        self, username: str, calculator_id: int, item_label: str, user_relationship: str
    ) -> bool:
        query = f"""
        MATCH (:User {{username: $username}})-[:{user_relationship}]->(item:{item_label} {{key: $item_key}})
        DETACH DELETE item
        RETURN count(item) AS deleted
        """
        with self._driver.session(database=self._database) as session:
            record = session.execute_write(
                lambda tx: tx.run(
                    query,
                    username=username,
                    item_key=f"{username}:{calculator_id}",
                ).single()
            )
        return bool(record and record["deleted"])

    def _has_item(
        self, username: str, calculator_id: int, item_label: str, user_relationship: str
    ) -> bool:
        query = f"""
        MATCH (:User {{username: $username}})-[:{user_relationship}]->(item:{item_label} {{key: $item_key}})
        RETURN count(item) > 0 AS found
        """
        with self._driver.session(database=self._database) as session:
            record = session.run(
                query,
                username=username,
                item_key=f"{username}:{calculator_id}",
            ).single()
        return bool(record and record["found"])

    def _update_item_notes(
        self,
        username: str,
        calculator_id: int,
        notes: str | None,
        item_label: str,
        user_relationship: str,
    ) -> bool:
        query = f"""
        MATCH (:User {{username: $username}})-[:{user_relationship}]->(item:{item_label} {{key: $item_key}})
        SET item.notes = $notes
        RETURN count(item) > 0 AS updated
        """
        with self._driver.session(database=self._database) as session:
            record = session.execute_write(
                lambda tx: tx.run(
                    query,
                    username=username,
                    item_key=f"{username}:{calculator_id}",
                    notes=(notes or "").strip(),
                ).single()
            )
        return bool(record and record["updated"])

    @classmethod
    def _calculator_from_record(cls, record: Any) -> Calculator:
        manufacturer_node = record["manufacturer"]
        manufacturer = (
            cls._manufacturer_from_node(manufacturer_node)
            if manufacturer_node is not None
            else None
        )
        properties = cls._properties(record["calculator"])
        enriched_data = properties.get("enriched_data", "{}")
        return Calculator(
            id=properties["id"],
            model=properties.get("model", ""),
            sold_from=properties.get("sold_from"),
            sold_to=properties.get("sold_to"),
            source_url=properties.get("source_url", ""),
            raw_row_text=properties.get("raw_row_text", ""),
            description=properties.get("description", ""),
            enriched_data=json.loads(enriched_data) if enriched_data else {},
            current_price=Decimal(properties.get("current_price", "0.00")),
            price_currency=properties.get("price_currency", "EUR"),
            price_last_updated=cls._native_datetime(properties.get("price_last_updated")),
            manufacturer=manufacturer,
        )

    @classmethod
    def _manufacturer_from_record(cls, record: Any) -> Manufacturer:
        manufacturer = cls._manufacturer_from_node(record["manufacturer"])
        manufacturer.calculators = [
            cls._calculator_from_record(
                {"calculator": calculator, "manufacturer": record["manufacturer"]}
            )
            for calculator in record["calculators"]
            if calculator is not None
        ]
        return manufacturer

    @classmethod
    def _manufacturer_from_node(cls, node: Any) -> Manufacturer:
        properties = cls._properties(node)
        return Manufacturer(id=properties.get("id"), name=properties.get("name", ""))

    @classmethod
    def _proposal_from_node(cls, node: Any) -> Proposal:
        properties = cls._properties(node)
        return Proposal(
            id=properties.get("id"),
            manufacturer_name=properties.get("manufacturer_name", ""),
            model=properties.get("model", ""),
            sold_from=properties.get("sold_from"),
            sold_to=properties.get("sold_to"),
            source_url=properties.get("source_url", ""),
            raw_row_text=properties.get("raw_row_text", ""),
            status=properties.get("status", "pending"),
            created_by=properties.get("created_by", "system"),
            created_at=cls._native_datetime(properties.get("created_at")),
        )

    @classmethod
    def _collection_item_from_record(
        cls, record: Any, username: str
    ) -> UserCalculatorCollection:
        calculator = cls._calculator_from_record(record)
        item = cls._properties(record["item"])
        return UserCalculatorCollection(
            username=username,
            calculator=calculator,
            added_at=cls._native_datetime(item.get("added_at")),
            notes=item.get("notes", ""),
        )

    @staticmethod
    def _properties(node: Any) -> dict[str, Any]:
        return dict(node) if node is not None else {}

    @staticmethod
    def _native_datetime(value: Any) -> datetime | None:
        if value is None:
            return None
        if hasattr(value, "to_native"):
            return value.to_native()
        if isinstance(value, str):
            return datetime.fromisoformat(value)
        if isinstance(value, datetime) and value.tzinfo is None:
            return value.replace(tzinfo=timezone.utc)
        return value
