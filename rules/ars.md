# AI Review Standard (ARS)

**Version:** 1.0
**Status:** Stable
**Category:** Core Standard

---

# Table of Contents

1. Purpose
2. Scope
3. Review Principles
4. Review Workflow
5. Review Categories
6. Findings Classification
7. Review Report
8. Completion Criteria

---

# 1. Purpose

The AI Review Standard (ARS) defines the methodology AI agents MUST follow when reviewing engineering deliverables.

The objectives of a review are to:

* verify correctness;
* reduce defects;
* improve quality;
* ensure compliance with applicable standards; and
* provide constructive, evidence-based feedback.

ARS complements:

* AES – AI Engineering Standard
* ACS – AI Coding Standard
* ADS – AI Documentation Standard

---

# 2. Scope

This standard applies to reviews of:

* source code;
* documentation;
* architecture;
* designs;
* configuration;
* infrastructure-as-code;
* automated tests;
* pull requests;
* change requests;
* migration plans;
* operational procedures.

---

# 3. Review Principles

## ARS-101 — Independent Review

Whenever practical, reviews SHOULD be performed by an agent other than the one that produced the deliverable.

If an independent reviewer is unavailable, the implementing agent MUST perform a structured self-review following this standard.

---

## ARS-102 — Evidence-Based Review

Review findings MUST be based on evidence.

Evidence MAY include:

* source code;
* documentation;
* configuration;
* tests;
* build output;
* architecture documentation;
* issue trackers;
* runtime behaviour.

The reviewer MUST distinguish between:

* verified facts;
* observations;
* assumptions; and
* recommendations.

---

## ARS-103 — Constructive Feedback

Reviews exist to improve the deliverable.

Feedback SHOULD be:

* objective;
* respectful;
* actionable;
* technically justified;
* proportional to the identified risk.

---

## ARS-104 — Risk-Based Depth

Review depth SHOULD be proportional to the potential impact of the change.

Changes affecting security, public APIs, data integrity, production systems, or architecture REQUIRE a more thorough review than cosmetic or low-risk changes.

---

# 4. Review Workflow

Every review SHOULD follow the same sequence.

---

## ARS-201 — Understand the Context

Before reviewing, understand:

* requirements;
* acceptance criteria;
* project standards;
* applicable AES, ACS and ADS rules;
* architecture;
* intended behaviour;
* scope of the change.

---

## ARS-202 — Verify Correctness

Verify that the implementation satisfies the requested behaviour.

Review:

* correctness;
* completeness;
* logical consistency;
* edge cases;
* requirement coverage.

---

## ARS-203 — Verify Existing Behaviour

Determine whether the implementation unintentionally changes existing behaviour.

Review:

* regression risks;
* backward compatibility;
* public interfaces;
* observable behaviour.

---

## ARS-204 — Verify Quality

Evaluate overall engineering quality, including:

* readability;
* maintainability;
* modularity;
* cohesion;
* coupling;
* complexity;
* naming;
* comments.

---

## ARS-205 — Verify Testing

Review:

* existing tests;
* new tests;
* regression tests;
* edge-case coverage;
* adherence to TDD where applicable.

Confirm that tests adequately verify the requested behaviour.

---

## ARS-206 — Verify Documentation

Review whether documentation:

* exists where required;
* matches the implementation;
* contains correct examples;
* reflects operational impact;
* remains internally consistent.

---

## ARS-207 — Verify Risk

Identify technical risks, including but not limited to:

* security;
* privacy;
* performance;
* reliability;
* maintainability;
* operational impact.

Document identified risks together with their potential consequences.

---

## ARS-208 — Verify Standards Compliance

Verify compliance with applicable standards, including:

* AES;
* ACS;
* ADS;
* company standards;
* project standards.

---

# 5. Review Categories

Each finding SHOULD be assigned one or more categories.

Suggested categories include:

* Correctness
* Requirements
* Architecture
* Maintainability
* Documentation
* Testing
* Security
* Performance
* Accessibility
* Operational Readiness
* Standards Compliance

Projects MAY define additional categories.

---

# 6. Findings Classification

Each finding MUST be assigned a severity.

## ARS-601 — Critical

A Critical finding:

* creates a significant security risk;
* risks data loss;
* prevents correct operation; or
* violates mandatory engineering requirements.

Critical findings MUST be resolved before approval.

---

## ARS-602 — Major

A Major finding significantly affects:

* correctness;
* maintainability;
* reliability;
* usability; or
* operational readiness.

Major findings SHOULD normally be resolved before approval.

---

## ARS-603 — Minor

Minor findings improve quality but do not prevent acceptance.

Examples include:

* naming improvements;
* documentation improvements;
* simplifications;
* readability enhancements.

---

## ARS-604 — Suggestion

Suggestions are optional recommendations intended to improve long-term quality, maintainability, or developer experience.

Suggestions MUST be clearly identified as optional.

---

# 7. Review Report

Every review SHOULD produce a structured report.

## Summary

Include:

* scope reviewed;
* overall result;
* reviewer confidence;
* overall risk assessment.

---

## Findings

For every finding include:

* identifier;
* category;
* severity;
* description;
* supporting evidence;
* recommendation.

---

## Standards Compliance

Reference applicable standards where relevant.

Example:

* AES-003 — Verified
* AES-103 — Verified
* ACS-205 — Compliant
* ADS-501 — Documentation Updated

---

## Final Recommendation

The review MUST conclude with one of the following outcomes:

* Approved
* Approved with Recommendations
* Changes Requested
* Rejected

The recommendation MUST include a brief justification.

---

# 8. Completion Criteria

A review is complete when:

* the requested scope has been reviewed;
* sufficient evidence has been examined;
* findings have been documented;
* applicable standards have been evaluated;
* risks have been identified;
* a final recommendation has been issued.

The review SHOULD leave the recipient with a clear understanding of:

* what is correct;
* what requires attention;
* why it matters; and
* how the deliverable can be improved.

Where applicable, the reviewer SHOULD include an overall confidence level in the review findings.
