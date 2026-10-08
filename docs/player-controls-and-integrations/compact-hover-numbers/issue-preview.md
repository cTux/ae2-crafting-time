# Final issue text for approval

This is the proposed replacement for the initial tracking text of #677.
It has not been applied. Approval is required by the repository planning skill.

Title: Compact large hover numbers and retain full values in chat

---

## Problem

Large throughput numbers make Crafting Time hover details hard to read. The
reported Crafting Plan screenshot shows long per-tick and per-second rates.

## Result

Shorten Crafting Time throughput in Crafting Plan and Crafting Status hover
details with decimal k/M/B/T/P/E suffixes and up to two decimal places. Mark
abbreviations with `~`, promote a rounded 1000 to the next suffix, and use
scientific notation beyond the E range. Keep small positive rates visible.

Add a default-on Compact hover numbers client display option. Turning it off
restores the current two-decimal hover format. It is independent of Compact
crafting amounts and detailed-tooltip visibility.

Ctrl+click keeps the existing public chat action and reports both per-tick and
per-second throughput without abbreviations, exponent notation or fixed
two-decimal rounding. Keep the exact integer requested amount. Full throughput
means the decimal value stored by the profiler, not arbitrary-precision data.

## Boundaries

This proposed scope covers Crafting Time throughput only. Keep native AE2
quantity lines, foreign tooltips, sample durations/list, accuracy, TTC calculations,
sorting, reset, packets, saved samples and chat audience unchanged. No new
throughput UI is added to optional mods. Cover all four supported targets and
English/Ukrainian text, GuideME and the GitHub wiki.

## Acceptance

- H1: Formatting boundaries, tiny/invalid rates and extreme finite values match
  the specification on both native screens.
- H2: Ctrl+click retains the integer amount and both full stored rates.
- H3: Units, samples, confidence, accuracy and native/foreign content stay intact.
- H4: Toggle, persistence, Done, Cancel, resets and independent settings work.
- H5: Click identity, server context, disabled chat, reset and cooldown stay intact.
- H6: Four-target builds/CI, reviewed UI evidence, locale parity and guide/wiki
  updates pass.

## Documentation

- [Specification](https://github.com/cTux/ae2-crafting-time/blob/codex/compact-hover-number-plan/docs/player-controls-and-integrations/compact-hover-numbers/spec.md)
- [Technical design](https://github.com/cTux/ae2-crafting-time/blob/codex/compact-hover-number-plan/docs/player-controls-and-integrations/compact-hover-numbers/technical-design.md)
- [Implementation plan](https://github.com/cTux/ae2-crafting-time/blob/codex/compact-hover-number-plan/docs/player-controls-and-integrations/compact-hover-numbers/implementation-plan.md)
- [English and Ukrainian player copy](https://github.com/cTux/ae2-crafting-time/blob/codex/compact-hover-number-plan/docs/player-controls-and-integrations/compact-hover-numbers/player-documentation.md)

## Related work and delivery

#616 owns throughput/TTC arithmetic overflow. If that issue blocks the real
extreme-rate fixture, resolve it before claiming end-to-end numeric acceptance;
formatting tests alone do not prove profiler correctness. #670 owns row text
containment and is separate.

Keep backlog until implementation is authorized. The documentation PR does not
implement or close this issue. Follow the repository's conventional commit,
hook-created PR, then local checks and current-head CI workflow. Do not mark the
feature finished until its implementation acceptance gate is met.
