import unittest
from pathlib import Path

from argon2 import PasswordHasher
from fastapi.testclient import TestClient

from python_migration.calculator_collection.api.server import build_app
from python_migration.calculator_collection.domain import UserAccount
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
                "password": "password123",
            },
        )
        self.assertEqual(response.status_code, 201, response.text)
        return response.json()

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
                "password": "password123",
                "confirm_password": "different-password",
            },
        )
        self.assertEqual(response.status_code, 400)

    def test_registration_login_and_authenticated_catalogue(self):
        response = self.client.post(
            "/api/auth/register",
            json={
                "username": "alice",
                "email": "alice@example.test",
                "password": "password123",
                "role": "ADMIN",
            },
        )
        self.assertEqual(response.status_code, 201, response.text)
        registration = response.json()
        token = registration["access_token"]
        self.assertEqual(registration["user"]["role"], "USER")
        self.assertNotEqual(self.repository.users["alice"].password_hash, "password123")

        profile = self.client.get(
            "/api/auth/me", headers={"Authorization": f"Bearer {token}"}
        )
        self.assertEqual(profile.json()["username"], "alice")

        login = self.client.post(
            "/api/auth/login",
            json={"username": "alice", "password": "password123"},
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


if __name__ == "__main__":
    unittest.main()
