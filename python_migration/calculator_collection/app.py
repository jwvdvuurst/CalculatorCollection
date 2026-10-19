from __future__ import annotations

from decimal import Decimal
from typing import Any

from .api.router import CalculatorCollectionRouter
from .repositories.graph_repository import GraphRepository
from .repositories.protocols import CalculatorCollectionRepository
from .services.calculator_service import CalculatorService
from .services.manufacturer_service import ManufacturerService
from .services.proposal_service import ProposalService
from .services.wishlist_service import WishlistService


class CalculatorCollectionApp:
    """Application façade that models the Python service entry point.

    This is designed to be lifted into an AWS-style deployment later without requiring
    a full rewrite of the business logic. The app can be exposed through FastAPI,
    Flask, or Lambda handlers while keeping the same service layer.
    """

    def __init__(self, repository: CalculatorCollectionRepository | None = None):
        self.repository = repository or GraphRepository()
        self.service = CalculatorService(self.repository)
        self.wishlist_service = WishlistService(self.repository)
        self.manufacturer_service = ManufacturerService(self.repository)
        self.proposal_service = ProposalService(self.repository)
        self.router = CalculatorCollectionRouter(self.service)

    def search_calculators(self, search: str | None = None, manufacturer_id: int | None = None,
                          min_price: str | None = None, max_price: str | None = None,
                          sort: str | None = None) -> list[dict[str, Any]]:
        return self.router.list_calculators(search, manufacturer_id, min_price, max_price, sort)

    def add_to_collection(self, username: str, calculator_id: int, notes: str | None = None) -> bool:
        return self.router.add_to_collection(username, calculator_id, notes)

    def get_user_collection(self, username: str) -> list[dict[str, Any]]:
        return self.router.get_collection(username)

    def add_to_wishlist(self, username: str, calculator_id: int, notes: str | None = None) -> bool:
        return self.wishlist_service.add_to_wishlist(username, calculator_id, notes)

    def get_wishlist(self, username: str):
        return self.wishlist_service.get_wishlist(username)

    def list_manufacturers(self, search: str | None = None, sort: str | None = None):
        return self.manufacturer_service.get_all_manufacturers(search, sort)

    def merge_manufacturers(self, target_id: int, source_id: int, new_name: str | None = None) -> bool:
        return self.service.merge_manufacturers(target_id, source_id, new_name)

    def submit_proposal(self, manufacturer_name: str, model: str, source_url: str = "", raw_row_text: str = "",
                       sold_from: int | None = None, sold_to: int | None = None, created_by: str = "system",
                       created_by_id: int | None = None):
        return self.proposal_service.submit_proposal(
            manufacturer_name,
            model,
            source_url,
            raw_row_text,
            sold_from,
            sold_to,
            created_by,
            created_by_id,
        )

    def list_proposals(self):
        return self.proposal_service.list_proposals()

    def filter_by_price(self, calculators: list[Any], min_price: str | None = None, max_price: str | None = None):
        lower = Decimal(min_price) if min_price else Decimal("0.00")
        upper = Decimal(max_price) if max_price else Decimal("999999999")
        return [calc for calc in calculators if lower <= calc.current_price <= upper]
