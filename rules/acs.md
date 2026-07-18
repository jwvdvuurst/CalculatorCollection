# AI Coding Standard (ACS)

**Version:** 1.0
**Status:** Stable
**Category:** Core Standard

---

# Table of Contents

1. Purpose
2. Scope
3. Architecture and Design
4. Code Quality
5. State and Side Effects
6. Error Handling
7. Security
8. Testing
9. Performance
10. Completion Criteria

---

# 1. Purpose

The AI Coding Standard (ACS) defines the coding practices that AI agents MUST follow when designing, implementing, modifying, or refactoring software.

This standard complements the AI Engineering Standard (AES) and provides implementation-level guidance.

---

# 2. Scope

This standard applies to:

* New software
* Existing software
* Bug fixes
* Refactoring
* Maintenance
* Scripts
* Libraries
* APIs
* User Interfaces

Unless explicitly instructed otherwise.

---

# 3. Architecture and Design

## ACS-101 — Understand Before Coding

Before writing or modifying code, the agent MUST understand:

* the purpose of the component;
* the surrounding architecture;
* existing coding patterns;
* project coding standards;
* build and deployment process;
* dependencies; and
* existing tests.

The agent MUST investigate before modifying software.

---

## ACS-102 — Follow Existing Architecture

The agent MUST preserve the architectural style of the project.

The agent MUST NOT introduce new architectural patterns, frameworks, or libraries unless explicitly requested or clearly justified.

Consistency is preferred over novelty.

---

## ACS-103 — Minimize Change Scope

Changes SHOULD be:

* focused;
* incremental;
* reviewable; and
* limited to the requested functionality.

The agent MUST avoid unrelated refactoring unless explicitly requested.

---

## ACS-104 — Preserve Public Interfaces

Unless explicitly instructed otherwise, existing public interfaces MUST remain backward compatible.

Breaking changes REQUIRE:

* documented justification;
* updated documentation; and
* updated tests.

---

# 4. Code Quality

## ACS-201 — Readability First

Code MUST prioritize readability over cleverness.

Future maintainers SHOULD be able to understand the implementation without requiring extensive explanation.

---

## ACS-202 — Single Responsibility

Functions, methods, classes and modules SHOULD have a single, well-defined responsibility.

Large or complex implementations SHOULD be decomposed into smaller units.

---

## ACS-203 — Meaningful Naming

Identifiers MUST clearly communicate intent.

Names SHOULD describe the domain rather than the implementation.

Avoid unnecessary abbreviations unless they are established project conventions.

---

## ACS-204 — Self-Documenting Code

Code SHOULD explain *what* it does through its structure.

Comments SHOULD explain:

* why;
* business rules;
* assumptions;
* design decisions;
* algorithms; or
* workarounds.

Comments MUST NOT merely repeat the implementation.

---

## ACS-205 — DRY

The agent SHOULD avoid unnecessary duplication.

Shared behaviour SHOULD be extracted into reusable components when this improves maintainability.

Premature abstraction SHOULD be avoided.

---

# 5. State and Side Effects

## ACS-301 — Minimize Mutable State

Mutable state SHOULD be minimized.

Prefer:

* immutable objects;
* immutable collections; and
* explicit state transitions.

---

## ACS-302 — Minimize Side Effects

Functions SHOULD avoid hidden side effects.

Whenever practical, prefer:

* pure functions;
* explicit inputs;
* explicit outputs; and
* deterministic behaviour.

---

## ACS-303 — Avoid Global State

Global variables SHOULD NOT be introduced.

Shared mutable state SHOULD be avoided.

Dependencies SHOULD be provided through dependency injection or explicit interfaces.

---

## ACS-304 — Functional Programming

Where practical, prefer functional programming techniques, including:

* composition;
* immutability;
* higher-order functions;
* declarative programming.

Functional programming SHOULD improve readability rather than reduce it.

---

# 6. Error Handling

## ACS-401 — Explicit Failure

Errors MUST be handled explicitly.

The agent MUST NOT silently ignore failures.

---

## ACS-402 — Meaningful Error Messages

Error messages SHOULD:

* describe the problem;
* explain the cause where known; and
* support troubleshooting.

Sensitive information MUST NOT be exposed.

---

## ACS-403 — Logging

Logging SHOULD support diagnostics without revealing confidential information.

Passwords, secrets and tokens MUST NEVER be logged.

---

# 7. Security

## ACS-501 — Secure Defaults

Security SHOULD be enabled by default.

The agent MUST avoid reducing security unless explicitly requested.

---

## ACS-502 — Secret Management

The agent MUST NEVER:

* hardcode passwords;
* hardcode API keys;
* hardcode certificates;
* hardcode tokens; or
* expose credentials.

Secrets MUST be obtained from secure configuration mechanisms.

---

## ACS-503 — Validate External Input

All external input SHOULD be treated as untrusted.

Validation SHOULD occur before processing.

---

# 8. Testing

## ACS-601 — Test-Driven Development

When practical, the agent SHOULD follow Test-Driven Development.

The preferred workflow is:

1. Define expected behaviour.
2. Write automated tests.
3. Verify that tests fail.
4. Implement the solution.
5. Verify that tests pass.
6. Refactor while preserving behaviour.

---

## ACS-602 — Regression Protection

Bug fixes SHOULD include regression tests whenever practical.

---

## ACS-603 — Existing Tests

The agent MUST understand existing tests before modifying behaviour.

Existing tests SHOULD be preserved unless intentionally replaced.

---

# 9. Performance

## ACS-701 — Measure Before Optimizing

Performance optimizations SHOULD be supported by evidence.

Avoid premature optimization.

---

## ACS-702 — Maintainability Before Micro-Optimization

Readable and maintainable code SHOULD be preferred unless measurable performance requirements dictate otherwise.

---

# 10. Completion Criteria

Before implementation is considered complete, the agent SHOULD verify:

* requirements have been implemented;
* project coding standards have been followed;
* builds succeed;
* tests pass;
* documentation has been updated where necessary;
* no unnecessary side effects have been introduced;
* backward compatibility has been preserved where applicable;
* security considerations have been reviewed; and
* the implementation is ready for independent review under ARS.

The implementation SHOULD reference applicable AES and ACS rule identifiers where appropriate.
