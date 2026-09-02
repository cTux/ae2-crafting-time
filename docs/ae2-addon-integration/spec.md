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
- TTC and stall details stay available in addon screens that reuse AE2's
  craft-confirm or crafting-status table renderer behavior.
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

NeoForge 1.21.1 also profiles NeoEco 21.1.1 and Lightning Tech 2.1.0-beta.2
custom CPU execution. Their smoke scenarios must submit a real craft, observe a
new server sample, and show its TTC in the reopened plan. Both remain optional.

### Keys

1. Normalize amounts through `AEKey.getAmountPerUnit()`.
2. Do not switch on addon IDs or concrete key classes when the `AEKey` contract
   gives the required value.
3. Verify profile identity before claiming support for a new key type. If two
   key types can share the same resource ID, handle that as a separate persisted
   data and packet compatibility change.
4. Applied Botanics mana (`botania:mana`) is recorded in raw mana units. Its
   pool-sized display unit must not round small positive samples to zero or
   label mana as mB. This is an explicit exception because AE2's display-unit
   size does not name the underlying mana unit.
5. Preserve existing mana history by converting recorded milli-pool amounts to
   mana (multiply by 1,000). Already-lost fractional precision cannot be
   recovered. Leave other resource histories unchanged.

### UI

1. Reuse the existing craft-confirm and crafting-status renderer hooks, text,
   color, sorting, cache, and request helpers.
2. Count an addon screen as generally covered only when it uses those concrete
   renderers or inherits the exact `getEntryDescription` and `getEntryTooltip`
   methods that own the TTC hooks. Reusing only `AbstractTableRenderer` is not
   enough.
3. Add a common `AEBaseScreen` hook only if one AE2 method supplies enough row or
   tooltip context without addon-specific reflection.
4. Keep a bespoke screen mixin as the last option.

### Compatibility and safety

- Optional-addon runtime metadata uses a minimum supported version with no
  upper cap. A tested development pin is not a maximum supported version.
  Updating an addon must not be refused merely because it is newer than our
  smoke-tested baseline. Keep Minecraft, loader, Java and required AE2 API
  boundaries separate; this does not promise compatibility with every future API.
- Record demonstrated upstream failures as diagnostics, not speculative loader
  caps. Fix our adapter when its hooks change and verify the requested versions.
- Cover all four supported release rows: 1.20.1 Forge, 1.20.1 Fabric, 1.21.1
  NeoForge, and 26.1.2 NeoForge.
- Keep optional mixins tolerant when their target addon is absent.
- Preserve server-owned stats and the existing `networkId + outputId` lookup
  unless a separately planned compatibility migration changes it.
- Do not add loader metadata or a compile dependency until real code needs it.
- Every executable branch added later needs full line and branch coverage.

### Development clients

- Provide one compatible and one latest client for every supported Minecraft
  and loader row.
- Ordinary `run-*` scripts use a fully pinned dependency graph. Its loader,
  AE2, API, addon, library, and tool versions are the lowest full-stack set
  currently known to start and support the planned checks together. It is a
  curated compatibility baseline, not each project's oldest published file.
- `run-*-latest` scripts ignore the compatible pins and resolve the newest
  loader, AE2, optional addon, required library, and test tool available for
  the exact Minecraft and loader target. They are diagnostic clients and may
  fail when an upstream release is incompatible.
- A project may be marked incompatible with the ordinary profile only after a
  reproduced full-stack conflict. It remains in the latest profile with the
  reason recorded in the matrix.
- Keep both profiles in one version matrix. Updating a compatible pin or the
  candidate list must update PowerShell and Bash clients together.
- Keep latest client game data separate from the compatible sandbox so a
  broken dependency cannot rewrite its world, config, or managed mod set.
- A candidate installed for compatibility testing is not automatically a
  supported integration.

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
- Its screen either reaches an existing concrete AE2 renderer hook or has a
  tested optional fallback.
- The addon is absent without mixin errors.
- The supported target rows that publish the addon pass CI and an in-game craft
  that starts, produces output, finishes, and shows TTC.
- Every ordinary run script resolves only its pinned compatible graph, while
  every matching `-latest` script resolves current versions without fallback.
- The four ordinary clients and four latest clients use separate managed mod
  directories, and resolution failures remain visible to the caller.
