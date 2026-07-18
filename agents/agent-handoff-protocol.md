# Agent Handoff Protocol

This protocol defines deterministic role handoffs for the Team AI workspace.

Team tooling resources are defined in `team-resources.md` and MUST be applied in all handoffs.
Jira/Confluence access channel MUST be `-atlassian-onprem` MCP.

## Contract format (mandatory)

Every handoff MUST include these fields:

- `handoff_id`: unique id (e.g., `H-20260717-001`)
- `from_role`: source role name
- `to_role`: target role name
- `application`: `Calculator Collection` | `Cross-App`
- `goal`: one-sentence target outcome
- `scope_in`: explicit included scope
- `scope_out`: explicit excluded scope
- `inputs`: referenced artifacts/requirements
- `resource_context`: Jira project + Confluence space expectation from `team-resources.md`
- `access_channel`: required value `-atlassian-onprem` for Jira/Confluence work
- `constraints`: standards, architecture, compatibility, operational constraints
- `deliverables_expected`: concrete expected outputs
- `acceptance_checks`: objective checks for acceptance
- `due_or_priority`: urgency or sequence priority
- `open_questions`: unresolved items (or `none`)
- `risks`: known risks (or `none`)
- `status`: `ready` | `blocked`

## Global quality gates

- No implementation moves to review without `acceptance_checks`.
- No story is marked complete without code review, documentation review, QA test, and automation impact decision.
- If `open_questions` is not empty, handoff is `blocked`.
- If `resource_context` is missing or inconsistent with `team-resources.md`, handoff is `blocked`.
- If `access_channel` is missing or not `-atlassian-onprem` for Jira/Confluence-related work, handoff is `blocked`.

## Role contracts

## Team Lead Orchestrator

**Receives from:** Human stakeholder, Product Owners, Scrum Masters, Architect, Lead Software Engineer

**Sends to:** All roles

**Required output:**
- Prioritized work packet with `goal`, `scope_in/out`, `to_role`, and ordered dependency chain.

## Overall Architect

**Receives from:** Team Lead Orchestrator, Lead Software Engineer, Senior Software Engineers

**Sends to:** Lead Software Engineer, Senior Software Engineers, Code Reviewer

**Required output:**
- Architecture decision and impact note:
 - affected interfaces/contracts
 - backward compatibility decision
 - integration and operational risk level

## Lead Software Engineer

**Receives from:** Team Lead Orchestrator, Overall Architect

**Sends to:** Senior Software Engineers, Code Reviewer, QA Test Automation Engineer

**Required output:**
- Technical execution packet:
 - decomposition into implementation tasks
 - dependency order
 - test strategy expectation
 - risk controls

## Product Owners (Calculator Collection)

**Receives from:** Team Lead Orchestrator, stakeholders

**Sends to:** Corresponding Scrum Master, Senior Software Engineer, QA Tester

**Required output:**
- Product requirement packet:
 - business context
 - value statement
 - acceptance criteria (testable)
 - non-scope

## Scrum Masters (Calculator Collection)

**Receives from:** Product Owner, Team Lead Orchestrator

**Sends to:** Corresponding Senior Software Engineer, reviewers, QA roles, Team Lead Orchestrator

**Required output:**
- Execution readiness packet:
 - task sequencing
 - blocker list
 - dependency tracking
 - definition of ready/done check

## Senior Software Engineers (Calculator Collection)

**Receives from:** Lead Software Engineer, corresponding Product Owner/Scrum Master, Overall Architect

**Sends to:** Developer roles, Code Reviewer, QA Tester, QA Test Automation Engineer

**Required output:**
- Implementation handoff packet:
 - design approach
 - task assignment by technology
 - verification evidence (build/tests)
 - known impacts and rollback notes

## Technology Developers

### C Developer
### C++ Developer
### Java Spring Developer
### Database JPA Engineer
### JavaScript TypeScript Developer
### C# DotNet Developer
### Scripting and Build Developer
### DevOps CI Engineer

**Receives from:** Senior Software Engineer

**Sends to:** Senior Software Engineer, Code Reviewer (via senior), QA Test Automation Engineer (when tests touched)

**Required output:**
- Implementation completion packet:
 - changed components
 - rationale
 - local validation evidence
 - unresolved technical debt/risk (if any)

## Code Reviewer

**Receives from:** Senior Software Engineer, Lead Software Engineer

**Sends to:** Senior Software Engineer, Team Lead Orchestrator, QA roles

**Required output:**
- Review report:
 - severity-classified findings
 - mandatory fixes vs suggestions
 - approval status (`approved` | `changes_requested`)

## Spring Security Reviewer

**Receives from:** Senior Software Engineer, Java Spring Developer, Code Reviewer

**Sends to:** Senior Software Engineer, Code Reviewer, QA roles

**Required output:**
- Security review report:
 - authentication/authorization findings
 - JWT/session handling observations
 - mandatory fixes vs defense-in-depth suggestions
 - approval status (`approved` | `changes_requested`)

## Documentation Reviewer

**Receives from:** Senior Software Engineer, Product Owner

**Sends to:** Senior Software Engineer, Product Owner, Team Lead Orchestrator

**Required output:**
- Documentation verification report:
 - docs updated/missing
 - implementation-doc alignment status
 - release/ops impact completeness

## QA Tester

**Receives from:** Scrum Master, Product Owner, Senior Software Engineer

**Sends to:** Senior Software Engineer, Product Owner, Team Lead Orchestrator

**Required output:**
- Functional validation report:
 - executed test scenarios
 - pass/fail by acceptance criterion
 - defect entries with repro steps

## QA Test Automation Engineer

**Receives from:** Lead Software Engineer, Senior Software Engineer, QA Tester

**Sends to:** Senior Software Engineer, Team Lead Orchestrator, QA Tester

**Required output:**
- Automation report:
 - tests added/updated
 - coverage impact summary
 - CI integration status
 - flaky/risk notes

## Handoff state model

Use this state path for every work item:

1. `defined` (Product Owner)
2. `planned` (Scrum Master)
3. `designed` (Architect/Lead Engineer/Senior Engineer)
4. `implemented` (Developer roles)
5. `code-reviewed` (Code Reviewer)
6. `docs-reviewed` (Documentation Reviewer)
7. `qa-validated` (QA Tester)
8. `automation-validated` (QA Test Automation Engineer, if applicable)
9. `done` (Team Lead Orchestrator)

A work item can only move forward one state at a time unless Team Lead Orchestrator explicitly records an exception in `risks` and `constraints`.
