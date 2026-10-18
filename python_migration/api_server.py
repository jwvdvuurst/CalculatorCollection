from __future__ import annotations

import sys
from pathlib import Path

if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import uvicorn

from python_migration.calculator_collection.api.server import app

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8001)
