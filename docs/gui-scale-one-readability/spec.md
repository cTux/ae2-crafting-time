# Readable text at GUI scale 1

Status: in-progress. Implementation and native CodexVM verification are tracked in
[PR #701](https://github.com/cTux/ae2-crafting-time/pull/701). Scale-1 Plan,
Status, CPU, and interaction evidence now covers Forge 1.20.1 and NeoForge
26.1.2. The deterministic Forge Status glyph contract is qualified for its
reviewed render environment. The focused Forge checks and NeoForge native base
suite passed. At tested head `7e52d14ff1e37b4601e9d1229d6d3e6034066a66`, the
paired Forge and NeoForge 46-image graphs each recorded 45 PASS and one FAIL:
native Force Unicode fragmented row glyphs at effective GUI scale 2. The option,
requested/effective-scale and restoration control passed; that control result
does not make the glyph review a PASS. The user accepted deferring this distinct
scale-2 legibility case to [follow-up #704](https://github.com/cTux/ae2-crafting-time/issues/704).
Keep this issue in progress until PR #701 merges. Native-GPU behavior remains
unverified.

Scope: Native Crafting Plan/Status row text and CPU-card TTC at effective GUI scale 1.

Issue: [#695](https://github.com/cTux/ae2-crafting-time/issues/695)

Planning: [Implementation plan](implementation-plan.md)

The presentation policy is settled below. The user selected CodexVM for runtime
verification and accepted native-GPU behavior as unverified. Completion records
that limit without describing VM evidence as native-hardware proof.

Font scope: preserve Minecraft’s native default-font behavior and use its real
Force Unicode option. Record requested and effective GUI scales separately.
On the verified native path, requested scale 1 selects effective scale 2; that
run is a scale-2 control, not effective-scale-1 uniform-font evidence. The wide
amount fixture is a width and containment probe: enlarge its intended numeric
glyphs while retaining the native default provider for other characters.

The user accepted deferring resource packs that force the uniform provider at
effective GUI scale 1 while bypassing native Force Unicode scaling. Their known
missing-stroke failures remain unresolved and retained. Correcting the wide
fixture does not fix or pass that configuration. This exception concerns font
rasterization only; ordinary scale-1 amounts, TTC, status labels and their
containment remain required.

Native Force Unicode at requested scale 1 is a separate mode: it selects
effective scale 2. On the tested PR #701 head, the native-font state and scale
control passed, but glyph review failed on Forge 1.20.1 and NeoForge 26.1.2.
The user accepted that effective-scale-2 readability exception under
[follow-up #704](https://github.com/cTux/ae2-crafting-time/issues/704). Preserve
the native option, requested/effective scales, semantic text, containment,
interaction and restoration behavior; do not treat the control PASS as a glyph
PASS or override the font option or GUI-scale setting. The native-default and
corrected width-probe requirements at scales 1, 2 and Auto remain unchanged.

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
| R1 | At effective GUI scale 1 under the approved native default-font behavior, amount, numeric TTC, Instant and status text have intact, distinguishable strokes in retained PNGs. The mod adds no fractional font reduction. Forced-uniform resource packs that bypass the native Force Unicode scale policy are an explicitly accepted, unresolved exception; their old failures remain failures. |
| R2 | All table description lines remain visible, inside their cell, clear of the icon and adjacent rows. The existing long-TTC fixture retains its full text when it fits the wide cell. |
| R3 | Every entry remains reachable by scrolling; native item hover and TTC Ctrl-click resolve the same entry before/after a scale change and at nonzero scroll. Sorting and selected CPU identity are unchanged. |
| R4 | CPU TTC remains inside its card, clear of progress/job content; a shortened preview has a complete matching tooltip including uncertainty/symbols. Name tooltip and selection still work. |
| R5 | Native-default and corrected wide-amount cases keep their documented behavior at scales 1, 2 and Auto, with background/shadow variants and disabled TTC options. Test actual native Force Unicode separately and record option state, requested/effective scales, resource-pack identity and restoration. Its state/scale control can pass while effective-scale-2 glyph legibility remains the accepted FAIL deferred to #704; never count that control as a glyph PASS. Restore the original font option and resource-pack state after the case. |
| R6 | Repeated settled-frame captures and a contemporaneous VM framebuffer view distinguish runtime loss from screenshot timing. Original failed captures remain unchanged. |
| R7 | A qualified exact-pixel contract covers deterministic text; deleting an unmasked foreground stroke fails it. Semantics or bounds alone cannot pass legibility. |
| R8 | Both rendering adapters compile and pass focused native startup/UI checks. Native-hardware investigation has a recorded authorized result or explicitly accepted scope decision; VM evidence is never described as native-hardware proof. |

No packet, persisted-world, estimator, addon-specific layout, or dependency
change belongs to this fix. All four release targets receive shared behavior;
runtime checks cover the two Minecraft rendering adapters.
