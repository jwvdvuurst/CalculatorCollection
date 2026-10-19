import time
import unittest
from dataclasses import replace
from datetime import datetime, timedelta, timezone
from unittest.mock import patch

import jwt

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
            " alice ", "ALICE@example.test", "strong-password123"
        )
        self.assertEqual(account.username, "alice")
        self.assertEqual(account.email, "alice@example.test")
        self.assertNotEqual(account.password_hash, "strong-password123")
        self.assertEqual(
            self.service.get_account_from_token(token).username, "alice"
        )

    def test_registration_rejects_duplicate_username_or_email(self):
        self.service.register("alice", "alice@example.test", "strong-password123")
        with self.assertRaises(AccountAlreadyExistsError):
            self.service.register("alice", "other@example.test", "strong-password123")
        with self.assertRaises(AccountAlreadyExistsError):
            self.service.register("bob", "ALICE@example.test", "strong-password123")

    def test_registration_cannot_claim_legacy_collection_identity(self):
        self.repository.user_collections["legacy-user"] = {}
        with self.assertRaises(AccountAlreadyExistsError):
            self.service.register(
                "legacy-user", "legacy@example.test", "strong-password123"
            )

    def test_login_rejects_invalid_credentials(self):
        with self.assertRaises(InvalidCredentialsError):
            self.service.login("missing", "strong-password123")
        self.service.register("alice", "alice@example.test", "strong-password123")
        with self.assertRaises(InvalidCredentialsError):
            self.service.login("alice", "wrong-password")

    def test_expired_and_tampered_tokens_are_rejected(self):
        _, expired_token = self.service.register(
            "alice", "alice@example.test", "strong-password123"
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

    def test_bootstrap_admin_requires_twelve_character_password(self):
        base_environment = {
            "CALCOL_ADMIN_USERNAME": "configured-admin",
            "CALCOL_ADMIN_EMAIL": "admin@example.test",
        }
        with patch.dict(
            "os.environ",
            {**base_environment, "CALCOL_ADMIN_PASSWORD": "12345678901"},
            clear=True,
        ):
            with self.assertRaisesRegex(RuntimeError, "at least 12 characters"):
                self.service.bootstrap_admin_from_environment()
        with patch.dict(
            "os.environ",
            {**base_environment, "CALCOL_ADMIN_PASSWORD": "123456789012"},
            clear=True,
        ):
            self.service.bootstrap_admin_from_environment()
        self.assertEqual(
            self.repository.get_user_by_username("configured-admin").role,
            "ADMIN",
        )

    def test_repository_prevents_removing_last_enabled_administrator(self):
        first_admin = self.service.create_managed_user(
            "admin-one",
            "admin-one@example.test",
            "strong-password123",
            "ADMIN",
        )
        self.assertFalse(
            self.repository.update_user(
                replace(first_admin, role="USER", enabled=False)
            )
        )
        self.assertFalse(self.repository.delete_user(first_admin.id))

        second_admin = self.service.create_managed_user(
            "admin-two",
            "admin-two@example.test",
            "strong-password123",
            "ADMIN",
        )
        self.assertTrue(
            self.repository.update_user(
                replace(first_admin, role="USER", enabled=False)
            )
        )
        self.assertTrue(self.repository.delete_user(first_admin.id))
        self.assertFalse(self.repository.delete_user(second_admin.id))

    def test_registration_and_managed_accounts_require_twelve_characters(self):
        with self.assertRaisesRegex(ValueError, "at least 12 characters"):
            self.service.register("short", "short@example.test", "12345678901")
        with self.assertRaisesRegex(ValueError, "at least 12 characters"):
            self.service.create_managed_user(
                "short-admin",
                "short-admin@example.test",
                "12345678901",
                "ADMIN",
            )
        registered, _ = self.service.register(
            "exact-twelve", "exact-twelve@example.test", "123456789012"
        )
        self.assertEqual(registered.username, "exact-twelve")
        managed = self.service.create_managed_user(
            "managed-twelve",
            "managed-twelve@example.test",
            "123456789012",
            "USER",
        )
        self.assertEqual(managed.username, "managed-twelve")

    def test_legacy_username_subject_tokens_are_rejected(self):
        token = jwt.encode(
            {
                "sub": "legacy-user",
                "iat": datetime.now(timezone.utc),
                "exp": datetime.now(timezone.utc) + timedelta(minutes=5),
                "token_type": "access",
            },
            "test-signing-key-long-enough-for-tests",
            algorithm="HS256",
        )
        self.assertIsNone(self.service.get_account_from_token(token))

    def test_reused_username_does_not_revalidate_old_token(self):
        account, token = self.service.register(
            "deleted-user", "deleted@example.test", "strong-password123"
        )
        proposal = self.repository.submit_proposal(
            "Casio",
            "FX-TEST",
            created_by=account.username,
            created_by_id=account.id,
        )
        self.assertTrue(self.repository.delete_user(account.id))
        self.assertIsNone(self.service.get_account_from_token(token))
        replacement, replacement_token = self.service.register(
            "deleted-user",
            "replacement@example.test",
            "strong-password123",
        )
        self.assertNotEqual(replacement.id, account.id)
        self.assertIsNone(self.service.get_account_from_token(token))
        self.assertEqual(
            self.service.get_account_from_token(replacement_token).id,
            replacement.id,
        )
        self.assertEqual(
            self.repository.list_proposals()[0].created_by,
            proposal.created_by,
        )
        self.assertEqual(
            self.repository.list_proposals()[0].created_by_id,
            account.id,
        )


if __name__ == "__main__":
    unittest.main()
