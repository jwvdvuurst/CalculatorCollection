import json
import os
import tempfile
import unittest
from decimal import Decimal
from pathlib import Path

from python_migration.calculator_collection.domain import Calculator, Manufacturer, UserCalculatorCollection
from python_migration.calculator_collection.repositories.graph_repository import GraphRepository
from python_migration.calculator_collection.services.calculator_service import CalculatorService


class CalculatorServiceTests(unittest.TestCase):
    def setUp(self):
        self.repository = GraphRepository()
        casio = Manufacturer(id=1, name="Casio")
        sharp = Manufacturer(id=2, name="Sharp")
        self.repository.seed([casio, sharp], [
            Calculator(id=1, model="fx-991", current_price=Decimal("89.95"), manufacturer=casio),
            Calculator(id=2, model="EL-531", current_price=Decimal("55.00"), manufacturer=sharp),
            Calculator(id=3, model="fx-82", current_price=Decimal("120.00"), manufacturer=casio),
        ])
        self.service = CalculatorService(self.repository)

    def test_search_calculators_matches_model_and_manufacturer(self):
        results = self.service.search_calculators("casio")
        self.assertEqual(len(results), 2)

    def test_search_calculators_applies_price_filters(self):
        results = self.service.search_calculators(None, Decimal("80.00"), Decimal("100.00"), "price_asc")
        self.assertEqual([calc.model for calc in results], ["fx-991"])

    def test_merge_manufacturers_reassigns_calculators(self):
        merged = self.service.merge_manufacturers(1, 2, "Casio & Sharp")
        self.assertTrue(merged)
        self.assertEqual(self.service.get_manufacturer_by_id(2), None)
        self.assertEqual(self.service.get_calculators_by_manufacturer(1)[0].manufacturer.name, "Casio & Sharp")

    def test_collection_operations(self):
        self.assertTrue(self.service.add_to_collection("alice", 1, "Owned"))
        self.assertEqual(self.service.get_user_collection_count("alice"), 1)
        self.assertTrue(self.service.is_in_collection("alice", 1))
        self.assertTrue(self.service.update_collection_notes("alice", 1, "Updated note"))
        self.assertEqual(self.service.get_user_collection("alice")[0].notes, "Updated note")

    def test_load_catalogue_from_json_file(self):
        repository = GraphRepository()
        json_path = os.path.join(os.path.dirname(__file__), "..", "..", "calculators.json")
        loaded = repository.load_catalogue_from_json_file(json_path)
        self.assertGreater(loaded, 0)
        self.assertGreater(len(repository.get_all_calculators()), 0)
        self.assertGreater(len(repository.get_all_manufacturers()), 0)

    def test_load_catalogue_splits_manufacturer_at_first_colon(self):
        catalogue = {
            "manufacturers": [
                {
                    "manufacturer": "Acme Calculators: Model one: revision two",
                    "models": [{"model": "Calculator Model One"}],
                },
                {
                    "manufacturer": "Acme Calculators: Model two",
                    "models": [{"model": "Calculator Model Two"}],
                },
                {
                    "manufacturer": "No Colon Manufacturer",
                    "models": [{"model": "NC-1"}],
                },
            ]
        }
        with tempfile.TemporaryDirectory() as directory:
            catalogue_path = Path(directory) / "catalogue.json"
            catalogue_path.write_text(json.dumps(catalogue), encoding="utf-8")
            repository = GraphRepository()
            loaded = repository.load_catalogue_from_json_file(catalogue_path)

        self.assertEqual(loaded, 3)
        self.assertEqual(
            {manufacturer.name for manufacturer in repository.get_all_manufacturers()},
            {"Acme Calculators", "No Colon Manufacturer"},
        )
        self.assertEqual(
            {calculator.model for calculator in repository.get_all_calculators()},
            {"Calculator Model One", "Calculator Model Two", "NC-1"},
        )

if __name__ == "__main__":
    unittest.main()
