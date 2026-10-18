import unittest
from decimal import Decimal

from python_migration.calculator_collection.domain import Calculator, Manufacturer
from python_migration.calculator_collection.repositories.graph_repository import GraphRepository
from python_migration.calculator_collection.services.wishlist_service import WishlistService


class WishlistServiceTests(unittest.TestCase):
    def setUp(self):
        self.repository = GraphRepository()
        casio = Manufacturer(id=1, name="Casio")
        self.repository.add_manufacturer(casio)
        self.repository.add_calculator(Calculator(id=1, model="fx-991", current_price=Decimal("89.95"), manufacturer=casio))
        self.service = WishlistService(self.repository)

    def test_add_and_remove_wishlist_item(self):
        self.assertTrue(self.service.add_to_wishlist("alice", 1, "Wanted"))
        self.assertEqual(self.service.get_wishlist_count("alice"), 1)
        self.assertTrue(self.service.is_in_wishlist("alice", 1))
        self.assertTrue(self.service.remove_from_wishlist("alice", 1))
        self.assertEqual(self.service.get_wishlist_count("alice"), 0)

    def test_update_wishlist_notes(self):
        self.service.add_to_wishlist("alice", 1, "Initial")
        self.assertTrue(self.service.update_wishlist_notes("alice", 1, "Updated note"))
        self.assertEqual(self.service.get_wishlist("alice")[0].notes, "Updated note")


if __name__ == "__main__":
    unittest.main()
