# AI Documentation Standard (ADS)

**Version:** 1.0
**Status:** Stable
**Category:** Core Standard

---

# Table of Contents

1. Purpose
2. Scope
3. Documentation Principles
4. Document Structure
5. Documentation Content
6. Documentation Quality
7. Living Documentation
8. Completion Criteria

---

# 1. Purpose

The AI Documentation Standard (ADS) defines the documentation practices that AI agents MUST follow when creating, modifying, or maintaining technical documentation.

Documentation is considered an integral part of software engineering and MUST evolve together with the implementation.

This standard complements the AI Engineering Standard (AES) and the AI Coding Standard (ACS).

---

# 2. Scope

This standard applies to all forms of technical documentation, including but not limited to:

* User documentation
* Developer documentation
* Administrator documentation
* Operator documentation
* Installation guides
* Configuration guides
* API documentation
* Architecture documentation
* Design documents
* Operational runbooks
* Troubleshooting guides
* Release notes
* Migration guides
* README files

Unless explicitly instructed otherwise.

---

# 3. Documentation Principles

## ADS-101 — Documentation is Part of the Deliverable

Documentation MUST be considered part of the software deliverable.

Significant implementation changes SHOULD include corresponding documentation updates.

---

## ADS-102 — Documentation Must Match the Implementation

Documentation MUST accurately reflect the current implementation.

If documentation and implementation conflict, the discrepancy MUST be corrected or explicitly reported.

---

## ADS-103 — Write for the Intended Audience

Every document MUST identify its intended audience.

Typical audiences include:

* End Users
* Developers
* Installers
* Operators
* Administrators
* Support Engineers
* Architects

A document MAY serve multiple audiences where appropriate.

---

## ADS-104 — Language

Documentation MUST be written in English unless explicitly instructed otherwise.

---

## ADS-105 — Output Format

Documentation SHOULD use Markdown unless another format is explicitly requested.

Alternative formats MAY include:

* JIRA Wiki
* HTML
* PDF
* AsciiDoc
* reStructuredText

---

# 4. Document Structure

## ADS-201 — Consistent Structure

Documents SHOULD follow a consistent structure appropriate to their purpose.

Where applicable, include:

* Title
* Purpose
* Scope
* Intended Audience
* Prerequisites
* Overview
* Main Content
* Examples
* Troubleshooting
* References
* Revision History

---

## ADS-202 — Table of Contents

Documents of moderate or greater size SHOULD include a Table of Contents.

---

## ADS-203 — Headings

Heading levels SHOULD be used consistently.

Documents SHOULD avoid unnecessary nesting.

---

## ADS-204 — Examples

Examples SHOULD be included whenever they improve understanding.

Examples MUST be accurate and consistent with the implementation.

---

# 5. Documentation Content

## ADS-301 — Explain the Why

Documentation SHOULD explain:

* why something exists;
* when it should be used;
* limitations;
* assumptions;
* design decisions; and
* operational impact.

Documentation SHOULD avoid merely repeating the implementation.

---

## ADS-302 — Practical Guidance

Documentation SHOULD contain practical information such as:

* examples;
* recommended practices;
* common pitfalls;
* troubleshooting guidance; and
* operational considerations.

---

## ADS-303 — Complete Project Documentation

Where applicable, projects SHOULD include documentation covering:

* installation;
* configuration;
* deployment;
* operation;
* monitoring;
* logging;
* backup;
* restore;
* upgrades;
* rollback;
* troubleshooting;
* security considerations.

---

## ADS-304 — Architecture Documentation

Projects SHOULD document significant architectural decisions.

Architecture documentation SHOULD describe:

* components;
* responsibilities;
* dependencies;
* interfaces;
* data flow; and
* important design decisions.

---

## ADS-305 — API Documentation

Public APIs SHOULD document:

* purpose;
* parameters;
* return values;
* exceptions;
* examples;
* versioning considerations.

---

# 6. Documentation Quality

## ADS-401 — Clarity

Documentation SHOULD be:

* accurate;
* concise;
* technically correct;
* consistent;
* easy to navigate.

Marketing language SHOULD be avoided unless explicitly requested.

---

## ADS-402 — Consistency

Terminology SHOULD remain consistent throughout the documentation.

The same concept SHOULD NOT be described using multiple names without explanation.

---

## ADS-403 — Evidence

Technical claims SHOULD be supported by implementation, configuration, tests, or authoritative references whenever practical.

---

## ADS-404 — Accessibility

Documentation SHOULD be accessible.

Use:

* meaningful headings;
* descriptive link text;
* readable tables;
* accessible code examples;
* alternative text for diagrams and images where applicable.

---

# 7. Living Documentation

## ADS-501 — Documentation Maintenance

Documentation MUST be maintained alongside the software.

Whenever behaviour changes, the agent SHOULD review affected documentation.

---

## ADS-502 — Keep Examples Current

Examples, screenshots, configuration snippets, commands, and API examples SHOULD remain consistent with the current implementation.

Obsolete examples SHOULD be updated or removed.

---

## ADS-503 — Version Awareness

Documentation SHOULD clearly indicate version-specific behaviour where applicable.

Changes between versions SHOULD be documented.

---

## ADS-504 — Missing Documentation

If significant documentation is missing, the agent SHOULD notify the user and ask whether the missing documentation should be created.

The absence of documentation MUST NOT be silently ignored.

---

# 8. Completion Criteria

Before documentation is considered complete, the agent SHOULD verify that:

* the documentation matches the implementation;
* the intended audience has been identified;
* terminology is consistent;
* examples are correct;
* links and references are valid;
* formatting is consistent;
* related documentation has been updated where necessary; and
* the documentation is ready for independent review under ARS.

The documentation SHOULD reference applicable AES, ACS, and ADS rule identifiers where appropriate.
