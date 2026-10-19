import unittest
from pathlib import Path
from threading import Event, Thread
from unittest.mock import patch

from argon2 import PasswordHasher
from fastapi.testclient import TestClient

from python_migration.calculator_collection.api.server import build_app
from python_migration.calculator_collection.domain import (
    Calculator,
    Manufacturer,
    UserAccount,
)
from python_migration.calculator_collection.repositories.graph_repository import (
    GraphRepository,
)
from python_migration.calculator_collection.services.auth_service import AuthService


def _seeded_repository() -> GraphRepository:
    repository = GraphRepository()
    catalogue_path = Path(__file__).resolve().parents[2] / "calculators.json"
    repository.load_catalogue_from_json_file(catalogue_path)
    return repository


class ApiLayerTests(unittest.TestCase):
    def setUp(self):
        self.repository = _seeded_repository()
        auth_service = AuthService(self.repository, "test-signing-key-long-enough-for-tests")
        self.client = TestClient(build_app(self.repository, auth_service))
        self.client.__enter__()

    def tearDown(self):
        self.client.__exit__(None, None, None)

    def register(self, username: str, email: str | None = None) -> dict:
        response = self.client.post(
            "/api/auth/register",
            json={
                "username": username,
                "email": email or f"{username}@example.test",
                "password": "strong-password123",
            },
        )
        self.assertEqual(response.status_code, 201, response.text)
        return response.json()

    def admin_headers(self) -> dict[str, str]:
        admin = UserAccount(
            username="admin",
            email="admin@example.test",
            password_hash=PasswordHasher().hash("admin-password123"),
            role="ADMIN",
        )
        self.assertTrue(self.repository.create_user(admin))
        login = self.client.post(
            "/api/auth/login",
            json={"username": admin.username, "password": "admin-password123"},
        )
        self.assertEqual(login.status_code, 200, login.text)
        return {"Authorization": f"Bearer {login.json()['access_token']}"}

    def test_health_endpoint_is_public(self):
        response = self.client.get("/api/health")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["status"], "ok")

    def test_frontend_origin_is_allowed(self):
        response = self.client.options(
            "/api/calculators",
            headers={
                "Origin": "http://localhost:5173",
                "Access-Control-Request-Method": "GET",
            },
        )
        self.assertEqual(
            response.headers["access-control-allow-origin"], "http://localhost:5173"
        )

    def test_api_requires_valid_bearer_token(self):
        self.assertEqual(self.client.get("/api/calculators").status_code, 401)
        self.assertEqual(
            self.client.get(
                "/api/calculators",
                headers={"Authorization": "Bearer not-a-valid-token"},
            ).status_code,
            401,
        )

    def test_registration_rejects_mismatched_confirmation(self):
        response = self.client.post(
            "/api/auth/register",
            json={
                "username": "alice",
                "email": "alice@example.test",
                "password": "strong-password123",
                "confirm_password": "different-password",
            },
        )
        self.assertEqual(response.status_code, 400)

    def test_registration_rejects_short_passwords(self):
        response = self.client.post(
            "/api/auth/register",
            json={
                "username": "short-password",
                "email": "short-password@example.test",
                "password": "12345678901",
            },
        )
        self.assertEqual(response.status_code, 422)
        self.assertNotIn("short-password", self.repository.users)

    def test_registration_login_and_authenticated_catalogue(self):
        response = self.client.post(
            "/api/auth/register",
            json={
                "username": "alice",
                "email": "alice@example.test",
                "password": "strong-password123",
                "role": "ADMIN",
            },
        )
        self.assertEqual(response.status_code, 201, response.text)
        registration = response.json()
        token = registration["access_token"]
        self.assertEqual(registration["user"]["role"], "USER")
        self.assertNotEqual(
            self.repository.users["alice"].password_hash, "strong-password123"
        )

        profile = self.client.get(
            "/api/auth/me", headers={"Authorization": f"Bearer {token}"}
        )
        self.assertEqual(profile.json()["username"], "alice")

        login = self.client.post(
            "/api/auth/login",
            json={"username": "alice", "password": "strong-password123"},
        )
        self.assertEqual(login.status_code, 200)
        catalog = self.client.get(
            "/api/calculators",
            headers={"Authorization": f"Bearer {login.json()['access_token']}"},
        )
        self.assertEqual(catalog.status_code, 200)
        self.assertGreater(len(catalog.json()), 0)

    def test_collection_and_wishlist_are_owned_by_authenticated_user(self):
        alice = self.register("alice")["access_token"]
        bob = self.register("bob")["access_token"]
        alice_headers = {"Authorization": f"Bearer {alice}"}
        bob_headers = {"Authorization": f"Bearer {bob}"}

        add_response = self.client.post(
            "/api/collection/alice/add",
            json={"calculator_id": 1, "notes": "Owned"},
            headers=alice_headers,
        )
        self.assertEqual(add_response.status_code, 200)
        collection = self.client.get("/api/collection/alice", headers=alice_headers)
        self.assertEqual(collection.status_code, 200)
        self.assertTrue(any(item["calculator_id"] == 1 for item in collection.json()))

        wishlist_response = self.client.post(
            "/api/wishlist/alice/add",
            json={"calculator_id": 2, "notes": "Wanted"},
            headers=alice_headers,
        )
        self.assertEqual(wishlist_response.status_code, 200)
        wishlist = self.client.get("/api/wishlist/alice", headers=alice_headers)
        self.assertTrue(any(item["calculator_id"] == 2 for item in wishlist.json()))

        self.assertEqual(
            self.client.get("/api/collection/alice", headers=bob_headers).status_code,
            403,
        )
        self.assertEqual(
            self.client.post(
                "/api/wishlist/alice/add",
                json={"calculator_id": 3},
                headers=bob_headers,
            ).status_code,
            403,
        )

    def test_proposal_owner_and_admin_moderation(self):
        alice_token = self.register("alice")["access_token"]
        alice_headers = {"Authorization": f"Bearer {alice_token}"}
        submitted = self.client.post(
            "/api/proposals",
            json={
                "manufacturer_name": "Olympia",
                "model": "SP 20",
                "created_by": "bob",
            },
            headers=alice_headers,
        )
        self.assertEqual(submitted.status_code, 200)
        proposal_id = submitted.json()["id"]
        self.assertEqual(submitted.json()["created_by"], "alice")

        bob_token = self.register("bob")["access_token"]
        bob_proposals = self.client.get(
            "/api/proposals", headers={"Authorization": f"Bearer {bob_token}"}
        )
        self.assertEqual(bob_proposals.json(), [])
        self.assertEqual(
            self.client.patch(
                f"/api/admin/proposals/{proposal_id}",
                json={"status": "approved"},
                headers={"Authorization": f"Bearer {bob_token}"},
            ).status_code,
            403,
        )

        admin_account = UserAccount(
            username="admin",
            email="admin@example.test",
            password_hash=PasswordHasher().hash("admin-password123"),
            role="ADMIN",
        )
        self.assertTrue(self.repository.create_user(admin_account))
        admin_login = self.client.post(
            "/api/auth/login",
            json={"username": "admin", "password": "admin-password123"},
        )
        admin_headers = {
            "Authorization": f"Bearer {admin_login.json()['access_token']}"
        }
        all_proposals = self.client.get("/api/proposals", headers=admin_headers)
        self.assertEqual(len(all_proposals.json()), 1)
        approved = self.client.patch(
            f"/api/admin/proposals/{proposal_id}",
            json={"status": "approved"},
            headers=admin_headers,
        )
        self.assertEqual(approved.status_code, 200)
        self.assertEqual(approved.json()["status"], "approved")
        calculator_id = approved.json()["created_calculator_id"]
        calculator = self.repository.get_calculator_by_id(calculator_id)
        self.assertIsNotNone(calculator)
        self.assertEqual(calculator.model, "SP 20")
        self.assertEqual(calculator.manufacturer.name.casefold(), "olympia")
        self.assertEqual(
            self.client.patch(
                f"/api/admin/proposals/{proposal_id}",
                json={"status": "approved"},
                headers=admin_headers,
            ).status_code,
            409,
        )

    def test_proposal_rejects_inverted_sales_period(self):
        token = self.register("proposal-owner")["access_token"]
        response = self.client.post(
            "/api/proposals",
            headers={"Authorization": f"Bearer {token}"},
            json={
                "manufacturer_name": "Test",
                "model": "Invalid period",
                "sold_from": 1980,
                "sold_to": 1970,
            },
        )
        self.assertEqual(response.status_code, 422)
        self.assertEqual(self.repository.list_proposals(), [])

    def test_legacy_invalid_proposal_cannot_be_approved(self):
        proposal = self.repository.submit_proposal(
            manufacturer_name="Test",
            model="Legacy invalid period",
            sold_from=1980,
            sold_to=1970,
        )
        admin_headers = self.admin_headers()
        calculator_count = len(self.repository.calculators)

        self.assertIsNone(
            self.repository.review_proposal(proposal.id, "approved", "admin")
        )
        response = self.client.patch(
            f"/api/admin/proposals/{proposal.id}",
            headers=admin_headers,
            json={"status": "approved"},
        )
        self.assertEqual(response.status_code, 409)
        self.assertIn("invalid sales period", response.json()["detail"])
        self.assertEqual(proposal.status, "pending")
        self.assertIsNone(proposal.created_calculator_id)
        self.assertEqual(len(self.repository.calculators), calculator_count)

    def test_concurrent_proposal_reviews_are_atomic(self):
        proposal = self.repository.submit_proposal(
            manufacturer_name="Test",
            model="Review race",
        )
        approval_entered = Event()
        release_approval = Event()
        rejection_started = Event()
        rejection_finished = Event()
        results = {}
        original_add_calculator = self.repository.add_calculator

        def hold_approval(calculator):
            approval_entered.set()
            if not release_approval.wait(timeout=5):
                raise TimeoutError("Test did not release the approval operation.")
            return original_add_calculator(calculator)

        def approve():
            results["approved"] = self.repository.review_proposal(
                proposal.id, "approved", "admin-a"
            )

        def reject():
            rejection_started.set()
            try:
                results["rejected"] = self.repository.review_proposal(
                    proposal.id, "rejected", "admin-b"
                )
            finally:
                rejection_finished.set()

        approval_thread = Thread(target=approve)
        rejection_thread = Thread(target=reject)
        with patch.object(
            self.repository, "add_calculator", side_effect=hold_approval
        ):
            approval_thread.start()
            try:
                self.assertTrue(approval_entered.wait(timeout=2))
                rejection_thread.start()
                self.assertTrue(rejection_started.wait(timeout=2))
                self.assertFalse(rejection_finished.wait(timeout=0.05))
            finally:
                release_approval.set()
                approval_thread.join(timeout=2)
                rejection_thread.join(timeout=2)

        self.assertFalse(approval_thread.is_alive())
        self.assertFalse(rejection_thread.is_alive())
        self.assertEqual(results["approved"].status, "approved")
        self.assertIsNone(results["rejected"])
        self.assertEqual(proposal.status, "approved")
        self.assertIsNotNone(proposal.created_calculator_id)

    def test_admin_calculator_and_manufacturer_management(self):
        admin_headers = self.admin_headers()
        created_manufacturer = self.client.post(
            "/api/admin/manufacturers",
            headers=admin_headers,
            json={"name": "Test maker"},
        )
        self.assertEqual(created_manufacturer.status_code, 201, created_manufacturer.text)
        manufacturer_id = created_manufacturer.json()["id"]
        self.assertEqual(
            self.client.post(
                "/api/admin/manufacturers",
                headers=admin_headers,
                json={"name": "test MAKER"},
            ).status_code,
            409,
        )
        created = self.client.post(
            "/api/admin/calculators",
            headers=admin_headers,
            json={
                "model": "Test 100",
                "manufacturer_id": manufacturer_id,
                "sold_from": 1980,
                "source_url": "https://example.test/calculator",
                "current_price": "12.50",
            },
        )
        self.assertEqual(created.status_code, 201, created.text)
        calculator_id = created.json()["id"]
        self.assertEqual(created.json()["manufacturer"], "Test maker")

        updated = self.client.put(
            f"/api/admin/calculators/{calculator_id}",
            headers=admin_headers,
            json={
                "model": "Test 200",
                "manufacturer_id": manufacturer_id,
                "current_price": "20.00",
            },
        )
        self.assertEqual(updated.status_code, 200, updated.text)
        self.assertEqual(updated.json()["model"], "Test 200")

        renamed = self.client.put(
            f"/api/admin/manufacturers/{manufacturer_id}",
            headers=admin_headers,
            json={"name": "Renamed maker"},
        )
        self.assertEqual(renamed.status_code, 200)
        self.assertEqual(
            self.repository.get_calculator_by_id(calculator_id).manufacturer.name,
            "Renamed maker",
        )
        source = self.repository.add_manufacturer(Manufacturer(name="Maker alias"))
        source_calculator = self.repository.add_calculator(
            Calculator(
                model="Alias model",
                manufacturer=source,
            )
        )
        merged = self.client.post(
            "/api/admin/manufacturers/merge",
            headers=admin_headers,
            json={
                "target_id": manufacturer_id,
                "source_id": source.id,
                "new_name": "Merged maker",
            },
        )
        self.assertEqual(merged.status_code, 200, merged.text)
        self.assertEqual(
            self.repository.get_calculator_by_id(source_calculator.id).manufacturer.name,
            "Merged maker",
        )
        owner = self.register("catalog-owner")["access_token"]
        owner_headers = {"Authorization": f"Bearer {owner}"}
        self.assertEqual(
            self.client.post(
                "/api/collection/catalog-owner/add",
                headers=owner_headers,
                json={"calculator_id": calculator_id},
            ).status_code,
            200,
        )
        self.assertEqual(
            self.client.post(
                "/api/wishlist/catalog-owner/add",
                headers=owner_headers,
                json={"calculator_id": calculator_id},
            ).status_code,
            200,
        )
        self.assertEqual(
            self.client.delete(
                f"/api/admin/calculators/{calculator_id}",
                headers=admin_headers,
            ).status_code,
            204,
        )
        self.assertEqual(
            self.client.get(
                "/api/collection/catalog-owner", headers=owner_headers
            ).json(),
            [],
        )
        self.assertEqual(
            self.client.get(
                "/api/wishlist/catalog-owner", headers=owner_headers
            ).json(),
            [],
        )
        self.assertIsNone(self.repository.get_calculator_by_id(calculator_id))

    def test_admin_user_management_protects_accounts(self):
        admin_headers = self.admin_headers()
        weak_account = self.client.post(
            "/api/admin/users",
            headers=admin_headers,
            json={
                "username": "weak-password-user",
                "email": "weak-password@example.test",
                "password": "12345678901",
                "role": "USER",
            },
        )
        self.assertEqual(weak_account.status_code, 422)
        self.repository.add_to_collection("legacy-placeholder", 1)
        listed_users = self.client.get("/api/admin/users", headers=admin_headers)
        self.assertEqual(listed_users.status_code, 200)
        self.assertNotIn(
            "legacy-placeholder",
            {user["username"] for user in listed_users.json()},
        )
        created = self.client.post(
            "/api/admin/users",
            headers=admin_headers,
            json={
                "username": "managed-user",
                "email": "managed@example.test",
                "password": "strong-password123",
                "role": "USER",
            },
        )
        self.assertEqual(created.status_code, 201, created.text)
        user_id = created.json()["id"]
        self.assertNotIn("password_hash", created.json())

        updated = self.client.put(
            f"/api/admin/users/{user_id}",
            headers=admin_headers,
            json={
                "email": "updated@example.test",
                "role": "ADMIN",
                "enabled": False,
            },
        )
        self.assertEqual(updated.status_code, 200, updated.text)
        self.assertEqual(updated.json()["role"], "ADMIN")
        self.assertFalse(updated.json()["enabled"])
        self.repository.add_to_collection("managed-user", 1)
        self.repository.add_to_wishlist("managed-user", 2)

        admin_id = self.repository.users["admin"].id
        self.assertEqual(
            self.client.put(
                f"/api/admin/users/{admin_id}",
                headers=admin_headers,
                json={
                    "email": "admin@example.test",
                    "role": "USER",
                    "enabled": False,
                },
            ).status_code,
            400,
        )
        self.assertEqual(
            self.client.delete(
                f"/api/admin/users/{admin_id}",
                headers=admin_headers,
            ).status_code,
            400,
        )
        self.assertEqual(
            self.client.delete(
                f"/api/admin/users/{user_id}",
                headers=admin_headers,
            ).status_code,
            204,
        )
        self.assertEqual(self.repository.get_user_collection_count("managed-user"), 0)
        self.assertEqual(self.repository.get_wishlist_count("managed-user"), 0)

    def test_reused_username_cannot_access_former_account_or_proposals(self):
        admin_headers = self.admin_headers()
        registered = self.register("former-user")
        user_headers = {
            "Authorization": f"Bearer {registered['access_token']}"
        }
        former_account_id = self.repository.users["former-user"].id
        proposal = self.client.post(
            "/api/proposals",
            headers=user_headers,
            json={"manufacturer_name": "Test", "model": "Former user's proposal"},
        )
        self.assertEqual(proposal.status_code, 200, proposal.text)
        self.assertEqual(
            self.repository.list_proposals()[0].created_by_id,
            former_account_id,
        )

        deleted = self.client.delete(
            f"/api/admin/users/{former_account_id}",
            headers=admin_headers,
        )
        self.assertEqual(deleted.status_code, 204, deleted.text)
        self.assertEqual(self.client.get("/api/auth/me", headers=user_headers).status_code, 401)

        re_registration = self.client.post(
            "/api/auth/register",
            json={
                "username": "former-user",
                "email": "replacement@example.test",
                "password": "strong-password123",
            },
        )
        self.assertEqual(re_registration.status_code, 201, re_registration.text)
        replacement_headers = {
            "Authorization": (
                f"Bearer {re_registration.json()['access_token']}"
            )
        }
        self.assertEqual(
            self.client.get("/api/auth/me", headers=user_headers).status_code,
            401,
        )
        self.assertEqual(
            self.client.get("/api/auth/me", headers=replacement_headers).status_code,
            200,
        )
        self.assertNotEqual(
            self.repository.users["former-user"].id,
            former_account_id,
        )

        other = self.register("other-user")
        other_headers = {"Authorization": f"Bearer {other['access_token']}"}
        self.assertEqual(
            self.client.get("/api/proposals", headers=replacement_headers).json(),
            [],
        )
        self.assertEqual(
            self.client.get("/api/proposals", headers=other_headers).json(),
            [],
        )

    def test_regular_user_cannot_access_admin_management(self):
        token = self.register("regular")["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        self.assertEqual(self.client.get("/api/admin/users", headers=headers).status_code, 403)
        self.assertEqual(
            self.client.post(
                "/api/admin/calculators",
                headers=headers,
                json={"model": "Denied"},
            ).status_code,
            403,
        )


if __name__ == "__main__":
    unittest.main()
