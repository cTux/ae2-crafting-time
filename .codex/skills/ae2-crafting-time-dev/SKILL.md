---
name: ae2-crafting-time-dev
description: Develop, debug, test, or port AE2 Crafting Time code and resources. Use `implement-planned-feature` for a decision-complete new feature, writing for docs-only work, and release for publishing.
---

# AE2 Crafting Time Development

## Start Here

1. Treat the GitHub issue list as the source of truth. Find the matching issue
   before starting any task or problem; create one when none exists, and keep
   the work linked to it.
2. Read `AGENTS.md` and inspect the target code, callers, supported-version
   counterparts, and closest feature document.
3. Read [references/code-map.md](references/code-map.md) only when the ownership
   or cross-version path is unclear.
4. Before changing executable behavior, read
   [references/testing-and-change-workflow.md](references/testing-and-change-workflow.md).
5. When the task changes repo-owned text, read
   [the writing skill](../ae2-crafting-time-writing/SKILL.md) before editing it.
6. For test-driver implementation or extension, use
   [the test-driver skill](../ae2-crafting-time-test-driver/SKILL.md).
7. Trace the real path end to end. Reuse an existing helper, source set, packet
   codec, resource, test pattern, or loader adapter before adding code.

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

- Put decisions in covered pure-Java code and leave Minecraft/loader adapters as
  delegation plus API conversion.
- Test changed boundaries through the nearest packet, NBT, resource, or script
  self-check described in the testing reference.
- Never weaken JaCoCo, exclusions, assertions, or other gates. Documentation and
  static resources need relevant validation, not fake unit tests.

## Change Boundaries

- Use the smallest shared root-cause fix; do not patch sibling callers
  individually.
- Treat wire layouts and persisted NBT as compatibility boundaries. Update every
  affected loader together and version the format or protocol when needed.
- Treat `scripts/release-matrix.json` as the supported-target source of truth.
- Keep build dependencies and loader metadata at the minimum supported versions.
- Optional-addon runtime ranges stay open-ended above that minimum; never turn
  a development-client pin or an unverified newer release into a loader cap.
  Record actual incompatibilities separately and verify changed addon APIs.
- Keep ordinary `run-*` clients on the pinned compatible graph and use
  `run-*-latest` clients for the newest available versions. Update both through
  `scripts/run-client-versions.json`.
- Update `docs/dependencies.md` in the same change whenever required
  or optional dependency support changes, or an existing integration changes
  its behavior, supported targets, versions, or status.
- When a reproduced third-party conflict makes the full graph impossible,
  record the compatible-profile exclusion and reason in that matrix, but keep
  the project in the latest diagnostic client.
- Verify a newest dependency's full Maven coordinate instead of assuming it
  still uses the minimum version's group and artifact.
- For matrix rows, dist tasks, jar naming, deployment, or publishing, use
  `ae2-crafting-time-release` instead of this skill.

## Completion

Follow `AGENTS.md` for branch, commit, PR, and validation ordering. Report only
checks that actually ran, and call unfinished CI pending rather than passed.

When UI smoke is authorized for a focused code change, use the prepared-smoke
skill's `-Changed` workflow after PR creation. Review the selection reasons;
keep full mode for explicit full/release verification. NOT_REQUIRED does not
waive the normal unit, boundary, or static checks.
