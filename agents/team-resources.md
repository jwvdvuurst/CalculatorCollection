# Team Resources

This file is the single source of truth for team-level tooling resources.

## Access channel (mandatory)

- For all Jira and Confluence operations, use MCP server: `atlassian-onprem`.
- This MCP is maintained by the team as a side project and is the default integration path.
- Do not use Atlassian Cloud assumptions or direct ad-hoc API usage unless explicitly required by an incident/workaround.

## Application resource mapping

 - 9.3
 - Jira project: `TEAM`
 - Confluence space: `TEAM`
- CJM
 - Jira project: `TEAM`
 - Confluence space: `TEAM`
- WinIBW4
 - Jira project: `WIN4`
 - Confluence: none (no dedicated space)

## External collaboration mapping

- Operations team (outside team; installation and 2nd-level support)
 - Jira project: `OPS`
 - Confluence: not defined here

## Usage rules

- For 9.3 and CJM items, default to Jira `TEAM` and Confluence `TEAM` unless explicitly overridden by task context.
- For WinIBW4 items, use Jira `WIN4` and do not assume Confluence documentation exists.
- For installation/support coordination with Operations, create or link Jira issues in `OPS` and cross-reference originating team issues.
- Execute Jira/Confluence actions via `atlassian-onprem` MCP tools and include instance/project context explicitly.
