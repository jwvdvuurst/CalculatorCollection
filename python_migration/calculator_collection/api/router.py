from __future__ import annotations

from decimal import Decimal
from typing import Any

from ..services.calculator_service import CalculatorService


class CalculatorCollectionRouter:
    def __init__(self, service: CalculatorService):
        self.service = service

    def list_calculators(self, search: str | None = None, manufacturer_id: int | None = None,
                         min_price: str | None = None, max_price: str | None = None,
                         sort: str | None = None) -> list[dict[str, Any]]:
        min_price_dec = Decimal(min_price) if min_price else None
        max_price_dec = Decimal(max_price) if max_price else None

        if manufacturer_id is not None:
            calculators = self.service.get_calculators_by_manufacturer(
                manufacturer_id, min_price_dec, max_price_dec, sort
            )
        else:
            calculators = self.service.search_calculators(search, min_price_dec, max_price_dec, sort)

        return [
            {
                "id": calc.id,
                "model": calc.model,
                "manufacturer": calc.manufacturer.name if calc.manufacturer else None,
                "current_price": str(calc.current_price),
                "price_currency": calc.price_currency,
            }
            for calc in calculators
        ]

    def get_calculator(self, calculator_id: int) -> dict[str, Any] | None:
        calc = self.service.repository.get_calculator_by_id(calculator_id)
        if calc is None:
            return None
        return {
            "id": calc.id,
            "model": calc.model,
            "manufacturer": calc.manufacturer.name if calc.manufacturer else None,
            "description": calc.description,
            "source_url": calc.source_url,
            "current_price": str(calc.current_price),
            "price_currency": calc.price_currency,
        }

    def list_manufacturers(self, search: str | None = None, sort: str | None = None) -> list[dict[str, Any]]:
        manufacturers = self.service.get_all_manufacturers(search, sort)
        return [
            {
                "id": manufacturer.id,
                "name": manufacturer.name,
                "calculator_count": len(manufacturer.calculators),
            }
            for manufacturer in manufacturers
        ]

    def add_to_collection(self, username: str, calculator_id: int, notes: str | None = None) -> bool:
        return self.service.add_to_collection(username, calculator_id, notes)

    def remove_from_collection(self, username: str, calculator_id: int) -> bool:
        return self.service.remove_from_collection(username, calculator_id)

    def get_collection(self, username: str) -> list[dict[str, Any]]:
        collection = self.service.get_user_collection(username)
        return [
            {
                "username": item.username,
                "calculator_id": item.calculator.id if item.calculator else None,
                "model": item.calculator.model if item.calculator else None,
                "notes": item.notes,
            }
            for item in collection
        ]
