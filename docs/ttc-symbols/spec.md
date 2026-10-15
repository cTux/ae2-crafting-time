# TTC symbols and tooltip headings

Status: ready-to-implement

Scope: Presentation and a default-On client visibility option for [#450](https://github.com/cTux/ae2-crafting-time/issues/450).

Planning: [Technical design](technical-design.md) and [reviewed implementation plan](implementation-plan.md).

The user authorized implementation, coverage of every existing TTC status, a
configuration option enabled by default, and no smoke testing. These decisions
supersede the original proposal's open palette and required runtime campaign.
Actual Minecraft glyph appearance remains unverified.

## Time decoration

When Show emoji is On, every displayed time value gets exactly one leading ⏱
(U+23F1): rows, CPU cards, totals, tooltips, chat, addon surfaces, durations,
typical times, sample tick timings, signed timing error and latest-job timings.
Preserve values, units, approximation and uncertainty: `~12:34?` becomes
`⏱ ~12:34?`. Each duration on a compound line gets its own symbol.
Rates such as items/s, ratios, coverage and counts are not displayed durations.
Missing data gets its information symbol instead of a numeric time symbol.

The time symbol is aqua independently of the value color. Measure decorated
components and use the available layout; do not silently drop the prefix on
compact surfaces. Use single code points with existing Minecraft fonts, without
emoji selectors or a custom bitmap font.

## Complete palette

| Symbol | Role and placement | Icon color |
| --- | --- | --- |
| ⏱ U+23F1 | Every displayed numeric time | Aqua |
| 🥇 U+1F947 | Confident TTC of at most 5ms: Instant ([#692](../ttc-milliseconds-instant/spec.md)) | Green |
| ⌛ U+231B | Waiting | Yellow |
| ⚠ U+26A0 | DELAYED, low confidence, expired provider link, Stored variant | Gold |
| ⚠ U+26A0 | NO PROVIDER, NO CHANNEL, NO TARGET, INPUT BLOCKED, LOCKED, NO SPACE | Red |
| ⚡ U+26A1 | NO POWER only | Red |
| ℹ U+2139 | Collecting/no data, unavailable history, information/details headings and hints | Aqua |
| ✓ U+2713 | Confirmed successful stats reset | Green |
| → U+2192 | Locate hint/confirmation and throughput heading | Aqua |
| ↻ U+21BB | Recurrent recipe status | Red |
| ⚙ U+2699 | Suggestions heading | Gold |

Every existing TTC status has a symbol in this table. Keep explanations,
suggestions and ordinary bullets readable as text. Keep low-confidence wording
conditional. Keep complete labels and existing label severity colors; neither
color nor icons alone explain a status. Reset success is not a new craft-completion
notification. Waiting and no-data remain alternative states. Deferred pictograms
for lock, box, link and statistics are unnecessary for this implementation.

## Configuration and boundaries

Add `showEmoji = true` to the client configuration, displayed as Show emoji in
Appearance (with a matching Ukrainian label). Existing files without this key
use On. Off restores text-only presentation everywhere, including local chat;
it leaves status visibility, values, colors, badges, shadow and actions intact.
Apply after successful Done, persist through restart, and restore On with reset.
Refresh visible chat from its original components when the setting changes.
Each recipient controls their own view; no server option or network change.

Prefix at presentation boundaries once, including nested components. Preserve
translation keys and placeholder order/count, click/hover actions, text wrapping,
severity, conditional wording and notification frequency. Keep icon styles separate
from adjoining labels/values. Preserve ordinary-row color classification when a
numeric value becomes a styled component. Retain the shared badge backgrounds.

Do not decorate identifiers, saved tags, commands, logs, exports, item names or
units. `StatsChatServer` and both `DelayedChatText` variants build undecorated
server messages; the receiving client decorates only the display copy. The common
blocked-chat translation needs reason-aware decoration, never unconditional lightning.

## Acceptance

| ID | Required result |
| --- | --- |
| A1 | All time surfaces above show one aqua ⏱ per numeric duration with original values, units and uncertainty. Compound and nested messages do not lose or duplicate symbols. |
| A2 | Every listed status and heading uses its selected symbol, with full labels and existing conditions. Non-power faults never use lightning. |
| A3 | Show emoji defaults On, saves/resets correctly, and Off removes decoration from local UI and chat while preserving content and behavior. |
| A4 | Existing colors, badges, shadow, actions, wrapping, rates, identifiers, raw logs and notification behavior are preserved; decorated width is used in layout. |
| A5 | Forge/Fabric 1.20.1, NeoForge 1.21.1 and NeoForge 26.1.2 build with their correct adapters. Both locales, GuideME and published wiki describe the feature and toggle. |
| A6 | Existing unit/component/resource checks and current-head CI provide implementation evidence. No Minecraft smoke is run; default/uniform fonts, GUI scales 2-4, resource packs, glyph baseline and visual overlap remain explicitly unverified. |

No new screenshots are required for this run. Do not present prior screenshots,
GitHub glyph rendering or concept art as evidence of the implemented appearance.

## Time glyph compatibility (#658)

Status: ready-to-implement

Scope: Restore a visible time symbol on the prepared NeoForge 26.1.2 client;
[#658](https://github.com/cTux/ae2-crafting-time/issues/658).

Planning: [Technical design](technical-design.md#time-glyph-compatibility-658)
and [implementation plan](implementation-plan.md#time-glyph-compatibility-658).

This follow-up replaces the baseline's time symbol with ⌚ (U+231A WATCH) on
all supported targets. It supersedes only the U+23F1 choice and the no-smoke
verification limit for this fix. Keep the rest of the palette and presentation
policy. No new option or font resource is needed.

| ID | Required result |
| --- | --- |
| G1 | Every existing numeric-duration surface uses exactly one ⌚ prefix when Show emoji is On. Values, units, approximation, uncertainty and current color boundaries remain unchanged. |
| G2 | Show emoji Off remains text-only. Other symbols, rates, counts, raw server messages and stored data remain unchanged. |
| G3 | On prepared NeoForge 26.1.2, inspect native row, footer/total, tooltip and CPU-card screenshots with default and uniform fonts at GUI scales 2 and 4. The symbol is recognizable, with no missing-glyph box, clipping or overlap. Record effective scale and font state. |
| G4 | A representative Forge 1.20.1 native smoke run confirms the replacement renders and preserves layout. Shared/component checks and all four supported-target builds pass; current-head CI is reported separately. |
| G5 | Current examples and English/Ukrainian player guides use the new symbol consistently; historical reports remain accurate records of U+23F1. |

Existing scenario runners and disposable fixtures provide the verification path.
Extend an existing native scenario only as needed to capture the required font,
scale and surface combinations. Do not add a standalone runner. Arbitrary third-party
font packs are outside this fix's guarantee.
