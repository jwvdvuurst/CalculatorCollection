from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from typing import Any, Optional


@dataclass
class Manufacturer:
    id: Optional[int] = None
    name: str = ""
    calculators: list["Calculator"] = field(default_factory=list)

    def add_calculator(self, calculator: "Calculator") -> None:
        if calculator not in self.calculators:
            self.calculators.append(calculator)
            calculator.manufacturer = self


@dataclass
class Calculator:
    id: Optional[int] = None
    model: str = ""
    sold_from: Optional[int] = None
    sold_to: Optional[int] = None
    source_url: str = ""
    raw_row_text: str = ""
    description: str = ""
    enriched_data: dict[str, Any] = field(default_factory=dict)
    current_price: Decimal = Decimal("0.00")
    price_currency: str = "EUR"
    price_last_updated: Optional[datetime] = None
    manufacturer: Optional[Manufacturer] = None

    def to_graph_record(self) -> dict[str, Any]:
        return {
            "labels": ["Calculator"],
            "properties": {
                "id": self.id,
                "model": self.model,
                "sold_from": self.sold_from,
                "sold_to": self.sold_to,
                "source_url": self.source_url,
                "description": self.description,
                "current_price": str(self.current_price),
                "price_currency": self.price_currency,
                "manufacturer_id": self.manufacturer.id if self.manufacturer else None,
                "manufacturer_name": self.manufacturer.name if self.manufacturer else None,
            },
        }


@dataclass
class UserCalculatorCollection:
    id: Optional[int] = None
    username: str = ""
    calculator: Optional[Calculator] = None
    added_at: Optional[datetime] = None
    notes: str = ""

    def __post_init__(self) -> None:
        if self.added_at is None:
            self.added_at = datetime.utcnow()


@dataclass
class UserAccount:
    username: str
    email: str
    password_hash: str = field(repr=False)
    role: str = "USER"
    enabled: bool = True


@dataclass
class Proposal:
    id: Optional[int] = None
    manufacturer_name: str = ""
    model: str = ""
    sold_from: Optional[int] = None
    sold_to: Optional[int] = None
    source_url: str = ""
    raw_row_text: str = ""
    status: str = "pending"
    created_by: str = "system"
    created_at: Optional[datetime] = None

    def __post_init__(self) -> None:
        if self.created_at is None:
            self.created_at = datetime.utcnow()


@dataclass
class Label:
    id: Optional[int] = None
    name: str = ""
    description: str = ""
