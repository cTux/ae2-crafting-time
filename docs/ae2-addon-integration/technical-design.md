# AE2 Addon Integration Technical Design

## Native-first layers

### 0. AE2 crafting execution

`CraftingCpuLogicMixin` remains the authoritative path for normal AE2 CPUs. It
records five different facts at their real owners:

```text
CraftingCpuLogic.executeCrafting -> ProfilerBridge.start
CraftingCpuLogic.insert          -> ProfilerBridge.complete
CraftingCpuLogic.trySubmitJob    -> ProfilerBridge.startJob
CraftingCpuLogic.finishJob       -> ProfilerBridge.finishJob
CraftingCpuLogic.tickCraftingLogic -> ProfilerBridge.updateCapacity
```

A subclass gets these hooks only while it inherits the hooked method. An addon
override must be checked method by method. This matters for OmniSequence: it
uses AE2 logic but also redirects part of `executeCrafting`, so another redirect
at the same call site can conflict.

### 1. AE2 crafting service observation

A future `CraftingServiceMixin` may observe:

- successful `submitJob` calls;
- CPUs returned by `getCpus()`;
- `isBusy()` and `getJobStatus()` transitions during `onServerEndTick`.

This is a fallback for job lifecycle, not a throughput profiler.
`ICraftingCPU` does not expose pattern dispatch or accepted output. AE2's
concrete service also keeps a `Set<CraftingCPUCluster>`, so a custom CPU is
visible only if the addon deliberately adds it to the service result or routes
submission through that service.

The observer must key state by CPU object identity and ignore CPUs already owned
by an execution mixin. The state transition logic belongs in pure shared Java;
the mixin should only read AE2 objects and delegate.

### 2. Custom execution adapters

When an addon owns the crafting loop, use one optional string-target mixin for
that loop. The adapter maps its real events to the same five `ProfilerBridge`
calls used by AE2. It must not introduce a second profiler, estimator, cache, or
packet.

Known cases:

- AdvancedAE already uses this layer.
- NeoEco needs hooks around its own CPU logic and pattern-bus dispatch.
- AE2 Lightning Tech needs hooks around its time-wheel CPU logic.
- OmniSequence stays in layer 0 unless runtime proof shows a missing event.

### 3. AE2 key contract

`AeKeyAmounts.normalize` already uses `AEKey.getAmountPerUnit()`. Because that
method delegates to `AEKeyType`, fluids, chemicals, energy, EMC, and future key
types can describe their own units without addon checks.

Applied Botanics uses `botania:mana` with 1,000,000 mana per displayed pool.
The generic milli-unit conversion would lose samples below 1,000 mana. A shared
pure-Java amount helper keeps this resource in raw mana and selects the `MANA`
profile unit. All other keys retain their current normalization. Tooltip and
chat labels come from the same profile-unit translation key.

On history load, convert only `botania:mana` entries recorded as `MILLIBUCKET`
from milli-pools to mana by multiplying each representable positive amount by
1,000. New `MANA` samples and unrelated entries remain unchanged. The named NBT
unit field stays compatible; snapshot protocol versions change on all loaders
because older clients cannot decode the new enum value.

`ProfilerBridge.key` currently identifies a profile with `AEKey.getId()`. Do not
silently change that format. Adding the `AEKeyType` ID would affect saved data,
packet request IDs, cache lookup, and reset messages, so it needs its own format
version and migration if a real collision is found.

### 4. AE2 UI paths

`CraftConfirmTableRendererMixin` and `CraftingStatusTableRendererMixin` are the
behavior seams. They append TTC and tooltip details through
`getEntryDescription` and `getEntryTooltip`. An addon is covered automatically
only when it uses those concrete renderers or a subclass inherits those hooked
methods.

`AbstractTableRendererMixin` decorates TTC lines and calculates row colors. It
does not add TTC to a custom renderer by itself.

An `AEBaseScreen` mixin is not assumed to exist. Add it only if source review
finds one stable method across supported AE2 versions that supplies the key,
amount, row position, and current network/CPU context needed by existing
helpers. Otherwise a small `@Pseudo` screen mixin is safer and more honest.

## Data ownership

The existing boundaries stay unchanged:

```text
server CPU/service hook
  -> ProfilerBridge
  -> CraftProfiler and TtcAccuracyTracker
  -> saved samples and bounded snapshot packets
  -> ClientStats cache
  -> AE2 or addon UI hook
```

The server owns sampling, accuracy, stalls, resets, and persistence. The client
only requests aggregate data and renders it.

## Duplicate and failure handling

- One CPU scope may have one active profiling owner.
- A service observer must skip an execution-instrumented CPU.
- Optional mixins use string targets and tolerate an absent addon, but a present
  supported target must fail CI or runtime verification when its required hook
  no longer matches.
- A failed job clears pending scope without recording successful accuracy.
- Unknown key types use their native amount contract. They are not coerced to a
  guessed addon unit.

## Version boundaries

Optional Forge/NeoForge metadata uses `[minimum,)`; Fabric suggestions use
`>=minimum`. Do not derive maximum versions from the pinned client graph.
The optional-integration validator rejects closed or capped ranges on every
release row. It leaves required Minecraft/loader/AE2 boundaries untouched.
Pins remain exact for repeatable development runs, while named-pack tests use
the explicitly selected pack versions and retain any real API/startup failure.

The hook contract is shared where the AE2 API matches, but every source layer
must be checked:

| Target | Main source layer | Extra concern |
| --- | --- | --- |
| 1.20.1 Forge | `mcCommon` + `mc1201` | Legacy AE2 15 signatures and optional Forge addons |
| 1.20.1 Fabric | `mcCommon` + `mc1201` | No NeoForge-only addon classes |
| 1.21.1 NeoForge | `mcCommon` + `mc1201` + `neoforge` | AE2 19 signatures |
| 26.1.2 NeoForge | `mcCommon` + `mc2612` + `neoforge` | AE2 26/Minecraft identifier changes and fewer optional UI integrations |

## Development-client profiles

`scripts/run-client-versions.json` is the source of truth for development
clients. Each target records:

- the projects installed in both profiles, plus any compatible-profile
  exclusion and its reproduced reason;
- the compatible loader, AE2, and Fabric API versions;
- exact compatible Modrinth version IDs for every top-level and transitive
  project in the locked graph;
- SHA-512-locked compatible and latest files for CurseForge-only dependencies.

Ordinary wrappers select `compatible`. The resolver rejects a compatible graph
when a required project has no lock entry, so a supposedly stable client cannot
silently pull a newer library. Latest wrappers select `latest`, ignore every
version lock, and resolve current target-compatible files from Modrinth and the
loader Maven metadata. CurseForge-only latest files are updated explicitly in
the same matrix because no anonymous version API is available.

Compatible clients keep the existing `versions/<target>/run` directory. Latest
clients use `versions/<target>/run-latest`. Gradle receives the selected game
directory so Forge's resolved-mod repository and every loader's world and
config state stay aligned with the chosen profile.

The release matrix remains authoritative for published targets and platform
dependency metadata. A regression check requires the run-client matrix to have
exactly the same target IDs, but development candidates do not become published
optional dependencies merely by appearing in a run profile.
