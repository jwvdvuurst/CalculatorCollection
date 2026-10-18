from __future__ import annotations

from ..repositories.protocols import WishlistRepository


class WishlistService:
    def __init__(self, repository: WishlistRepository):
        self.repository = repository

    def get_wishlist(self, username: str):
        return self.repository.get_wishlist(username)

    def get_wishlist_count(self, username: str) -> int:
        return self.repository.get_wishlist_count(username)

    def add_to_wishlist(self, username: str, calculator_id: int, notes: str | None = None) -> bool:
        return self.repository.add_to_wishlist(username, calculator_id, notes)

    def remove_from_wishlist(self, username: str, calculator_id: int) -> bool:
        return self.repository.remove_from_wishlist(username, calculator_id)

    def is_in_wishlist(self, username: str, calculator_id: int) -> bool:
        return self.repository.is_in_wishlist(username, calculator_id)

    def update_wishlist_notes(self, username: str, calculator_id: int, notes: str | None) -> bool:
        return self.repository.update_wishlist_notes(username, calculator_id, notes)
