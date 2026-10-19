from __future__ import annotations

import asyncio
import os
import sys
from contextlib import asynccontextmanager
from dataclasses import replace
from decimal import Decimal
from pathlib import Path
from typing import Any, AsyncIterator, Literal

from dotenv import load_dotenv
from fastapi import Depends, FastAPI, HTTPException, Query, Request, Response, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field, model_validator

if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

load_dotenv(Path(__file__).resolve().parents[3] / ".env", override=False)

from python_migration.calculator_collection.adapters.graph.neo4j_repository import (
    Neo4jGraphRepository,
)
from python_migration.calculator_collection.app import CalculatorCollectionApp
from python_migration.calculator_collection.domain import (
    Calculator,
    Manufacturer,
    UserAccount,
)
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
    password: str = Field(..., min_length=12, max_length=1024)
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

    @model_validator(mode="after")
    def validate_sales_period(self) -> ProposalPayload:
        if (
            self.sold_from is not None
            and self.sold_to is not None
            and self.sold_to < self.sold_from
        ):
            raise ValueError("The end of the sales period cannot precede its start.")
        return self


class ProposalStatusPayload(BaseModel):
    status: Literal["approved", "rejected"]


class CalculatorAdminPayload(BaseModel):
    model: str = Field(..., min_length=1, max_length=200)
    manufacturer_id: int | None = Field(default=None, ge=1)
    sold_from: int | None = None
    sold_to: int | None = None
    source_url: str = Field(default="", max_length=2000)
    raw_row_text: str = ""
    description: str = ""
    current_price: Decimal = Field(default=Decimal("0.00"), ge=0)
    price_currency: str = Field(default="EUR", min_length=3, max_length=3)


class ManufacturerPayload(BaseModel):
    name: str = Field(..., min_length=1, max_length=200)


class ManufacturerMergePayload(BaseModel):
    target_id: int = Field(..., ge=1)
    source_id: int = Field(..., ge=1)
    new_name: str | None = Field(default=None, max_length=200)


class AdminUserCreatePayload(BaseModel):
    username: str = Field(..., min_length=1, max_length=80)
    email: str = Field(..., min_length=3, max_length=254)
    password: str = Field(..., min_length=12, max_length=1024)
    role: Literal["USER", "ADMIN"] = "USER"
    enabled: bool = True


class AdminUserUpdatePayload(BaseModel):
    email: str = Field(..., min_length=3, max_length=254)
    role: Literal["USER", "ADMIN"]
    enabled: bool


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


def _calculator_from_payload(
    payload: CalculatorAdminPayload,
    manufacturer: Manufacturer | None,
) -> Calculator:
    if not payload.model.strip():
        raise HTTPException(status_code=400, detail="Calculator model is required.")
    if (
        payload.sold_from is not None
        and payload.sold_to is not None
        and payload.sold_to < payload.sold_from
    ):
        raise HTTPException(
            status_code=400,
            detail="The end of the sales period cannot precede its start.",
        )
    currency = payload.price_currency.strip().upper()
    if len(currency) != 3 or not currency.isascii() or not currency.isalpha():
        raise HTTPException(
            status_code=400,
            detail="Currency must be a three-letter code.",
        )
    return Calculator(
        model=payload.model.strip(),
        manufacturer=manufacturer,
        sold_from=payload.sold_from,
        sold_to=payload.sold_to,
        source_url=payload.source_url.strip(),
        raw_row_text=payload.raw_row_text,
        description=payload.description,
        current_price=payload.current_price,
        price_currency=currency,
    )


def _calculator_response(calculator: Calculator) -> dict[str, Any]:
    return {
        "id": calculator.id,
        "model": calculator.model,
        "manufacturer_id": (
            calculator.manufacturer.id if calculator.manufacturer else None
        ),
        "manufacturer": (
            calculator.manufacturer.name if calculator.manufacturer else None
        ),
        "sold_from": calculator.sold_from,
        "sold_to": calculator.sold_to,
        "source_url": calculator.source_url,
        "raw_row_text": calculator.raw_row_text,
        "description": calculator.description,
        "current_price": str(calculator.current_price),
        "price_currency": calculator.price_currency,
    }


