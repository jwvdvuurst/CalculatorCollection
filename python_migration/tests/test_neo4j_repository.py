import unittest
from decimal import Decimal
from unittest.mock import patch

from python_migration.calculator_collection.adapters.graph.neo4j_repository import (
    Neo4jGraphRepository,
)
from python_migration.calculator_collection.domain import Calculator, Manufacturer


class FakeResult(list):
    def single(self):
        return self[0] if self else None

    def consume(self):
        return None


class FakeSession:
    def __init__(self, result=None):
        self.result = FakeResult(result or [])
        self.calls = []

    def __enter__(self):
        return self

    def __exit__(self, *_):
        return None

    def run(self, query, **parameters):
        self.calls.append((query, parameters))
        return self.result

    def execute_read(self, callback):
        return callback(self)

    def execute_write(self, callback, *args):
        return callback(self, *args)


class FakeDriver:
    def __init__(self, session):
        self.fake_session = session

    def session(self, **_):
        return self.fake_session


class Neo4jRepositoryTests(unittest.TestCase):
    def test_search_uses_bound_filters_and_maps_graph_nodes(self):
        session = FakeSession(
            [
                {
                    "calculator": {
                        "id": 7,
                        "model": "fx-991",
                        "current_price": "89.95",
                        "price_currency": "EUR",
                    },
                    "manufacturer": {"id": 3, "name": "Casio"},
                }
            ]
        )
        repository = Neo4jGraphRepository(FakeDriver(session))

        results = repository.search_calculators(
            " casio ", Decimal("50.00"), Decimal("100.00")
        )

        self.assertEqual(len(results), 1)
        self.assertEqual(results[0].id, 7)
        self.assertEqual(results[0].manufacturer.name, "Casio")
        self.assertEqual(results[0].current_price, Decimal("89.95"))
        query, parameters = session.calls[0]
        self.assertIn("CONTAINS toLower($search)", query)
        self.assertEqual(
            parameters,
            {"search": "casio", "min_price": 50.0, "max_price": 100.0},
        )

    def test_seed_catalogue_writes_in_bounded_batches(self):
        session = FakeSession()
        repository = Neo4jGraphRepository(FakeDriver(session))
        calculators = [
            Calculator(id=index, model=f"Model {index}", manufacturer=Manufacturer(
                id=1, name="Casio"
            ))
            for index in range(501)
        ]

        seeded = repository.seed_catalogue(calculators)

        self.assertEqual(seeded, 501)
        batch_writes = [
            parameters["rows"]
            for query, parameters in session.calls
            if "UNWIND $rows AS row" in query
        ]
        self.assertEqual([len(batch) for batch in batch_writes], [500, 1])
        self.assertEqual(batch_writes[0][0]["manufacturer_name"], "Casio")

    def test_user_lookup_uses_stable_graph_id(self):
        session = FakeSession(
            [
                {
                    "user": {
                        "id": 17,
                        "username": "renamed-or-reused",
                        "email": "person@example.test",
                        "password_hash": "argon2-hash",
                        "role": "USER",
                        "enabled": True,
                    }
                }
            ]
        )
        repository = Neo4jGraphRepository(FakeDriver(session))

        account = repository.get_user_by_id(17)

        self.assertEqual(account.id, 17)
        self.assertEqual(account.username, "renamed-or-reused")
        self.assertIn("{id: $user_id}", session.calls[0][0])
        self.assertEqual(session.calls[0][1], {"user_id": 17})

    def test_startup_migrations_assign_missing_ids_and_backfill_safely(self):
        session = FakeSession(
            [{"node_id": "user-node-1"}, {"node_id": "user-node-2"}]
        )
        repository = Neo4jGraphRepository(FakeDriver(session))

        repository._assign_missing_user_ids()
        repository._backfill_proposal_owners()

        sequence_sync_query = session.calls[0][0]
        self.assertIn("coalesce(max(user.id), 0)", sequence_sync_query)
        missing_users_query = session.calls[1][0]
        self.assertIn("WHERE user.id IS NULL", missing_users_query)
        self.assertIn("ORDER BY user.username", missing_users_query)
        assignment_calls = session.calls[2:4]
        self.assertEqual(
            [parameters["node_id"] for _, parameters in assignment_calls],
            ["user-node-1", "user-node-2"],
        )
        for assign_query, _ in assignment_calls:
            self.assertIn("sequence.value = sequence.value + 1", assign_query)
            self.assertIn("elementId(user) = $node_id", assign_query)
        owner_query = session.calls[4][0]
        self.assertIn("user.created_at <= proposal.created_at", owner_query)
        self.assertIn("proposal.created_by_id = user.id", owner_query)

    def test_proposal_approval_requires_a_valid_sales_period(self):
        session = FakeSession()

        result = Neo4jGraphRepository._review_proposal_tx(
            session, proposal_id=11, status="approved", reviewed_by="admin"
        )

        self.assertIsNone(result)
        query, parameters = session.calls[0]
        self.assertIn("proposal.sold_to >= proposal.sold_from", query)
        self.assertEqual(parameters, {"proposal_id": 11, "status": "approved"})

    def test_neo4j_password_is_required(self):
        with patch.dict("os.environ", {}, clear=True):
            with self.assertRaisesRegex(RuntimeError, "CALCOL_NEO4J_PASSWORD"):
                Neo4jGraphRepository.from_environment()


if __name__ == "__main__":
    unittest.main()
