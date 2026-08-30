---
name: implement-planned-feature
description: Implement an approved AE2 Crafting Time feature from its issue, spec, technical design, and implementation plan. The main agent implements and reviews it; not for planning or unresolved requirements.
---

# Implement A Planned Feature

The main agent owns every decision, implementation change, and review.

## Confirm The Plan Is Ready

1. Read `AGENTS.md`, the GitHub issue, the feature's `spec.md`,
   `technical-design.md`, and `implementation-plan.md`, plus
   [the development skill](../ae2-crafting-time-dev/SKILL.md) and its linked
   testing workflow.
2. Reconcile the documents with current code before editing. Resolve changed
   repository facts yourself. Ask the user about any product, architecture,
   compatibility, scope, or migration choice that the documents do not settle.
3. Do not edit while decisions, contradictions, or missing acceptance-to-test
   mappings remain. Use `plan-new-feature` to repair incomplete planning.

## Implement The Plan

Implement the supplied plan directly. Preserve unrelated work, treat the issue
and planning documents as read-only, and stop for user direction when the plan
does not determine a product, architecture, compatibility, scope, migration, or
publishing decision. Follow the repository's pre-PR test restriction while
adding all planned tests.

## Review The Work

Inspect the complete diff yourself. Trace every acceptance criterion through the
implementation and tests, then verify:

- the code follows the approved design and existing ownership/source-set seams;
- shared root causes are fixed once and all affected loader/version boundaries
  stay synchronized;
- validation, security, wire and persistence compatibility, translations,
  metadata, and optional integrations remain correct where applicable;
- changed behavior has the required line, branch, boundary, and regression
  coverage without weakened gates;
- no speculative, unrelated, or unplanned change entered the diff.

Fix each finding and re-review the diff. Repeat until no material finding
remains.

## Finish

Only after review passes, follow `AGENTS.md`: make the feature's conventional
commit, let the hook create or update the PR, then inspect required CI. Decide
the response to any failure and apply the exact corrective edit. Read the final
PR and diff back before reporting the implementation, review findings and fixes,
checks, coverage state, and remaining risks. Never merge or release without
separate authorization.
