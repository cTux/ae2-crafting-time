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

| Criterion | Required result |
| --- | --- |
| C1 | Regular and delayed TTC labels use the existing 90-font-pixel horizontal limit; short labels and unrelated keys keep their behavior. |
| C2 | The existing 1B/2B/3B status fixture checks the exact full estimate and its observed bounds inside its originating row cell, alongside compact amount containment. |
| C3 | Native `standard-status-controls` verifies default and uniform fonts at scales 1, 2 and Auto on all four supported targets. Require full text and cell containment at all three scales, and readable estimates at representative scales 2 and Auto. Also verify `standard-plan-controls` and `delayed-status` on 26.1.2 NeoForge and `badge-background` on 1.20.1 Forge. |
| C4 | Review retained native PNGs and matching sidecars, recording hashes, actual scale, cell/text bounds and readability. Preserve all eight scale-1 readability FAIL receipts as a known limitation tracked in the [scale-1 readability follow-up #689](https://github.com/cTux/ae2-crafting-time/issues/689). Semantic completeness and containment do not constitute a readability pass. |
| C5 | Shared logic keeps full line/branch coverage, existing component behavior passes, and GuideME/wiki explain the fitted long row text without promising arbitrary-font readability. |

For C3 provenance, six cases have native source
`e3e2c374e53e629d9c3f3e56cf02b0aa9c7d2f1e`. The 1.20.1 Forge
`standard-status-controls` archive retains source
`52240e6e7efce3d6c66c7624e15b842f5b549dc5`; only a separate host-validator
recheck at the e3e2 head was accepted. The original wrapper and gate FAIL remain
preserved. This validator recheck is not a new native run.

The original issue requests representative GUI scales without naming scale 1.
This scope preserves AE2's existing row font and requires readability at scales
2 and Auto while retaining scale-1 exact-text and containment checks.
The requester chose to preserve AE2's row font and defer scale-1 readability,
including any regression caused by horizontal fitting, to #689. Matched
before/after readability is unresolved; it is outside this fix's approved
completion gate, and no non-regression claim is made.
Reviewed scale-1 glyphs are about
five physical pixels high and remain insufficiently readable. The native
vertical transform is unchanged from the investigation and documentation bases,
but the new TTC horizontal compression may contribute to the readability loss.
Source comparison is not matched before/after image proof. Track broader
scale-1 row presentation in the [scale-1 readability follow-up #689](https://github.com/cTux/ae2-crafting-time/issues/689), preserve the failed
receipts, and make no claim of scale-1 readability. Arbitrary resource packs and labels are also not
covered by a universal readability guarantee.
