from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass
class GraphDatabaseAdapter:
    """Graph-oriented persistence adapter that behaves like a lightweight Neo4j facade.

    This sits behind the Python business layer and keeps the application ready to swap
    to real graph infrastructure (Neo4j, Cosmos DB, Neptune, etc.) without changing the
    service contracts.
    """

    nodes: dict[str, dict[str, Any]] = field(default_factory=dict)
    edges: list[dict[str, Any]] = field(default_factory=list)

    def add_node(self, label: str, key: str, properties: dict[str, Any] | None = None) -> dict[str, Any]:
        node = {"label": label, "key": key, "properties": properties or {}}
        self.nodes[f"{label}:{key}"] = node
        return node

    def add_relationship(self, source_key: str, relationship: str, target_key: str, properties: dict[str, Any] | None = None) -> dict[str, Any]:
        edge = {
            "source": source_key,
            "relationship": relationship,
            "target": target_key,
            "properties": properties or {},
        }
        self.edges.append(edge)
        return edge

    def get_node(self, label: str, key: str) -> dict[str, Any] | None:
        return self.nodes.get(f"{label}:{key}")

    def find_nodes(self, label: str, **filters: Any) -> list[dict[str, Any]]:
        matches: list[dict[str, Any]] = []
        for node in self.nodes.values():
            if node["label"] != label:
                continue
            props = node["properties"]
            if all(props.get(name) == value for name, value in filters.items()):
                matches.append(node)
        return matches

    def merge_calculator(self, calculator: Any) -> dict[str, Any]:
        manufacturer = calculator.manufacturer
        manufacturer_key = f"manufacturer:{manufacturer.name if manufacturer else 'unknown'}"
        self.add_node("Manufacturer", manufacturer_key.split(":", 1)[1], {"name": manufacturer.name if manufacturer else "Unknown"})
        self.add_node("Calculator", str(calculator.id), {
            "model": calculator.model,
            "sold_from": calculator.sold_from,
            "sold_to": calculator.sold_to,
            "current_price": str(calculator.current_price),
            "price_currency": calculator.price_currency,
            "source_url": calculator.source_url,
            "description": calculator.description,
        })
        self.add_relationship(f"manufacturer:{manufacturer.name if manufacturer else 'unknown'}", "MANUFACTURES", str(calculator.id))
        return self.get_node("Calculator", str(calculator.id)) or {}

    def merge_collection(self, username: str, calculator_id: int, notes: str | None = None) -> dict[str, Any]:
        node_key = f"{username}:{calculator_id}"
        self.add_node("CollectionItem", node_key, {"username": username, "calculator_id": calculator_id, "notes": notes or ""})
        self.add_relationship(f"user:{username}", "OWNS", str(calculator_id))
        return self.get_node("CollectionItem", node_key) or {}
