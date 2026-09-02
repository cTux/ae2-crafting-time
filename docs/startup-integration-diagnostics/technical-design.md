# Startup Integration Diagnostics Technical Design

Implements the [specification](spec.md). Execution order and proof requirements
are in the [implementation plan](implementation-plan.md).

## Research baseline

Inspected on 2026-09-02 at `b17f7dd` (the fetched `origin/master`), against the
open [issue #193](https://github.com/cTux/ae2-crafting-time/issues/193), which had
no comments. This is source research, not a new runtime compatibility result.

The following repository paths are authoritative for the current implementation:

| Evidence | Finding |
| --- | --- |
| `versions/<target>/src/main/java/com/ctux/ae2craftingtime/mc1201/Ae2CraftingTime.java` and sibling client entrypoints | Register config, networking, key mappings, and server-start callbacks without a startup integration report. NeoForge registration is deferred to its payload event. Registering that listener does not confirm payload registration. |
| [Shared pre-26 mixin config](../../shared/src/mc1201/resources/ae2craftingtime.mixins.json), [1.21.1 config](../../versions/1.21.1-neoforge/src/main/resources/ae2craftingtime.mixins.json), [26.1.2 config](../../versions/26.1.2-neoforge/src/main/resources/ae2craftingtime.mixins.json) | All use `required: false`, `defaultRequire: 1`, and no Mixin plugin. The shared pre-26 list also reaches Fabric; packaging a string target does not establish supported Fabric addon behavior. |
| [Forge build](../../versions/1.20.1-forge/build.gradle) and [extra mixin config](../../versions/1.20.1-forge/src/main/resources/ae2craftingtime-advancedae.mixins.json) | Forge compiles the shared AdvancedAE mixin and registers its extra configuration in the production JAR manifest. AdvancedAE is not NeoForge-only, although Forge has no separate declared optional dependency row. |
| [Core CPU mixin](../../shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/mixin/CraftingCpuLogicMixin.java) | Dispatch, accepted output, submission, finish, and capacity flow into `ProfilerBridge`. Inherited methods share these hooks; an override can bypass an individual hook. |
| [AdvancedAE mixin](../../shared/src/neoforge/java/com/ctux/ae2craftingtime/mc1201/mixin/AdvancedCraftingCpuLogicMixin.java) | String target plus typed optional fields/casts. Submission, finish, and capacity use `require = 0`; one firing hook cannot confirm all five paths. |
| [NeoEco mixin](../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/ECOCraftingCpuLogicMixin.java) and [Lightning Tech mixin](../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/Ae2LtTimeWheelCraftingCpuLogicMixin.java) | Custom stateful execution adapters. NeoEco defers finish during insertion and covers two dispatch signatures. Lightning Tech's reflective `successfulDispatches` lookup currently throws with its cause when incompatible. |
| [Crafting Tree helper](../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/CraftingTreeTtc.java) and its two widget mixins | Reflection failures become null; several casts happen in callers. Old and new package variants use optional injections. Old widget also mutates spacing; both retain per-frame estimate/color caches. |
| [ME Requester mixin](../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/MERequesterScreenMixin.java) | `drawFG` uses required shadows; reflective `getKey`/`getAmount` failures become empty/zero, indistinguishable from legitimate empty requests. |
| [Selected CPU lookup](../../shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/StatsRequestContext.java) | Reflects `advancedAE$advCpu` only after the ordinary CPU is null; missing field currently returns null silently. Network lookup can continue without a selected CPU. |
| [Wireless tooltip hook](../../shared/src/mc1201/java/com/ctux/ae2craftingtime/mc1201/mixin/WirelessTerminalScreenMixin.java) and its [26 copy](../../shared/src/mc2612/java/com/ctux/ae2craftingtime/mc1201/mixin/WirelessTerminalScreenMixin.java) | A shared AE2 `MEStorageScreen` hook filters WCWT/WCT class names and Import Export Card menu interface names. It is not three optional string-target mixins. |
| [Key amounts](../../shared/src/mcCommon/java/com/ctux/ae2craftingtime/mc1201/AeKeyAmounts.java), [dependencies](../dependencies.md), [addon design](../ae2-addon-integration/technical-design.md) | Native key/CPU/provider compatibility often has no addon initializer. Pins include candidates and focused-only profiles; installed does not mean supported. |

The older player-controls design omits newer wireless behavior and describes
AdvancedAE as NeoForge-only. Use the source inventory here for this feature;
correct those existing descriptions when implementing, without changing support.

## Upstream failure boundaries

Mixin's [`@Pseudo` contract](https://raw.githubusercontent.com/SpongePowered/Mixin/master/src/main/java/org/spongepowered/asm/mixin/Pseudo.java)
allows unavailable targets; it does not make a present, incompatible class safe.
[`Inject.require`](https://raw.githubusercontent.com/SpongePowered/Mixin/master/src/main/java/org/spongepowered/asm/mixin/injection/Inject.java)
controls minimum injection matches. Zero allows a missing injection, so neither
class presence nor successful mixin processing proves that handler runs.

[`IMixinConfigPlugin`](https://raw.githubusercontent.com/SpongePowered/Mixin/master/src/main/java/org/spongepowered/asm/mixin/extensibility/IMixinConfigPlugin.java)
can veto a target before application, and exposes pre/post-application callbacks.
These run in the transformer context and must avoid loading game classes.
`postApply` is not per-injection success evidence or runtime health evidence.
There is no rollback contract in that API for partially transformed classes.

[Forge lifecycle documentation](https://docs.minecraftforge.net/en/1.20.x/concepts/lifecycle/)
places side-specific setup on the corresponding physical side and notes parallel
events. [NeoForge's event documentation](https://docs.neoforged.net/docs/concepts/events/)
also separates lifecycle registration from runtime events.
[FabricLoader 0.14.21](https://maven.fabricmc.net/docs/fabric-loader-0.14.21/net/fabricmc/loader/api/FabricLoader.html)
exposes mod containers and physical environment without loading addon classes.
Use each build's actual loader APIs; do not introduce a dependency upgrade.

Decision: add no Mixin plugin, bytecode scanner, global error handler, or changed
injection requirement. Runtime observations meet the reporting requirement and
read-only reflection supplies a bounded recovery seam. A later preflight veto
would need separate evidence for bytecode availability, remapping, every required
member/injection, and zero partial-state leakage; current research does not prove
that boundary across four loaders. No CPU-adapter crash prevention is promised.

## Integration inventory

F = 1.20.1 Forge, B = 1.20.1 Fabric, N = 1.21.1 NeoForge,
X = 26.1.2 NeoForge. These are reporting scopes, not new support declarations.
Every row exists in the fixed catalogue on every build. Other targets report
`skipped: target_not_supported`; on applicable targets evaluate physical side,
presence, then configuration. Actual metadata may still be included for a mod
installed on an unsupported target. No entire mod-list dump is needed.

| Integration / metadata ID | Targets | Capability scope and evidence owner |
| --- | --- | --- |
| Crafting Tree / `ae2ct` | F B N | Client: node estimate/layout, tooltip, details/reset routing; old or new widget mixin plus successful reflection. Package variants are alternatives, not two integrations. |
| ME Requester / `merequester` | F B N | Client: request rows and total; `drawFG` plus valid reflective request reads. |
| AdvancedAE / `advanced_ae` | F N X | Logical server: five custom CPU event hooks, plus independent selected-CPU lookup. Report Forge's packaged adapter despite missing optional metadata declaration. |
| NeoEco / `neoecoae` | F | Logical server: custom CPU events, with normal and FastPath dispatch evidence distinguished. N's development pin does not establish that adapter. |
| Lightning Tech / `ae2lt` | F | Logical server: custom CPU events, including reflective capacity read. N/X pins remain outside this adapter's scope. |
| WCWT / `wcwt` | F N | Client: one-item tooltip, observed after the matching terminal filter. |
| Wireless Terminals / `ae2wtlib` | F B N X | Client: one-item tooltip, observed after WCT filter. |
| Import Export Card / `ae2insertexportcard`, `ae2importexportcard` | F N X | Client: one-item tooltip after the matching menu-interface filter; one canonical integration, retain the actual installed alias/version. |
| Applied Mekanistics / `appmek` | F N | Native AE2 key normalization and shared CPU hooks. |
| Applied Flux / `appflux` | F N X | Native AE2 key normalization and shared CPU hooks. |
| Applied Botanics / `appbot` | F B N | Native mana normalization and shared CPU hooks. Original/fork share ID; metadata cannot reliably distinguish them. N is declared but unpinned. |
| OmniSequence / `molecularmanipulator` | F N | Shared CPU hooks. |
| ExtendedAE / `expatternprovider` | F B N X | Shared provider/CPU and AE2 UI hooks. |
| ExtendedAE-Plus / `extendedae_plus` | F N | Shared CPU and AE2 UI hooks. |
| BM Addon / `bmaddon` | F N X | Shared CPU and AE2 UI hooks. |
| Crazy AE2 Addons / `crazyae2addons` | F | Shared CPU and AE2 UI hooks. |
| MEGA Cells / `megacells` | F B N | Shared CPU and AE2 UI hooks. |
| OMNI Cells / `ae2omnicells` | F N X | Shared CPU and AE2 UI hooks. |
| ProjectCell / `projectcell` | F N | Shared storage/crafting path: CPU and AE2 UI hooks. |
| AppliedE / `appliede` | F N | Shared storage/key/crafting path: CPU and AE2 UI hooks. TPS Fix is an alternative with the same ID. |
| Modern AE2 Additions / `mae2` | F | Shared CPU and AE2 UI hooks. |
| Advanced Peripherals / `advancedperipherals` | F N | Native AE2 submission and CPU hooks; no ComputerCraft TTC API. |
| AE2 Things / `ae2things` | F B N | Native storage/crafting path: CPU and AE2 UI hooks; no bespoke machine UI claim. N is unpinned. |
| Expanded AE / `expandedae` | F N | Native CPU and AE2 UI hooks; annotate focused-only compatibility, never claim coexistence with OmniSequence. |
| Network Analyser / `ae2netanalyser` | F B N X | Compatibility-only: INFO skipped with `no_owned_adapter`; version/presence is informational, not health verification. |
| AEInfinityBooster / `aeinfinitybooster` | F N X | Compatibility-only range behavior: INFO skipped with `no_owned_adapter`; no range test at startup. |

Validate IDs against existing fixtures and actual loader metadata during the
implementation inventory check. Alternative IDs are aliases, not a second
success. Do not guess fork identity from a version string. Required runtime mods
belong in context; transitive libraries, test-driver mods, Thunderbolt, and other
development-only candidates do not become separate supported integration rows.

## State, data flow, and lifetime

Add `core/IntegrationDiagnostics.java` under `shared/src/main/java`: a fixed
catalogue, capability states, deterministic aggregation, bounded reasons, and
one-time transition decisions. Keep it Minecraft-free. Use the existing JUnit
dependency; no framework, service registry, or configurable plugin system.

Add `mc1201/IntegrationLog.java` under `shared/src/mcCommon/java`: the process-local
instance and an SLF4J logger named `ae2craftingtime`. Thin loader entrypoints pass
plain target/environment/version/mod metadata and actual registration results.
Helpers and mixins report fixed capability IDs. Never retain world, player,
screen, CPU, reflected receiver, or throwable objects in the diagnostics state.

```text
loader metadata -> context + fixed catalogue + initial outcomes -> one summary
registration return / real hook return / read-only reflection failure
  -> process-local capability transition -> one ordinary log message
```

Synchronize state transitions and snapshot creation; deduplicate before formatting
or logging. Each fixed capability can go pending -> confirmed, pending -> disabled,
or confirmed -> disabled. Disabled is terminal until process restart. Duplicate
observations are no-ops. A configuration skip may become pending if an existing
config setting is later enabled; do not emit the same transition twice. Track the
finite emitted state set, not arbitrary exception messages or object identities.

Unknown metadata is `unknown`, never a guessed build pin. Escape newlines and
control characters in external version/reason text, cap each at 256 characters,
and retain the first diagnostic for a disabled capability. Log the throwable
directly on the first unexpected failure rather than serializing its stack.
Do not record inventory contents, coordinates, network IDs, player names, or
server addresses. Diagnostic data never enters packets or world saves.

Aggregation precedence: required fatal failure -> failed; any disabled capability
with another confirmed/pending -> partial; only disabled applicable capabilities
-> failed; otherwise any pending -> pending; otherwise any confirmed -> initialized;
otherwise skipped. Side/config skips are excluded from applicable capability
counts. Emit one initial line per integration, then only changed capability
outcomes. The optional summary counts the 26 catalogue rows exactly once.

Use the metadata ID as the canonical integration ID, except Import Export Card,
whose canonical ID is `ae2importexportcard`. Core capability IDs are
`config-registration`, `network-registration`, `key-registration`,
`cpu-submit`, `cpu-dispatch`, `cpu-output`, `cpu-finish`, `cpu-capacity`,
`key-normalization`, `mana-normalization`, and `plan-`/`status-` plus
`row`, `tooltip`, `total`, `sort`, `details`, or `reset`. Custom CPUs reuse the
five `cpu-` suffixes under their own integration ID; NeoEco additionally requires
`cpu-dispatch-fastpath`. AdvancedAE adds `selected-cpu`. Tree uses `layout`,
`node`, `tooltip`, `details`, `reset`; requester uses `request-read`, `row`,
`total`; each wireless entry uses `tooltip`.

CPU/UI/native-key scopes in the inventory depend on all applicable capabilities
in the corresponding group above; mana adds `mana-normalization`. Record Tree
variant evidence separately as `old`/`new`, but either variant can confirm each
logical capability. An unused alternative does not block confirmation. A failed
variant remains visible as degradation even if the other succeeds; disable only
that variant. The finite variant set keeps this bounded without startup probes.

## Loader registration and confirmation

Emit context before our config/network setup. At the end of the common entrypoint,
emit the available-check summary even if client or payload registration is still
pending. It is explicitly `phase=entrypoint_checks`, not "all startup succeeded".
Do not attach it to `ServerStartedEvent`, which repeats between integrated worlds.

- Forge: read `ModList` container versions and `FMLEnvironment.dist`; use the
  Minecraft and Forge metadata entries. Confirm config and channel registration
  only after their existing calls return. Client key mappings confirm after the
  existing client event handler completes.
- Fabric: use `FabricLoader.getModContainer`, metadata version strings, and
  `getEnvironmentType`. Confirm common and client networking in their respective
  initializers; key mapping registration belongs to the client initializer.
- NeoForge: read `ModList`/`ModContainer` metadata and `FMLEnvironment.dist`.
  Constructor listener registration confirms only listener installation. Confirm
  payload registration inside `StatsNetwork.register` after actual registration;
  key mappings confirm in the existing side-restricted client class.

Known required registration exceptions get contextual ERROR then propagate with
the same cause. Do not wrap whole entrypoints in `catch (Throwable)`, or claim a
summary was emitted after an abort. Earlier loader failures retain loader logs.

## Hook evidence rules

Record at existing successful operation boundaries, without changing return
values, cancellation behavior, sample ordering, or gameplay calls:

| Scope | Confirmation needed |
| --- | --- |
| Core/custom CPU | Separate submit, dispatch, accepted-output, finish, and capacity observations after their `ProfilerBridge` call returns. Observations mean the named hook ran, not that all possible recipes passed. NeoEco normal/FastPath dispatch are separate bits; no activity leaves them pending. |
| Core AE2 UI | Separate plan/status row-description and tooltip callbacks, screen totals/sort paths, and details/reset routes. Do not infer them from screen construction. Alternatives for pre-26 status constructor signatures share one sort capability. |
| Crafting Tree | Separate frame/layout, node data/estimate, tooltip, and details/reset routing observations for the active package variant. All reflected members needed by the current operation must resolve and have valid types before confirming it. Missing learned TTC is normal, not adapter failure. |
| ME Requester | Confirm request decoding on a real valid row, row-label path and total path separately. Empty screens, fulfilled requests, and unavailable network stats remain normal; they do not confirm an unexecuted path. |
| Wireless | Attribute confirmation to each matched installed integration after its tooltip is appended; overlapping WCT/card filters can confirm both, never unrelated installed terminals. |
| AdvancedAE selected CPU | Confirm the reflected field access contract only when AdvancedAE is installed and the fallback path is used. Null field value is valid "no selected CPU", not an API mismatch. |
| Native integrations | Report `mode=shared-hooks`. Depend on the matching observed core hook capabilities (CPU, UI, key normalization as listed above). Confirmation says `shared_hooks_observed; addon_job_not_verified`. Never call generic hook observations addon-specific execution proof. For mana also require the actual mana normalization branch to run. |

Native key normalization confirms a successful real call to `AeKeyAmounts`;
generic key-path readiness does not prove the optional mod's resource contract.
Such distinctions remain in the reason even after the shared-hook scope confirms.
Installed native integrations depend on shared capability states rather than
introducing per-addon CPU scanners or speculative class hierarchies.

Existing `enabled`/`showInTree` settings must be reported as capability skips where
they disable behavior. A callback returning because configuration is off does not
confirm enabled functionality. Keep dormant client-process server hooks pending
until an integrated world exercises them. A multiplayer client does not claim
remote server hook activation.

On Forge/NeoForge, registration does not mean the config file has loaded. Leave
configuration-dependent readiness pending until the existing config value can
be read at its normal lifecycle/use point; do not read an unloaded spec in the
constructor for this report. Fabric may report values after its config load
returns. Observation gates use the existing setting at the actual operation.

The following are format examples, not captured runtime evidence:

```text
[ae2craftingtime] integration=ae2ct mod=ae2ct version=1.0.1 state=pending capability=node reason=awaiting_hook
[ae2craftingtime] integration=ae2ct capability=node variant=new state=confirmed reason=hook_and_data_read_observed
[ae2craftingtime] integration=merequester capability=request-read state=disabled outcome=failed action=disabled reason=missing_getAmount
[ae2craftingtime] phase=entrypoint_checks initialized=0 skipped=24 pending=2 partial=0 failed=0
```

The disabled requester example is WARN; confirmations, pending states, and the
summary are INFO. Attach the actual installed version to transition records too;
it is omitted in the shorter examples only for readability.

## Recovery decision table

| Failure boundary | Decision and lost functionality |
| --- | --- |
| Optional mod absent or target/side inapplicable | INFO skip using metadata/string inventory; no new class loading and no recovery needed. |
| Crafting Tree field/method absent, inaccessible, incompatible arguments or result type | WARN and disable that package variant's TTC additions for the process. Original tree remains usable; no TTC badges, modified spacing, extra tooltip, or TTC control routing from that variant. Other integrations and server history stay intact. |
| ME Requester read-only `getKey`/`getAmount` contract fails | WARN and disable our request overlay as a unit. Lose row labels and total; preserve original requester behavior. Do not sum a subset of readable rows. |
| AdvancedAE selected-CPU field contract fails while addon is installed | WARN partial: disable that optional lookup, keep the grid and return null CPU as today. Lose selected AdvancedAE CPU diagnostics, retain network aggregates and CPU profiling. |
| An invoked optional getter throws | Unwrap `InvocationTargetException`. A nonfatal exception confined to one of the read-only boundaries above gets one WARN with original cause, then disables that capability. Propagate any `Error` cause; do not swallow linkage, initialization, VM, or assertion failures. |
| Lightning Tech capacity reflection, any CPU hook, `ProfilerBridge`, core networking or persistence failure | No new recovery. Add context only at an owned boundary and propagate, preserving the original cause. Disabling midway could strand pending samples or leave stale diagnostic state. |
| Missing shadow, wrong method descriptor, redirect conflict, linkage/class initialization, Mixin transform failure | Preserve Mixin/JVM failure. Entrypoint try/catch cannot reliably cover these; neither `@Pseudo` nor `required: false` is a recovery guarantee. |
| Loader dependency validation or a crash in the addon itself | Keep loader/addon diagnostic. Our report may never initialize. No attempt to suppress it or mark the addon healthy. |

Use exact reflection handling for `NoSuchFieldException`, `NoSuchMethodException`,
`IllegalAccessException`, `SecurityException`, `InaccessibleObjectException`,
reflection argument mismatch, and explicit return-type mismatch. These catches
surround only owned reads. Do not catch unrelated render, packet-send, inventory,
or gameplay exceptions. A null receiver/value is classified by each existing
caller's contract, not automatically an error. Expected field lookup fallback
(`getField` then `getDeclaredField`) is one attempt, not a warning for its first
miss. Select methods by compatible argument types as well as arity; an ambiguous
match is an incompatibility, not an arbitrary invocation.

For Crafting Tree, update all callers of `readField`/`invoke`/`call` together.
Resolve and validate data before mutating spacing, lines, caches, or click state.
On failure clear the variant's estimate/color caches and restore old-widget base
spacing for the active instance; other instances restore before their next draw.
Build tooltip additions locally and call the original tooltip exactly once even
when our read fails. Do not send SHOW/RESET or cancel the host click on failure.
The new widget has no extra-spacing mutation to undo. The disabled fast path must
avoid further reflection and must still perform any intercepted original call.

Requester builds all visible estimates before drawing: one contract failure drops
the entire current overlay and disables future overlays. Legitimate null/empty
keys, zero amounts, or missing stats keep their existing meaning. Do not substitute
failure with an empty successful estimate. No profile cache/history reset is part
of any recovery. Logger failure is not a reason to change gameplay state.

## Compatibility and validation

Reuse `main` for decisions, `mcCommon` for the logging bridge and shared hooks,
`mc1201` for pre-26 UI, `mc2612` for 26 UI, and existing loader entrypoints.
AdvancedAE's shared mixin is also compiled by Forge: keep that source free of
NeoForge loader types. No new dependency, config, packet, NBT, registry, locale,
or supported-version change is required. Logs are stable English maintainer text.

Source inspection establishes where recovery is safe; it does not prove a patched
game still starts. Require actual packaged-client and dedicated-server logs plus
controlled incompatible fixtures before implementation is accepted. In particular,
test startup absence with Forge's typed AdvancedAE mixin and Fabric's shared
pre-26 list, and test deferred optional injections instead of merely counting
mixin registrations. See the plan's acceptance mapping and completion gate.
