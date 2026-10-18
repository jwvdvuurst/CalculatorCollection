from __future__ import annotations

from decimal import Decimal
from typing import Iterable, Sequence

from ..domain import Calculator, Manufacturer, UserCalculatorCollection
from ..repositories.protocols import CalculatorCollectionRepository


class CalculatorService:
    def __init__(self, repository: CalculatorCollectionRepository):
        self.repository = repository

    def get_all_calculators(self):
        return self.repository.get_all_calculators()

    def search_calculators(self, search: str | None, min_price: Decimal | None = None, max_price: Decimal | None = None, sort: str | None = None):
        results = self.repository.search_calculators(search, min_price, max_price)
        return self._apply_sort(results, sort)

    def get_calculators_by_manufacturer(self, manufacturer_id: int, min_price: Decimal | None = None, max_price: Decimal | None = None, sort: str | None = None):
        results = self.repository.get_calculators_by_manufacturer(manufacturer_id, min_price, max_price)
        return self._apply_sort(results, sort)

    def get_all_manufacturers(self, search: str | None = None, sort: str | None = None):
        results = self.repository.search_manufacturers(search)
        return self._apply_manufacturer_sort(results, sort)

    def merge_manufacturers(self, target_manufacturer_id: int, source_manufacturer_id: int, new_name: str | None = None) -> bool:
        return self.repository.merge_manufacturers(target_manufacturer_id, source_manufacturer_id, new_name)

    def update_manufacturer(self, manufacturer_id: int, new_name: str) -> Manufacturer | None:
        return self.repository.update_manufacturer(manufacturer_id, new_name)

    def get_manufacturer_by_id(self, manufacturer_id: int) -> Manufacturer | None:
        return self.repository.get_manufacturer_by_id(manufacturer_id)

    def get_user_collection(self, username: str):
        return self.repository.get_user_collection(username)

    def get_user_collection_count(self, username: str) -> int:
        return self.repository.get_user_collection_count(username)

    def add_to_collection(self, username: str, calculator_id: int, notes: str | None = None) -> bool:
        return self.repository.add_to_collection(username, calculator_id, notes)

    def remove_from_collection(self, username: str, calculator_id: int) -> bool:
        return self.repository.remove_from_collection(username, calculator_id)

    def is_in_collection(self, username: str, calculator_id: int) -> bool:
        return self.repository.is_in_collection(username, calculator_id)

    def update_collection_notes(self, username: str, calculator_id: int, notes: str | None) -> bool:
        return self.repository.update_collection_notes(username, calculator_id, notes)

    def _apply_sort(self, items: Sequence[Calculator], sort: str | None):
        if sort is None:
            return list(items)

        if sort == "price_asc":
            return sorted(items, key=lambda calc: (calc.current_price, calc.id or 0))
        if sort == "price_desc":
            return sorted(items, key=lambda calc: (calc.current_price, calc.id or 0), reverse=True)
        if sort == "model_asc":
            return sorted(items, key=lambda calc: (calc.model.lower(), calc.id or 0))
        if sort == "model_desc":
            return sorted(items, key=lambda calc: (calc.model.lower(), calc.id or 0), reverse=True)
        if sort == "manufacturer_asc":
            return sorted(items, key=lambda calc: ((calc.manufacturer.name if calc.manufacturer else "").lower(), calc.id or 0))
        if sort == "manufacturer_desc":
            return sorted(items, key=lambda calc: ((calc.manufacturer.name if calc.manufacturer else "").lower(), calc.id or 0), reverse=True)
        if sort == "year_asc":
            return sorted(items, key=lambda calc: (calc.sold_from or 0, calc.id or 0))
        if sort == "year_desc":
            return sorted(items, key=lambda calc: (calc.sold_from or 0, calc.id or 0), reverse=True)
        return list(items)

    def _apply_manufacturer_sort(self, items: Sequence[Manufacturer], sort: str | None):
        if sort is None:
            return list(items)

        if sort == "name_asc":
            return sorted(items, key=lambda m: m.name.lower())
        if sort == "name_desc":
            return sorted(items, key=lambda m: m.name.lower(), reverse=True)
        if sort == "count_desc":
            return sorted(items, key=lambda m: len(m.calculators), reverse=True)
        if sort == "count_asc":
            return sorted(items, key=lambda m: len(m.calculators))
        return list(items)
