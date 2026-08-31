# GuideME Guide Technical Design

## Evidence

- The repository's [architecture](../architecture.md) and
  [feature map](../feature-coverage.md) define the supported targets and shipped
  behavior.
- GuideME's official
  [data-driven guide documentation](https://github.com/AppliedEnergistics/GuideME/blob/main/docs/docs/20-data-driven-guides.md)
  supports guide definitions and pages entirely through resource packs.
- GuideME's official
  [authoring documentation](https://github.com/AppliedEnergistics/GuideME/blob/main/docs/docs/30-authoring/index.md)
  defines navigation, item targets, Markdown/MDX content, and relative links.
- GuideME's official
  [translation documentation](https://github.com/AppliedEnergistics/GuideME/blob/main/docs/docs/60-translation.md)
  defines language folders and fallback behavior.
- The `1.21.1 NeoForge` build already resolves GuideME with AE2, and the
  `26.1.2 NeoForge` AE2 artifact declares GuideME transitively. AE2 15 on the
  two 1.20.1 rows instead packages the pre-GuideME internal guidebook.

## Resource Layout

Shared resources own one guide definition and both languages:

```text
shared/src/main/resources/assets/ae2craftingtime/
  guideme_guides/guide.json
  guides/ae2craftingtime/guide/
    index.md
    estimates.md
    screens.md
    diagnostics.md
    controls-and-configuration.md
    integrations.md
    troubleshooting.md
    _uk_ua/
      index.md
      estimates.md
      screens.md
      diagnostics.md
      controls-and-configuration.md
      integrations.md
      troubleshooting.md
```

`guide.json` supplies the translatable guide name and no custom model. Pages
use frontmatter for navigation order and selected AE2 item targets. The start
page links to every child page, and every child is present in the navigation
tree.

The shared resource directory is already packaged by every release row. The
1.20.1 loaders ignore the unknown GuideME resources. No conditional resource
copy task is needed.

## Discovery And Opening

GuideME automatically loads the data-driven definition as
`ae2craftingtime:guide`. Pages declare only relevant existing AE2 autocrafting
items as `item_ids`, such as crafting terminals, Crafting CPU components,
Pattern Providers, and Molecular Assemblers.

GuideME owns the open-guide key, tooltip targeting, guide choice, navigation,
and search. AE2 Crafting Time adds no key mapping or opening code. If smoke
testing shows that a selected item target conflicts ambiguously with AE2's own
guide, narrow the target list to items where GuideME presents a deterministic
choice; do not add a new UI merely to bypass GuideME's native behavior.

## Content Ownership

The implementation reads `docs/feature-coverage.md` first, then uses only its
shipped player-facing sources:

- `docs/profiling-and-diagnostics/` for learning, confidence, accuracy, delay,
  and bottleneck claims;
- `docs/time-to-craft-plan.md`, `docs/ttc-colored-text.md`, and
  `docs/ttc-sorting.md` for visible estimates;
- `docs/player-controls-and-integrations/` for screens, controls, config, and
  optional UI behavior;
- `docs/ae2-addon-integration/` and `DEPENDENCIES.md` for current integration
  support;
- `docs/server-client-stats.md` and `docs/world-save-persistence.md` for
  multiplayer, privacy, and persistence.

The guide paraphrases those sources for players. It does not copy planned
sections from the feature map. Each diagnostic says exactly what the observed
state proves and keeps causes framed as possibilities unless the server has
verified them.

## Localization

English pages are the default. Ukrainian pages live below `_uk_ua` with the
same filenames, navigation parents, positions, item targets, headings, and
relative links. The guide name uses the existing language JSON files so the
definition does not duplicate visible labels.

GuideME falls back to the English page or asset when a localized resource is
missing, but the repository check treats any English/Ukrainian page-set drift
as a failure.

## Validation

The root `build.gradle` owns a `checkGuideResources` task, and the root `test`
task depends on it so the existing CI command runs it. The task uses Groovy's
built-in `JsonSlurper` for the guide definition and line-oriented checks for
frontmatter and Markdown links. It scans the guide resources and verifies:

- the definition and start page exist;
- English and Ukrainian page paths match;
- navigation positions and parent ids are valid and unique among siblings;
- every relative Markdown link resolves;
- selected item ids are namespaced and duplicated only when intentional; and
- required shipped-content headings exist.

Distribution checks inspect the two modern JARs for the definition and all
pages. Prepared-client smoke checks on `1.21.1 NeoForge` and `26.1.2 NeoForge`
prove discovery, item-context opening, navigation, search, both themes, and
English/Ukrainian rendering.

## Failure And Compatibility Rules

- Broken content fails resource validation before release.
- GuideME parser errors or missing pages fail the prepared-client smoke check.
- No packet, persistence, config, or protocol version changes are involved.
- No loader metadata change is needed unless the existing AE2 metadata does
  not actually enforce a compatible GuideME at implementation time; verify the
  packaged dependency metadata before deciding.
- If the two GuideME versions require different data formats, prefer the
  common supported subset. Split only the minimum incompatible resource, not
  the whole guide.
