# Keep long row estimates inside their cells

Status: ready-to-implement

Scope: Restore native Crafting Plan and Crafting Status row containment.

Issue: [#670](https://github.com/cTux/ae2-crafting-time/issues/670)

Planning: [Implementation plan](implementation-plan.md)

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
| C3 | Native `standard-status-controls` passes default and uniform fonts at scales 1, 2 and Auto on 26.1.2 NeoForge and the other three supported targets. Required changed-selector coverage also passes. |
| C4 | Review retained native PNGs and matching sidecars, recording hashes, actual scale, cell/text bounds and readability. Semantic PASS alone cannot satisfy this criterion. |
| C5 | Shared logic keeps full line/branch coverage, existing component behavior passes, and GuideME/wiki explain the fitted long row text without promising arbitrary-font readability. |

The finite reported estimate must remain readable at the required checkpoints.
This fix does not establish readability for every resource pack or arbitrarily
long label. A failed required visual checkpoint needs correction before closure.
