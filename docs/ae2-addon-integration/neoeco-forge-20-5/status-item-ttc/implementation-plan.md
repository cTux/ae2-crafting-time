# Implement and verify NeoEco Status item TTC

[Scope and acceptance](spec.md) · [Technical design](technical-design.md)

1. Keep the supported release matrix from [#760](../spec.md). Verify the
   published 20.5.9 and 20.5.10 add-on JAR hashes and the exact Status
   descriptors. Add the Forge-only optional registration and the bounded
   server item-page hook. Use existing server options, profiler data and
   amount conversion. Cover empty work, mixed quantities, quantity overflow,
   pause, disabled keys and incomplete wire data in focused tests.
2. Carry the optional estimate through native row decoding and processing
   copies. Add the bounded client grid description and full card footer.
   Preserve the native rows when the option is off or the payload is missing.
   Check the normal and crowded inline bounds, the tooltip metadata order,
   and the untouched task page.
3. Update both GuideME locales, the Status rows option text as needed, and
   the GitHub wiki with the support range and the existing toggle. Keep
   loader and dependency floors unchanged.
4. Extend the prepared Forge NeoEco native Status fixture to run a real
   mixed-output job and verify the received row identity, separate learned
   TTC text, stored-only and unlearned entries, scroll or page replacement,
   the option toggle, and a second CPU or job without the first job's
   values. Capture the rendered grid and hover card. Reuse the pause,
   cancellation, restart and exact-order cases from #760. Verify missing row
   payload using a previous server artifact or a test-only payload omission.
5. Run the prepared client on 20.5.10 and repeat the focused Status smoke on
   20.5.9. Inspect the report, screenshot bounds and clean exit. Run the
   focused legacy and add-on-absent checks as appropriate for the mixin
   registration and check the NeoForge catalog and production JAR.
6. Recheck the exact PR head's tests, build artifacts and complete review
   history. Merge when the gates pass and verify issue closure. Mark the
   spec finished after the merge and link the evidence.

| Acceptance | Required proof |
| --- | --- |
| N766-1 | Unit coverage for quantity and eligibility plus a native grid with two independently timed outputs and an unknown row. |
| N766-2 | Reviewed normal and crowded grid/card captures with the toggle on and off. |
| N766-3 | Real selection, page or job changes and native lifecycle checks with no stale row estimate. |
| N766-4 | Missing payload, absent add-on, both older Forge paths, and NeoForge contract and packaging checks. |
