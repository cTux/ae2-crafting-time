# Compact amounts research

Research date: 2026-09-23. Scope: source inspection and format design for
[#438](https://github.com/cTux/ae2-crafting-time/issues/438).

The user's follow-up selects positional `4/10/200`, missing-category `-`, and
single-category `A10`/`C10`/`S10`, with TTC background and text color. This resolves
the original issue's format comparison. A further follow-up requires an on/off
option. The amended plan adds an independent client Displays switch, default on;
it supersedes the original plan's coupling to the row-TTC switch. Neither
follow-up supplies runtime evidence.

The current `OptionFeature`, `FeatureOptions`, `ClientConfigFile` and
`OptionsScreen` enumerate client-owned switches, default them on, persist them,
and render their toggles. They can supply this option without a custom control.
The generic runtime gate also checks server profiling, so native quantity
formatting must read its local feature switch directly to stay independent.

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

## Implementation verification (2026-09-25)

PR [#524](https://github.com/cTux/ae2-crafting-time/pull/524) implements the scope.
The four compatible-graph `standard-status-controls` runs below used production
and status-driver code at `b71cde15567d97b321b0928fdbeaa96da845fc32`.
Each passed with two distinct client processes and exit code 0. All 36 images
per target were manually reviewed, including ten amount cases, default and
wider-font scales 1/2/Auto, native full-value tooltips, warning colors, option
combinations, and saved-off/relaunch-on states. Image hashes matched their
sidecars. The automatic visual gate remains `REVIEW_REQUIRED` because these
images have no comparison baselines; the manual review found no visual defect.

| Target | Loader / AE2 / Java | Reviewed status archive |
| --- | --- | --- |
| 1.20.1 Forge | 47.4.23 / 15.4.10 / 17 | `build/ui-smoke/archives/20260924T145043363Z-cc8b457e` |
| 1.20.1 Fabric | 0.19.5 / 15.1.0 / 17 | `build/ui-smoke/archives/20260924T145851986Z-b348fe06` |
| 1.21.1 NeoForge | 21.1.251 / 19.2.17 / 21 | `build/ui-smoke/archives/20260924T150713151Z-9b91527f` |
| 26.1.2 NeoForge | 26.1.2.109 / 26.1.10-beta / 25 | `build/ui-smoke/archives/20260924T151042986Z-db9736e5` |

The later driver-only changes add the connected non-operator Options checkpoint,
clear its screenshot cursor, repair a 26.1.2 stale-click assertion, and recover
Forge test-client read interest. They do not change production or status-scenario
rendering. At `35d4baedc80e62b17c5b6c155782af4fee804eaa`, four canonical
`cpu-list-total-ttc` connected runs passed on sealed bundles (bundle fingerprint
`0875F350D1D88C3386D59DDF744971C6137283155D1AB6AA73F051E57FCD94C2`):

| Target | Bundle SHA-256 | Connected archive |
| --- | --- | --- |
| 1.20.1 Forge | `9C32DCF56DF4E4B4367248BA8130E134B9C1EA48A007187797DC360D98A743EC` | `build/ui-smoke/connected-438/forge-final-35d4baed` |
| 1.20.1 Fabric | `2F9563B753185E7EE7B6D1A396869ED8D3E4DEEF9D690321BF990061D31493B5` | `build/ui-smoke/connected-438/fabric-final-35d4baed` |
| 1.21.1 NeoForge | `B86296B213D2DFA59EC086183C882E90E21B205A055436E9E3AEB94412B7E712` | `build/ui-smoke/connected-438/neo121-final-35d4baed` |
| 26.1.2 NeoForge | `6F35886DEDA83C8EF5BE87A863D434841C3270EA0BF0F0D0600C7B853E3E17A9` | `build/ui-smoke/connected-438/neo261-final-35d4baed` |

Each connected run used two clean client phases, a real non-operator server
snapshot, and the native Options controls. Its four reviewed screenshots show
unobscured off/on labels and native/compact saved rows; all 16 image hashes
match sidecars. `status-nonop-options.json` records `editable=false` and both
saves, while `fixture-hashes.json` records an unchanged disposable server.

Formatter, config, locale, fallback, and color tests passed, as did the four
test-driver JAR builds, the Gradle test suite, and the all-version JAR build.
At the tested implementation head, GitHub's `Gradle tests` and `Build all mod
JARs` checks both passed.

The addon-key status fixture at `5309f06774e7d9f9cf50f5c7d50545808bcaee6c`
then passed `standard-status-controls` on all four compatible graphs. Each run
used two client processes with exit code 0. The fixture records loaded addon
keys, captures only real supported key rows, and asserts native AE2 SLOT text,
FULL tooltip values, and badge bounds. No addon key is substituted with an item.

| Target | Addon keys exercised | Reviewed campaign / external archive |
| --- | --- | --- |
| 1.20.1 Forge | Applied Botanics mana and Applied Mekanistics oxygen | `20260925T085927147Z` / `20260925T091053806Z-145c7b3e` |
| 1.20.1 Fabric | Applied Botanics mana | `20260925T091111361Z` / `20260925T091924071Z-8c6e18d1` |
| 1.21.1 NeoForge | Applied Mekanistics oxygen | `20260925T091936623Z` / `20260925T092851706Z-6554f5eb` |
| 26.1.2 NeoForge | Neither addon installed; absence asserted | `20260925T092904184Z` / `20260925T093132343Z-eb0c9536` |

All 152 images were manually inspected and their sidecar SHA-256 values matched.
Original-resolution addon captures show unobscured native SLOT badges of
`4/10/200` mana and `.004/.01/.2` oxygen, with FULL tooltip values of
`4/10/200 pools` and `0.004/0.01/0.2 B`, respectively. The automatic visual gate remains
`REVIEW_REQUIRED` because no baselines exist. In the four status runs, the
`status-relaunch-off/on.png` cursor tooltip covers the compact option label;
the independent connected Q6 Options captures above show both labels clearly,
and the status config and saved-row checks verify relaunch persistence.
The addon fixture changed only the status test driver and smoke validation;
production and connected Q6 code paths stayed the same as the earlier runs.
