# Audit missing tests and executable coverage

Tracking: [#378](https://github.com/cTux/ae2-crafting-time/issues/378).

This is a recurring procedure, not a coverage result. Each run answers whether
the current default branch has shipped behavior without a useful automated
test or testable executable code below 100% line and branch coverage.

## Scope and evidence

Start from a clean checkout of the current default branch and record its exact
commit. Inventory executable production behavior in shared, compatibility,
loader/version-specific, build, and repository-script paths. Separate generated
code, resources, and genuinely declarative glue; do not hide executable decisions
under broad exclusions.

Map each behavior to its nearest useful existing check: pure unit tests, data
round trips and limits, script checks, or prepared-client/dedicated-server smoke
for Minecraft, mixins, loaders, and optional integrations. Record the source set
and runtime boundary each suite actually exercises.

## Run and resolve

Use the existing unit, contract, script, JaCoCo, and applicable smoke workflows.
Follow current repository/skill ordering: implementation changes receive their
hook-created PR before local checks; report GitHub CI separately. An audit needs
actual measurements, not an assumption from a previously green build.

Record before/after line and branch coverage per source set or meaningful
boundary. Inspect every missed executable line/branch and missing behavior check.
Add the smallest behavior-focused test at the relevant seam. Fix in-scope gaps
without weakening thresholds, expanding exclusions, testing private implementation
details, or adding empty assertions to raise a percentage.

Retain the shared module's 100% gate. Where full coverage is unsafe or infeasible,
name every remaining line/branch, the reason, nearest substitute evidence, and
the condition that would allow automation later. A high aggregate cannot excuse
an untested security, persistence, packet, compatibility, or failure path.
Update coverage configuration and testing guidance only when their boundary or
workflow changes. Do not manufacture changes when no gap exists.

## Per-run report

After fixes merge, comment on #378 with Yes or No to the opening question,
the audited commit, inventory/exclusions, executed suites, smoke boundaries,
before/after coverage per boundary, each finding and resolution, and every
justified remainder. Link PRs, reports, CI, and retained runtime evidence.
For a clean run, state no fix was needed and link the measurements supporting it.

Use `Refs #378`, never a closing keyword. Remove only the current run's claim
and leave the recurring issue open. This documentation-only addition neither
starts a runtime campaign nor answers the audit question.
