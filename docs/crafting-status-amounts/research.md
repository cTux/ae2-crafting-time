# Compact amounts research

Research date: 2026-09-23. Scope: source inspection and format design for
[#438](https://github.com/cTux/ae2-crafting-time/issues/438).

The user's follow-up selects positional `4/10/200`, missing-category `-`, and
single-category `A10`/`C10`/`S10`, with TTC background and text color. This resolves
the original issue's format comparison. It does not supply runtime evidence.

## Source findings

Repository baseline inspected: `6745f510b5d63a2ccd73c3093a8155c5cc5fa089`.
The current [shared status mixin](../../shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingStatusTableRendererMixin.java)
appends visible TTC at `getEntryDescription` RETURN and extends a separate
tooltip method. [Badge classification](../../shared/src/main/java/com/ctux/ae2craftingtime/core/CraftingRowState.java)
and the two existing renderer adapters supply a reusable drawing path.

The following upstream source revisions were retrieved and inspected through
the GitHub API; these are source checks, not claims of executing their artifacts.

| AE2 target/version | Pinned renderer source | Result |
| --- | --- | --- |
| Forge 15.4.10 | [b4b08d9](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/client/gui/me/crafting/CraftingStatusTableRenderer.java) | Mutable description; stored/active/pending getters; SLOT values; separate FULL tooltip |
| Fabric 15.4.10 | [2700593](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/270059383037770acdcd13a6884436ba2211984a/src/main/java/appeng/client/gui/me/crafting/CraftingStatusTableRenderer.java) | Same quantity and tooltip contract |
| NeoForge 19.0.24 | [330e163](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/330e16349549e9976f0891cc37291a6407f6599a/src/main/java/appeng/client/gui/me/crafting/CraftingStatusTableRenderer.java) | Same quantity and tooltip contract |
| NeoForge 19.2.17 | [79ee2c7](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a/src/main/java/appeng/client/gui/me/crafting/CraftingStatusTableRenderer.java) | Same quantity and tooltip contract |
| NeoForge 26.1.10-beta | [3a051bb](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/3a051bb473de0b8fd329b39db4262f731d17e7e5/src/client/java/appeng/client/gui/me/crafting/CraftingStatusTableRenderer.java) | Same contract; upstream client source moved to `src/client` |

The [19.0.24 table](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/330e16349549e9976f0891cc37291a6407f6599a/src/main/java/appeng/client/gui/me/crafting/AbstractTableRenderer.java)
and [26.1.10-beta table](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/3a051bb473de0b8fd329b39db4262f731d17e7e5/src/client/java/appeng/client/gui/me/crafting/AbstractTableRenderer.java)
both use 67-by-22 cells and half-scale right-aligned text. They center the text
block using the description list's length. Fewer description lines therefore
need no cell-height rewrite. Tooltips, colored cell backgrounds and hit bounds
are separate methods/paths; preserving them is feasible but still needs tests.

The implementation can keep quantity composition in the shared mixin. Drawing
requires small changes in the already-existing older/newer API adapters. Merely
clearing the returned list would erase other mods' additions; selective native
component replacement with a fallback is the planned compatibility boundary.
No ordering guarantee exists for arbitrary third-party return injectors.

## Format comparison

| Candidate | Example | Decision |
| --- | --- | --- |
| Positional plus single-category prefixes | `4/10/200`, `A10` | Selected by the user; compact, stable order and explicit absent positions |
| Three labeled values | `A4 C10 S200` | More repeated text; conflicts with the requested multi-category format |
| Icons or state-specific colors | Three icons with values | Needs extra glyphs or a color legend; conflicts with the requested common TTC text color |
| Total with a breakdown | `214 (4/10/200)` | Wider and adds an unnecessary aggregate that can imply progress |

The [format mockup](format-mockup.svg) illustrates all presence combinations.
It is a schematic, not a Minecraft screenshot or measured font/readability
result. The specification's rule table is authoritative.

## What is proven and what remains

| Question | Evidence/state |
| --- | --- |
| All required presence combinations defined? | Yes: specification covers all eight masks, including trailing absent scheduled and all-zero |
| Shared source hook viable across the inspected versions? | Yes at source level; runtime application remains unverified |
| Full quantity tooltip can remain intact? | Separate FULL formatting path confirmed; future regression check required |
| Background and color reuse possible? | Existing badge registry, renderer adapters, client options and TTC components confirmed |
| Default/wide font, GUI scale and large-value readability? | Not measured; width-bound design and explicit runtime gate provided |
| Fractional fluid and addon-key appearance? | Delegated to native key formatter by design; actual visual output unverified |
| English/Ukrainian text? | Symbols and proposed translated legend specified; no runtime localization claim |
| Other mods' mixin ordering? | Conservative fallback designed; actual combinations unverified |
| Prototype smoke on all four targets? | Not run in this documentation-only task; required before feature completion |

See the [design](technical-design.md) and [implementation plan](implementation-plan.md)
for the exact checks. Keep the research issue open after the planning merge.
