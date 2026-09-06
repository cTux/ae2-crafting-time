# Status Chapter Technical Design

## Decision

Extend the shared guide with one parent and ten child Markdown pages. Derive
inventory/order from current renderers, translations, and status types; derive
meaning from matching status designs. Reuse seven retained screenshots and add
three provider-dispatch captures.

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
    images/*.png
  _uk_ua/statuses/*.md
```

`statuses/index.md` uses parent `index.md`, position `2`, a renderer-safe icon,
and localized titles **Chapter 3: Statuses** / **Розділ 3: Стани**. Children use
positions `0..9` in spec order. Features owns position `1`; Chapter 1 remains `0`.

## Sources and screenshots

| Page | Source | Image |
| --- | --- | --- |
| NO SPACE | no-space docs | `crafting-status-no-space.png` |
| NO PROVIDER | no-provider docs | `crafting-status-no-provider.png` |
| NO POWER | no-power docs | `crafting-status-no-power.png` |
| LOCKED | provider-dispatch docs/current code | new focused capture |
| INPUT BLOCKED | provider-dispatch docs/current code | new focused capture |
| NO TARGET | provider-dispatch docs/current code | new focused capture |
| Waiting | waiting-to-start docs | `crafting-status-waiting.png` |
| DELAYED | profiling docs | delayed and diagnostics gallery images |
| No data yet | TTC/profiling docs | `crafting-plan-no-data.png` |
| Estimated | TTC docs | `crafting-status-running.png` |

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
