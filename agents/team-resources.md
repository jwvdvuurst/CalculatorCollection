# Team Resources

This file is the single source of truth for team-level tooling resources.

## Access channel (mandatory)

- For all Jira and Confluence operations, use MCP server: `atlassian-onprem`.
- This MCP is maintained by the team as a side project and is the default integration path.
- Do not use Atlassian Cloud assumptions or direct ad-hoc API usage unless explicitly required by an incident/workaround.

## Application resource mapping

 - Calculator Collection
 - Jira project: `TEAM`
 - Confluence space: `TEAM`

## External collaboration mapping

- Operations team (outside team; installation and 2nd-level support)
 - Jira project: `OPS`
 - Confluence: not defined here

## Usage rules

- For Calculator Collection items, default to Jira `TEAM` and Confluence `TEAM` unless explicitly overridden by task context.
- For installation/support coordination with Operations, create or link Jira issues in `OPS` and cross-reference originating team issues.
- Execute Jira/Confluence actions via `atlassian-onprem` MCP tools and include instance/project context explicitly.
