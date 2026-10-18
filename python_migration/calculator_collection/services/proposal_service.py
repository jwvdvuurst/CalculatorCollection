from __future__ import annotations

from ..repositories.protocols import ProposalRepository


class ProposalService:
    def __init__(self, repository: ProposalRepository):
        self.repository = repository

    def submit_proposal(self, manufacturer_name: str, model: str, source_url: str = "", raw_row_text: str = "",
                       sold_from: int | None = None, sold_to: int | None = None, created_by: str = "system"):
        proposal = self.repository.submit_proposal(
            manufacturer_name=manufacturer_name,
            model=model,
            source_url=source_url,
            raw_row_text=raw_row_text,
            sold_from=sold_from,
            sold_to=sold_to,
            created_by=created_by,
        )
        return {
            "id": proposal.id,
            "manufacturer_name": proposal.manufacturer_name,
            "model": proposal.model,
            "status": proposal.status,
            "source_url": proposal.source_url,
            "raw_row_text": proposal.raw_row_text,
            "created_by": proposal.created_by,
        }

    def list_proposals(self):
        return [
            {
                "id": proposal.id,
                "manufacturer_name": proposal.manufacturer_name,
                "model": proposal.model,
                "status": proposal.status,
                "source_url": proposal.source_url,
                "raw_row_text": proposal.raw_row_text,
                "created_by": proposal.created_by,
            }
            for proposal in self.repository.list_proposals()
        ]

    def approve_proposal(self, proposal_id: int):
        proposal = self.repository.set_proposal_status(proposal_id, "approved")
        if proposal is None:
            return None
        return {
            "id": proposal.id,
            "status": proposal.status,
            "model": proposal.model,
        }
