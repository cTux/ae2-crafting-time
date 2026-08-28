---
name: ae2-crafting-time-dev
description: Develop, debug, test, document, port, or maintain any part of AE2 Crafting Time. Use for core profiling and TTC logic, AE2 or Minecraft integration, mixins, packets, persistence, loader modules, optional-mod UI, configs, resources, Gradle wiring, scripts, release automation, and project docs. Also load ae2-crafting-time-release for distribution or publishing work.
---

# AE2 Crafting Time Development

## Start Here

1. Read `AGENTS.md`, `README.md`, and `docs/working-with-project.md`.
2. Read [references/code-map.md](references/code-map.md), then the closest feature document under `docs/`.
3. When the task changes repo-owned text, read
   [the writing skill](../ae2-crafting-time-writing/SKILL.md) before editing it.
4. Trace the real path end to end and inspect every caller plus each supported-version counterpart before editing.
5. Reuse an existing helper, source set, packet codec, resource, test pattern, or loader adapter before adding code.
6. Read [references/testing-and-change-workflow.md](references/testing-and-change-workflow.md) before changing anything.

## Non-Negotiable Design Rules

- Server owns profiling, retained samples, persistence, resets, accuracy, stalls, and aggregate stats. Client owns requests, cache, formatting, sort state, and input only.
- Put every calculation, branch, parser, validation rule, and state transition that can be Minecraft-free in `shared/src/main/java`.
- Keep AE2/Minecraft and loader code as thin, branchless adapters wherever possible.
- Reuse `mcCommon` across every target, `mc1201` across 1.20.1/1.21.1, `mc2612` for 26.1.2, and `neoforge` only for both NeoForge targets. Add loader copies only when APIs require them.
- Preserve the `networkId + outputId` profile identity, normalized item/fluid units, bounded packets, server-side trust decisions, and exact world-save compatibility.
- Keep craft-plan and crafting-status UI behavior consistent unless the underlying semantics differ. Append through AE2 paths instead of replacing renderers.
- Keep optional integrations optional: string-target mixins or compile-only dependencies must not make absent mods required.
- Update English and Ukrainian translation keys together and keep loader metadata, mixin lists, dependencies, docs, and code truthful to one another.
- Keep docs, skills, changelogs, translations, and metadata in the casual,
  direct project voice. Never trade technical accuracy for personality.

## 100% Coverage Rule

Every new or changed executable behavior must have 100% line and branch coverage.

- Move decisions into covered pure-Java code and test every branch, boundary, invalid input, and state transition.
- Do not add uncovered decisions to mixins, packet registration, SavedData, entrypoints, or loader glue. If direct coverage is impractical, extract the decision into `shared` and leave only delegation in the adapter.
- Add packet round-trip, NBT round-trip, and structural/resource checks in the nearest shared or loader test source set when those boundaries change.
- Cover every changed PowerShell branch with the closest deterministic script self-test; release automation belongs in `test-deploy-changed.ps1`.
- Do not weaken JaCoCo, exclude changed logic, delete meaningful assertions, or claim 100% from test counts. The PR CI coverage gate is authoritative.
- Documentation-only and static-resource-only changes need relevant validation, not fake unit tests.

## Change Boundaries

- Use the smallest shared root-cause fix; do not patch sibling callers individually.
- Treat wire layouts and persisted NBT as compatibility boundaries. Update every affected loader together and version the format/protocol when compatibility changes.
- Treat `scripts/release-matrix.json` as the supported-target source of truth.
- Keep build dependencies and loader metadata at the minimum supported versions.
- Let each `run-*.bat` client use the newest versions available for its exact Minecraft and loader target.
- Verify the newest dependency's full Maven coordinate instead of assuming it still uses the minimum version's group and artifact.
- For matrix rows, dist tasks, jar naming, deployment, or publishing, also load `ae2-crafting-time-release` and follow `docs/release.md`.

## Completion

- Work on a branch and make each fix or feature one conventional commit.
- Do not run repository tests locally before the hook creates the PR. Let required GitHub CI run `test` and `jacocoTestReport`.
- Run required post-PR self-checks before handoff. Required GitHub CI remains
  authoritative but may still be pending.
- After pushing, read back the PR, complete diff, and current required-check
  state. Report the task done without waiting for CI unless the user explicitly
  asks you to wait. Call unfinished checks pending or in progress; never report
  them as passed.
