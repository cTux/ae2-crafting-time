# Completed Craft Total TTC Technical Design

This design implements the [specification](spec.md) for
[#325](https://github.com/cTux/ae2-crafting-time/issues/325).

## Current behavior and cause

The server sends a total TTC scoped to the selected crafting CPU. The client
stores it in `ClientStatsCache`, and both `CraftingCPUScreenMixin` variants read
that cached value while composing the Crafting Status title.

Status rows request fresh statistics only while they have outputs to inspect.
After completion, AE2 may send an empty list or retain rows with zero active and
pending amounts. Neither form guarantees another total-TTC packet, so the last
cached value can remain available and the title keeps drawing it.

## Visibility rule

The status screen is the authoritative presentation boundary. Before formatting
the cached total, inspect `CraftingStatus#getEntries()` and require at least one
entry where:

```text
activeAmount + pendingAmount > 0
```

If no entry has remaining work, clear the screen's pending title component and
return AE2's unmodified title. This covers both empty completed views and AE2
versions that retain a final zero-amount row.

Do not erase learned stats or add a request. The cached total remains scoped by
CPU context, while current AE2 status decides whether it is meaningful to draw.
The next active status update can use a fresh server snapshot through the
existing request path.

## Code seams

- `shared/src/mc1201/.../mixin/CraftingCPUScreenMixin.java`: apply the visibility
  guard for Minecraft 1.20.1 and 1.21.1.
- `shared/src/mc2612/.../mixin/CraftingCPUScreenMixin.java`: apply the same guard
  for Minecraft 26.1.2.
- `shared/src/testDriver1201/.../StandardAe2Scenario.java`: make the shared
  `craft-lifecycle` completion observation reject a remaining header TTC. This
  driver is reused by every release-matrix target.

No server, packet, cache schema, mixin registration, translation, or persistence
change is required.

## State and failure behavior

- `status == null` continues to suppress the total.
- Positive remaining work preserves the existing cached-total formatting,
  width check, badge placement, and color.
- Missing or stale cached data still produces no total through the existing
  `OptionalLong` path.
- A completed status cannot render a cached total, even if no clearing packet
  arrives.

## Verification map

| Acceptance area | Proof |
| --- | --- |
| Running total remains | Existing `craft-lifecycle` running-status observation |
| Empty completed status hides total | Extended completion assertion and retained screenshot |
| Retained zero-amount rows hide total | Visibility predicate uses amounts, not list emptiness |
| All supported targets | Shared scenario runs through every release-matrix client |
| No unrelated behavior changes | Targeted builds plus existing lifecycle checks |

## Rejected alternatives

- Clearing all CPU cache state on completion would discard unrelated waiting
  and block state and still require detecting completion.
- Sending a special clearing packet adds network work for a presentation rule
  already answered by AE2's status amounts.
- Checking only for an empty entry list misses AE2 versions that retain a final
  zero-amount row.
