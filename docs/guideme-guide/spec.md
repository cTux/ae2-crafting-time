# GuideME Guide Specification

Issue: [#144](https://github.com/cTux/ae2-crafting-time/issues/144)

## Goal

Give players a complete, searchable in-game explanation of AE2 Crafting Time on
the supported targets that use GuideME.

The guide is player documentation. It explains what the mod displays, what its
claims mean, and what a player can do. It does not expose implementation details
that are not useful for playing the mod.

## Player Experience

- GuideME discovers a guide named `AE2 Crafting Time` with the id
  `ae2craftingtime:guide`.
- Pressing GuideME's open-guide key while hovering a relevant AE2 autocrafting
  item offers or opens the most relevant AE2 Crafting Time page.
- The start page gives a short explanation and links to every topic.
- Navigation and full-text search reach every page.
- English is the default language. Ukrainian has a complete matching
  translation; GuideME's normal language fallback still applies.

No new mod item, recipe, command, screen button, config option, packet, or
server state is added.

## Content

The guide must explain every shipped player-facing feature, grouped into these
pages:

1. **Overview and quick start:** purpose, installation expectations, where TTC
   appears, and how the first useful samples are learned.
2. **How estimates work:** measured production throughput, retained samples,
   low confidence, outlier filtering, totals, prediction accuracy, and the
   limits of an estimate.
3. **Crafting screens:** Crafting Plan and Crafting Status rows, totals, badges,
   colors, tooltips, and sort modes.
4. **Waiting, delays, and bottlenecks:** every shipped row state and its
   precedence, what each state proves, and the actionable hints the mod can
   support without guessing.
5. **Controls and configuration:** Ctrl-click details, Ctrl-Alt-click reset,
   the configurable mouse binding, every shipped config key, defaults, ranges,
   and restart requirements.
6. **Optional integrations:** the exact supported targets and behavior for AE2:
   Crafting Tree, ME Requester, Applied Mekanistics, AdvancedAE, Neo ECO AE
   Extension, and AE2 Lightning Tech.
7. **Persistence, multiplayer, and troubleshooting:** world-scoped learning,
   server authority, shared aggregate data, privacy boundaries, missing-data
   causes, stale samples, and when reset is appropriate.

Content is sourced from the shipped-feature map and its linked specifications.
Planned features must not be described as available. If implementation lands
after another feature, the guide is reconciled with the shipped-feature map at
implementation time.

## Compatibility

- Enable the guide on `1.21.1 NeoForge` and `26.1.2 NeoForge`, where AE2 uses
  standalone GuideME.
- Do not add a second guide implementation for `1.20.1 Forge` or `1.20.1
  Fabric`; AE2 15 contains its older internal guidebook rather than GuideME.
- Use GuideME's data-driven guide definition and Markdown resources. Do not add
  compile-time GuideME API use unless a verified acceptance criterion cannot be
  met with resources alone.
- GuideME remains supplied by the compatible AE2 dependency. AE2 Crafting Time
  does not register or bundle another copy.
- A missing or incompatible GuideME installation must be handled by AE2's
  existing dependency contract, not a new AE2 Crafting Time fallback UI.

## Non-goals

- Developer architecture, packet layouts, mixin hooks, build instructions, or
  release procedures.
- A custom guide item, recipe, command, screen button, or website export.
- Replacing the README and repository developer documentation.
- Backporting standalone GuideME to Minecraft 1.20.1.
- Documenting planned behavior as if it already ships.

## Acceptance Criteria

- GuideME discovers `ae2craftingtime:guide` on both GuideME-capable release
  rows without Java registration code.
- Hovering each selected AE2 autocrafting entry item and pressing GuideME's
  open-guide key reaches the intended page or an unambiguous guide choice.
- The start page, navigation, relative links, and full-text search reach every
  page.
- The seven content areas above cover every entry in
  `docs/feature-coverage.md` that is both shipped and player-facing.
- Every explanation distinguishes measured facts from estimates and avoids
  claiming an unverified root cause.
- Every config key, default, range, supported target, control, optional
  integration, and visible state matches the implementation-time repository
  source of truth.
- English and Ukrainian have identical page sets, navigation topology, item
  targets, and internal links.
- Guide resources are present in the `1.21.1 NeoForge` and `26.1.2 NeoForge`
  distribution JARs and do not change runtime behavior on either 1.20.1 JAR.
- Resource/link validation and prepared-client smoke checks pass for both
  GuideME-capable targets.

See [technical-design.md](technical-design.md) and
[implementation-plan.md](implementation-plan.md).
