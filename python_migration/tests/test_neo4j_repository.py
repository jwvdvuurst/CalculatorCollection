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

    def test_neo4j_password_is_required(self):
        with patch.dict("os.environ", {}, clear=True):
            with self.assertRaisesRegex(RuntimeError, "CALCOL_NEO4J_PASSWORD"):
                Neo4jGraphRepository.from_environment()


if __name__ == "__main__":
    unittest.main()
