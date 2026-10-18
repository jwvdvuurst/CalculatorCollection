from __future__ import annotations

from ..repositories.protocols import ManufacturerRepository


class ManufacturerService:
    def __init__(self, repository: ManufacturerRepository):
        self.repository = repository

    def get_all_manufacturers(self, search: str | None = None, sort: str | None = None):
        manufacturers = self.repository.search_manufacturers(search)
        if sort == "name_asc":
            return sorted(manufacturers, key=lambda m: m.name.lower())
        if sort == "name_desc":
            return sorted(manufacturers, key=lambda m: m.name.lower(), reverse=True)
        if sort == "count_desc":
            return sorted(manufacturers, key=lambda m: len(m.calculators), reverse=True)
        if sort == "count_asc":
            return sorted(manufacturers, key=lambda m: len(m.calculators))
        return manufacturers

    def search_manufacturers(self, search: str | None = None):
        return self.repository.search_manufacturers(search)

    def merge_manufacturers(self, target_id: int, source_id: int, new_name: str | None = None) -> bool:
        return self.repository.merge_manufacturers(target_id, source_id, new_name)

    def update_manufacturer(self, manufacturer_id: int, new_name: str):
        return self.repository.update_manufacturer(manufacturer_id, new_name)
