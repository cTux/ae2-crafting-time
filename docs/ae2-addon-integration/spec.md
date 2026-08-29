# AE2 Addon Integration Spec

## Goal

Support as many AE2 addons as possible through AE2's own extension points. An
addon should get TTC support without addon-specific code when it reuses AE2's
crafting execution, key, or table UI behavior.

The integration must stay optional. Installing AE2 Crafting Time must not make
any addon required.

## Player outcomes

- Crafts run by compatible addon CPUs contribute the same throughput samples as
  normal AE2 CPUs.
- TTC and stall details stay available in addon screens that reuse AE2's table
  renderer.
- New `AEKey` types use their AE2 unit size without a hard-coded addon list.
- Unsupported custom crafting loops fail safely. They must not create misleading
  samples or crash when the addon is absent.

## Requirements

### CPU profiling

1. Keep `CraftingCpuLogicMixin` as the main execution hook.
2. Treat inherited hooked methods as covered. Treat each overridden hooked
   method as a possible gap until its source is checked.
3. A service-level observer may track submission and busy/idle transitions only
   for CPUs returned by AE2's crafting service.
4. Do not call service observation universal. AE2's concrete
   `CraftingService` stores `CraftingCPUCluster` objects, and `ICraftingCPU` does
   not expose dispatch, accepted output, grid, or level.
5. Add a per-addon `@Pseudo` mixin only when the addon owns a custom execution
   method that AE2 cannot expose.
6. Avoid double recording when a CPU is visible through more than one layer.

### Keys

1. Normalize amounts through `AEKey.getAmountPerUnit()`.
2. Do not switch on addon IDs or concrete key classes when the `AEKey` contract
   gives the required value.
3. Verify profile identity before claiming support for a new key type. If two
   key types can share the same resource ID, handle that as a separate persisted
   data and packet compatibility change.

### UI

1. Reuse the existing table renderer, text, color, sorting, cache, and request
   helpers.
2. Count an addon screen as generally covered only when it inherits the exact
   AE2 method that owns the hook.
3. Add a common `AEBaseScreen` hook only if one AE2 method supplies enough row or
   tooltip context without addon-specific reflection.
4. Keep a bespoke screen mixin as the last option.

### Compatibility and safety

- Cover all four supported release rows: 1.20.1 Forge, 1.20.1 Fabric, 1.21.1
  NeoForge, and 26.1.2 NeoForge.
- Keep optional mixins tolerant when their target addon is absent.
- Preserve server-owned stats and the existing `networkId + outputId` lookup
  unless a separately planned compatibility migration changes it.
- Do not add loader metadata or a compile dependency until real code needs it.
- Every executable branch added later needs full line and branch coverage.

## Not included

- Guessing addon crafting methods from discovered `ICraftingCPU` classes.
- Replacing AE2 renderers.
- Exposing TTC through unrelated addon APIs unless players have a concrete use
  for it.
- Promising support from a mod name or download page without source or runtime
  verification.

## Acceptance criteria

An addon is marked covered only when all relevant checks pass:

- Its CPU path is classified as inherited AE2 execution, service-only
  lifecycle, or custom execution.
- Its key types preserve correct identity and normalized amounts.
- Its screen either reaches an existing AE2 hook or has a tested optional
  fallback.
- The addon is absent without mixin errors.
- The supported target rows that publish the addon pass CI and an in-game craft
  that starts, produces output, finishes, and shows TTC.
