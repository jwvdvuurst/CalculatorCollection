from __future__ import annotations

from decimal import Decimal
from typing import Iterable, Protocol

from ..domain import Calculator, Manufacturer, Proposal, UserAccount, UserCalculatorCollection


class CalculatorRepository(Protocol):
    def add_calculator(self, calculator: Calculator) -> Calculator: ...

    def get_all_calculators(self) -> list[Calculator]: ...

    def get_calculator_count(self) -> int: ...

    def get_calculator_by_id(self, calculator_id: int) -> Calculator | None: ...

    def update_calculator(self, calculator: Calculator) -> Calculator | None: ...

    def delete_calculator(self, calculator_id: int) -> bool: ...

    def search_calculators(
        self,
        search: str | None,
        min_price: Decimal | None = None,
        max_price: Decimal | None = None,
    ) -> list[Calculator]: ...

    def get_calculators_by_manufacturer(
        self,
        manufacturer_id: int,
        min_price: Decimal | None = None,
        max_price: Decimal | None = None,
    ) -> list[Calculator]: ...

class ManufacturerRepository(Protocol):
    def add_manufacturer(self, manufacturer: Manufacturer) -> Manufacturer: ...

    def get_all_manufacturers(self) -> list[Manufacturer]: ...

    def get_manufacturer_count(self) -> int: ...

    def search_manufacturers(self, search: str | None = None) -> list[Manufacturer]: ...

    def get_manufacturer_by_id(self, manufacturer_id: int) -> Manufacturer | None: ...

    def is_catalogue_seeded(self) -> bool: ...

    def update_manufacturer(self, manufacturer_id: int, new_name: str) -> Manufacturer | None: ...

    def merge_manufacturers(
        self, target_id: int, source_id: int, new_name: str | None = None
    ) -> bool: ...

    def seed_catalogue(self, calculators: Iterable[Calculator]) -> int: ...


class CollectionRepository(Protocol):
    def get_user_collection(self, username: str) -> list[UserCalculatorCollection]: ...

    def get_user_collection_count(self, username: str) -> int: ...

    def add_to_collection(
        self, username: str, calculator_id: int, notes: str | None = None
    ) -> bool: ...

    def remove_from_collection(self, username: str, calculator_id: int) -> bool: ...

    def is_in_collection(self, username: str, calculator_id: int) -> bool: ...

    def update_collection_notes(
        self, username: str, calculator_id: int, notes: str | None
    ) -> bool: ...


class WishlistRepository(Protocol):
    def get_wishlist(self, username: str) -> list[UserCalculatorCollection]: ...

    def get_wishlist_count(self, username: str) -> int: ...

    def add_to_wishlist(
        self, username: str, calculator_id: int, notes: str | None = None
    ) -> bool: ...

    def remove_from_wishlist(self, username: str, calculator_id: int) -> bool: ...

    def is_in_wishlist(self, username: str, calculator_id: int) -> bool: ...

    def update_wishlist_notes(
        self, username: str, calculator_id: int, notes: str | None
    ) -> bool: ...


class ProposalRepository(Protocol):
    def submit_proposal(
        self,
        manufacturer_name: str,
        model: str,
        source_url: str = "",
        raw_row_text: str = "",
        sold_from: int | None = None,
        sold_to: int | None = None,
        created_by: str = "system",
        created_by_id: int | None = None,
    ) -> Proposal: ...

    def list_proposals(self) -> list[Proposal]: ...

    def set_proposal_status(self, proposal_id: int, status: str) -> Proposal | None: ...

    def review_proposal(
        self, proposal_id: int, status: str, reviewed_by: str
    ) -> Proposal | None: ...


class UserRepository(Protocol):
    def get_user_by_username(self, username: str) -> UserAccount | None: ...

    def get_user_by_id(self, user_id: int) -> UserAccount | None: ...

    def create_user(self, user: UserAccount) -> bool: ...

    def get_all_users(self) -> list[UserAccount]: ...

    def update_user(self, user: UserAccount) -> bool: ...

    def delete_user(self, user_id: int) -> bool: ...

    def update_last_login(self, username: str) -> None: ...


class CalculatorCollectionRepository(
    CalculatorRepository,
    ManufacturerRepository,
    CollectionRepository,
    WishlistRepository,
    ProposalRepository,
    UserRepository,
    Protocol,
):
    def close(self) -> None: ...
