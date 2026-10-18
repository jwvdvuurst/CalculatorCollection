from __future__ import annotations

from decimal import Decimal, InvalidOperation
from typing import Any, Iterable, Mapping

from ..domain import Calculator, Manufacturer, UserCalculatorCollection


def normalize_manufacturer_name(name: str | None) -> str:
    if name is None:
        return "Unknown"
    return " ".join(part.strip() for part in str(name).split()).title()


def normalize_model_name(model: str | None) -> str:
    if model is None:
        return ""
    return " ".join(part.strip() for part in str(model).split())


def parse_decimal_value(value: Any, default: Decimal = Decimal("0.00")) -> Decimal:
    if value is None or value == "":
        return default
    try:
        return Decimal(str(value))
    except (InvalidOperation, TypeError, ValueError):
        return default


def convert_java_calculator_to_python(
    raw: Mapping[str, Any],
    manufacturer: Manufacturer | None = None,
) -> Calculator:
    raw_manufacturer = raw.get("manufacturer") or raw.get("manufacturer_name") or "Unknown"
    manufacturer = manufacturer or Manufacturer(name=normalize_manufacturer_name(raw_manufacturer))

    calculator = Calculator(
        id=raw.get("id"),
        model=normalize_model_name(raw.get("model") or raw.get("name")),
        sold_from=raw.get("soldFrom"),
        sold_to=raw.get("soldTo"),
        source_url=raw.get("sourceUrl") or "",
        raw_row_text=raw.get("rawRowText") or "",
        description=raw.get("description") or "",
        enriched_data=raw.get("enrichedData") or {},
        current_price=parse_decimal_value(raw.get("currentPrice")),
        price_currency=str(raw.get("priceCurrency") or "EUR").upper(),
        manufacturer=manufacturer,
    )
    manufacturer.add_calculator(calculator)
    return calculator


def build_collection_summary(entries: Iterable[UserCalculatorCollection]) -> dict[str, Any]:
    grouped: dict[str, int] = {}
    total_value = Decimal("0.00")

    for entry in entries:
        if entry.calculator is None:
            continue
        manufacturer_name = entry.calculator.manufacturer.name if entry.calculator.manufacturer else "Unknown"
        grouped[manufacturer_name] = grouped.get(manufacturer_name, 0) + 1
        total_value += entry.calculator.current_price

    return {
        "total_items": sum(grouped.values()),
        "manufacturers": grouped,
        "total_value": str(total_value),
    }
