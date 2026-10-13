# Keep long row estimates inside their cells

Status: finished

Scope: Restore native Crafting Plan and Crafting Status row containment.

Issue: [#670](https://github.com/cTux/ae2-crafting-time/issues/670)

Planning: [Implementation plan](implementation-plan.md)

Implementation: [PR #688](https://github.com/cTux/ae2-crafting-time/pull/688),
merged as [ce0414ec9ae1e46c98189f5c376718b12a3221aa](https://github.com/cTux/ae2-crafting-time/commit/ce0414ec9ae1e46c98189f5c376718b12a3221aa).
Tested implementation head: `e3e2c374e53e629d9c3f3e56cf02b0aa9c7d2f1e`.
Six native cases have source at that head. The 1.20.1 Forge
`standard-status-controls` archive retains source
`52240e6e7efce3d6c66c7624e15b842f5b549dc5`; its separate host-validator
recheck at e3e2 is not a new native run or a relabeled original gate. The
accepted seven-case native evidence and retained visual verdicts are recorded
in [issue #670 comment 6124650100](https://github.com/cTux/ae2-crafting-time/issues/670#issuecomment-6124650100).
Build and Gradle tests passed on the tested head ([Build](https://github.com/cTux/ae2-crafting-time/actions/runs/38242890242),
[Gradle tests](https://github.com/cTux/ae2-crafting-time/actions/runs/38242890268)).
The scope was clarified in [PR #690](https://github.com/cTux/ae2-crafting-time/pull/690):
AE2's existing row font is preserved; scale-1 readability, including possible
horizontal-fit regression, remains an unresolved limitation tracked in
[follow-up #689](https://github.com/cTux/ae2-crafting-time/issues/689). All
eight scale-1 readability failures remain failures, with no non-regression
claim. Wiki publication was read back in [issue #670 comment 6123468901](https://github.com/cTux/ae2-crafting-time/issues/670#issuecomment-6123468901).

The 26.1.2 status fixture has 1 billion stored, 2 billion active and 3 billion
pending items. Its 5 billion remaining items produce `⏱ ~6944444:26:40?`.
The reported uniform-font Auto capture starts six GUI pixels outside its cell.
That historical capture records the bug; it is not a matched before image.

Reuse the existing horizontal badge fit for regular and delayed TTC row labels.
Keep the full estimate, symbols and uncertainty marker. Text at or below the
existing width limit keeps its current size. Preserve colors, vertical size,
shadow, optional background and compact amounts. This restores layout without
adding an option or changing time arithmetic, tooltips, chat, packets or saves.
Both renderer families serve the four supported targets; the fit also applies
to Crafting Plan rows through the same renderer.

The later [scale-1 readability change](../../gui-scale-one-readability/spec.md)
uses a wider, single-column native table at effective GUI scale 1. There,
unscaled text takes priority over horizontal compression; an exceptional line
that still exceeds its cell shows an ellipsis preview and its full value on
hover. This does not alter the completed #670 evidence above.

| Criterion | Required result |
| --- | --- |
| C1 | Regular and delayed TTC labels use the existing 90-font-pixel horizontal limit; short labels and unrelated keys keep their behavior. |
| C2 | The existing 1B/2B/3B status fixture checks the exact full estimate and its observed bounds inside its originating row cell, alongside compact amount containment. |
| C3 | For #695, verify native-default and corrected width-probe text at effective scales 1, 2 and Auto. Exercise actual Force Unicode separately, recording requested and effective scales. Compile and check production remapping for all four release targets; focused native verification covers Forge 1.20.1 and NeoForge 26.1.2, representing both rendering adapters. Retain the earlier four-target evidence below as historical provenance. The forced-uniform effective-scale-1 glyph failure remains a separate accepted exception under [#649](https://github.com/cTux/ae2-crafting-time/issues/649). For the native control, preserve option state, semantic text, containment and restoration. Its requested-1/effective-2 glyph fragmentation is the known FAIL deferred to [#704](https://github.com/cTux/ae2-crafting-time/issues/704); an option/scale/restoration control PASS is not a glyph-legibility PASS. Native Force Unicode Auto at effective scale 4 remains a readable control. Do not override the font option or GUI-scale setting. Also verify `standard-plan-controls` and `delayed-status` on 26.1.2 NeoForge and `badge-background` on 1.20.1 Forge. |
| C4 | Review retained native PNGs and matching sidecars, recording hashes, actual scale, cell/text bounds and readability. Preserve all eight scale-1 readability FAIL receipts as a known limitation tracked in the [scale-1 readability follow-up #689](https://github.com/cTux/ae2-crafting-time/issues/689). Semantic completeness and containment do not constitute a readability pass. |
| C5 | Shared logic keeps full line/branch coverage, existing component behavior passes, and GuideME/wiki explain the fitted long row text without promising arbitrary-font readability. |

For C3 provenance, six cases have native source
`e3e2c374e53e629d9c3f3e56cf02b0aa9c7d2f1e`. The 1.20.1 Forge
`standard-status-controls` archive retains source
`52240e6e7efce3d6c66c7624e15b842f5b549dc5`; only a separate host-validator
recheck at the e3e2 head was accepted. The original wrapper and gate FAIL remain
preserved. This validator recheck is not a new native run.

The original #670 scope requested representative GUI scales without naming
scale 1. Its earlier #689 follow-up recorded eight scale-1 readability failures
and made no non-regression claim; keep those historical receipts and verdicts
unchanged. Issue #695 later authorized a separate native-default scale-1
readability correction, currently tracked in [PR #701](https://github.com/cTux/ae2-crafting-time/pull/701).
That newer work does not relabel the eight #689 failures or turn containment
into a readability result.

The native Force Unicode requested-1/effective-2 glyph failures are a separate
accepted deferral under [#704](https://github.com/cTux/ae2-crafting-time/issues/704).
The forced-uniform resource-pack case at effective scale 1, which bypasses the
native Force Unicode scale policy, remains separately deferred under
[#649](https://github.com/cTux/ae2-crafting-time/issues/649). Neither exception
reduces the native-default readability requirement in #695. Arbitrary resource
packs and labels are not covered by a universal readability guarantee.

The original #688 review found scale-1 glyphs about five physical pixels high.
The native vertical transform was unchanged from that investigation, while the
new TTC horizontal compression could have contributed to the historical loss.
That source comparison was not matched before/after image proof and does not
establish whether the behavior was inherited or introduced. Preserve that
uncertainty; the later #695 and #704 evidence records distinct font modes and
their separate visual outcomes.
