# Status Chapter Technical Design

## Decision

Extend the shared guide with one parent and eleven child Markdown pages. Derive
inventory/order from current renderers, translations, and status types; derive
meaning from matching status designs. Reuse seven retained screenshots and add
three provider-dispatch captures. The planned Recurrent extension adds one
reviewed native-plan capture.

This implements the [specification](spec.md) for
[#305](https://github.com/cTux/ae2-crafting-time/issues/305).

## Evidence and ownership

`CraftingStatusTableRendererMixin`, `CraftingRowState`, `CraftProfiler`, and
`ProviderDispatchTracker` define visible precedence. `TtcText` and both locale
files define labels/tooltips. Commit `8169fad2` / PR #293 shipped NO TARGET,
INPUT BLOCKED, and LOCKED after the older provider-dispatch planning snapshot;
current code/history supersede that stale status line. Existing status specs
define conditions and clearing. The screenshot gallery retains seven states.

```text
shared/src/main/resources/assets/ae2craftingtime/guides/ae2craftingtime/guide/
  index.md
  getting-started.md
  statuses/
    index.md
    no-space.md
    no-provider.md
    no-power.md
    locked.md
    input-blocked.md
    no-target.md
    waiting.md
    delayed.md
    no-data-yet.md
    estimated.md
    recurrent.md
    images/*.jpg
  _uk_ua/statuses/*.md
```

`statuses/index.md` uses parent `index.md`, position `2`, a renderer-safe icon,
and localized titles **Chapter 3: Statuses** / **Розділ 3: Стани**. Children use
positions `0..10` in spec order. Features owns position `1`; Chapter 1 remains `0`.

## Sources and screenshots

| Page | Source | Image |
| --- | --- | --- |
| NO SPACE | no-space docs | `crafting-status-no-space.jpg` |
| NO PROVIDER | no-provider docs | `crafting-status-no-provider.jpg` |
| NO POWER | no-power docs | `crafting-status-no-power.jpg` |
| LOCKED | provider-dispatch docs/current code | new focused capture |
| INPUT BLOCKED | provider-dispatch docs/current code | new focused capture |
| NO TARGET | provider-dispatch docs/current code | new focused capture |
| Waiting | waiting-to-start docs | `crafting-status-waiting.jpg` |
| DELAYED | profiling docs | delayed and diagnostics gallery images |
| No data yet | TTC/profiling docs | `crafting-plan-no-data.jpg` |
| Estimated | TTC docs | `crafting-status-running.jpg` |

Copy retained images without recompression. New captures follow the gallery
design, are visually reviewed, enter `docs/images/README.md`, and identify
fixture data in captions.

## Links, validation, and packaging

Every child links to landing, adjacent status, and the relevant `../features/`
page. Blocking pages cross-link the closest alternative. Locale targets match.

Extend `checkGuideResources` to verify exact manifest/positions, locale peers,
declared labels against translations, parents/icons, local images, internal
targets, reachability, and return links. Each child requires one image and three
internal links. Runtime UI tests remain authority for rendered status.

Use existing shared copy/Fabric transformation and extend four-JAR assertions.
Manual QA triggers each status through prepared scenarios and compares page to
label, tooltip, precedence, mixed-batch behavior, and clearing. Add the three
missing captures to the existing provider-dispatch scenario. Missing parity,
broken links, unsupported Markdown, unreadable captures, or target mismatch
blocks completion.

## Alternatives not chosen

One matrix is too shallow for evidence and recovery. Generated pages make prose
and translation review worse. Remote/repository-only images fail offline.

## Recurrent extension (#412)

The original ten pages already exist. Implement only this extension for
[#412](https://github.com/cTux/ae2-crafting-time/issues/412), keeping their positions
0..9. Add `statuses/recurrent.md` and `_uk_ua/statuses/recurrent.md` at position 10,
with parent `statuses/index.md` and localized navigation titles.

Update both landing pages: retain the first ten links, then put Recurrent in a
separate Crafting Plan paragraph. Explain that navigation order is not a shared
priority between plan and running-job states. Add a Next link from each
`estimated.md`. The new page links back to `estimated.md`, `index.md`, and
`../features/time-estimates.md`, preserving identical locale link targets.

Use the current `TtcText` keys `text.ae2craftingtime.plan.recurrent` and
`text.ae2craftingtime.plan.recurrent_hint`, plus the
[recurrence design](../../recurrent-crafting-status/technical-design.md), as the
text/behavior evidence. Show the formatted quantity example; do not copy `%s`
into player prose. Do not change Java, packets, persistence, or detection.

Append `recurrent.md: text.ae2craftingtime.plan.recurrent` to `statusPages` in
root `build.gradle`'s `checkGuideResources`. Reuse its existing count/order,
previous/next, translation, image, and packaging checks. Keep the existing map
as the sole inventory. Its label check must continue to handle the `%s` label
format without requiring a literal placeholder in the guide.

Capture the real native-plan `recurrent-plan` fixture through the prepared
client workflow, or reuse an existing reviewed capture only after inspecting it.
Package one crop at `statuses/images/crafting-plan-recurrent.png`, with the same
image reference in both locales. Record its source in the existing gallery
records under `docs/images/`; preserve native pixels and exclude private data.
Missing usable evidence blocks implementation completion, not this planning PR.

Reuse the shared resource pipeline and Fabric 1.20.1 transformation. Verify
Forge/Fabric 1.20.1 and both NeoForge targets. No dependencies, recipe changes,
new guide system, or server migration are needed. S11 maps to localized prose,
S12 to navigation/manifest changes, and S13 to the inspected capture. Existing
S3 and S5-S10 cover links, translations, packaging, and guide rendering.

## GitHub wiki update (S14)

The wiki published under #411 uses `Status-Estimated.md` and
`Ukrainian-Status-Estimated.md`, locale-specific landing pages `Statuses.md` and
`Ukrainian-Statuses.md`, and shared images under `images/statuses/`. Preserve
those conventions in the separate `ae2-crafting-time.wiki.git` repository.

After the canonical book change is merged, add `Status-Recurrent.md` and
`Ukrainian-Status-Recurrent.md` from the corresponding guide pages. Strip GuideME
navigation frontmatter and translate page links to wiki names without `.md`.
The new pages link to the matching locale's status landing, estimate page, and
`Feature-Time-Estimates` / `Ukrainian-Feature-Time-Estimates` page. Update both
landing pages and both estimate pages to expose Recurrent, keeping the separate
Crafting Plan explanation. The existing sidebar already links both landings.

Copy the reviewed PNG unchanged to `images/statuses/crafting-plan-recurrent.png`
and use it from both wiki pages. Do not introduce separately authored behavior
or a new synchronization framework. Start from the latest wiki checkout and
preserve unrelated wiki edits. Commit and publish the bounded page/image changes;
record the canonical source commit, wiki commit, and live page URLs as evidence.
If publication or rendered links/images fail, leave #412 incomplete until fixed.
