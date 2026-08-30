# Collecting Data Status Technical Design

## Decision

Treat a missing or unusable cached estimate as a display state. Do not add a
server state for it.

The client already knows everything needed to choose the row text:

```text
eligible AE2 row
  -> request existing server stats
  -> cached stats produce an estimate? yes -> show existing TTC
                                    no  -> show Collecting data
```

For the running Crafting Status table, the existing delayed warning still wins
when stats include a stall diagnostic.

## State order

Resolve each row in this order:

1. If its craft amount is not positive, append nothing.
2. Request its stats through `ClientStatsRequests`.
3. If no cached stats exist, append the collecting-data line.
4. If the status row has a stall diagnostic, append the delayed line and stop.
   Do not append an estimate for the same row.
5. If `TimeEstimate.format(...)` returns text, append the normal TTC line.
6. If stats exist but cannot produce a usable estimate, append the
   collecting-data line instead of leaving the row blank.

This keeps invalid or incomplete historical data honest without introducing a
second error label.

## Reuse the current text path

Add one method to `TtcText`, named for example `ttcCollectingData()`. It should
build the existing outer TTC component with a new translated value:

```text
text.ae2craftingtime.ttc = "%s"
text.ae2craftingtime.collecting_data = "Collecting data"
```

The resulting component still has `text.ae2craftingtime.ttc` as its outer
translation key. That matters because both `AbstractTableRendererMixin`
versions already recognize that key and draw the TTC badge background. No new
renderer redirect or translation-key allowlist is needed.

The placeholder uses the normal bold TTC treatment with a neutral gray text
color. It is not added to `TtcColorContext`, because there is no duration to
place on the green-to-red scale.

## Changed code paths

### Crafting Plan

`CraftConfirmTableRendererMixin.ae2craftingtime$appendTtc(...)` already owns the
visible row line. Keep the positive `craftAmount` guard. Request stats, then
append either the existing formatted estimate or the placeholder.

The tooltip remains unchanged. Its existing details and reset hints are enough;
adding another explanation line would repeat the visible state.

### Crafting Status

`CraftingStatusTableRendererMixin.ae2craftingtime$appendTtc(...)` already owns
the running row line. Keep its `activeAmount + pendingAmount` calculation and
guard. Preserve the priority of `DELAYED`, then use the placeholder only
when no formatted estimate is available.

The tooltip remains unchanged until real stats exist. A fake throughput or
sample count would be misleading.

## Unchanged boundaries

- `CraftProfiler` keeps collecting and retaining samples on the server.
- `ProfileStats.reliableEstimate()` stays true only with at least three samples
  and no filtered outlier.
- `TimeEstimate` keeps formatting low-confidence estimates with `?`.
- `StatsRequestC2S`, `StatsSnapshotS2C`, packet codecs, and packet limits do not
  change.
- `ClientStatsCache` keeps representing unknown stats as an absent entry.
- SavedData and its version do not change.
- Sorting and totals keep treating a missing estimate as unknown.

## Files expected to change during implementation

- `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/TtcText.java`
- `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftConfirmTableRendererMixin.java`
- `shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingStatusTableRendererMixin.java`
- `shared/src/main/resources/assets/ae2craftingtime/lang/en_us.json`
- `shared/src/main/resources/assets/ae2craftingtime/lang/uk_ua.json`
- `shared/src/mc1201Test/java/com/ctux/ae2craftingtime/mc1201/TtcTextTest.java`

No loader-specific Java file should change. `mcCommon` supplies the behavior to
all supported targets.

## Layout constraint

AE2 renders table text at half scale in a 67-pixel cell, right-aligned before
the item icon. The exact English phrase is much wider than a normal estimate,
and Ukrainian can be wider still. In-game verification must cover the longest
translation at each supported target's normal GUI scale.

Do not replace AE2's renderer or add a new scaling system for one label. If a
translation overlaps, shorten that translated value while preserving the
meaning. The English discussion wording remains the preferred text.

## Verification design

- Extend `TtcTextTest` to prove the placeholder keeps the outer TTC translation
  key and contains the collecting-data translation component.
- Parse both locale JSON files and compare keys through the repository's
  existing static checks or the smallest equivalent check.
- Let required GitHub CI run the shared and four target test rows after the
  implementation commit.
- In a development client, verify fresh, low-confidence, reliable, reset, and
  delayed rows in both standard crafting windows. Check text bounds visually.
