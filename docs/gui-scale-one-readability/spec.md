# Readable text at GUI scale 1

Status: in-progress. Implementation and native CodexVM verification are tracked in
[PR #701](https://github.com/cTux/ae2-crafting-time/pull/701). Scale-1 Plan,
Status, CPU, and interaction evidence now covers Forge 1.20.1 and NeoForge
26.1.2. The deterministic Forge Status glyph contract is qualified for its
reviewed render environment. The focused Forge checks and NeoForge native base
suite passed. Final review and current-head CI remain open.

Scope: Native Crafting Plan/Status row text and CPU-card TTC at effective GUI scale 1.

Issue: [#695](https://github.com/cTux/ae2-crafting-time/issues/695)

Planning: [Implementation plan](implementation-plan.md)

The presentation policy is settled below. The user selected CodexVM for runtime
verification and accepted native-GPU behavior as unverified. Completion records
that limit without describing VM evidence as native-hardware proof.

## Behavior

At effective GUI scale 1, draw native crafting-table text at one physical pixel
per font pixel. Reuse the table's existing 203-pixel width as one wide column,
with 54-pixel cells and a one-pixel border. Keep its existing height and show as
many complete rows as fit (two in both supported tables). The height contains
all five supported Plan lines, including a stored-variant warning and TTC when
compact amounts is off. Scroll through the remaining entries. Scale 2 and
larger retain their existing layout. Changing scale while a screen is open
updates rendering, scroll range, and hit testing together.

Keep every amount and status line, its order, color, symbols, and estimate
meaning. Keep the item icon, tooltips, sorting, selection, and Ctrl-click
behavior. Do not change the player's GUI-scale setting. The correction follows
the existing row/CPU feature switches; it adds no setting.

At scale 1, CPU TTC uses an unscaled font and the existing card bounds. Reserve
the name line for it and shorten the CPU name as already supported. If the full
TTC cannot fit, show a readable ellipsis preview and include the complete TTC in
the native CPU tooltip. This narrow scale-1 exception supersedes the
[CPU specification's](../cpu-list-total-ttc/spec.md) requirement to fit every
digit directly in a card. Never squeeze the preview below one physical pixel
per font pixel. Other card content and all larger-scale behavior stay intact.

The wider table fits the existing long-TTC regression without compression. For
an exceptional resource-pack/localized string wider than its available cell,
use the same readable preview/full-tooltip rule. This supersedes horizontal
compression only at scale 1; [long-TTC containment](../player-controls-and-integrations/long-ttc-containment/spec.md)
continues to apply at every scale. Full hover text must be added where it does
not already exist; a tooltip containing only the CPU name is insufficient.

## Acceptance

| ID | Required result |
| --- | --- |
| R1 | Scale-1 amount, numeric TTC, Instant, and status text have intact, distinguishable strokes in retained PNGs. Font pixels are not fractionally reduced. |
| R2 | All table description lines remain visible, inside their cell, clear of the icon and adjacent rows. The existing long-TTC fixture retains its full text when it fits the wide cell. |
| R3 | Every entry remains reachable by scrolling; native item hover and TTC Ctrl-click resolve the same entry before/after a scale change and at nonzero scroll. Sorting and selected CPU identity are unchanged. |
| R4 | CPU TTC remains inside its card, clear of progress/job content; a shortened preview has a complete matching tooltip including uncertainty/symbols. Name tooltip and selection still work. |
| R5 | Scale 2/Auto, default/uniform fonts, background on/off, shadow on/off, and disabled TTC options keep their documented behavior. |
| R6 | Repeated settled-frame captures and a contemporaneous VM framebuffer view distinguish runtime loss from screenshot timing. Original failed captures remain unchanged. |
| R7 | A qualified exact-pixel contract covers deterministic text; deleting an unmasked foreground stroke fails it. Semantics or bounds alone cannot pass legibility. |
| R8 | Both rendering adapters compile and pass focused native startup/UI checks. Native-hardware investigation has a recorded authorized result or explicitly accepted scope decision; VM evidence is never described as native-hardware proof. |

No packet, persisted-world, estimator, addon-specific layout, or dependency
change belongs to this fix. All four release targets receive shared behavior;
runtime checks cover the two Minecraft rendering adapters.
