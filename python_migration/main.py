from __future__ import annotations

import sys
from decimal import Decimal
from pathlib import Path

if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from python_migration.calculator_collection.app import CalculatorCollectionApp
from python_migration.calculator_collection.domain import Calculator, Manufacturer


def bootstrap_demo_app() -> CalculatorCollectionApp:
    app = CalculatorCollectionApp()
    json_path = Path(__file__).resolve().parent.parent / "calculators.json"

    if json_path.exists() and not app.repository.get_all_calculators():
        loaded = app.repository.load_catalogue_from_json_file(json_path)
        if loaded:
            return app

    casio = Manufacturer(id=1, name="Casio")
    sharp = Manufacturer(id=2, name="Sharp")

    app.repository.add_manufacturer(casio)
    app.repository.add_manufacturer(sharp)
    app.repository.add_calculator(Calculator(id=1, model="fx-991", current_price=Decimal("89.95"), manufacturer=casio))
    app.repository.add_calculator(Calculator(id=2, model="EL-531", current_price=Decimal("55.00"), manufacturer=sharp))
    return app


if __name__ == "__main__":
    app = bootstrap_demo_app()
    print(app.search_calculators())
