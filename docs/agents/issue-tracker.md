# Issue tracker

Use GitHub Issues in the repository identified by `git remote -v` (currently
`cTux/ae2-crafting-time`), through `gh`. Resolve a bare number as an issue or PR
before acting. Read full bodies, comments, labels, and linked planning documents.
GitHub is the source of truth; do not introduce a local-markdown tracker.

For new work, find a matching issue first. Follow the project writing/development
skill if none exists. Follow [AGENTS.md](../../AGENTS.md#issue-labels) for labels.
Use body files for multiline writes, sanitize private data, read every mutation
back, and stop on ambiguous results rather than blindly duplicating a write.

The `to-spec` skill owns planning issue-text approval and the three-document
contract. Triage briefs summarize that contract, not replace it. Use linked
sub-issues and native dependencies when available; otherwise explicit blocker
links in bodies. A dependency becoming ready never removes an implementation hold.

External PR queue discovery is off by default. Explicitly named PRs are in scope.
Do not merge or close implementation issues merely because a skill finishes;
follow the authorized merge/closure workflow and verified completion conditions.

## Wayfinding operations

Use an ordinary parent issue for the decision map and child issues for decisions.
Record `Decision kind: research|prototype|grilling|task` in each child body.
Query the parent's open children and their blockers for the ready frontier.
Use the normal project labels, never `wayfinder:*` or upstream triage labels.
Keep decisions linked and preserve history when scope changes.
