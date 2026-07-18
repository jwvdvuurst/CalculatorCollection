# AI Prompt Standard (APS)

**Version:** 1.0
**Status:** Stable
**Category:** Core Standard

---

# Table of Contents

1. Purpose
2. Scope
3. Task Specification Principles
4. Task Definition
5. Context Management
6. Deliverables
7. Communication
8. Multi-Agent Collaboration
9. Completion Criteria

---

# 1. Purpose

The AI Prompt Standard (APS) defines how engineering tasks SHOULD be communicated between humans and AI agents, and between AI agents themselves.

The purpose of APS is to ensure that work is:

* clearly defined;
* reproducible;
* verifiable;
* reviewable; and
* transferable.

APS complements:

* AES – AI Engineering Standard
* ACS – AI Coding Standard
* ADS – AI Documentation Standard
* ARS – AI Review Standard

---

# 2. Scope

This standard applies to all engineering tasks, including:

* software development;
* debugging;
* architecture;
* documentation;
* code review;
* analysis;
* investigation;
* migration;
* planning;
* operational support.

---

# 3. Task Specification Principles

## APS-101 — Define the Objective

Every task MUST clearly describe the desired outcome.

The objective SHOULD describe **what** is to be achieved rather than **how** it should be implemented unless implementation constraints are part of the requirements.

---

## APS-102 — Define Scope

Every task SHOULD explicitly define:

* what is included;
* what is excluded;
* assumptions;
* constraints;
* known limitations.

---

## APS-103 — Define Success

Whenever practical, tasks SHOULD include measurable acceptance criteria.

Acceptance criteria SHOULD allow an independent reviewer to determine whether the task has been successfully completed.

---

## APS-104 — Prefer Facts over Interpretation

Task descriptions SHOULD contain verified facts.

Known assumptions, uncertainties and hypotheses SHOULD be identified explicitly.

---

# 4. Task Definition

A complete engineering task SHOULD contain the following information where applicable.

---

## APS-201 — Background

Provide sufficient background for understanding the problem.

Examples include:

* business context;
* technical context;
* previous decisions;
* related issues.

---

## APS-202 — Requirements

Describe the required behaviour.

Requirements SHOULD be:

* complete;
* testable;
* unambiguous;
* technically precise.

---

## APS-203 — Constraints

Document known constraints.

Examples include:

* technology;
* programming language;
* framework;
* compatibility;
* performance;
* security;
* regulatory requirements.

---

## APS-204 — References

Reference relevant supporting material where available.

Examples include:

* JIRA
* Confluence
* SharePoint
* Architecture documentation
* Design documents
* README
* ADRs
* Issue history

The AI agent SHOULD consult these resources before making assumptions.

---

# 5. Context Management

## APS-301 — Provide Relevant Context

Provide sufficient context to complete the task without unnecessary information.

Context SHOULD remain relevant to the requested work.

---

## APS-302 — Identify Applicable Standards

Identify standards that apply.

Examples include:

* AES
* ACS
* ADS
* ARS
* company standards
* project standards

---

## APS-303 — State Assumptions

When assumptions are unavoidable they SHOULD be documented explicitly.

Assumptions SHOULD be minimized through investigation.

---

## APS-304 — Clarify Ambiguity

If ambiguity prevents confident implementation, the AI agent SHOULD ask concise clarification questions before proceeding.

---

# 6. Deliverables

## APS-401 — Specify Deliverables

Expected deliverables SHOULD be identified.

Examples include:

* source code;
* documentation;
* architecture proposal;
* test plan;
* review report;
* migration plan;
* customer communication;
* operational procedure.

---

## APS-402 — Specify Output Format

The required output format SHOULD be identified whenever relevant.

Examples include:

* Markdown;
* JIRA Wiki;
* Plain text;
* JSON;
* YAML;
* CSV;
* diagrams;
* code;
* patches.

---

## APS-403 — Identify Review Requirements

Where applicable, identify:

* review expectations;
* testing expectations;
* documentation requirements;
* approval requirements.

---

# 7. Communication

## APS-501 — Communicate Clearly

Task descriptions SHOULD be:

* concise;
* technically accurate;
* complete;
* structured.

---

## APS-502 — State Unknowns

Unknown information SHOULD be identified rather than guessed.

The AI agent SHOULD distinguish between:

* facts;
* assumptions;
* observations;
* recommendations.

---

## APS-503 — Record Decisions

Significant engineering decisions SHOULD be documented together with their rationale.

---

# 8. Multi-Agent Collaboration

## APS-601 — Handover

When handing work to another AI agent, provide:

* completed work;
* remaining work;
* assumptions;
* supporting evidence;
* unresolved questions;
* identified risks.

---

## APS-602 — Preserve Context

Agents SHOULD preserve relevant context between tasks while avoiding unnecessary duplication.

---

## APS-603 — Reuse Existing Work

Before creating new work, agents SHOULD determine whether suitable work already exists.

Avoid unnecessary duplication of:

* code;
* documentation;
* investigations;
* analyses.

---

# 9. Completion Criteria

A task specification is considered complete when:

* the objective is clear;
* scope is defined;
* constraints are documented;
* relevant context is available;
* applicable standards are identified;
* deliverables are specified;
* acceptance criteria are defined where practical;
* review expectations are documented.

A complete task specification SHOULD allow an independent AI agent to perform the work with minimal clarification while remaining compliant with AES, ACS, ADS and ARS.
