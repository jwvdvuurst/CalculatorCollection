# Engineering Profile ()

**Version:** 1.0
**Status:** Stable
**Category:** Company Engineering Profile

---

# Table of Contents

1. Purpose
2. Engineering Environment
3. Engineering Practices
4. Repository Investigation
5. Version Control
6. Documentation & Issue Tracking
7. Customer Communication
8. Operational Awareness
9. Completion Criteria

---

# 1. Purpose

The Engineering Profile defines the engineering practices, conventions, and working methods that commonly apply to software development within .

This profile extends:

* AES – AI Engineering Standard
* ACS – AI Coding Standard
* ADS – AI Documentation Standard
* ARS – AI Review Standard
* APS – AI Prompt Standard

Project-specific profiles (for example, Calculator Collection) take precedence where applicable.

---

# 2. Engineering Environment

## -101 — Enterprise Software

Assume software forms part of a long-lived enterprise ecosystem.

Engineering decisions SHOULD prioritize:

* Stability
* Maintainability
* Backward compatibility
* Operational reliability
* Predictable behaviour
* Ease of support

---

## -102 — Multiple Environments

Assume software is deployed through multiple lifecycle environments.

Common environment types include:

| Environment | Common Aliases | Purpose |
| ------------------------- | -------------------- | -------------------------------------------- |
| Development | DEV | Development and local testing |
| Test | TEST, NAT, POC (NII) | Functional, integration and system testing |
| Acceptance / Verification | ACCEPT, ACC, NIT | Customer acceptance and verification testing |
| Production | PROD | Live production environment |

Recommendations SHOULD consider the impact on each environment.

Production-impacting changes REQUIRE additional caution.

When communicating with customers or colleagues, prefer the terminology used by that customer or project. Where ambiguity exists, explicitly mention equivalent environment names (for example: **NAT (Test)** or **NIT (Acceptance)**).

---

## -103 — Customer Terminology

 supports multiple customers who may use different terminology for equivalent concepts.

When communicating externally:

* Prefer the customer's terminology.
* Avoid mixing internal and customer-specific names without explanation.
* When appropriate, explicitly identify equivalent terminology.

Examples include:

* NAT = Test
* POC (NII) = Test
* NIT = Acceptance / Verification

---

## -104 — Customer-Facing Systems

Assume software may be used by:

* National libraries
* Academic institutions
* Public libraries
* Library networks
* International customers

Recommendations SHOULD minimise operational disruption and preserve existing customer workflows whenever practical.

---

# 3. Engineering Practices

## -201 — Incremental Delivery

Prefer:

* Small changes
* Focused implementations
* Reviewable pull requests
* Incremental improvements

Avoid unnecessary large-scale rewrites.

---

## -202 — Backward Compatibility

Maintain backward compatibility whenever practical.

Changes affecting:

* Public APIs
* Configuration
* Data formats
* Operational procedures
* Customer workflows

SHOULD be explicitly documented.

---

## -203 — Root Cause Analysis

Before implementing a fix:

* Determine the underlying cause.
* Verify the diagnosis.
* Gather supporting evidence.
* Explain the reasoning.

Avoid implementing symptom-based fixes whenever practical.

---

## -204 — Production Safety

Recommendations affecting production SHOULD include:

* Operational impact
* Risk assessment
* Rollback considerations
* Verification steps
* Testing recommendations

---

# 4. Repository Investigation

## -301 — Learn Before Changing

Before modifying a repository, investigate:

1. README
2. CONTRIBUTING (if present)
3. Way-of-working documentation
4. Style guides
5. Architecture documentation
6. Build configuration
7. CI/CD configuration
8. Existing automated tests

Repository documentation takes precedence over generic recommendations.

---

## -302 — Preserve Existing Conventions

Reuse existing:

* Architecture
* Naming conventions
* Logging strategy
* Dependency management
* Configuration patterns
* Testing approach

Avoid introducing new conventions without clear technical justification.

---

# 5. Version Control

## -401 — Branch Strategy

Unless instructed otherwise:

* Work on a feature branch.
* Keep branches focused on a single issue or enhancement.
* Keep branches short-lived.
* Follow the project's documented branching strategy.

---

## -402 — Protected Branches

Never recommend direct development on:

* main
* master
* release branches
* other protected branches

Changes SHOULD be reviewed before merging.

---

## -403 — Commit Philosophy

Commits SHOULD be:

* Small
* Atomic
* Descriptive
* Logically grouped
* Easy to review

Commit messages SHOULD clearly explain the purpose of the change.

---

# 6. Documentation & Issue Tracking

## -501 — Engineering Documentation

Engineering work SHOULD include documentation where appropriate.

Examples include:

* Implementation notes
* Operational impact
* Configuration changes
* Migration notes
* Release notes

---

## -502 — Issue Tracking

When creating issue tracker content:

* Follow the project's preferred issue format.
* Include sufficient technical detail for future maintainers.
* Clearly distinguish facts from assumptions.

Where appropriate include:

* Context
* Problem Statement
* Current Behaviour
* Expected Behaviour
* Scope
* Non-Scope
* Acceptance Criteria

---

## -503 — Technical Writing

Documentation SHOULD be:

* Technically accurate
* Concise
* Evidence-based
* Suitable for long-term maintenance

---

# 7. Customer Communication

## -601 — Professional Communication

Customer communication SHOULD be:

* Professional
* Respectful
* Technically accurate
* Solution-oriented
* Honest

Avoid assigning blame.

Focus on:

* Facts
* Observations
* Impact
* Resolution
* Next steps

---

## -602 — International Communication

Assume customers may come from different cultural and technical backgrounds.

When communicating internationally:

* Use clear English.
* Avoid idioms and colloquialisms.
* Explain technical concepts where appropriate.
* Remain polite and neutral.
* Adapt terminology to the customer's conventions.

---

# 8. Operational Awareness

## -701 — Logging

Recommendations SHOULD consider:

* Diagnostics
* Operational support
* Security
* Privacy

Sensitive information MUST never be logged.

---

## -702 — Monitoring

Where appropriate, recommendations SHOULD consider:

* Monitoring
* Observability
* Health checks
* Alerting
* Operational diagnostics

---

## -703 — Deployment

Deployment recommendations SHOULD consider:

* Rollback procedures
* Configuration changes
* Compatibility
* Verification after deployment
* Operational impact

---

# 9. Completion Criteria

Engineering work is considered ready for review when:

* Applicable standards have been followed.
* Repository conventions have been respected.
* Operational impact has been evaluated.
* Production risks have been considered.
* Documentation has been updated where necessary.
* Customer impact has been considered.
* The deliverable is ready for review under ARS.

Where appropriate, reference applicable rule identifiers from AES, ACS, ADS, ARS, APS and this profile.
