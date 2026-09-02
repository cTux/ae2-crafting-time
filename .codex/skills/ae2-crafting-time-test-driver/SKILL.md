---
name: ae2-crafting-time-test-driver
description: Extend or maintain the AE2 Crafting Time development-only UI test driver and its scenarios. Use the prepared-client smoke skill to run scenarios.
---

# AE2 Crafting Time Test Driver

Read `docs/test-driver/spec.md`, `docs/test-driver/technical-design.md`, and the
existing fixture closest to the requested scenario.

## Add an optional-mod CPU scenario

- Name it `<mod>-cpu`.
- Extend `AddonCpuFixture` with only the mod-specific placement, formation, and
  CPU lookup. Register its class name in `AddonCpuFixture.FIXTURES` so other
  optional fixture classes are not loaded.
- Add the optional mod to `testDriverCompileOnly` unless the inherited main
  compile classpath already supplies it. Keep production metadata, runtime, and
  artifacts independent of the fixture.
- Reuse the common state machine, check set, result validation, and derived
  `<mod>-profiled-plan.png` name. Do not add a runner scenario allowlist or a
  branch in `CraftPlanScenario`.
- Extend `TestDriverCoreTest` for registry behavior and the smallest relevant
  fixture boundary. Keep driver content out of production JARs and `dist`.
- Update `docs/dependencies.md` in the same change whenever a target
  or mod gains, loses, or changes TestDriver or automation-test coverage.

For a different UI flow, update the spec and design first. Reuse the current
driver artifact and runner unless the new target proves a loader boundary.

## Run it

Follow [the screenshot archive contract](../../../docs/ui-smoke-evidence.md)
when running or extending scenarios. Capture each distinct UI checkpoint and
map checks to images without treating screenshots as proof of server-only state.

Use `run-ae2-client-smoke`. UI checks must run inside CodexVM with an 8 GiB
client and a maximized Minecraft window. Run `scripts/run-ui-smoke.ps1` for the
Forge 1.20.1 driver; use `-Scenario <name>`, `-Latest`, or `-Interactive` only
when the request needs them. Use `launch-prism-test-modpack` only when the user
explicitly asks to test a named modpack.
