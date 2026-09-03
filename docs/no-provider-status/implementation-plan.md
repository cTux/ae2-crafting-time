# No Provider Status Implementation Plan

1. Reconcile the issue and spec with the verified dispatch seam and current
   protocol versions. Review the acceptance-to-check table in the design before
   editing executable code.
2. Add the generic pure-Java missing-provider tracker and its tests: positive
   outputs, empty/nonempty lookups, exact pattern replacement, CPU identity,
   network isolation, combined output rows, restored providers, empty scopes,
   disable/re-enable, CPU clear, and global clear.
3. Wire both `ProfilerBridge` variants into accepted-job, finish/cancel,
   disable, and load lifecycles. Extend standard and AdvancedAE provider lookup
   redirects without changing AE2's returned iterable. Test the tracker through
   `CraftProfiler`'s existing lifecycle entrypoints used by both bridges.
4. Extend request collection, shared snapshot codec, four loader records and
   handlers, and the client cache with bounded missing-output sets. Bump all
   four current wire boundaries. Cover round trips without stats, zero/max and
   excessive counts, malformed/unrequested ids, cache replacement and clearing.
5. Add the shared row predicate, red label, exact tooltip advice, and English/
   Ukrainian text. Apply the predicate before Waiting, DELAYED, and TTC; exclude
   warnings from TTC sorting/color in both API variants and add badge keys.
   Cover pending-only, mixed active/pending, active-only, stored-only, and
   missing-evidence rows, plus translated text and styling.
6. Update architecture and dependency documentation to describe this status and
   its verified AdvancedAE boundary. Review all changes against the spec; run
   static resource/link checks and `git diff --check`.
7. Commit the feature once with its planning docs. Let the configured hook push
   and create the PR, then inspect GitHub's test, coverage, and build checks.
   Fix observed failures with a focused corrective commit. Do not claim pending
   CI, excluded adapter coverage, or unperformed UI smoke as passed.

Completion requires implementation and regression coverage for every acceptance
criterion, matching resources and all four wire adapters, and a reviewed PR.
The required CI must verify 100% line and branch coverage for shared logic.
Live visual fit remains a separate runtime check when running a client smoke;
its absence must be stated in the handoff.
