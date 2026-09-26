# AE2 Crafting Priority coexistence

Tracking: [#442](https://github.com/cTux/ae2-crafting-time/issues/442).

Hold: The issue is marked `do-not-implement-yet`. This page plans qualification;
no compatibility result is claimed.

## Targets and overlapping behavior

The issue identifies Forge 1.20.1, Fabric 1.20.1, and NeoForge 1.21.1 as overlapping
published targets. It records no NeoForge 26.1.2 artifact. Recheck the official
inventory before testing; do not advertise unsupported combinations from metadata.

Crafting Priority changes craft requests, CPU/status screens, CPU labels, and
provider dispatch order. Review its source, mixins, public API, required AE2
versions, and installation sides against Crafting Time lifecycle, profiler,
packet, and UI hooks.

Use exact compatible files in isolated diagnostic profiles. Do not add the addon
to ordinary compatible profiles until coexistence is proven.

## Controlled scenario

1. Launch each overlapping target and inspect dependency, classloading, mixin,
   and networking errors with both mods installed on the required sides.
2. Submit competing processing-pattern jobs to the same machine with different
   priorities and blocking mode enabled. Confirm the higher-priority job gets the
   next available machine while outputs, profile samples, and TTC remain correct.
3. Exercise planning, CPU selection, submission, execution, cancellation, and
   completion. Confirm priority resets to zero after the job ends and no stale
   TTC/profiler state remains.
4. Inspect craft-confirm, CPU, and Status screens for overlapping controls,
   navigation errors, clipping, or TTC sorting regressions. Priority-decorated
   CPU names must not change CPU identity or break selection.
5. For failures, repeat with each mod removed separately before attributing a
   conflict or changing Crafting Time.

## Completion evidence

Retain exact Minecraft, loader, AE2, Crafting Time, and Crafting Priority versions,
configuration, source commit, logs, dispatch/output observations, and reviewed
screen captures for all three overlapping targets. Unsupported and untested
targets must remain explicit.

Update [dependencies](../dependencies.md) and prepared-client matrices with
confirmed support or required exclusions. Link minimal reproducers, fixes, and
current-head CI. Launch success alone does not establish dispatch or UI coexistence.
