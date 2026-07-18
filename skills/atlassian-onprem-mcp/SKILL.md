---
name: -onprem-atlassian-mcp
description: "Use the local on-prem Atlassian MCP for Jira and Confluence tasks. Use when the user asks to search, create, update, transition, comment on, attach files to, or link Jira issues; or to search, read, create, update, label, attach files to, or link Confluence pages. Also use for on-prem Atlassian requests mentioning jira-emea..org, jira..org, confluence..org, EMEA vs global routing, Jira attachments, Jira transitions, remote links, or Confluence page linking."
---
# On-Prem Atlassian MCP

## When to use this skill

Use this skill for Jira or Confluence requests in environments where Atlassian Cloud is not applicable and the local MCP server should be the primary integration path.

Trigger examples:

- "Create/update/search Jira issue"
- "Change Jira assignee or state"
- "List Jira transitions" or "move this ticket to Reviewed"
- "Attach this markdown file to Jira" or "download the ticket attachment"
- "Link this Jira issue to a Confluence page"
- "Search Confluence" or "Create Confluence page"
- "Use jira-emea..org / jira..org / confluence..org"

## What this skill is for

This skill teaches agents how to communicate with the local Atlassian MCP safely and predictably.

Use it to:

- choose the right Jira or Confluence tool for the task
- route Jira requests to EMEA vs global correctly
- prefer server-compatible search behavior for on-prem Jira
- use explicit transition discovery instead of guessing workflow states
- handle Jira and Confluence attachments with base64 payloads
- create or remove Jira links to Confluence pages using Jira remote links

Do not use this skill for Atlassian Cloud-only APIs or assumptions like account IDs always being present.

## Required environment assumptions

Before executing tools, assume these environment variables must be present:

- `JIRA_EMEA_PAT`
- `JIRA_GLOBAL_PAT`
- `CONFLUENCE_PAT`

And these base URLs (default values):

- `JIRA_EMEA_BASE_URL=https://jira-emea..org`
- `JIRA_GLOBAL_BASE_URL=https://jira..org`
- `CONFLUENCE_BASE_URL=https://confluence..org`

## Jira routing rules

1. If a tool supports `instance` and user explicitly wants an instance:
 - use `instance: "emea"` or `instance: "global"` exactly as requested.
2. Otherwise use `instance: "auto"` and route by project key.
3. EMEA project keys: ``, `WIN4`, `OPS`, `LBS`.
4. Any other project key routes to global Jira.
5. Derive project key from issue key prefix when needed (`-123` -> ``).

If project routing is ambiguous and no issue key is available, ask for `projectKey` or explicit `instance`.

## Operating rules for agents

1. Prefer the most specific tool available instead of composing broad searches and local post-processing.
2. For Jira identity-sensitive requests, use `jira_get_current_user` instead of assuming the username.
3. For JQL searches, prefer `jira_search_issues_jql` over `jira_search_issues` when fields, expand, or compatibility fallback matter.
4. For workflow moves, call `jira_get_transitions` before using a human state name when there is any ambiguity.
5. For editable field discovery, call `jira_get_editmeta` before writing unusual or custom fields.
6. For attachment uploads and downloads, use the MCP's base64 contract rather than assuming direct local file-path support.
7. When linking Jira to Confluence, use the dedicated link tools instead of writing raw URLs into comments unless the user explicitly wants a comment.
8. If the server returns a routing error in `auto` mode, retry with `projectKey`, `issueKey`, or explicit `instance`.

## Jira tool selection

