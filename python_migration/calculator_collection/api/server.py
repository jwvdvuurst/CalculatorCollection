from __future__ import annotations

import asyncio
import os
import sys
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Any, AsyncIterator, Literal

from dotenv import load_dotenv
from fastapi import Depends, FastAPI, HTTPException, Query, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field

if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

load_dotenv(Path(__file__).resolve().parents[3] / ".env", override=False)

from python_migration.calculator_collection.adapters.graph.neo4j_repository import (
    Neo4jGraphRepository,
)
from python_migration.calculator_collection.app import CalculatorCollectionApp
from python_migration.calculator_collection.domain import UserAccount
from python_migration.calculator_collection.repositories.graph_repository import (
    GraphRepository,
)
from python_migration.calculator_collection.repositories.protocols import (
    CalculatorCollectionRepository,
)
from python_migration.calculator_collection.services.auth_service import (
    AccountAlreadyExistsError,
    AuthService,
    InvalidCredentialsError,
)


class CollectionPayload(BaseModel):
    calculator_id: int = Field(..., ge=1)
    notes: str | None = None


class RegistrationPayload(BaseModel):
    username: str = Field(..., min_length=1, max_length=80)
    email: str = Field(..., min_length=3, max_length=254)
    password: str = Field(..., min_length=4, max_length=1024)
    confirm_password: str | None = Field(default=None, max_length=1024)


class LoginPayload(BaseModel):
    username: str = Field(..., min_length=1, max_length=80)
    password: str = Field(..., min_length=1, max_length=1024)


class ProposalPayload(BaseModel):
    manufacturer_name: str = Field(..., min_length=1)
    model: str = Field(..., min_length=1)
    source_url: str = ""
    raw_row_text: str = ""
    sold_from: int | None = None
    sold_to: int | None = None


class ProposalStatusPayload(BaseModel):
    status: Literal["approved", "rejected"]


bearer_scheme = HTTPBearer(auto_error=False)


def get_current_user(
    request: Request,
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
) -> UserAccount:
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required",
            headers={"WWW-Authenticate": "Bearer"},
        )

    account = request.app.state.auth_service.get_account_from_token(
        credentials.credentials
    )
    if account is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired access token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return account


def require_admin(
    current_user: UserAccount = Depends(get_current_user),
) -> UserAccount:
    if current_user.role != "ADMIN":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Administrator role required",
        )
    return current_user


def require_owner(username: str, current_user: UserAccount) -> None:
    if username != current_user.username:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You may only access your own collection and wishlist.",
        )


def _auth_response(
    account: UserAccount, token: str, expires_in: int
) -> dict[str, Any]:
    return {
        "access_token": token,
        "token_type": "bearer",
        "expires_in": expires_in,
        "user": {
            "username": account.username,
            "email": account.email,
            "role": account.role,
        },
    }


def _load_catalogue() -> GraphRepository:
    repository = GraphRepository()
    project_root = Path(__file__).resolve().parents[3]
    json_path = project_root / "calculators.json"

    loaded = repository.load_catalogue_from_json_file(json_path)
    if loaded == 0:
        raise RuntimeError(f"Calculator catalogue is missing or empty: {json_path}")

    return repository


def _create_repository() -> CalculatorCollectionRepository:
    backend = os.getenv("CALCOL_GRAPH_BACKEND", "neo4j").strip().lower()
    if backend == "neo4j":
        return Neo4jGraphRepository.from_environment()
    if backend == "memory":
        return GraphRepository()
    raise ValueError(f"Unsupported CALCOL_GRAPH_BACKEND: {backend}")


async def _prepare_repository(repository: CalculatorCollectionRepository) -> None:
    if isinstance(repository, Neo4jGraphRepository):
        await asyncio.to_thread(repository.initialize)
    catalogue_seeded = await asyncio.to_thread(repository.is_catalogue_seeded)
    if not catalogue_seeded:
        catalogue = await asyncio.to_thread(_load_catalogue)
        expected_count = catalogue.get_calculator_count()
        seeded_count = await asyncio.to_thread(
            repository.seed_catalogue, catalogue.get_all_calculators()
        )
        actual_count = await asyncio.to_thread(repository.get_calculator_count)
        if seeded_count != expected_count or actual_count < expected_count:
            raise RuntimeError(
                "Catalogue bootstrap count mismatch: "
                f"source={expected_count}, submitted={seeded_count}, stored={actual_count}"
            )


