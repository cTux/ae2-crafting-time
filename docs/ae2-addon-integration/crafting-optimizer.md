# AE2 Crafting Optimizer coexistence

Tracking: [#441](https://github.com/cTux/ae2-crafting-time/issues/441).

Hold: The issue is marked `do-not-implement-yet`. This is a qualification plan,
not an executed compatibility result.

## Target and source review

The issue identifies overlapping ACO files for Forge 1.20.1 and NeoForge 1.21.1.
Fabric 1.20.1 and NeoForge 26.1.2 are unsupported by the recorded addon inventory;
recheck official artifacts before a campaign rather than treating this as a
permanent upstream limitation.

Review ACO source, mixins, configuration, required AE2 versions, and installation
sides against Crafting Time calculation/CPU/profiling/packet/UI hooks. ACO changes
calculation deduplication, pattern lookup caches, tick budgets, and long orders.
Successful startup alone cannot establish correct profiling.

Use isolated diagnostic profiles with the newest compatible exact files.
Do not promote the addon to ordinary compatible profiles before qualification.

## Controlled scenarios

Compare ACO enabled and disabled with the same dependencies and fixtures:

- Native crafting and a processing-pattern job: plan, submit, execute, and
  complete with correct outputs and normal TTC/profile updates.
- Repeated and concurrent equivalent requests: no merged unrelated profiler
  identities, missing samples, double-counted returns, or stale TTC.
- Per-CPU/per-grid execution budgets: progress and completion remain observable
  across ticks, including long-order paths actually supported by that version.
  Do not describe an ordinary CPU as having extended capacity.
- Cancellation and world reload: state and persistence remain consistent.
- UI and networking: no classloading, mixin, packet, or screen errors.

For any failure, remove each mod separately as controls before attributing it,
changing Crafting Time, or reporting an upstream problem.

## Completion evidence

Retain exact Minecraft, loader, AE2, Crafting Time, and ACO versions/configuration,
source commit, logs, outputs, profiler observations, and reviewed UI captures for
both overlapping targets. Mark unsupported and untested targets explicitly.

Document confirmed support or exclusions in [dependencies](../dependencies.md)
and prepared-client matrices. Link minimal reproducers, fixes and current-head CI.
Metadata resolution or a single launch is insufficient to close #441.
