# Working With This Project

## First Checks

Start from the repo root:

```powershell
.\gradlew.bat projects
```

Use the printed Gradle project names. The current modules are:

```text
:shared
:fabric_1_20_1
:mc_1_20_1_forge
:mc_1_21_1_neoforge
```

## Code Boundaries

- Reuse existing shared code, resources, docs, tests, and Gradle wiring as much
  as possible before adding version-specific copies.
- Put Minecraft-free logic in `shared/src/main/java`.
- Put AE2/Minecraft 1.20.1-compatible shared code in `shared/src/mc1201/java`.
- Put loader-only entrypoints, networking glue, saved-data glue, metadata, and tests under the matching `versions/<minecraft>-<loader>` folder.
- Keep `scripts/release-matrix.json` as the source of truth for build/release rows.

## Normal Development

Run the smallest check that covers the touched code:

```powershell
.\gradlew.bat :shared:test
.\gradlew.bat :mc_1_20_1_forge:test
.\gradlew.bat :fabric_1_20_1:test
.\gradlew.bat :mc_1_21_1_neoforge:test
```

For cross-loader shared changes, run:

```powershell
.\gradlew.bat test
```

For jar output, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all-versions.ps1
```

## TTC And Profiling Rules

- Server owns profiling, retained samples, persistence, and aggregate stats.
- Client owns display cache and UI formatting only.
- Singleplayer and dedicated server-client play use the same logical-server
  stats request/response flow.
- Status row TTC estimates use `activeAmount + pendingAmount`.
- The running-job total uses AE2's elapsed time and overall completed-work
  progress instead of summing row estimates that may execute in parallel.
- Throughput samples aggregate all concurrent crafting-CPU batches for the same
  network output from the first dispatch until the output becomes idle.
- Pending batches are scoped by crafting CPU and cleared when its job finishes
  or its retained stats are reset.
- Fluid estimates must be checked in normalized units before changing math.
- If a value looks wrong, inspect saved/runtime samples before editing the estimator.

## UI Rules

- Reuse the existing TTC display, color, and sort helpers before adding UI code.
- Keep crafting plan and crafting status behavior consistent unless the difference is intentional.
- Prefer mixins that append to AE2's existing text/tooltip paths over renderer replacement.

## Release Work

Read [release.md](release.md) first.

Check automation with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-deploy-changed.ps1
```

Do not add a release matrix row until its Gradle module builds a real jar.
