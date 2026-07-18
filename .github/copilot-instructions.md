# Team Copilot Instructions

## Scope

These instructions apply to assets curated in this workspace and to cross-repo guidance maintained for the team.

## Defaults

- Prefer reusable guidance over repo-local duplication.
- Keep instructions short, explicit, and testable.
- Separate stable rules from task-specific prompts.
- Record repo assumptions close to the repo they affect.
- When guidance differs by repo, create a repo-named subfolder instead of mixing rules together.

## Content layout

- Put durable guidance in `instructions/`.
- Put non-negotiable constraints in `rules/`.
- Put agent definitions and role descriptions in `agents/`.
- Put proven playbooks and examples in `skills/`.
- Put rationale and decision records in `docs/`.

## Editing bar

- Prefer minimal edits to existing guidance.
- Remove stale instructions instead of layering conflicting ones.
- Avoid tool-specific wording unless the behavior depends on a specific tool.

## Review bar

- New guidance should answer one clear use case.
- Each rule should be actionable without extra interpretation.
- If a rule cannot be verified in practice, rewrite it until it can be.
