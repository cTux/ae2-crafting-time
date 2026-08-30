---
name: implement-planned-feature
description: Implement an approved AE2 Crafting Time feature from its issue, spec, technical design, and implementation plan. Delegates decision-free coding to GPT-5.3 Codex Spark; not for planning or unresolved requirements.
---

# Implement A Planned Feature

The parent agent owns every decision and reviews every change. The implementation
agent only applies the approved plan.

## Confirm The Plan Is Ready

1. Read `AGENTS.md`, the GitHub issue, the feature's `spec.md`,
   `technical-design.md`, and `implementation-plan.md`, plus
   [the development skill](../ae2-crafting-time-dev/SKILL.md) and its linked
   testing workflow.
2. Reconcile the documents with current code before delegation. Resolve changed
   repository facts yourself. Ask the user about any product, architecture,
   compatibility, scope, or migration choice that the documents do not settle.
3. Do not delegate while decisions, contradictions, or missing acceptance-to-test
   mappings remain. Use `plan-new-feature` to repair incomplete planning.

## Delegate Pure Implementation

Spawn exactly one sub-agent with:

- model: `gpt-5.3-codex-spark`;
- reasoning effort: `medium`;
- normal Standard execution path, with no fast, pro, or alternate-model
  override;
- `fork_turns: "none"` so it receives only the implementation packet.

If the exact model or medium effort is unavailable, stop and report it instead
of substituting. When the collaboration API exposes no Standard-tier selector,
use its normal path without a tier override.

Give it the issue URL, exact planning-document paths, permitted scope, acceptance
criteria, required tests, target/version boundaries, and the exact local skill
and workflow paths it must read. Tell it:

- implement the supplied plan exactly and preserve unrelated work;
- make no product, architecture, compatibility, scope, migration, or publishing
  decision;
- treat the issue, spec, technical design, and implementation plan as read-only;
  report any mismatch to the parent instead of rewriting the plan;
- stop and report the exact blocker when the plan does not determine an action;
- do not delegate further, commit, push, open a PR, post externally, or publish;
- follow the repository's pre-PR test restriction while adding all planned tests.

Do not edit the same workspace concurrently. Wait for the implementation agent
to finish or report a blocker. The parent decides every blocker and sends an
exact follow-up instruction; never ask the implementation agent to choose.

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

For each finding, make the decision yourself and send the same agent a precise
correction. Re-review the new diff. Repeat until no material finding remains.

## Finish

Only after review passes, follow `AGENTS.md`: make the feature's conventional
commit, let the hook create or update the PR, then inspect required CI. Decide
the response to any failure and delegate only the exact corrective edit. Read
the final PR and diff back before reporting the implementation, review findings
and fixes, checks, coverage state, and remaining risks. Never merge or release
without separate authorization.
