---
name: plan-new-feature
description: Plan a new AE2 Crafting Time feature by creating its GitHub issue, specification, technical design, and implementation plan. Use before implementation; not for documented bugs or internal refactors.
---

# Plan A New Feature

Produce a decision-complete issue and matching planning documents before code is
changed. Do not invent product behavior or leave design choices for the
implementation agent.

## Establish The Feature

1. Read `AGENTS.md`, `docs/architecture.md`, the closest feature documents, and
   the code seams needed to verify current behavior and supported targets.
2. Resolve the goal, player-visible behavior, boundaries, compatibility, and
   acceptance criteria from the request and repository evidence. Ask the user
   only about decisions that materially change the feature.
3. Reuse an existing feature directory when the requirement extends it.
   Otherwise create `docs/<feature-slug>/` with:
   - `spec.md`: goal, player behavior and rules, compatibility, non-goals, and
     testable acceptance criteria;
   - `technical-design.md`: evidence, affected components and interfaces, state
     and data flow, validation, failures, compatibility or migration, and only
     decision-protecting alternatives;
   - `implementation-plan.md`: ordered implementation slices, exact ownership
     seams, tests and boundary checks, cross-version work, verification, and a
     measurable completion gate.

Keep the specification solution-neutral. Make the technical design and
implementation plan precise enough that implementation requires no product,
architecture, compatibility, or scope decisions.

## Self-Review

Review the three documents together before creating the issue:

- Every requested behavior and non-goal appears in the spec.
- Every acceptance criterion maps to a design path and implementation/test step.
- Server/client ownership, source-set reuse, all affected release-matrix rows,
  packets, persistence, security limits, translations, and optional integrations
  are covered when relevant.
- The documents agree on names, state, ordering, compatibility, and completion.
- Claims are backed by repository or upstream evidence, and no unresolved
  decision is disguised as an implementation detail.

Fix every review finding, then reread the result from the implementer's point of
view. Stop and ask the user if a decision still remains.

## Create The GitHub Issue

Draft one issue title and body covering the problem, resulting behavior,
non-goals, acceptance criteria, relevant risks, and the three document paths.
Show the exact title and body and get explicit approval before creating the
external issue. Any later issue-text change requires fresh approval. If the
feature already has an open issue, update that issue after the same approval
instead of creating a duplicate.

Create the approved issue with `gh issue create` using a body file, then read it
back. Add its URL to the spec and rerun consistency review across the issue and
all three documents. If creation fails or the result is uncertain, stop instead
of retrying blindly.

## Finish

Run relevant documentation and link checks plus `git diff --check`. Follow
`AGENTS.md` for the single planning commit and hook-created PR. Report the issue
and PR URLs, document paths, review corrections, assumptions, and actual checks.
