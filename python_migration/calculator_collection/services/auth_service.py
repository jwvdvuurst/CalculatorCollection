from __future__ import annotations

import os
from datetime import datetime, timedelta, timezone

import jwt
from argon2 import PasswordHasher
from argon2.exceptions import InvalidHashError, VerifyMismatchError

from ..domain import UserAccount
from ..repositories.protocols import UserRepository


class InvalidCredentialsError(Exception):
    pass


class AccountAlreadyExistsError(Exception):
    pass


MINIMUM_PASSWORD_LENGTH = 12


class AuthService:
    def __init__(
        self,
        repository: UserRepository,
        token_secret: str,
        token_lifetime: timedelta = timedelta(minutes=30),
    ) -> None:
        if len(token_secret.encode("utf-8")) < 32:
            raise ValueError("CALCOL_JWT_SECRET must contain at least 32 bytes.")
        if token_lifetime.total_seconds() <= 0:
            raise ValueError("Access-token lifetime must be positive.")
        self._repository = repository
        self._token_secret = token_secret
        self._token_lifetime = token_lifetime
        self._password_hasher = PasswordHasher()
        self._dummy_password_hash = self._password_hasher.hash(
            "unused-account-password"
        )

    @property
    def expires_in(self) -> int:
        return int(self._token_lifetime.total_seconds())

    @classmethod
    def from_environment(cls, repository: UserRepository) -> AuthService:
        token_secret = os.getenv("CALCOL_JWT_SECRET")
        if not token_secret:
            raise RuntimeError("CALCOL_JWT_SECRET must be set before starting the API.")
        if len(token_secret.encode("utf-8")) < 32:
            raise RuntimeError(
                "CALCOL_JWT_SECRET must contain at least 32 bytes. "
                "Use a randomly generated secret in the repository-root .env "
                "or in the API process environment."
            )
        return cls(repository, token_secret)

    def register(self, username: str, email: str, password: str) -> tuple[UserAccount, str]:
        normalized_username = username.strip()
        normalized_email = email.strip().casefold()
        if not normalized_username:
            raise ValueError("Username is required.")
        if not normalized_email or "@" not in normalized_email:
            raise ValueError("A valid email is required.")
        if len(password) < MINIMUM_PASSWORD_LENGTH:
            raise ValueError(
                f"Password must be at least {MINIMUM_PASSWORD_LENGTH} characters long."
            )

        account = UserAccount(
            username=normalized_username,
            email=normalized_email,
            password_hash=self._password_hasher.hash(password),
        )
        if not self._repository.create_user(account):
            raise AccountAlreadyExistsError("Username or email is already registered.")
        return account, self._create_token(account)

    def login(self, username: str, password: str) -> tuple[UserAccount, str]:
        account = self._repository.get_user_by_username(username.strip())
        password_hash = account.password_hash if account else self._dummy_password_hash
        try:
            valid_password = self._password_hasher.verify(password_hash, password)
        except (InvalidHashError, VerifyMismatchError):
            valid_password = False
        if account is None or not account.enabled or not valid_password:
            raise InvalidCredentialsError("Invalid username or password.")
        self._repository.update_last_login(account.username)
        account.last_login = datetime.now(timezone.utc)
        return account, self._create_token(account)

    def create_managed_user(
        self,
        username: str,
        email: str,
        password: str,
        role: str,
        enabled: bool = True,
    ) -> UserAccount:
        normalized_username = username.strip()
        normalized_email = email.strip().casefold()
        normalized_role = role.strip().upper()
        if not normalized_username:
            raise ValueError("Username is required.")
        if not normalized_email or "@" not in normalized_email:
            raise ValueError("A valid email is required.")
        if len(password) < MINIMUM_PASSWORD_LENGTH:
            raise ValueError(
                f"Password must be at least {MINIMUM_PASSWORD_LENGTH} characters long."
            )
        if normalized_role not in {"USER", "ADMIN"}:
            raise ValueError("Role must be USER or ADMIN.")
        account = UserAccount(
            username=normalized_username,
            email=normalized_email,
            password_hash=self._password_hasher.hash(password),
            role=normalized_role,
            enabled=enabled,
        )
        if not self._repository.create_user(account):
            raise AccountAlreadyExistsError("Username or email is already registered.")
        return account

    def get_account_from_token(self, token: str) -> UserAccount | None:
        try:
            claims = jwt.decode(
                token,
                self._token_secret,
                algorithms=["HS256"],
                options={"require": ["exp", "iat", "sub", "token_type"]},
            )
        except jwt.InvalidTokenError:
            return None

        if claims.get("token_type") != "access":
            return None
        subject = claims.get("sub")
        if (
            claims.get("identity_version") != 2
            or not isinstance(subject, str)
            or not subject.isascii()
            or not subject.isdecimal()
        ):
            return None

        account = self._repository.get_user_by_id(int(subject))
        if account is None or not account.enabled:
            return None
        return account

    def bootstrap_admin_from_environment(self) -> None:
        password = os.getenv("CALCOL_ADMIN_PASSWORD")
        email = os.getenv("CALCOL_ADMIN_EMAIL")
        username = os.getenv("CALCOL_ADMIN_USERNAME", "admin").strip()
        if not password and not email and not os.getenv("CALCOL_ADMIN_USERNAME"):
            return
        if not password or not email or not username:
            raise RuntimeError(
                "Bootstrap administrator requires CALCOL_ADMIN_USERNAME, "
                "CALCOL_ADMIN_EMAIL, and CALCOL_ADMIN_PASSWORD."
            )
        if len(password) < 12 or len(password.encode("utf-8")) > 72:
            raise RuntimeError(
                "Bootstrap administrator password must be at least 12 characters "
                "and no more than 72 UTF-8 bytes."
            )
        normalized_email = email.strip().casefold()
        if "@" not in normalized_email:
            raise RuntimeError("Bootstrap administrator email must be valid.")

        existing = self._repository.get_user_by_username(username)
        if existing is not None:
            if existing.role != "ADMIN":
                raise RuntimeError(
                    "Configured bootstrap administrator already exists without "
                    "the ADMIN role."
                )
            return

        account = UserAccount(
            username=username,
            email=normalized_email,
            password_hash=self._password_hasher.hash(password),
            role="ADMIN",
        )
        if not self._repository.create_user(account):
            raise RuntimeError(
                "Could not create bootstrap administrator; the username or email "
                "is already in use."
            )

    def _create_token(self, account: UserAccount) -> str:
        if account.id is None:
            raise RuntimeError("Cannot issue an access token without a persistent user ID.")
        issued_at = datetime.now(timezone.utc)
        return jwt.encode(
            {
                "sub": str(account.id),
                "iat": issued_at,
                "exp": issued_at + self._token_lifetime,
                "token_type": "access",
                "identity_version": 2,
            },
            self._token_secret,
            algorithm="HS256",
        )
