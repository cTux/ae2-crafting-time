# Compact hover numbers

Status: ready-to-implement

Scope: Compact throughput in Crafting Time hover details, with unabbreviated
throughput in the existing Ctrl+click chat action.

Issue: [#677](https://github.com/cTux/ae2-crafting-time/issues/677)

Planning: [Reviewed implementation plan](implementation-plan.md) and
[technical design](technical-design.md).

Hold: Implementation remains in backlog until starting it is authorized.
The user approved the [final issue text](issue-preview.md) and throughput-only
scope on 2026-10-08. The issue, specification, design and plan agree.

## Goal and scope

Large learned rates currently make the throughput line hard to scan. The supplied
screenshot shows long item-per-tick and item-per-second values in a Crafting Plan
tooltip. This is a presentation request, not proof of an OmniSequence defect.

Apply this change to Crafting Time's throughput line in native Crafting Plan and
Crafting Status hover details. Keep native AE2 quantities, foreign tooltip lines,
resource names and identifiers intact. The screenshot's native `To Craft` amount
therefore remains full-sized under this approved scope. No new throughput line is
added to Crafting Tree or ME Requester.

## Player behavior

- Add **Compact hover numbers** in **Options > Client > Displays**, default on.
  On abbreviates throughput; off restores two-decimal hover text for valid
  positive rates. Both modes show `?` for nonpositive or nonfinite rates.
  This choice is independent of **Compact crafting amounts** and detailed-tooltip
  visibility. Done applies and saves, Cancel discards, reset restores on, and the
  choice survives relaunch without changing other options.
- Use decimal suffixes `k`, `M`, `B`, `T`, `P`, `E` for powers of 1,000 through
  10^18. These suffixes and the decimal point are the same in English and Ukrainian.
  Keep translated resource units and `/t` and `/s` meanings unchanged.
- Values below 1,000 retain the existing two decimals, except a positive value
  below 0.005 displays `<0.01` rather than zero. Tier-zero values never promote:
  `999.995` renders `1000.00`. At or above 1,000 use at most
  two decimals, round half up and remove trailing zeroes. Promote to the next
  suffix when rounding would produce 1,000. At the E ceiling use scientific
  notation with up to three significant digits when the scaled value rounds
  to 1,000. Prefix abbreviated/scientific values with `~` so they cannot be
  mistaken for full values. Nonpositive or nonfinite throughput displays `?`.
- Ctrl+click keeps the existing public server-chat action and selected output.
  Report both per-tick and per-second rates without suffixes, exponent notation,
  grouping, or fixed two-decimal rounding. Preserve the existing exact integer
  requested amount. Keep sample averages, latest sample, confidence and accuracy
  semantics unchanged. Chat formatting does not depend on the client's toggle.
- Here “full” means the decimal representation of the stored floating-point rate.
  It does not promise arbitrary-precision measurement or recover digits lost
  before formatting. Chat is still subject to its existing enable switch, eligible
  context, permission checks and cooldown. No click action is added for disabled
  chat or rows without stats.
- Keep the existing Ctrl+click hint; update its localized description to say that
  it publishes full throughput values to chat. Respect control-hint visibility.

| Input rate | Compact hover |
| --- | --- |
| 0.004 | `<0.01` |
| 1 | `1.00` |
| 999.99 | `999.99` |
| 999.995 | `1000.00` |
| 1,000 | `~1k` |
| 1,234 | `~1.23k` |
| 999,995 | `~1M` |
| 92,233,720,368,547,760 | `~92.23P` |
| 1,844,674,407,370,955,300 | `~1.84E` |
| 10^21 | `~1e21` |

These are formatting examples, not claims that this task reproduced those rates.

## Compatibility and non-goals

Cover Forge 1.20.1, Fabric 1.20.1, NeoForge 1.21.1 and NeoForge 26.1.2.
Items, fluids, chemicals and mana retain their current normalized units.
Keep server authority, packets, saved samples, calculations, TTC sorting, reset,
provider locate and chat audience unchanged. Only the local display choice adds
persisted state; missing keys get the new default.

Do not shorten the sample list, change `<0.001 ticks`, compact times or accuracy
percentages, reformat native AE2 amounts, add configurable precision, or change
row layout. This does not fix arithmetic overflow in
[#616](https://github.com/cTux/ae2-crafting-time/issues/616) or row containment in
[#670](https://github.com/cTux/ae2-crafting-time/issues/670). If #616 blocks a real
extreme-rate fixture, record that dependency and leave that acceptance gate open.

## Acceptance criteria

| ID | Observable result |
| --- | --- |
| H1 | Shared text/component tests cover the complete table for both native screen call paths, including scientific notation and the E ceiling. Native-screen runtime captures cover ordinary through exascale rates; synthetic scientific and invalid inputs are component-test evidence only. |
| H2 | Ctrl+click reports the selected output's unchanged integer amount and both full rates, including a rate that would lose digits under two-decimal rounding. No compact suffix leaks into chat. |
| H3 | Units, sample counts, sample durations, confidence, accuracy and native/foreign tooltip content keep their existing meaning and order. ITEM/MB/MANA component and normalization boundary tests cover non-item resource semantics; runtime throughput captures use items. |
| H4 | On/off, Done, Cancel, resets, missing config key and relaunch work independently of compact crafting amounts and detailed-tooltip visibility. |
| H5 | Sorted/scrolled rows keep click identity; disabled chat, missing stats, reset, network isolation and cooldown retain their behavior. No packets or saved samples change. |
| H6 | English and Ukrainian placeholders match; guide and wiki explain abbreviated hover, full stored rates and public Ctrl+click chat. Four-target builds and reviewed UI evidence pass, with no new overlap caused by throughput text. |

The [implementation plan](implementation-plan.md) maps every criterion to checks.
