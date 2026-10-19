# Python Migration Blueprint

This folder contains the Python reimplementation blueprint for the Calculator Collection application. It preserves the existing Spring Java business logic as the functional reference while moving the runtime model to a modular, graph-backed, AWS-friendly architecture.

## Goals

- Reimplement the application in Python in place.
- Keep the Java implementation as the business-logic blueprint.
- Replace the relational model with graph-first persistence patterns.
- Modularize services for future Lambdas and AWS deployment.
- Maintain component isolation between API, domain, persistence, and orchestration layers.

## Core structure

- `calculator_collection/domain.py` – canonical domain models
- `calculator_collection/lambdas/converters.py` – Lambda-style transformation functions
- `calculator_collection/repositories/protocols.py` – repository contracts consumed by services
- `calculator_collection/repositories/graph_repository.py` – in-memory repository used by tests
- `calculator_collection/adapters/graph/neo4j_repository.py` – persistent local Neo4j adapter
- `calculator_collection/services/calculator_service.py` – Java service behavior translated to Python
- `calculator_collection/api/router.py` – application-facing API façade
- `calculator_collection/app.py` – top-level app orchestration point
- `calculator_collection/adapters/graph/neo4j_adapter.py` – lightweight graph test adapter

## Graph-first design

The application models relationships as graph edges, for example:

- Manufacturer -> MANUFACTURES -> Calculator
- User -> OWNS -> Calculator
- CollectionItem -> BELONGS_TO -> User

This offers better traversal and future migration to Neo4j, Amazon Neptune, or other graph stores.

## Local graph database

The API uses Neo4j Community locally through the official Python driver. Amazon
Neptune Database is the intended AWS production target; the application uses a
repository contract so a Neptune adapter can be added independently. Neo4j
Cypher is not a drop-in substitute for Neptune openCypher, so validate queries
against each adapter rather than assuming full engine compatibility.

Install and start the local database with Docker Desktop:

```powershell
Copy-Item .env.example .env
# Edit .env and replace the placeholder with a local development password.
docker compose --env-file .env --profile graph up -d neo4j
```

Set the matching password for the Python API process and start the API:

```powershell
$env:CALCOL_GRAPH_BACKEND = "neo4j"
python -m python_migration.api_server
```

The API automatically loads the repository-root `.env` file without replacing
environment variables already set by the process. It reads `CALCOL_NEO4J_*`,
`CALCOL_JWT_SECRET`, and optional `CALCOL_ADMIN_*` values from that file. Keep
the file local and do not commit it.

The API requires a separate JWT signing secret of at least 32 bytes. Keep it
stable across API restarts and private to the API deployment. Accounts register
with a username, email, and a password of at least 12 characters; passwords are
stored as Argon2id hashes.
Registration always assigns the `USER` role. To provision the first
administrator, set `CALCOL_ADMIN_USERNAME`, `CALCOL_ADMIN_EMAIL`, and
`CALCOL_ADMIN_PASSWORD` before starting the API. The administrator password
must be at least 12 characters and at most 72 UTF-8 bytes. Bootstrap never
overwrites an existing account. Do not reuse placeholder catalog usernames
that already have legacy collection data.

Every API endpoint other than `/api/health`, `/api/auth/register`, and
`/api/auth/login` requires a valid bearer token. Collection and wishlist
operations are limited to the authenticated account, and proposal moderation
requires the `ADMIN` role. The React client keeps access tokens in memory and
requires a new sign-in after a page reload.

Administrators can create, edit, and delete calculators; rename and merge
manufacturers; create, update, disable, and delete user accounts; and approve or
reject calculator proposals. Approval creates a calculator and reuses an
existing manufacturer name when possible. An administrator cannot disable,
demote, or delete their own account, and the API prevents removal of the last
enabled administrator. The React Admin page exposes these operations.
Access tokens identify accounts by immutable graph user ID; deleting an account
and reusing its username does not transfer existing tokens or proposal
ownership. Existing username-subject tokens are rejected after this change,
requiring users to sign in again. Startup assigns IDs to older graph user nodes
and backfills proposal ownership only when the current account predates the
proposal; older proposals without an unambiguous account match remain visible
to administrators, not regular users.

Admin API routes are under `/api/admin`: calculator CRUD uses
`/calculators`; manufacturer creation, rename, and merge use `/manufacturers`
and `/manufacturers/merge`; user management uses `/users`; and proposal review
uses `PATCH /proposals/{id}`.

This is not yet a complete migration of the Spring admin panel. Labels,
calculator images and links, enrichment, and bulk import/export workflows are
not implemented in the Python API/UI.

Generate a suitable signing secret and place the output after
`CALCOL_JWT_SECRET=` in the root `.env`:

```powershell
python -c "import secrets; print(secrets.token_urlsafe(48))"
```

The graph ports are bound to localhost, and the database uses a named Docker
volume. The first API startup creates uniqueness constraints and imports
`calculators.json` only when the graph contains no calculators. Existing graph
data is not overwritten. Keep `.env` local; do not reuse this development
credential in shared or production environments.

Use `CALCOL_GRAPH_BACKEND=memory` only when deliberately running the
non-persistent in-memory adapter. Set `CALCOL_NEO4J_URI` and
`CALCOL_NEO4J_DATABASE` to override the local defaults.

## Lambda-style modules

The conversion modules are intentionally small, stateless, and single-purpose so they can later be lifted into AWS Lambda functions or serverless orchestration flows.

## Verification

Run the migration tests:

```bash
python -m unittest discover -s python_migration/tests -t .
```

## React UI

The UI is maintained as a separate React/TypeScript project in [`../ui`](../ui).
It calls the Calculator Collection API; it does not start or duplicate backend
business logic.

Start the API in one terminal:

```bash
python -m python_migration.api_server
```

Start the frontend in another terminal:

```bash
cd ui
npm install
npm run dev
```

The Vite development server proxies `/api` requests to `http://127.0.0.1:8001`.
For a separately hosted frontend, set `VITE_API_BASE_URL` to the API base URL
(including `/api`) and configure `CALCOL_CORS_ORIGINS` on the API with the
frontend's origin.
