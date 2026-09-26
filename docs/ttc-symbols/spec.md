# TTC symbols and tooltip headings

Status: draft

Scope: The presentation proposal in [#450](https://github.com/cTux/ae2-crafting-time/issues/450).

Open decisions: Confirm the remaining non-time palette and placements, and
qualify actual glyphs in Minecraft. GitHub rendering and concept art are not
font-compatibility evidence. This page does not claim implementation.

## Required time decoration

Every displayed time value gets exactly one leading ⏱ (U+23F1), including compact
row overlays, CPU cards, totals, tooltips, chat, addon surfaces, durations, typical
times, and sample timings. Preserve values, units, approximation and uncertainty:
`~12:34?` becomes `⏱ ~12:34?`. Each duration on a compound line gets its own symbol.
Rate units such as items/s are not separate displayed durations.

Keep the symbol aqua independently of the existing value color. Do not remove
the required prefix just to fit a compact surface; measure text and adjust its
available layout. If the glyph cannot be supported, record the incompatibility
and resolve a tested alternative before implementation approval.

## Candidate non-time palette

| Symbol | Role | Proposed icon style |
| --- | --- | --- |
| ⌛ U+231B | Waiting | Yellow |
| ⚠ U+26A0 | Delay or low confidence | Gold; retain label severity |
| ⚠ U+26A0 | Other blocking faults | Red |
| ⚡ U+26A1 | NO POWER only | Red, not a throughput icon |
| ℹ U+2139 | Information or missing data | Aqua; supporting text gray |
| ✓ U+2713 | Confirmed successful reset | Green |
| → U+2192 | Locate hint or throughput | Aqua, optional |
| ↻ U+21BB | Recurrent recipe problem | Red, optional; retain label |
| ⚙ U+2699 | Suggestions heading | Gold, optional |

Defer 🔒 LOCKED, 📦 NO SPACE, 🔗 connection states, and 📊 statistics until
glyph size and meaning are verified. Use ⚠ plus the full blocked label meanwhile.
Do not use icons or color as the sole explanation. A confirmed reset is not a
new craft-completion notification. Waiting and no-data examples are alternative
states, not simultaneous statuses.

The issue also compares ⏲, ◷, ⌚, and 🕒 as possible time glyph alternatives.
Choose one consistently after review; do not alternate by duration. ⌛ remains
reserved for waiting. These are proposals, not verified fallback support.

## Composition and boundaries

Trace shared `TtcText`, both `DelayedChatText` variants, `StatsChatServer`,
and compact/integration renderers. Prefix at the presentation boundary once,
including nested components. `StatsChatMessages` requests chat; it does not
format the server response. Select blocked icons by reason: decorating a shared
blocked translation with lightning would mislabel non-power failures.

Use individually styled components, restoring adjoining label/value styles.
Preserve click/hover actions, translation keys and placeholder order/count,
wrapping, severity, conditional low-confidence wording, and notification frequency.
Do not decorate identifiers, saved tags, commands, logs, exports, item names, or
units. Use existing font/component support; a custom bitmap font is not approved.
Reconcile legibility with the independent text-shadow preference.

## Acceptance and completion

Check all selected characters on Forge/Fabric 1.20.1, NeoForge 1.21.1 and
NeoForge 26.1.2, in English and Ukrainian. Exercise default and forced
Unicode/uniform fonts, GUI scales 2–4, and a font-changing resource pack.
Record missing glyphs, baseline, width, overlap, and readability against dark
tooltips and translucent chat. Retain readable labels if optional symbols fail.

Capture normal, waiting, no-data, delayed, no-power and another blocked tooltip,
reset chat, and provider links. Check all compact/addon time surfaces, exactly-once
prefixing, preserved uncertainty, and no style leakage. Update both locales,
GuideME and wiki presentation guidance when the behavior ships. Link tested
commits, existing text/tooltip checks, current-head CI, and reviewed captures.
