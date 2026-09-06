# Feature Chapter Specification

Issue: [#304](https://github.com/cTux/ae2-crafting-time/issues/304)

Status: planned.

## Goal

Add **Chapter 2: Features** to the in-game guide so players can understand every
shipped non-status feature without leaving Minecraft. Every feature gets a
detailed page, a relevant screenshot, and links to useful related pages.

## Pages

`features/index.md` links to these pages in order:

| Page | What it explains |
| --- | --- |
| `time-estimates.md` | Row and total TTC in Crafting Plan and Crafting Status, partial coverage, and the running-job critical path. |
| `learning-throughput.md` | How completed output becomes retained, per-network timing history. |
| `confidence.md` | Samples, used samples, low confidence, and outlier filtering. |
| `job-accuracy.md` | Prediction coverage, error, MAPE, and actual-to-TTC ratio. |
| `delay-diagnostics.md` | Delayed-output detection, bottleneck hints, notifications, and provider highlighting. |
| `sorting-and-colors.md` | AE2/longest/shortest sorting and relative fast-to-slow colors. |
| `details-and-reset.md` | Tooltips, Ctrl-click details, and Ctrl-Alt-click reset. |
| `saved-history.md` | World-save persistence, network scope, and runtime-only state. |
| `configuration.md` | Current common settings and what each changes. |
| `crafting-tree.md` | AE2: Crafting Tree estimates, totals, details, and reset. |
| `me-requester.md` | ME Requester row estimates and total hints. |
| `addon-support.md` | Applied Mekanistics keys and supported addon crafting CPUs. |
| `guide-book.md` | How to obtain, open, search, and navigate the guide. |

Chapter 1 remains `getting-started.md`. The separate [Statuses chapter](../status-chapter/spec.md)
is Chapter 3 and owns the individual status pages.

## Page requirements

Every feature page must:

- explain its use, location, controls, a concrete example, and important limits;
- include at least one real in-game screenshot with useful alt text and a caption
  that identifies seeded/demo values when applicable;
- link back to the landing page, to its previous/next page, and to at least one
  other relevant feature, status, introduction, or Chapter 1 page;
- use player language and keep Java, packet, and internal class names out.

The landing page links to all 13 feature pages, Chapter 1, Statuses, and the
introduction. The first and last child link back instead of wrapping.

## Screenshots

Reuse reviewed `docs/images/` captures where they clearly show the feature.
Copy selected images into the guide resource tree; shipped pages cannot depend
on repository-only paths or remote URLs. Capture only missing evidence. Keep
native pixels, crop to the relevant UI, and exclude accounts, addresses, chat,
coordinates, unrelated worlds, and test controls. Each page has its own relevant
image; reuse one image on two pages only when each caption names a different
visible part.

## Languages and compatibility

English and Ukrainian have identical paths, navigation order, image references,
and links. Ukrainian is a complete natural translation. Both GuideME themes
remain readable.

The chapter ships anywhere the current guide ships: 1.20.1 Forge through
optional GuideME, 1.20.1 Fabric through AE2's bundled guide renderer, and
1.21.1/26.1.2 NeoForge through GuideME. Missing optional GuideME keeps current
behavior. This changes shared guide resources and existing target transformations
only: no Java, packets, profiler state, config, recipes, or dependencies.

## Non-goals

- Planned, unreleased, development-only, status, or repository-tooling content.
- Runtime behavior changes, video, animation, remote media, custom widgets,
  item bindings, or a second guide implementation.

## Acceptance criteria

| ID | Observable result |
| --- | --- |
| F1 | Chapter 2 links to exactly the 13 listed pages. |
| F2 | Every shipped non-status player-facing feature in `docs/feature-coverage.md` maps to one page; statuses map to Chapter 3, while tooling and internal transport are excluded. |
| F3 | Every page satisfies the content, screenshot, caption, and cross-link rules. |
| F4 | Introduction, Chapter 1, Features, and Statuses form a complete graph with no orphan page. |
| F5 | English and Ukrainian paths, navigation metadata, images, and links match. |
| F6 | All images are local, readable at native aspect ratio, and free of private or unrelated information. |
| F7 | All four distributions contain target-correct resources; opening, navigation, search, themes, and optional-dependency behavior do not regress. |
| F8 | Validation rejects a missing page, translation, image, reciprocal link, or incorrect navigation position. |
| F9 | Documentation/link checks pass and reviewed client evidence covers both guide renderer paths. |

See the [technical design](technical-design.md) and
[implementation plan](implementation-plan.md).
