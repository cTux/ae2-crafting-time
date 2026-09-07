# Completed Craft Total TTC Implementation Plan

Implement [#325](https://github.com/cTux/ae2-crafting-time/issues/325) from the
[specification](spec.md) and [technical design](technical-design.md).

## 1. Guard status-title TTC by remaining work

- Update the `shared/src/mc1201` and `shared/src/mc2612`
  `CraftingCPUScreenMixin` copies.
- In the existing title hook, keep clearing the pending title component first,
  then return AE2's title unless the status contains an entry whose active plus
  pending amount is positive.
- Leave cached stats, packets, row rendering, sorting, and Crafting Plan totals
  unchanged.

Gate: both screen variants compile and an active status still follows the
existing total-TTC formatting path.

## 2. Protect the completed transition

- Add one `total-cleared` check to the shared `craft-lifecycle` scenario.
- In the completed-status phase, fail while a header-level
  `text.ae2craftingtime.ttc` component is present, then record the check and the
  existing completed screenshot.
- Keep the current running-status, output, profile-sample, and completion checks
  so the scenario proves the full transition rather than only a static empty
  screen.

Gate: the scenario observes total TTC during active crafting and no total TTC
after the same job finishes.

## 3. Verify every supported target

- After the hook-created implementation PR exists, run the development skill's
  targeted shared tests and all four release-matrix build checks.
- Run prepared-client `craft-lifecycle` smoke on Minecraft 1.20.1 Forge,
  Minecraft 1.20.1 Fabric, Minecraft 1.21.1 NeoForge, and Minecraft 26.1.2
  NeoForge.
- Run documentation/link checks and `git diff --check`; verify GitHub CI
  separately.

Completion gate: every acceptance criterion has passing automated or retained
UI evidence on all four targets, all required checks pass, and no
repository-owned warning remains.
