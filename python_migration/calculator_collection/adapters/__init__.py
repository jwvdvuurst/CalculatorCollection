"""Adapter layer for external systems and persistence backends."""

from .graph.neo4j_adapter import GraphDatabaseAdapter

__all__ = ["GraphDatabaseAdapter"]
