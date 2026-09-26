# Documentation lifecycle

Use a status for a defined delivery scope, not for how polished its prose looks.
Writing a complete plan makes it ready to implement; it does not finish the feature.

| Status | Meaning | Evidence needed |
| --- | --- | --- |
| `draft` | Planning is incomplete or needs renewed review. | Name open decisions, missing requirements, or deferred scope. |
| `ready-to-implement` | The spec, design and plan agree and settle implementation decisions. | Link the tracking issue and reviewed plan; map acceptance criteria to checks. |
| `in-progress` | Implementation or required verification has started but is incomplete. | Link the implementation PR or investigation and name the remaining gate. |
| `finished` | The defined scope is delivered and its required checks are complete. | Link merged implementation (or a completed research/no-change outcome) and verification evidence. |

## Where the status lives

Put one canonical block immediately below the title in `docs/<feature>/spec.md`.
For an existing standalone proposal, use its current entry document, such as
`cpu-bound-stats/index.md`. Keep paths stable. Companion designs and plans link
to the canonical block instead of copying a mutable status.

```markdown
Status: ready-to-implement

Scope: Initial implementation.

Issue: [#123](https://github.com/OWNER/REPO/issues/123)

Planning: [Reviewed plan](implementation-plan.md)
```

Use separate blank lines or Markdown hard line breaks between rendered fields.
Add implementation and verification links when available. Name the exact remaining
work for `in-progress`; an unknown or unlinked completion gate is not a pass.

If a document contains a shipped baseline and a later addition, give each scope
its own block. Put the addition's block under its existing heading. Other documents
link to that section; they do not repeat its status. Keep historical headings when
renaming them would break links, and explain that their planning wording is historical.

Reference pages, operating instructions, dependency inventories, and dated research
or test reports do not need implementation statuses. They describe facts or evidence;
link them from the applicable scope. A completed report does not finish the feature it
investigated. Keep the feature index as navigation, not another hand-maintained status table.

## Updating a status

- Planning starts at `draft`; move to `ready-to-implement` only after consistency
  review and required planning decisions/approvals. Recheck older plans against current code.
- Move to `in-progress` when implementation or execution starts. Keep it there through
  required CI, review, and runtime verification; a merged PR alone is insufficient.
- Move to `finished` after the documented completion gate, linking the exact tested
  revision and results when available. Record verification limits without weakening that gate.
- For a later change, add a separate scope; do not silently relabel an already finished baseline.
  Reopen an existing scope only when its own acceptance is no longer satisfied.
- Record a pause, cancellation, dependency or authorization hold beside the status.
  `ready-to-implement` describes the plan and never overrides `backlog`,
  approval requirements, or a missing external prerequisite. Deferred proposals need
  renewed review before resuming; closed-as-unneeded does not mean `finished`.

GitHub issues remain the work tracker. A documentation block summarizes the evidence
for its named scope; it does not override issue instructions. Issue closure, planning-PR
merge, elapsed time, and an unreviewed screenshot do not prove implementation completion.
When evidence is missing, state what is unverified rather than claiming failure or success.
Reconcile later evidence before relying on an older blocked PR description. A passing
check for one target or follow-up does not finish a broader acceptance matrix.
The initial status audit is dated 2026-09-22; it classifies retained evidence and does not
claim a new runtime campaign.

Status/evidence maintenance may update these blocks and links without changing approved
requirements. Changes to behavior, scope, design or acceptance still follow the planning
workflow. For tasks that cannot merge without separate authorization, leave the status
`in-progress`; finish it in an authorized merge follow-up after all gates pass.
