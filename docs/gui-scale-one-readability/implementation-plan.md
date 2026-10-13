# Scale-1 readability implementation plan

[Scope and status](spec.md)

1. Deliver these documents and the requirement clarification below through the
   documentation workflow. Keep the native-hardware authority/scope decision
   visibly pending; it blocks final completion, not implementation. Keep #695 claimed.
2. On the implementation branch, inspect the pinned AE2 source/bytecode in both
   adapters and map table render, scroll range, hover bounds, and both TTC click
   consumers. Implement the shared geometry policy and pure boundary tests,
   including scale changes, last partial row, nonzero scroll, cell borders,
   disabled behavior and native mode. Do not replace AE2's renderer.
3. Prepare the capture diagnostic as part of the reviewed implementation;
   execute it only in the workflow's verification step after the reviewed
   implementation commit has created its PR. Use existing Forge 1.20.1 compatible
   `standard-status-controls` and `cpu-list-total-ttc` cases. Compare repeated
   settled-frame captures with a contemporaneous VNC view. A pre-fix run must
   use an immutable retained artifact/bundle with verified SHA and hashes;
   otherwise use the current candidate for the capture/readback comparison and
   retain the original archived failures. Do not rebuild a mislabeled baseline
   from candidate source. If this diagnostic disproves downsampling as the cause,
   return through the workflow's correction step, revise the patch and plan,
   and repeat affected review/checks on the corrected head.
4. Apply the shared scale-1 geometry to rendering/scroll/hover/click consumers.
   Use one-pixel table and CPU text. Add styled ellipsis/full-tooltip fallback
   only when measured text cannot fit. Cover shared CPU layout/fit decisions,
   including unknown/expired/disabled TTC, exact fit and overflow. Preserve the
   existing scale-2+ path. Implement the design's call-site readiness check in
   the existing mixin plugin and verify that incomplete hooks return native
   geometry coherently. Update both adapters and changed mixin registrations.
5. Extend existing native scenarios for one-column indexing, scrolling, scale
   transitions, long TTC, complete amount/status lines, tooltip recovery and
   CPU collision checks. Include at least three description lines and multiple
   pages of entries. Keep real observed bounds in sidecars. Update the CPU and
   long-TTC docs, GuideME and wiki source to describe the narrow scale-1 preview
   exception and the full hover value; update English/Ukrainian together if new
   player-facing text is needed.
6. Review and commit following the repository hook order. Run required shared
   coverage/component checks only after PR creation. Build every release target
   and check remapped production injection targets for changed optional mixins.
   Use changed-scope smoke selection, inspect its reasons, and select the
   existing compatible Forge 1.20.1 and NeoForge 26.1.2 native CPU/table cases
   as the minimum two-adapter runtime set. Other targets need an extra runtime
   check only if source/bytecode differences expose a distinct changed path.
7. Review English captures at scale 1, 2 and Auto, default/uniform fonts,
   background and shadow on/off. Check real item hover, TTC Ctrl-click, scroll
   and selection. Use base-only native fixtures when no addon path changed;
   do not turn this into a full dependency campaign. Archive every attempt.
8. Qualify deterministic scale-1 amount/TTC and CPU text regions using the
   existing exact-pixel contract engine. Add its smallest self-check that a
   missing unmasked foreground stroke fails. Keep environment/font identity,
   original PNGs and manual legibility review. A changing estimate cannot be
   masked wholesale and called a text contract; freeze the fixture value.
9. Complete the native diagnostic only through the separately authorized path,
   or record the explicitly accepted limitation without claiming native proof.
   Finish current-head CI and review gates, retain evidence and update canonical
   status only when every required criterion is supported.

## Criterion mapping

| Criteria | Change | Check |
| --- | --- | --- |
| R1, R2 | Shared geometry, table text adapter | Covered geometry cases; scale-1 PNG stroke review and full long-TTC fixture |
| R3 | Renderer/scroll/hover and screen click consumers | First/last entries, nonzero scroll, sort and scale transitions |
| R4 | CPU scale/fit and tooltip | Exact fit/overflow unit cases; card collision and matching full hover |
| R5 | Native-mode passthrough and option behavior | Scale 2/Auto, font, shadow and background cases |
| R6 | Existing synchronous capture diagnostic | Repeated settled PNG plus contemporaneous VNC comparison |
| R7 | Existing exact-pixel visual contract | Good reference accepted, erased foreground stroke rejected |
| R8 | Both adapters and authorized diagnostics | Four-target compile/remap; two-adapter focused startup/UI; native result or accepted limit |

## Exact issue clarification for delivery

Append the following to the original issue body, preserving all original
observations, reproduction details and evidence links:

> Implementation clarification: at effective GUI scale 1, use one wider column
> inside the existing native table footprint and show fewer rows with scrolling,
> so all description lines render without fractional font reduction. Preserve
> the player's GUI-scale setting and existing scale-2+ behavior. CPU cards keep
> their geometry and use readable unscaled TTC; an overlong label may use an
> ellipsis preview only when the complete value is available in its tooltip.
> Apply the same preview/full-tooltip fallback to exceptional table text that
> exceeds the wider cell. Preserve semantic fields, symbols, uncertainty and
> interaction behavior. This is a narrow scale-1 exception to earlier
> no-truncation presentation requirements. Investigate settled-frame capture
> timing during post-PR verification and correct the patch if it disproves the
> proposed rendering cause. Native-hardware investigation
> remains pending separate execution authority and must not be inferred from
> VMware results. Preserve the original captures and qualify a visual contract
> that rejects a missing foreground stroke.
