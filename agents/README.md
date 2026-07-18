# Team Agent Roster

This folder defines the workspace agent team model.

## Team topology

- **Orchestration**
 - `team-lead-orchestrator.agent.md`
- **Cross-application leadership**
 - `overall-architect.agent.md`
 - `lead-software-engineer.agent.md`
- **Per-application delivery triads**
- Calculator Collection: `product-owner.agent.md`, `scrum-master.agent.md`, `senior-software-engineer.agent.md`
- **Technology developers**
 - `developer-c.agent.md`
 - `developer-cpp.agent.md`
 - `developer-java-spring.agent.md`
 - `database-jpa-engineer.agent.md`
 - `developer-javascript-typescript.agent.md`
 - `developer-csharp-dotnet.agent.md`
 - `developer-scripting-build.agent.md`
 - `devops-ci-engineer.agent.md`
- **Quality and review**
 - `code-reviewer.agent.md`
 - `spring-security-reviewer.agent.md`
 - `documentation-reviewer.agent.md`
 - `qa-tester.agent.md`
 - `qa-test-automation-engineer.agent.md`

## Working agreements

1. The team lead orchestrates and assigns work to the correct role.
2. Product Owner defines value and acceptance criteria; Scrum Master drives execution flow.
3. Senior engineers own technical implementation per application.
4. Language/technology developers implement scoped tasks under senior engineer guidance.
5. Code and documentation review are mandatory quality gates before completion.
6. QA tester validates behavior; QA test automation engineer adds/maintains automated coverage.

## Deterministic handoffs

Use the role-specific input/output contract in [agent-handoff-protocol.md](agent-handoff-protocol.md) for all role transitions.

## Team resources

Canonical mapping is documented in [team-resources.md](team-resources.md).
