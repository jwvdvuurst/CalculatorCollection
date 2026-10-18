import unittest
from decimal import Decimal

from python_migration.calculator_collection.domain import Calculator, Manufacturer, UserCalculatorCollection
from python_migration.calculator_collection.lambdas.converters import (
    build_collection_summary,
    convert_java_calculator_to_python,
    normalize_manufacturer_name,
    normalize_model_name,
    parse_decimal_value,
)


class ConversionTests(unittest.TestCase):
    def test_normalize_manufacturer_name(self):
        self.assertEqual(normalize_manufacturer_name("  casio   "), "Casio")
        self.assertEqual(normalize_manufacturer_name(None), "Unknown")

    def test_normalize_model_name(self):
        self.assertEqual(normalize_model_name("  fx-991  ex "), "fx-991 ex")
        self.assertEqual(normalize_model_name(None), "")

    def test_parse_decimal_value(self):
        self.assertEqual(parse_decimal_value("125.50"), Decimal("125.50"))
        self.assertEqual(parse_decimal_value("bad"), Decimal("0.00"))

    def test_convert_java_calculator_to_python(self):
        record = {
            "id": 42,
            "model": "  fx-82  " ,
            "soldFrom": 1985,
            "soldTo": 1998,
            "sourceUrl": "https://example.test",
            "currentPrice": "89.95",
            "priceCurrency": "eur",
            "manufacturer": "casio",
        }

        calculator = convert_java_calculator_to_python(record)

        self.assertEqual(calculator.model, "fx-82")
        self.assertEqual(calculator.manufacturer.name, "Casio")
        self.assertEqual(calculator.current_price, Decimal("89.95"))
        self.assertEqual(calculator.price_currency, "EUR")

    def test_build_collection_summary(self):
        casio = Manufacturer(name="Casio")
        sharp = Manufacturer(name="Sharp")
        calc1 = Calculator(model="fx-82", current_price=Decimal("89.95"), manufacturer=casio)
        calc2 = Calculator(model="EL-531", current_price=Decimal("55.00"), manufacturer=sharp)

        entries = [
            UserCalculatorCollection(username="alice", calculator=calc1),
            UserCalculatorCollection(username="alice", calculator=calc2),
        ]

        summary = build_collection_summary(entries)
        self.assertEqual(summary["total_items"], 2)
        self.assertEqual(summary["total_value"], "144.95")
        self.assertEqual(summary["manufacturers"]["Casio"], 1)


if __name__ == "__main__":
    unittest.main()
