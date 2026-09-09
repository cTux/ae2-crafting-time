# Total TTC in the Crafting CPU list

Status: planned; this PR contains documentation only.
Tracking issue: [#324](https://github.com/cTux/ae2-crafting-time/issues/324).

## Goal

Compare the remaining time of running crafts without selecting every CPU.
Show each CPU's total time to craft (TTC) inside the top-right corner of its
card in the left Crafting CPUs list, as requested in the reference screenshot.

## Player behavior

- Show the CPU's own remaining-job total, including for unselected CPUs. Use
  the same estimate and formatting as the Crafting Status title: `~39s`,
  `~1:01`, or `~1:02:15`. Keep the title badge.
- Preserve the current critical-path estimate and its partial-data behavior.
  This is neither elapsed time nor the final output's individual row estimate.
- Idle CPUs, unknown estimates, and zero totals have no badge, matching the
  title's existing omission rules. A stalled job keeps whatever estimate the
  title would show; do not introduce warning labels in CPU cards.
- Use the title's text color, shadow, and dark badge background. Align the badge
  to the card's top-right interior. Keep the card size, order, selection tint,
  click area, job icon, amount, bottom progress bar, and scrollbar behavior.
- Reserve space for TTC on the name line. Shorten an overlapping name with an
  ellipsis; the existing CPU tooltip still shows the full name. Fit long time
  values without dropping digits or spilling outside the card.
- Refresh visible CPUs without selecting them. The selected card and title use
  the same received value. Allow normal network latency; hide an expired value
  instead of presenting it indefinitely as current.
- Scrolling, CPU removal, job completion/cancellation/replacement, changing
  networks, closing the screen, and reconnecting must not transfer a cached
  total to a different CPU or job. Removed or idle rows stop showing the badge
  as soon as the client observes that state; otherwise the next successful
  refresh replaces the estimate.

## Compatibility and boundaries

Cover all four release rows: Forge 1.20.1, Fabric 1.20.1, NeoForge 1.21.1,
and NeoForge 26.1.2. Integrated and dedicated servers use the same server-owned
estimate path. Native CPU cards supplied by supported addons inherit the badge
when the existing profiler knows their CPU scope. Unknown addon CPU scopes show
no estimate. Separate addon screens are outside this change.

Reuse existing TTC text and the CPU tooltip; no new translated wording or
configuration is needed. Preserve English and Ukrainian support through static
resource and layout checks, including long Cyrillic CPU names. Runtime smoke
uses English (`en_us`) only, following the
[shared smoke policy](../automated-ui-testing/spec.md#smoke-policy). This feature
adds no save data and does not reconstruct estimates for jobs restored without
a retained runtime dependency graph.

## Non-goals

Do not change TTC math or learning, sort CPUs by TTC, change item-row statuses,
add interactions/tooltips/settings, change crafting execution, create a network
monitor, or publish a release. This planning PR does not implement the feature
and does not close the implementation issue.

## Acceptance criteria

| ID | Observable result |
| --- | --- |
| A1 | Three busy CPUs with different totals show their own badges without being selected first. |
| A2 | Selected card and title show identical formatted totals from the same snapshot; title placement remains unchanged. |
| A3 | Idle, unknown, zero, and expired values produce no badge; partial/stalled estimates retain title semantics. |
| A4 | Badges fit at the top right on selected and unselected cards; long names/times, English/Ukrainian text, and GUI scales do not overlap other content. Static resource/layout checks preserve both languages; runtime UI evidence uses `en_us` only. |
| A5 | Scrolling, list reordering/removal, same-output job replacement, completion/cancellation, menu reopen, network changes, and reconnects never reuse another context's total. |
| A6 | Requests only read CPUs from the requesting player's current menu/network; malformed, excessive, or stale requests cannot leak other grids or create unbounded work. |
| A7 | All four targets build and pass the focused regression tests; integrated and dedicated-server UI evidence demonstrates A1-A5. |

See the [technical design](technical-design.md) and
[implementation plan](implementation-plan.md).
