# Engineering Team Profile ()

**Version:** 1.0
**Status:** Stable
**Category:** Engineering Team Profile

---

# Table of Contents

1. Purpose
2. Team Scope
3. Engineering Philosophy
4. Investigation Workflow
5. Development Workflow
6. Documentation & Issue Tracking
7. Customer Support & Communication
8. Release Management
9. Code Review Expectations
10. Completion Criteria

---

# 1. Purpose

The Engineering Team Profile defines the engineering practices, workflows and conventions followed by the Engineering Team.

This profile extends:

* AES – AI Engineering Standard
* APS – AI Prompt Standard
* ACS – AI Coding Standard
* ADS – AI Documentation Standard
* ARS – AI Review Standard
* JVU – John van der Vuurst Engineering Profile
* – Engineering Profile

Project-specific profiles (such as CJM or WinIBW4) take precedence where applicable.

---

# 2. Team Scope

## -101 — Supported Systems

The Engineering Team develops and maintains software related to the ecosystem.

Typical projects include:

* 
* CJM
* WinIBW4
* CCWeb
* ISNI integrations
* Supporting tools and utilities

This profile applies to all engineering work performed within this ecosystem.

---

## -102 — Engineering Objectives

Engineering work SHOULD prioritize:

* Correctness
* Stability
* Backward compatibility
* Operational reliability
* Maintainability
* Incremental improvement

Avoid unnecessary complexity.

---

# 3. Engineering Philosophy

## -201 — Evidence-Based Engineering

Engineering decisions SHOULD be based on evidence.

Evidence may include:

* source code;
* logs;
* tests;
* configuration;
* documentation;
* customer reports;
* reproduction of the issue.

Avoid conclusions based solely on assumptions.

---

## -202 — Root Cause Analysis

Before implementing a fix:

1. Understand the reported behaviour.
2. Gather evidence.
3. Reproduce the issue whenever practical.
4. Determine the root cause.
5. Verify the diagnosis.
6. Implement the smallest appropriate solution.

Avoid treating symptoms without understanding the underlying cause.

---

## -203 — Incremental Delivery

Prefer:

* small pull requests;
* isolated fixes;
* reviewable implementations;
* low-risk deployments.

Large-scale refactoring SHOULD only be performed when explicitly planned.

---

# 4. Investigation Workflow

## -301 — Standard Investigation Process

Unless circumstances require otherwise, investigations SHOULD follow this sequence:

1. Read the issue description.
2. Review related issues.
3. Consult relevant documentation.
4. Examine the implementation.
5. Review automated tests.
6. Analyse logs and diagnostics.
7. Reproduce the issue where practical.
8. Determine the root cause.
9. Propose or implement a solution.

---

## -302 — Verify Before Concluding

Do not conclude an investigation until the available evidence supports the conclusion.

Where uncertainty remains:

* identify unknowns;
* document assumptions; and
* recommend additional investigation where appropriate.

---

# 5. Development Workflow

## -401 — Implementation Expectations

A completed implementation SHOULD include, where applicable:

* source code;
* automated tests;
* documentation updates;
* configuration updates;
* release notes;
* issue tracker updates.

---

## -402 — Preserve Existing Behaviour

Unless explicitly required otherwise:

* preserve existing behaviour;
* minimise customer impact;
* avoid unnecessary breaking changes.

Behavioural changes SHOULD be clearly documented.

---

## -403 — Production Awareness

Always consider:

* deployment impact;
* rollback procedures;
* operational monitoring;
* customer workflows;
* compatibility with existing environments.

---

# 6. Documentation & Issue Tracking

## -501 — Issue Documentation

Issues SHOULD clearly distinguish between:

* Context
* Problem Statement
* Current Behaviour
* Expected Behaviour
* Scope
* Non-Scope
* Acceptance Criteria

Facts SHOULD be distinguished from assumptions.

---

## -502 — Engineering Documentation

Documentation SHOULD explain:

* why a change was made;
* what changed;
* operational implications;
* migration considerations;
* known limitations.

---

## -503 — Release Notes

Release notes SHOULD describe:

* customer-visible changes;
* bug fixes;
* behavioural changes;
* configuration changes;
* operational considerations.

Avoid implementation details unless they are relevant to customers or operators.

---

# 7. Customer Support & Communication

## -601 — Technical Support

Support responses SHOULD distinguish between:

* Observations
* Analysis
* Evidence
* Conclusions
* Recommendations

Avoid speculation.

---

## -602 — Customer Communication

Customer communication SHOULD:

* remain technically accurate;
* be solution-oriented;
* avoid assigning blame;
* explain customer impact;
* clearly describe next steps.

---

## -603 — International Customers

Assume customers may come from different technical and cultural backgrounds.

Use:

* clear English;
* consistent terminology;
* concise explanations;
* respectful communication.

---

# 8. Release Management

## -701 — Deployment Readiness

Before recommending deployment, consider:

* testing completed;
* rollback available;
* compatibility verified;
* documentation updated;
* operational impact understood.

---

## -702 — Risk Assessment

For significant changes, identify:

* deployment risks;
* customer impact;
* operational impact;
* mitigation strategies.

---

# 9. Code Review Expectations

## -801 — Review Objectives

Reviews SHOULD verify:

* correctness;
* maintainability;
* regression risk;
* documentation;
* testing;
* operational impact.

Style issues SHOULD be secondary unless they affect maintainability.

---

## -802 — Review Outcome

Review comments SHOULD:

* explain the reasoning;
* reference evidence;
* distinguish mandatory findings from suggestions;
* provide actionable recommendations.

---

# 10. Completion Criteria

Engineering work is considered ready for delivery when:

* the implementation satisfies the requirements;
* the root cause has been verified (where applicable);
* testing has been completed;
* documentation has been updated;
* release notes have been prepared where required;
* operational impact has been evaluated;
* customer impact has been considered;
* the deliverable is ready for review under ARS.

Where appropriate, reference applicable rule identifiers from AES, APS, ACS, ADS, ARS, and this profile.
