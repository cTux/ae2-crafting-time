# NeoEco renderer crash in the compatible smoke client

Status: finished

Scope: Quarantine the crashing NeoEco 21.1.1 development-client dependency on
Minecraft 1.21.1 NeoForge and verify the remaining compatible catalogue.

Issue: [#627](https://github.com/cTux/ae2-crafting-time/issues/627)

Planning: [Reviewed plan](implementation-plan.md) and [design](technical-design.md).

Implementation: [PR #732](https://github.com/cTux/ae2-crafting-time/pull/732)
merged at [b61f88d](https://github.com/cTux/ae2-crafting-time/commit/b61f88d948d4ed2345ce4d02068062823b4646aa).

Verification: [Issue #627 evidence comment](https://github.com/cTux/ae2-crafting-time/issues/627#issuecomment-6251676191)
records tested commit [e15e421](https://github.com/cTux/ae2-crafting-time/commit/e15e421ce486d5bb175041749224a8a314442796),
38 distinct cases across four graphs / 42 executions, 306 manually reviewed
screenshots, and passing local checks and CI.

## Problem and intended behavior

Two compatible campaigns crashed after `badge-background` wrote its result.
NeoEco's geometry callback read outside a `RenderChunkRegion` and raised
`ArrayIndexOutOfBoundsException: Index -21 out of bounds for length 9`.
The earlier case passed; later addon cases had not run.

The released NeoEco 21.1.1 callback retains a mutable section origin across an
asynchronous rebuild. Upstream later copied that origin before registering the
callback. The source and released bytecode establish the defect; existing crash
reports establish its runtime impact. A new unsafe-version reproduction or
instrumentation for exact historical coordinates is not required for this scope.

The compatible client must omit this known-crashing dependency and its CPU case.
Keep NeoEco in the latest diagnostic inventory, with its CPU fixture available
by explicit request. That availability does not qualify newer NeoEco APIs;
[their adapter migration remains #457](https://github.com/cTux/ae2-crafting-time/issues/457).

## Acceptance criteria

- **NE627-1:** Only the 1.21.1 NeoForge compatible graph excludes NeoEco, with a
  concrete renderer-failure reason and #627 link. Its coverage is
  `EXCLUDED` / `NOT_APPLICABLE`. An explicit compatible request for the excluded
  project fails clearly instead of loading it.
- **NE627-2:** The ordinary 1.21.1 catalogue omits `neoeco-cpu`. Latest dependency
  selection retains NeoEco, and explicit latest `neoeco-cpu` diagnostics remain
  addressable through the existing focused-case catalogue. No full compatible
  campaign automatically schedules a latest NeoEco graph. Latest availability
  must not be reported as a successful adapter or CPU check.
- **NE627-3:** Coverage validation accepts a direct fixture registered in the
  suite or the target's explicit focused cases. Unknown or stale fixture names
  still fail. Existing other-target selection and coverage behavior remains valid.
- **NE627-4:** The complete remaining 1.21.1 NeoForge compatible catalogue passes
  on the prepared 21.1.251 loader and Java 21. Retain the exact tested commit,
  dependency hashes, selection, per-case results, clean exit, English screenshots,
  visual review and timing evidence. Focused or partial success cannot pass this gate.
- **NE627-5:** Dependency documentation records the exclusion, actual catalogue
  count and diagnostic limitation. Link the upstream correction and preserve both
  historical failed campaigns. Production integrations, optional minimum versions,
  Forge coverage and other targets remain unchanged.

## Boundaries

This restores a usable, honestly reported compatible smoke graph. It does not fix
NeoEco's released renderer, upgrade its execution API, qualify its latest version,
or add a production compatibility mixin. Do not change fixture reset timing or hide
fatal errors. No upstream issue or comment is needed: the upstream correction is
already public, and this task does not authorize posting upstream.

## Runtime verification

The complete remaining 1.21.1 NeoForge compatible catalogue passed on NeoForge
21.1.251 and Java 21: the primary graph passed 35/35 cases. The three focused
graphs also passed, for 38 distinct selected cases across four graphs and 42
executions. All six native-client processes exited successfully and the guest
shut down cleanly. The immutable archive contains all 813 manifest entries with
matching size and SHA-256.

The automated visual gate reported `REVIEW_REQUIRED` only because this render
environment has no qualified comparison baseline; it reported zero mismatches.
Manual review covered all 306 screenshots, including their capture-sidecar
hashes, dimensions and GUI bounds, and found no discrepancy. The failed setup
attempt on the earlier commit ran zero UI cases; after correcting OmniSequence's
LDLib2 dependency closure, the passing campaign started fresh.

The compatible graph intentionally excludes NeoEco, so this result does not
qualify NeoEco itself or its newer execution APIs. Latest NeoEco diagnostics
remain available; API qualification is tracked separately in [#457](https://github.com/cTux/ae2-crafting-time/issues/457).
