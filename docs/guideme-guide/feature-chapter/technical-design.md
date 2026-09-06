# Feature Chapter Technical Design

## Decision

Extend the existing data-driven guide with one parent and 13 child Markdown
pages. Keep one explicit English/Ukrainian source tree under shared resources and
reuse existing screenshot evidence. No runtime code or new docs framework.

This implements the [specification](spec.md) for
[#304](https://github.com/cTux/ae2-crafting-time/issues/304).

## Evidence and ownership

`docs/feature-coverage.md` defines the shipped inventory;
`docs/guideme-guide/technical-design.md` defines the shared bilingual tree and
four-target transformations; `docs/images/README.md` records screenshot origin
and seeded/live limits. Planned features and contributor tooling stay excluded.

```text
shared/src/main/resources/assets/ae2craftingtime/guides/ae2craftingtime/guide/
  index.md                            # add Chapters 2 and 3 links
  getting-started.md                  # add chapter links
  features/
    index.md
    time-estimates.md
    learning-throughput.md
    confidence.md
    job-accuracy.md
    delay-diagnostics.md
    sorting-and-colors.md
    details-and-reset.md
    saved-history.md
    configuration.md
    crafting-tree.md
    me-requester.md
    addon-support.md
    guide-book.md
    images/*.png
  _uk_ua/features/*.md                # identical paths and shared images
```

`features/index.md` uses parent `index.md`, position `1`, a renderer-safe
vanilla icon, and localized titles **Chapter 2: Features** / **Розділ 2:
Можливості**. Children use parent `features/index.md` and positions `0..12` in
spec order. Statuses reserves root position `2`; Chapter 1 stays `0`.

## Sources and screenshots

| Page | Factual source | Initial image |
| --- | --- | --- |
| Time estimates | `docs/time-to-craft-plan.md` | `crafting-plan-estimate.png` |
| Learning throughput | profiling spec/design | `crafting-status-running.png` |
| Confidence | profiling spec/design | `ttc-low-confidence.png` |
| Job accuracy | profiling spec/design | `ttc-job-accuracy.png` |
| Delay diagnostics | profiling/provider-locate docs | `crafting-status-ttc-bottleneck-diagnostics.png` |
| Sorting and colors | TTC sorting/color docs | `ttc-sort-longest.png` |
| Details and reset | player-controls docs | `ttc-details-chat.png` |
| Saved history | world-save persistence docs | new focused capture |
| Configuration | player-controls docs/current config | new focused capture |
| Crafting Tree | player-controls docs | `crafting-tree-estimate.png` |
| ME Requester | player-controls docs | `me-requester-estimate.png` |
| Addon support | addon-integration docs | new supported-addon capture |
| Guide book | guide-book spec | new inventory/guide capture |

Copy reused images without recompression. New captures follow
`docs/images/DESIGN.md`, enter the gallery, and carry captions that identify
fixtures. Filenames are lowercase kebab-case.

## Links, validation, and packaging

Every child links to its landing page, adjacent page, and relevant
`../statuses/` or `../getting-started.md` content. Relative targets are identical
between locales.

Extend `checkGuideResources` with a manifest that verifies exact paths,
positions, parents, icons, locale peers, image references, link targets, landing
reachability, and return links. Each child requires one local image and three
internal guide links. Missing translations are errors even though GuideME can
fallback. Extend existing JAR assertions with representative page, translation,
image, and link assets in every release-matrix row.

Manual QA opens both renderer paths and checks navigation, search, all pages,
images, links, both themes, locale switching, and optional GuideME absence.
Unsupported Markdown, unreadable screenshots, broken links, or package drift
block completion.

## Alternatives not chosen

One long page fails the one-page-per-feature requirement. Generated prose hides
review and translation changes. Repository-only or remote images fail offline,
so images ship with the guide.
