# Collecting Data Status Implementation Plan

Implement this as one small feature commit. There is no migration or staged
rollout.

## 1. Add the localized TTC value

1. Add `text.ae2craftingtime.collecting_data` to English and Ukrainian.
2. Add `TtcText.ttcCollectingData()` using the existing
   `text.ae2craftingtime.ttc` wrapper.
3. Apply `ChatFormatting.GRAY` while keeping the existing bold TTC treatment.
4. Extend `TtcTextTest` to check the outer TTC key and nested collecting-data
   value.

Done when the new component follows the current TTC styling path without a new
renderer key.

## 2. Show it in the Crafting Plan table

Update `CraftConfirmTableRendererMixin.ae2craftingtime$appendTtc(...)`:

1. Keep the `craftAmount > 0` guard.
2. Request stats through the existing cooldown.
3. Append the normal formatted estimate when available.
4. Otherwise append the neutral collecting-data line.

Do not change tooltips, totals, sorting, cache behavior, or request batching.

## 3. Show it in the Crafting Status table

Update `CraftingStatusTableRendererMixin.ae2craftingtime$appendTtc(...)`:

1. Keep the `activeAmount + pendingAmount > 0` guard.
2. Keep requesting stats through the existing cooldown.
3. Keep `Waiting` above every other display state.
4. Keep `DELAYED` above the estimate and placeholder states.
5. Append the normal formatted estimate when available.
6. Otherwise append the neutral collecting-data line.

Do not add placeholder tooltip statistics.

## 4. Review before the implementation commit

- Confirm only the six files listed in the technical design changed.
- Confirm English and Ukrainian keys match and both JSON files parse.
- Check `git diff --check` and inspect the complete diff.
- Do not change packets, SavedData, `CraftProfiler`, `TimeEstimate`, totals, or
  optional integrations.

Commit with a conventional title. Per repository workflow, do not run Gradle
tests before the commit creates the PR.

## 5. Verify after the PR exists

Read the hook-created PR and its complete diff. Required CI must cover all four
published rows.

In a matching development client, check:

1. A never-crafted plan row immediately shows `Collecting data`.
2. A never-crafted running row shows the same state.
3. One completed sample changes it to a low-confidence estimate ending in `?`.
4. Three clean samples remove `?`.
5. A delayed row still shows `DELAYED`.
6. A pending-only row waiting for its first dispatch still shows `Waiting`.
7. Clearing the stats returns the row to collecting data when it is not waiting.
8. Stored-only, missing-only, completed, and zero-work rows get no new line.
9. English and Ukrainian labels stay inside the table cell and do not cover the
   item icon or adjacent cells.

If the label does not fit, shorten only the translated value. Add renderer
scaling only if a concise translation cannot preserve the requested meaning.

## Done

The feature is ready when CI is green on all supported targets, the two standard
crafting windows pass the state checks above, both locales fit, and no wire or
save format changed.
