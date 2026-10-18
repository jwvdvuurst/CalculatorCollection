import time
import unittest
from datetime import timedelta
from unittest.mock import patch

from python_migration.calculator_collection.repositories.graph_repository import (
    GraphRepository,
)
from python_migration.calculator_collection.services.auth_service import (
    AccountAlreadyExistsError,
    AuthService,
    InvalidCredentialsError,
)


class AuthServiceTests(unittest.TestCase):
    def setUp(self):
        self.repository = GraphRepository()
        self.service = AuthService(
            self.repository,
            "test-signing-key-long-enough-for-tests",
            token_lifetime=timedelta(seconds=1),
        )

    def test_register_hashes_password_and_issues_valid_token(self):
        account, token = self.service.register(
            " alice ", "ALICE@example.test", "password123"
        )
        self.assertEqual(account.username, "alice")
        self.assertEqual(account.email, "alice@example.test")
        self.assertNotEqual(account.password_hash, "password123")
        self.assertEqual(
            self.service.get_account_from_token(token).username, "alice"
        )

    def test_registration_rejects_duplicate_username_or_email(self):
        self.service.register("alice", "alice@example.test", "password123")
        with self.assertRaises(AccountAlreadyExistsError):
            self.service.register("alice", "other@example.test", "password123")
        with self.assertRaises(AccountAlreadyExistsError):
            self.service.register("bob", "ALICE@example.test", "password123")

    def test_registration_cannot_claim_legacy_collection_identity(self):
        self.repository.user_collections["legacy-user"] = {}
        with self.assertRaises(AccountAlreadyExistsError):
            self.service.register(
                "legacy-user", "legacy@example.test", "password123"
            )

    def test_login_rejects_invalid_credentials(self):
        with self.assertRaises(InvalidCredentialsError):
            self.service.login("missing", "password123")
        self.service.register("alice", "alice@example.test", "password123")
        with self.assertRaises(InvalidCredentialsError):
            self.service.login("alice", "wrong-password")

    def test_expired_and_tampered_tokens_are_rejected(self):
        _, expired_token = self.service.register(
            "alice", "alice@example.test", "password123"
        )
        time.sleep(1.1)
        self.assertIsNone(self.service.get_account_from_token(expired_token))
        self.assertIsNone(self.service.get_account_from_token(f"{expired_token}x"))

    def test_short_signing_key_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "at least 32 bytes"):
            AuthService(self.repository, "short")
        with patch.dict("os.environ", {"CALCOL_JWT_SECRET": "short"}):
            with self.assertRaisesRegex(RuntimeError, "at least 32 bytes"):
                AuthService.from_environment(self.repository)

    def test_configured_bootstrap_admin_is_created_idempotently(self):
        with patch.dict(
            "os.environ",
            {
                "CALCOL_ADMIN_USERNAME": "configured-admin",
                "CALCOL_ADMIN_EMAIL": "admin@example.test",
                "CALCOL_ADMIN_PASSWORD": "admin-password123",
            },
            clear=True,
        ):
            self.service.bootstrap_admin_from_environment()
            self.service.bootstrap_admin_from_environment()

        account = self.repository.get_user_by_username("configured-admin")
        self.assertIsNotNone(account)
        self.assertEqual(account.role, "ADMIN")


if __name__ == "__main__":
    unittest.main()
