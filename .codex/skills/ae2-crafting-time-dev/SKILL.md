---
name: ae2-crafting-time-dev
description: Work on AE2 Crafting Time feature, bugfix, docs, and test tasks in this repository. Use when changing TTC UI, AE2 profiling, server/client stats packets, world-save persistence, shared source layout, loader modules, or project documentation.
---

# AE2 Crafting Time Dev

## Workflow

1. Read `README.md` and `docs/working-with-project.md` for current layout and commands.
2. For feature or bug work, read the closest docs file under `docs/` before editing code.
3. Keep pure Java logic in `shared/src/main/java`; keep AE2/Minecraft-facing shared code in `shared/src/mc1201/java`.
4. Keep loader-only glue under the matching `versions/<minecraft>-<loader>` module.
5. Run the smallest Gradle check that covers the touched code.

## Project Rules

- Server owns profiling, persistence, and aggregate stats.
- Client owns display cache and formatting only.
- Reuse existing TTC helpers before adding new UI paths.
- Status TTC uses `activeAmount + pendingAmount`.
- For suspicious fluid TTC, inspect normalized units and saved/runtime samples before changing math.
- Verify actual Gradle project names with `.\gradlew.bat projects` before using module tasks.

## Checks

Use one of:

```powershell
.\gradlew.bat :shared:test
.\gradlew.bat :mc_1_20_1_forge:test
.\gradlew.bat :fabric_1_20_1:test
.\gradlew.bat :mc_1_21_1_neoforge:test
.\gradlew.bat test
```
