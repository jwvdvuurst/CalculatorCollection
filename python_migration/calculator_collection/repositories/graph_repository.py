from __future__ import annotations

import json
from dataclasses import dataclass, field
from datetime import datetime, timezone
from decimal import Decimal
from pathlib import Path
from typing import Iterable

from ..domain import (
    Calculator,
    Manufacturer,
    Proposal,
    UserAccount,
    UserCalculatorCollection,
)
from .protocols import CalculatorCollectionRepository


@dataclass
class GraphRepository:
    calculators: dict[int, Calculator] = field(default_factory=dict)
    manufacturers: dict[int, Manufacturer] = field(default_factory=dict)
    user_collections: dict[str, dict[int, UserCalculatorCollection]] = field(default_factory=dict)
    wishlist_items: dict[str, dict[int, UserCalculatorCollection]] = field(default_factory=dict)
    proposals: dict[int, Proposal] = field(default_factory=dict)
    users: dict[str, UserAccount] = field(default_factory=dict)

    def close(self) -> None:
        return None

    def add_manufacturer(self, manufacturer: Manufacturer) -> Manufacturer:
        if manufacturer.id is None:
            manufacturer.id = max(self.manufacturers.keys(), default=0) + 1
        self.manufacturers[manufacturer.id] = manufacturer
        return manufacturer

    def add_calculator(self, calculator: Calculator) -> Calculator:
        if calculator.id is None:
            calculator.id = max(self.calculators.keys(), default=0) + 1
        if calculator.manufacturer is not None:
            manufacturer = self.manufacturers.get(calculator.manufacturer.id)
            if manufacturer is None:
                manufacturer = self.add_manufacturer(calculator.manufacturer)
            calculator.manufacturer = manufacturer
            if calculator not in manufacturer.calculators:
                manufacturer.calculators.append(calculator)
        self.calculators[calculator.id] = calculator
        return calculator

    def get_all_calculators(self) -> list[Calculator]:
        return list(self.calculators.values())

    def get_calculator_count(self) -> int:
        return len(self.calculators)

    def get_calculator_by_id(self, calculator_id: int) -> Calculator | None:
        return self.calculators.get(calculator_id)

    def search_calculators(self, search: str | None, min_price: Decimal | None = None, max_price: Decimal | None = None) -> list[Calculator]:
        query = (search or "").strip().lower()
        results = list(self.calculators.values())

        if query:
            results = [
                calc for calc in results
                if query in (calc.model or "").lower() or query in (calc.manufacturer.name if calc.manufacturer else "").lower()
            ]

        if min_price is not None or max_price is not None:
            lower = min_price if min_price is not None else Decimal("0.00")
            upper = max_price if max_price is not None else Decimal("999999999")
            results = [
                calc for calc in results
                if calc.current_price is not None and lower <= calc.current_price <= upper
            ]

        return results

    def get_calculators_by_manufacturer(self, manufacturer_id: int, min_price: Decimal | None = None, max_price: Decimal | None = None) -> list[Calculator]:
        results = [
            calc for calc in self.calculators.values()
            if calc.manufacturer is not None and calc.manufacturer.id == manufacturer_id
        ]
        if min_price is not None or max_price is not None:
            lower = min_price if min_price is not None else Decimal("0.00")
            upper = max_price if max_price is not None else Decimal("999999999")
            results = [calc for calc in results if lower <= calc.current_price <= upper]
        return results

    def get_all_manufacturers(self) -> list[Manufacturer]:
        return list(self.manufacturers.values())

    def get_manufacturer_count(self) -> int:
        return len(self.manufacturers)

    def search_manufacturers(self, search: str | None = None) -> list[Manufacturer]:
        query = (search or "").strip().lower()
        if not query:
            return self.get_all_manufacturers()
        return [m for m in self.manufacturers.values() if query in m.name.lower()]

    def get_manufacturer_by_id(self, manufacturer_id: int) -> Manufacturer | None:
        return self.manufacturers.get(manufacturer_id)

    def is_catalogue_seeded(self) -> bool:
        return bool(self.calculators)

    def update_manufacturer(self, manufacturer_id: int, new_name: str) -> Manufacturer | None:
        manufacturer = self.manufacturers.get(manufacturer_id)
        if manufacturer is None:
            return None
        manufacturer.name = new_name.strip()
        return manufacturer

    def merge_manufacturers(self, target_id: int, source_id: int, new_name: str | None = None) -> bool:
        if target_id == source_id:
            return False

        target = self.manufacturers.get(target_id)
        source = self.manufacturers.get(source_id)
        if target is None or source is None:
            return False

        if new_name and new_name.strip():
            target.name = new_name.strip()

        for calculator in list(self.calculators.values()):
            if calculator.manufacturer and calculator.manufacturer.id == source_id:
                calculator.manufacturer = target
                if calculator not in target.calculators:
                    target.calculators.append(calculator)

        source.calculators = []
        del self.manufacturers[source_id]
        return True

    def get_user_collection(self, username: str) -> list[UserCalculatorCollection]:
        return list((self.user_collections.get(username) or {}).values())

    def get_user_collection_count(self, username: str) -> int:
        return len(self.user_collections.get(username, {}))

    def add_to_collection(self, username: str, calculator_id: int, notes: str | None = None) -> bool:
        if calculator_id not in self.calculators:
            return False
        username_items = self.user_collections.setdefault(username, {})
        if calculator_id in username_items:
            return False
        collection_item = UserCalculatorCollection(
            username=username,
            calculator=self.calculators[calculator_id],
            added_at=datetime.now(timezone.utc),
            notes=notes.strip() if notes and notes.strip() else "",
        )
        username_items[calculator_id] = collection_item
        return True

    def remove_from_collection(self, username: str, calculator_id: int) -> bool:
        username_items = self.user_collections.get(username, {})
        if calculator_id not in username_items:
            return False
        del username_items[calculator_id]
        return True

    def is_in_collection(self, username: str, calculator_id: int) -> bool:
        return calculator_id in self.user_collections.get(username, {})

    def update_collection_notes(self, username: str, calculator_id: int, notes: str | None) -> bool:
        username_items = self.user_collections.get(username, {})
        if calculator_id not in username_items:
            return False
        username_items[calculator_id].notes = (notes or "").strip()
        return True

    def get_wishlist(self, username: str) -> list[UserCalculatorCollection]:
        return list((self.wishlist_items.get(username) or {}).values())

    def get_wishlist_count(self, username: str) -> int:
        return len(self.wishlist_items.get(username, {}))

    def add_to_wishlist(self, username: str, calculator_id: int, notes: str | None = None) -> bool:
        if calculator_id not in self.calculators:
            return False
        username_items = self.wishlist_items.setdefault(username, {})
        if calculator_id in username_items:
            return False
        wishlist_item = UserCalculatorCollection(
            username=username,
            calculator=self.calculators[calculator_id],
            added_at=datetime.now(timezone.utc),
            notes=notes.strip() if notes and notes.strip() else "",
        )
        username_items[calculator_id] = wishlist_item
        return True

    def remove_from_wishlist(self, username: str, calculator_id: int) -> bool:
        username_items = self.wishlist_items.get(username, {})
        if calculator_id not in username_items:
            return False
        del username_items[calculator_id]
        return True

    def is_in_wishlist(self, username: str, calculator_id: int) -> bool:
        return calculator_id in self.wishlist_items.get(username, {})

    def get_user_by_username(self, username: str) -> UserAccount | None:
        return self.users.get(username)

    def create_user(self, user: UserAccount) -> bool:
        if (
            user.username in self.users
            or user.username in self.user_collections
            or user.username in self.wishlist_items
            or any(
                account.email.casefold() == user.email.casefold()
                for account in self.users.values()
            )
        ):
            return False
        self.users[user.username] = user
        return True

    def update_wishlist_notes(self, username: str, calculator_id: int, notes: str | None) -> bool:
        username_items = self.wishlist_items.get(username, {})
        if calculator_id not in username_items:
            return False
        username_items[calculator_id].notes = (notes or "").strip()
        return True

    def submit_proposal(self, manufacturer_name: str, model: str, source_url: str = "", raw_row_text: str = "",
                       sold_from: int | None = None, sold_to: int | None = None, created_by: str = "system") -> Proposal:
        proposal_id = max(self.proposals.keys(), default=0) + 1
        proposal = Proposal(
            id=proposal_id,
            manufacturer_name=manufacturer_name,
            model=model,
            sold_from=sold_from,
            sold_to=sold_to,
            source_url=source_url,
            raw_row_text=raw_row_text,
            created_by=created_by,
        )
        self.proposals[proposal_id] = proposal
        return proposal

    def list_proposals(self) -> list[Proposal]:
        return list(self.proposals.values())

    def set_proposal_status(self, proposal_id: int, status: str) -> Proposal | None:
        proposal = self.proposals.get(proposal_id)
        if proposal is None:
            return None
        proposal.status = status
        return proposal

    def load_catalogue_from_json_file(self, json_path: str | Path) -> int:
        path = Path(json_path)
        if not path.exists():
            return 0

        with path.open("r", encoding="utf-8") as handle:
            payload = json.load(handle)

        loaded_count = 0
        manufacturer_index: dict[str, Manufacturer] = {}
        for entry in payload.get("manufacturers", []):
            manufacturer_value = str(entry.get("manufacturer") or "").strip()
            manufacturer_name = (
                manufacturer_value.partition(":")[0].strip()
                if ":" in manufacturer_value
                else manufacturer_value
            )
            if not manufacturer_name:
                continue

            manufacturer = manufacturer_index.get(manufacturer_name)
            if manufacturer is None:
                manufacturer = Manufacturer(name=manufacturer_name)
                manufacturer = self.add_manufacturer(manufacturer)
                manufacturer_index[manufacturer_name] = manufacturer

            for model_entry in entry.get("models", []):
                model_name = str(model_entry.get("model") or "").strip()
                if not model_name:
                    continue

                calculator = Calculator(
                    model=model_name,
                    sold_from=model_entry.get("sold_from"),
                    sold_to=model_entry.get("sold_to"),
                    source_url=str(model_entry.get("source_url") or ""),
                    raw_row_text=str(model_entry.get("raw_row_text") or ""),
                    description="",
                    current_price=Decimal("0.00"),
                    manufacturer=manufacturer,
                )
                self.add_calculator(calculator)
                loaded_count += 1

        return loaded_count

    def seed(self, manufacturers: Iterable[Manufacturer], calculators: Iterable[Calculator]) -> None:
        for manufacturer in manufacturers:
            self.add_manufacturer(manufacturer)
        for calculator in calculators:
            self.add_calculator(calculator)

    def seed_catalogue(self, calculators: Iterable[Calculator]) -> int:
        seeded = 0
        for calculator in calculators:
            self.add_calculator(calculator)
            seeded += 1
        return seeded
