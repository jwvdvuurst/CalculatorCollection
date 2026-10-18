import unittest
from decimal import Decimal

from python_migration.calculator_collection.domain import Calculator, Manufacturer
from python_migration.calculator_collection.repositories.graph_repository import GraphRepository
from python_migration.calculator_collection.services.manufacturer_service import ManufacturerService
from python_migration.calculator_collection.services.proposal_service import ProposalService


class ManufacturerAndProposalTests(unittest.TestCase):
    def setUp(self):
        self.repository = GraphRepository()
        casio = Manufacturer(id=1, name="Casio")
        sharp = Manufacturer(id=2, name="Sharp")
        self.repository.add_manufacturer(casio)
        self.repository.add_manufacturer(sharp)
        self.repository.add_calculator(Calculator(id=1, model="fx-991", current_price=Decimal("89.95"), manufacturer=casio))
        self.repository.add_calculator(Calculator(id=2, model="EL-531", current_price=Decimal("55.00"), manufacturer=sharp))
        self.manufacturer_service = ManufacturerService(self.repository)
        self.proposal_service = ProposalService(self.repository)

    def test_manufacturer_search_and_merge(self):
        results = self.manufacturer_service.search_manufacturers("cas")
        self.assertEqual(results[0].name, "Casio")
        self.assertTrue(self.manufacturer_service.merge_manufacturers(1, 2, "Casio & Sharp"))

    def test_submit_and_list_proposals(self):
        proposal = self.proposal_service.submit_proposal(
            manufacturer_name="Olympia",
            model="SP 20",
            source_url="https://example.com/sp20",
            raw_row_text="Olympia SP 20",
            created_by="demo-user",
        )
        self.assertEqual(proposal["status"], "pending")
        self.assertEqual(len(self.proposal_service.list_proposals()), 1)


if __name__ == "__main__":
    unittest.main()
