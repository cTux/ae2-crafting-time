---
name: implement
description: Implement and review an approved AE2 feature spec or ticket. Use to-spec when feature decisions are missing; development for direct bug fixes.
metadata:
  upstream-repository: https://github.com/mattpocock/skills
  upstream-revision: 49dd158d1076134a641b33efb035946536778336
  upstream-path: skills/engineering/implement
license: MIT
---

# Implement

Implement the supplied spec or ticket. Fetch a supplied issue reference and state
its title first. The main agent owns every decision, implementation change, and review.
For a documented bug or internal refactor, use the development skill directly;
new feature planning documents are not prerequisites for those tasks.

## Confirm The Plan Is Ready

1. Read `AGENTS.md`, the GitHub issue, the feature's `spec.md`,
   `technical-design.md`, and `implementation-plan.md`, plus
   [the development skill](../ae2-crafting-time-dev/SKILL.md) and its linked
   testing workflow.
2. Reconcile the documents with current code before editing. Resolve changed
   repository facts yourself. Ask the user about any product, architecture,
   compatibility, scope, or migration choice that the documents do not settle.
3. Do not edit while decisions, contradictions, or missing acceptance-to-test
   mappings remain. Use `to-spec` to repair incomplete planning.

## Implement The Plan

Implement the supplied plan directly. Preserve unrelated work, treat the issue
and approved requirements, design and acceptance criteria as read-only. Maintain
only lifecycle status, implementation links and verification evidence under
[the documentation lifecycle](../../../docs/documentation-status.md). Mark the
active scope `in-progress` when implementation starts; mark it `finished` only
after merge and all documented checks, with evidence. Keep completed baselines
separate from follow-ups. Stop for user direction when the plan
does not determine a product, architecture, compatibility, scope, migration, or
publishing decision. Follow the repository's pre-PR test restriction while
adding all planned tests. Use the `tdd` discipline at the approved interfaces
when its red-green execution is permitted by that ordering; never claim an
observed red run before actually executing one. After PR creation, run only
the checks required by the development skill and approved plan.

## Review The Work

Inspect the complete diff yourself. Trace every acceptance criterion through the
implementation and tests, then verify:

- the code follows the approved design and existing ownership/source-set seams;
- shared root causes are fixed once and all affected loader/version boundaries
  stay synchronized;
- validation, security, wire and persistence compatibility, translations,
  metadata, and optional integrations remain correct where applicable;
- new TTC statuses and features have an on/off option and appear in the
  GuideME book and GitHub wiki; changes to existing ones appear in both;
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
