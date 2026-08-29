# AE2 Addon Integration Implementation Plan

Implement one verified layer at a time. Each executable change gets its own
conventional commit and required CI run.

## Phase 1: Prove existing native coverage

1. For every candidate CPU, inspect whether each hooked
   `CraftingCpuLogic` method is inherited, overridden, redirected, or replaced.
2. Record the exact supported mod version and Minecraft/loader row.
3. Test OmniSequence first. Replace the current competing redirect only if an
   in-game craft proves that its inherited path misses `ProfilerBridge.start`.
4. Close candidates that already work through layer 0 without adding code.

Done when a normal output-producing craft starts, records output, finishes, and
shows TTC without duplicate samples.

## Phase 2: Spike service-level lifecycle observation

1. On one custom CPU, verify whether AE2's `CraftingService.getCpus()` returns
   it and whether submission goes through `submitJob`.
2. If it does, extract a small pure-Java busy-state tracker with tests for new,
   busy, finished, cancelled, removed, and duplicate CPUs.
3. Add thin `CraftingServiceMixin` adapters for the supported AE2 API source
   sets.
4. Do not merge the observer if it sees only vanilla `CraftingCPUCluster`
   instances already covered by layer 0.

Done when the spike proves additional real coverage without double-counting.

## Phase 3: Add only required custom CPU adapters

Work in this order because the source investigations already identify the hook
points:

1. NeoEco (`ECOCraftingCPULogic`).
2. AE2 Lightning Tech (`Ae2LtTimeWheelCraftingCpuLogic`).
3. Any later addon whose execution path still bypasses layers 0 and 1.

For each addon:

- reuse `ProfilerBridge` and the AdvancedAE adapter pattern;
- hook actual pattern dispatch, accepted output, finish, and used capacity;
- keep the mixin optional and absent-mod safe;
- add resource membership checks and the closest boundary tests;
- verify cancellation, partial output, parallel work, and a successful craft.

## Phase 4: Verify key types without addon handlers

1. Exercise each candidate `AEKey` with its real `getAmountPerUnit()` value.
2. Confirm the same normalized amount reaches profiling, estimates, snapshots,
   display, and reset lookup.
3. Add no code when the native contract works.
4. If two types produce the same profile ID, stop and plan a separate saved-data
   and packet format migration before changing `ProfileKey`.

Done when every tested key type either works through `AeKeyAmounts` or has a
specific, reproduced contract gap.

## Phase 5: Reuse AE2 UI seams

1. Check whether each candidate screen inherits AE2's table renderer methods.
2. Verify inherited screens through `AbstractTableRendererMixin` without new
   code.
3. Look for one stable `AEBaseScreen` method only after the table path is ruled
   out.
4. Add a bespoke `@Pseudo` mixin only for a fully custom screen or API, starting
   with the player-visible crafting screens.
5. Close candidates such as range boosters or visual tools when they have no TTC
   surface.

## Final compatibility sweep

- Run required CI for every changed supported row.
- Launch each named modpack only through its matching Prism test workflow.
- Test with each optional addon present and absent.
- Check that samples are recorded once, persisted, requested, reset, and shown.
- Update `DEPENDENCIES.md`, loader metadata, and candidate status only for code
  and versions that were actually verified.