- **Resolve current Jira identity**: `jira_get_current_user`
- **Read issue**: `jira_get_issue`
- **Search issues**: `jira_search_issues_jql` preferred, `jira_search_issues` acceptable for simple JQL
- **Create issue**: `jira_create_issue`
- **Comment**: `jira_add_comment`
- **Get comments**: `jira_get_comments`
- **List transitions**: `jira_get_transitions`
- **Transition issue**: `jira_transition_issue`
- **Editable field schema**: `jira_get_editmeta`
- **Update assignee/state**: `jira_update_issue_state_or_assignee`
- **Update issue fields**: `jira_update_issue_fields`
- **Attachment operations**: `jira_list_attachments`, `jira_get_attachment`, `jira_download_attachment`, `jira_add_attachment`
- **Link Jira issue to Confluence page**: `jira_link_confluence_page`, `jira_unlink_confluence_page`
- **Components**: `jira_list_components`, `jira_get_component`, `jira_set_component`, `jira_remove_component`, `jira_list_by_component`, `jira_search_by_component`
- **Labels**: `jira_list_labels`, `jira_add_label`, `jira_remove_label`, `jira_search_by_label`
- **Link management**: `jira_link_issues`, `jira_get_links`, `jira_unlink_issues`
- **Recently updated queries**: `jira_list_issues_updated`, `jira_list_issues_updated_on_date`, `jira_list_issues_updated_in_period`, `jira_list_issues_updated_by_user`
- **Commenter in period**: `jira_list_issues_by_commenter_in_period`
- **Filter by status**: `jira_list_issues_by_state`

### Jira usage patterns

- If the user asks for extra issue fields or changelog data, pass `fields` and `expand` to `jira_get_issue`.
- If the query may be long or instance-specific, prefer `jira_search_issues_jql` because it can fall back from GET to POST.
- If the user only knows a workflow state name, use `jira_get_transitions` first and then `jira_transition_issue` or `jira_update_issue_state_or_assignee`.
- If attaching generated content, base64-encode it locally first and send it with `jira_add_attachment`.
- If downloading attachment content for later processing, use `jira_download_attachment` and consume `contentBase64`.
- If linking an issue to a Confluence page, provide `issueKey` and `pageId`; unlink with `pageId` or `remoteLinkId`.

For state/assignee changes:

- Prefer `transitionName` when the user gives a human workflow state.
- Use `transitionId` when provided explicitly.
- Use `assigneeName` for Jira Server/Data Center style usernames unless `assigneeAccountId` is explicitly provided.
- Use `unassign: true` only when the user requests unassignment.
- If both assignee and state changes are requested, perform both in a single call.

## Confluence tool selection

- **Get page by ID**: `confluence_get_page`
- **Get page by title**: `confluence_get_page_by_title`
- **Search pages**: `confluence_search`
- **Create page**: `confluence_create_page`
- **Update page**: `confluence_update_page`
- **Label operations**: `confluence_get_labels`, `confluence_set_label`, `confluence_remove_label`
- **List by labels**: `confluence_list_by_labels`
- **Attachment operations**: `confluence_list_attachments`, `confluence_get_attachment`, `confluence_add_attachment`, `confluence_update_attachment`

When creating pages, content must be Confluence storage-format compatible.

## Common workflows

### Find and inspect a Jira issue

1. Route with `issueKey` or `projectKey`.
2. Use `jira_get_issue` for one issue.
3. Use `jira_search_issues_jql` for filtered issue discovery.

### Move a Jira issue safely

1. Call `jira_get_transitions` if the target state is not guaranteed.
2. Use `jira_transition_issue` for transition-only work.
3. Use `jira_update_issue_state_or_assignee` if transition and assignee should happen together.

### Upload or download Jira attachments

1. Call `jira_list_attachments` to discover attachment IDs.
2. Use `jira_get_attachment` for metadata.
3. Use `jira_download_attachment` to retrieve bytes as base64.
4. Use `jira_add_attachment` to upload base64 content.

### Link Jira and Confluence

1. Find or verify the Confluence page ID.
2. Call `jira_link_confluence_page` with `issueKey` and `pageId`.
3. Call `jira_unlink_confluence_page` with `remoteLinkId` when known, otherwise with `pageId`.

## Known constraints and expectations

- Jira search result windows are limited; use `startAt` and `maxResults` deliberately.
- On-prem Jira behavior may differ from Cloud; avoid Cloud-specific assumptions.
- Some Jira instances reject special JQL helpers; when in doubt, use plain JQL through `jira_search_issues_jql`.
- Attachment upload and download are base64-oriented at the MCP boundary.
- Jira-to-Confluence linking is implemented as a Jira remote link to the Confluence page.

## Safety and output behavior

- Never expose PAT values in responses, logs, or committed files.
- Keep user-visible summaries concise and include which Jira instance was used.
- If an API call fails due to permission/workflow constraints, report the exact failing action and suggest the minimal next input needed.
- If a transition or field update fails, prefer reporting the available transitions or editable fields rather than guessing a retry payload.