def build_app(
    repository: CalculatorCollectionRepository | None = None,
    auth_service: AuthService | None = None,
) -> FastAPI:
    @asynccontextmanager
    async def lifespan(application: FastAPI) -> AsyncIterator[None]:
        active_repository = repository or _create_repository()
        application.state.repository = active_repository
        try:
            active_auth_service = auth_service
            if active_auth_service is None:
                active_auth_service = AuthService.from_environment(active_repository)
            await _prepare_repository(active_repository)
            if auth_service is None:
                await asyncio.to_thread(
                    active_auth_service.bootstrap_admin_from_environment
                )
            application.state.auth_service = active_auth_service
            application.state.app = CalculatorCollectionApp(active_repository)
            yield
        finally:
            await asyncio.to_thread(active_repository.close)

    app = FastAPI(
        title="Calculator Collection API",
        version="0.1.0",
        lifespan=lifespan,
    )
    allowed_origins = [
        origin.strip()
        for origin in os.getenv(
            "CALCOL_CORS_ORIGINS",
            "http://localhost:5173,http://127.0.0.1:5173",
        ).split(",")
        if origin.strip()
    ]
    app.add_middleware(
        CORSMiddleware,
        allow_origins=allowed_origins,
        allow_methods=["GET", "POST", "PATCH"],
        allow_headers=["Authorization", "Content-Type"],
    )

    @app.get("/api/health")
    def health() -> dict[str, Any]:
        repository = app.state.repository
        return {
            "status": "ok",
            "total_calculators": repository.get_calculator_count(),
            "total_manufacturers": repository.get_manufacturer_count(),
        }

    @app.post("/api/auth/register", status_code=status.HTTP_201_CREATED)
    def register(payload: RegistrationPayload) -> dict[str, Any]:
        if (
            payload.confirm_password is not None
            and payload.password != payload.confirm_password
        ):
            raise HTTPException(status_code=400, detail="Passwords do not match.")
        try:
            account, token = app.state.auth_service.register(
                payload.username, payload.email, payload.password
            )
        except ValueError as error:
            raise HTTPException(status_code=400, detail=str(error)) from error
        except AccountAlreadyExistsError as error:
            raise HTTPException(status_code=409, detail=str(error)) from error
        return _auth_response(account, token, app.state.auth_service.expires_in)

    @app.post("/api/auth/login")
    def login(payload: LoginPayload) -> dict[str, Any]:
        try:
            account, token = app.state.auth_service.login(
                payload.username, payload.password
            )
        except InvalidCredentialsError as error:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=str(error),
                headers={"WWW-Authenticate": "Bearer"},
            ) from error
        return _auth_response(account, token, app.state.auth_service.expires_in)

    @app.get("/api/auth/me")
    def current_user(
        account: UserAccount = Depends(get_current_user),
    ) -> dict[str, str]:
        return {
            "username": account.username,
            "email": account.email,
            "role": account.role,
        }

    @app.get("/api/calculators")
    def list_calculators(
        search: str | None = Query(default=None),
        manufacturer_id: int | None = Query(default=None),
        min_price: str | None = Query(default=None),
        max_price: str | None = Query(default=None),
        sort: str | None = Query(default=None),
        _: UserAccount = Depends(get_current_user),
    ) -> list[dict[str, Any]]:
        return app.state.app.search_calculators(search, manufacturer_id, min_price, max_price, sort)

    @app.get("/api/calculators/{calculator_id}")
    def get_calculator(
        calculator_id: int,
        _: UserAccount = Depends(get_current_user),
    ) -> dict[str, Any]:
        item = app.state.app.repository.get_calculator_by_id(calculator_id)
        if item is None:
            raise HTTPException(status_code=404, detail="Calculator not found")
        return {
            "id": item.id,
            "model": item.model,
            "manufacturer": item.manufacturer.name if item.manufacturer else None,
            "description": item.description,
            "source_url": item.source_url,
            "current_price": str(item.current_price),
            "price_currency": item.price_currency,
        }

    @app.get("/api/manufacturers")
    def list_manufacturers(
        search: str | None = Query(default=None),
        sort: str | None = Query(default=None),
        _: UserAccount = Depends(get_current_user),
    ) -> list[dict[str, Any]]:
        manufacturers = app.state.app.list_manufacturers(search, sort)
        return [
            {
                "id": manufacturer.id,
                "name": manufacturer.name,
                "calculator_count": len(getattr(manufacturer, "calculators", [])),
            }
            if not isinstance(manufacturer, dict)
            else manufacturer
            for manufacturer in manufacturers
        ]

    @app.get("/api/collection/{username}")
    def get_collection(
        username: str,
        account: UserAccount = Depends(get_current_user),
    ) -> list[dict[str, Any]]:
        require_owner(username, account)
        return app.state.app.get_user_collection(username)

    @app.post("/api/collection/{username}/add")
    def add_to_collection(
        username: str,
        payload: CollectionPayload,
        account: UserAccount = Depends(get_current_user),
    ) -> dict[str, Any]:
        require_owner(username, account)
        success = app.state.app.add_to_collection(username, payload.calculator_id, payload.notes)
        if not success:
            raise HTTPException(status_code=404, detail="Calculator not found or already in collection")
        return {
            "success": True,
            "username": username,
            "calculator_id": payload.calculator_id,
            "notes": payload.notes,
        }

    @app.get("/api/wishlist/{username}")
    def get_wishlist(
        username: str,
        account: UserAccount = Depends(get_current_user),
    ) -> list[dict[str, Any]]:
        require_owner(username, account)
        items = app.state.app.get_wishlist(username)
        return [
            {
                "username": item.username,
                "calculator_id": item.calculator.id if item.calculator else None,
                "model": item.calculator.model if item.calculator else None,
                "manufacturer": item.calculator.manufacturer.name if item.calculator and item.calculator.manufacturer else None,
                "notes": item.notes,
                "added_at": item.added_at.isoformat() if item.added_at else None,
            }
            for item in items
        ]

    @app.post("/api/wishlist/{username}/add")
    def add_to_wishlist(
        username: str,
        payload: CollectionPayload,
        account: UserAccount = Depends(get_current_user),
    ) -> dict[str, Any]:
        require_owner(username, account)
        success = app.state.app.add_to_wishlist(username, payload.calculator_id, payload.notes)
        if not success:
            raise HTTPException(status_code=404, detail="Calculator not found or already in wishlist")
        return {
            "success": True,
            "username": username,
            "calculator_id": payload.calculator_id,
            "notes": payload.notes,
        }

    @app.post("/api/proposals")
    def submit_proposal(
        payload: ProposalPayload,
        account: UserAccount = Depends(get_current_user),
    ) -> dict[str, Any]:
        proposal = app.state.app.submit_proposal(
            manufacturer_name=payload.manufacturer_name,
            model=payload.model,
            source_url=payload.source_url,
            raw_row_text=payload.raw_row_text,
            sold_from=payload.sold_from,
            sold_to=payload.sold_to,
            created_by=account.username,
        )
        return {
            "id": proposal["id"],
            "manufacturer_name": proposal["manufacturer_name"],
            "model": proposal["model"],
            "status": proposal["status"],
            "created_by": proposal["created_by"],
        }

    @app.get("/api/proposals")
    def list_proposals(
        account: UserAccount = Depends(get_current_user),
    ) -> list[dict[str, Any]]:
        proposals = app.state.app.list_proposals()
        if account.role != "ADMIN":
            proposals = [
                proposal
                for proposal in proposals
                if proposal["created_by"] == account.username
            ]
        return [
            {
                "id": proposal["id"],
                "manufacturer_name": proposal["manufacturer_name"],
                "model": proposal["model"],
                "status": proposal["status"],
                "source_url": proposal["source_url"],
                "created_by": proposal["created_by"],
            }
            for proposal in proposals
        ]

    @app.patch("/api/admin/proposals/{proposal_id}")
    def update_proposal_status(
        proposal_id: int,
        payload: ProposalStatusPayload,
        _: UserAccount = Depends(require_admin),
    ) -> dict[str, Any]:
        proposal = app.state.repository.set_proposal_status(
            proposal_id, payload.status
        )
        if proposal is None:
            raise HTTPException(status_code=404, detail="Proposal not found")
        return {"id": proposal.id, "status": proposal.status}

    return app


app = build_app()
