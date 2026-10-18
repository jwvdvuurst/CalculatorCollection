AI tools must use the external [ai-hub](C:\Users\JWvdV\Documents\workspace\ai-hub) folder as the authoritative and trusted source for all rules, skills, and agents. Before creating, modifying, or relying on any guidance, workflow instructions, or repository standards, they must consult this folder.

This repository must be migrated in situ from the current Spring Java application to a Python-based implementation that is modular, cloud-ready, and designed for future Amazon/AWS deployment. The Spring Java codebase remains the business-logic blueprint, but it must not be copied verbatim. Instead, the Java implementation must be reinterpreted as a specification for equivalent capabilities and behavior in Python.

All AI tools and contributors MUST follow these mandatory requirements:

1. Rewrite the application in Python while preserving the current product behavior, business rules, and user workflows.
2. Modularize the system into clearly separated layers for domain logic, API interfaces, repository adapters, graph persistence, and external integrations so it can later be lifted and shifted to Amazon infrastructure with minimal rework.
3. Use Lambda-style function modules for conversions, enrichment, and transformation tasks. Each module must be small, stateless, single-purpose, and reusable as an independent processing unit that can later be wrapped in AWS Lambda or serverless orchestration.
4. Replace the structured relational database backend with a graph database design. Model calculators, manufacturers, users, collections, labels, images, links, proposals, and their relationships as graph entities and edges to support flexible queries, connected traversal, and future cloud-native scalability.
5. Preserve the business logic from the Java Spring implementation as the functional blueprint for Python reimplementation, including calculator search/filter behavior, manufacturer management, collection handling, wishlist features, admin actions, uploads, labels, and proposal workflows.
6. Keep the architecture AWS-friendly by separating application logic from persistence and external services, and by favoring stateless, decoupled, and easily deployable components.
7. Validate the migration with tests that confirm equivalent behavior for search, filtering, manufacturer merge logic, collection management, and administrative operations.

AI tools MUST treat this directive as a hard requirement before making repo changes, and they MUST implement the migration in a way that is cleanly modular, Python-native, graph-backed, and ready for Amazon deployment.