# Compact crafting-status amounts

Status: ready-to-implement

Scope: One quantity line in the native AE2 crafting-status cell.

Issue: [#438](https://github.com/cTux/ae2-crafting-time/issues/438)

Planning: [Reviewed implementation plan](implementation-plan.md),
[technical design](technical-design.md), and [research](research.md).

The planning review covers consistency and source feasibility. No prototype or
Minecraft readability test has run for this scope. The issue's runtime research
gates remain open; merging these documents does not ship or finish the feature.

## Behavior

Replace the separate Available, Crafting, and Scheduled quantity lines with one
compact line above the existing TTC/status line. The order is always available,
crafting, scheduled. These are separate amounts, not a total or a progress fraction.

| Available | Crafting | Scheduled | Visible quantity line |
| --- | --- | --- | --- |
| 4 | 10 | 200 | `4/10/200` |
| 0 | 10 | 200 | `-/10/200` |
| 10 | 0 | 200 | `10/-/200` |
| 4 | 10 | 0 | `4/10/-` |
| 10 | 0 | 0 | `A10` |
| 0 | 10 | 0 | `C10` |
| 0 | 0 | 10 | `S10` |
| 0 | 0 | 0 | No quantity line |

With two or three positive amounts, keep all three positions and use `-` for
each absent amount. With one positive amount, use its `A`, `C`, or `S` prefix.
Use no spaces around slashes or after prefixes. Non-positive amounts are absent,
matching AE2's existing visibility rule. Do not add the amounts together.

Each value uses AE2's compact formatting for that resource, including its units
and abbreviation rules. Preserve fractional fluids and addon resource semantics;
never treat fluid storage units as item counts. Tooltips retain AE2's full labeled
amounts. Zero categories remain omitted there, as in AE2 today.

## Appearance and controls

- Use the same rounded background, configured color and opacity, padding, and
  shadow as TTC. Keep the quantity line separate from TTC/status.
- Copy the displayed TTC/status line's text color, including fast/slow colors,
  collecting, waiting, delayed, and blocking warnings. When no TTC/status line is
  present, use the configured normal TTC color (`TOTAL`, initially `#E0E0E0`).
  Keep quantity text regular weight, even beside a bold warning.
- Fit the complete quantity line into the existing text area without wrapping,
  dropping a category, or overlapping the icon or adjacent cell. Retain AE2's
  compact values and horizontally shrink only when the measured text exceeds
  the available width. Full amounts stay readable in the tooltip.
- Use the existing status-row display switch: when it is disabled, retain AE2's
  original quantity lines. Add no separate setting in this scope.
- Keep `A`, `C`, `S`, `/`, and `-` stable in English and Ukrainian. Add a localized
  tooltip legend identifying the order and prefixes; retain native localized
  labels and full values. The legend appears even for available-only rows and
  when detailed profiling tooltips are disabled.
- Preserve cell colors, icon, hover area, sorting, selection, scrolling, details,
  reset, provider-locate actions, TTC calculation, and warning precedence.

## Compatibility and boundaries

Cover all four release targets: 1.20.1 Forge, 1.20.1 Fabric, 1.21.1 NeoForge,
and 26.1.2 NeoForge. Native AE2 item/fluid keys and installed addon keys use the
same display contract. No server, network protocol, profiling, persistence,
craft-confirm, Crafting Tree, or ME Requester behavior changes are included.

Preserve other mods' description lines. If the original AE2 quantity components
cannot be identified safely, leave the native quantities intact and continue the
existing TTC behavior. Do not erase unknown text or claim arbitrary mixin
compatibility before testing the actual mod combination.

## Acceptance criteria

| ID | Observable result |
| --- | --- |
| Q1 | All eight rows in the table above match exactly; live transitions update without stale prefixes or placeholders. |
| Q2 | Large item values, fractional fluids, and supported addon keys retain AE2 formatting and full tooltip values without arithmetic overflow. |
| Q3 | Quantity background and RGB match the current TTC/status styling and live configuration; available-only fallback is defined above. |
| Q4 | The complete quantity badge fits the text area with default and wide resource-pack fonts at supported GUI scales; TTC and icon remain readable. |
| Q5 | Native full-value tooltips, translated legend, controls, sort, selection, hover and warning precedence remain correct. |
| Q6 | Disabled status rows restore native quantities; foreign description lines survive; unrecognized native lines take the documented fallback. |
| Q7 | Tests, builds and reviewed English UI smoke evidence cover all four targets; Ukrainian text and format parity pass non-smoke checks. |

The [plan](implementation-plan.md) maps each criterion to its verification gate.