def _user_response(account: UserAccount) -> dict[str, Any]:
    return {
        "id": account.id,
        "username": account.username,
        "email": account.email,
        "role": account.role,
        "enabled": account.enabled,
        "created_at": account.created_at.isoformat() if account.created_at else None,
        "last_login": account.last_login.isoformat() if account.last_login else None,
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
        allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE"],
        allow_headers=["Authorization", "Content-Type"],
    )

    def find_user(user_id: int) -> UserAccount | None:
        return next(
            (user for user in app.state.repository.get_all_users() if user.id == user_id),
            None,
        )

    def prevent_last_admin_removal(
        user: UserAccount, new_role: str, new_enabled: bool
    ) -> None:
        if user.role == "ADMIN" and user.enabled and (
            new_role != "ADMIN" or not new_enabled
        ):
            enabled_admins = [
                account
                for account in app.state.repository.get_all_users()
                if account.role == "ADMIN" and account.enabled
            ]
            if len(enabled_admins) <= 1:
                raise HTTPException(
                    status_code=409,
                    detail="The last enabled administrator cannot be removed.",
                )

    def get_admin_manufacturer(manufacturer_id: int | None) -> Manufacturer | None:
        if manufacturer_id is None:
            return None
        manufacturer = app.state.repository.get_manufacturer_by_id(manufacturer_id)
        if manufacturer is None:
            raise HTTPException(status_code=404, detail="Manufacturer not found.")
        return manufacturer

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
        if not payload.manufacturer_name.strip() or not payload.model.strip():
            raise HTTPException(
                status_code=400,
                detail="Proposal manufacturer and model are required.",
            )
        proposal = app.state.app.submit_proposal(
            manufacturer_name=payload.manufacturer_name,
            model=payload.model,
            source_url=payload.source_url,
            raw_row_text=payload.raw_row_text,
            sold_from=payload.sold_from,
            sold_to=payload.sold_to,
            created_by=account.username,
            created_by_id=account.id,
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
                if proposal["created_by_id"] == account.id
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
        account: UserAccount = Depends(require_admin),
    ) -> dict[str, Any]:
        existing_proposal = next(
            (
                item
                for item in app.state.repository.list_proposals()
                if item.id == proposal_id
            ),
            None,
        )
        if (
            payload.status == "approved"
            and existing_proposal is not None
            and existing_proposal.sold_from is not None
            and existing_proposal.sold_to is not None
            and existing_proposal.sold_to < existing_proposal.sold_from
        ):
            raise HTTPException(
                status_code=409,
                detail="Proposal has an invalid sales period and cannot be approved.",
            )
        proposal = app.state.repository.review_proposal(
            proposal_id, payload.status, account.username
        )
        if proposal is None:
            exists = any(
                item.id == proposal_id for item in app.state.repository.list_proposals()
            )
            raise HTTPException(
                status_code=409 if exists else 404,
                detail="Proposal has already been reviewed."
                if exists
                else "Proposal not found.",
            )
        return {
            "id": proposal.id,
            "status": proposal.status,
            "reviewed_by": proposal.reviewed_by,
            "created_calculator_id": proposal.created_calculator_id,
        }

    @app.post("/api/admin/calculators", status_code=status.HTTP_201_CREATED)
    def create_calculator(
        payload: CalculatorAdminPayload,
        _: UserAccount = Depends(require_admin),
    ) -> dict[str, Any]:
        manufacturer = get_admin_manufacturer(payload.manufacturer_id)
        calculator = app.state.repository.add_calculator(
            _calculator_from_payload(payload, manufacturer)
        )
        return _calculator_response(calculator)

    @app.get("/api/admin/calculators")
    def list_admin_calculators(
        search: str | None = Query(default=None),
        _: UserAccount = Depends(require_admin),
    ) -> list[dict[str, Any]]:
        return [
            _calculator_response(calculator)
            for calculator in app.state.repository.search_calculators(search)
        ]

    @app.put("/api/admin/calculators/{calculator_id}")
    def update_calculator(
        calculator_id: int,
        payload: CalculatorAdminPayload,
        _: UserAccount = Depends(require_admin),
    ) -> dict[str, Any]:
        manufacturer = get_admin_manufacturer(payload.manufacturer_id)
        calculator = _calculator_from_payload(payload, manufacturer)
        calculator.id = calculator_id
        updated = app.state.repository.update_calculator(calculator)
        if updated is None:
            raise HTTPException(status_code=404, detail="Calculator not found.")
        return _calculator_response(updated)

    @app.delete("/api/admin/calculators/{calculator_id}", status_code=204)
    def delete_calculator(
        calculator_id: int,
        _: UserAccount = Depends(require_admin),
    ) -> Response:
        if not app.state.repository.delete_calculator(calculator_id):
            raise HTTPException(status_code=404, detail="Calculator not found.")
        return Response(status_code=204)

    @app.put("/api/admin/manufacturers/{manufacturer_id}")
    def update_manufacturer(
        manufacturer_id: int,
        payload: ManufacturerPayload,
        _: UserAccount = Depends(require_admin),
    ) -> dict[str, Any]:
        name = payload.name.strip()
        if not name:
            raise HTTPException(status_code=400, detail="Manufacturer name is required.")
        manufacturer = app.state.repository.update_manufacturer(manufacturer_id, name)
        if manufacturer is None:
            raise HTTPException(status_code=404, detail="Manufacturer not found.")
        return {"id": manufacturer.id, "name": manufacturer.name}

    @app.post("/api/admin/manufacturers", status_code=status.HTTP_201_CREATED)
    def create_manufacturer(
        payload: ManufacturerPayload,
        _: UserAccount = Depends(require_admin),
    ) -> dict[str, Any]:
        name = payload.name.strip()
        if not name:
            raise HTTPException(status_code=400, detail="Manufacturer name is required.")
        existing = app.state.repository.search_manufacturers(name)
        if any(manufacturer.name.casefold() == name.casefold() for manufacturer in existing):
            raise HTTPException(status_code=409, detail="Manufacturer already exists.")
        manufacturer = app.state.repository.add_manufacturer(Manufacturer(name=name))
        return {"id": manufacturer.id, "name": manufacturer.name}

    @app.post("/api/admin/manufacturers/merge")
    def merge_manufacturers(
        payload: ManufacturerMergePayload,
        _: UserAccount = Depends(require_admin),
    ) -> dict[str, bool]:
        if payload.target_id == payload.source_id:
            raise HTTPException(status_code=400, detail="Choose two different manufacturers.")
        if payload.new_name is not None and not payload.new_name.strip():
            raise HTTPException(status_code=400, detail="New name cannot be blank.")
        merged = app.state.repository.merge_manufacturers(
            payload.target_id,
            payload.source_id,
            payload.new_name,
        )
        if not merged:
            raise HTTPException(status_code=404, detail="Manufacturer not found.")
        return {"merged": True}

    @app.get("/api/admin/users")
    def list_users(
        _: UserAccount = Depends(require_admin),
    ) -> list[dict[str, Any]]:
        return [_user_response(user) for user in app.state.repository.get_all_users()]

    @app.post("/api/admin/users", status_code=status.HTTP_201_CREATED)
    def create_user(
        payload: AdminUserCreatePayload,
        _: UserAccount = Depends(require_admin),
    ) -> dict[str, Any]:
        try:
            user = app.state.auth_service.create_managed_user(
                payload.username,
                payload.email,
                payload.password,
                payload.role,
                payload.enabled,
            )
        except ValueError as error:
            raise HTTPException(status_code=400, detail=str(error)) from error
        except AccountAlreadyExistsError as error:
            raise HTTPException(status_code=409, detail=str(error)) from error
        return _user_response(user)

    @app.put("/api/admin/users/{user_id}")
    def update_user(
        user_id: int,
        payload: AdminUserUpdatePayload,
        admin: UserAccount = Depends(require_admin),
    ) -> dict[str, Any]:
        user = find_user(user_id)
        if user is None:
            raise HTTPException(status_code=404, detail="User not found.")
        if user.id == admin.id:
            raise HTTPException(
                status_code=400,
                detail="You cannot change your own account.",
            )
        prevent_last_admin_removal(user, payload.role, payload.enabled)
        email = payload.email.strip().casefold()
        if not email or "@" not in email:
            raise HTTPException(status_code=400, detail="A valid email is required.")
        updated_user = replace(
            user,
            email=email,
            role=payload.role,
            enabled=payload.enabled,
        )
        if not app.state.repository.update_user(updated_user):
            raise HTTPException(
                status_code=409,
                detail=(
                    "Email is already in use, or the change would remove the "
                    "last enabled administrator."
                ),
            )
        return _user_response(updated_user)

    @app.delete("/api/admin/users/{user_id}", status_code=204)
    def delete_user(
        user_id: int,
        admin: UserAccount = Depends(require_admin),
    ) -> Response:
        user = find_user(user_id)
        if user is None:
            raise HTTPException(status_code=404, detail="User not found.")
        if user.id == admin.id:
            raise HTTPException(status_code=400, detail="You cannot delete your own account.")
        prevent_last_admin_removal(user, "USER", False)
        if not app.state.repository.delete_user(user_id):
            raise HTTPException(
                status_code=409,
                detail="User could not be deleted; the last enabled administrator must remain.",
            )
        return Response(status_code=204)

    return app


app = build_app()
