# Supporting mod audit, 2026-09-20

Part of [#452](https://github.com/cTux/ae2-crafting-time/issues/452).
Only five verified version pins and GuideME's documented maximum change.
Minimum requirements, loader pins, integration code, and required dependency
versions stay unchanged. Each run selects one supporting mod and its existing
required graph, rather than the full optional-addon suite.

Official Modrinth release metadata was checked on September 19:
[JEI Forge](https://modrinth.com/mod/jei/version/IrfJO7PN),
[JEI Fabric](https://modrinth.com/mod/jei/version/l4UV8hII),
[JEI NeoForge 26.1.2](https://modrinth.com/mod/jei/version/AM5ddBPW),
[GuideME](https://modrinth.com/mod/guideme/version/hFpGwC6q), and
[Neo Vitae](https://modrinth.com/mod/neo-vitae/version/oDNiS7Ki).
JEI on 1.21.1 remains unchanged: its new MezzConfig prerequisite is tracked in
[#462](https://github.com/cTux/ae2-crafting-time/issues/462).

## Automated focused checks

Runtime source: `106fa660bd980b2bc7265d38aaeb4142b4b1b2c8`.
Subsequent changes document results and the verified GuideME maximum.
The integration merge also retains the separately verified addon and loader
maxima from PRs #456 and #464. Their adjacent catalogue rows were combined
without changing any selected artifact. No combined-graph smoke was run; these
receipts continue to describe each original focused graph and its loader.
All four CodexVM `craft-plan` runs passed semantic assertions, archive, and
cleanup. Every plan, three sort states, and tooltip screenshot was manually
reviewed: readable TTC labels and controls, no overlapping or clipped plan UI.
JEI's item grid/search rendered alongside the plan. These checks do not assert
JEI recipe-transfer behavior or Neo Vitae machine behavior.

Automatic visual gates remain `REVIEW_REQUIRED` without qualified baselines;
manual review is separately PASS. Screenshots are 2558 x 1093 at automatic GUI
scale 4 (640 x 274 logical screen).

| Client / mod | Previous pin | Verified pin | Campaign | Seconds |
|---|---|---|---|---:|
| Forge 1.20.1 / JEI | 15.56.0.204 | 15.59.0.212 | `20260920T060614127Z` | 256.655 |
| Fabric 1.20.1 / JEI | 15.56.0.204 | 15.59.0.212 | `20260920T061032890Z` | 246.563 |
| NeoForge 26.1.2 / JEI | 29.34.0.88 | 29.37.0.100 | `20260920T061441581Z` | 57.837 |
| NeoForge 26.1.2 / Neo Vitae | 26.1.2-1.0.27 | 26.1.2-1.1.26 | `20260920T061541537Z` | 58.702 |

Receipts: `build/ui-smoke/campaigns/<campaign>/compatible/{result,gate}.json`.
Resolved JARs, hashes, profiles, selection, screenshots, and sidecars are retained
with each campaign and its gate-named archive under
`E:/games/mc-instances/.codex-test-results/ui-smoke/clients/`.

## GuideME 21.1.19 manual check

Previous verified maximum: 21.1.17. The minimum stays 21.1.0.
On Minecraft 1.21.1, NeoForge 21.1.238, Java 21, AE2 19.2.17, and GuideME
21.1.19, a disposable copy of the prepared fixture opened our guide book.
The test-driver JAR was inert; this was a manual VNC check, not an automated
scenario or automatic visual-gate pass. The maximized VM capture is 2558 x 1199
including window chrome/taskbar, with `guiScale:0`.

Observed PASS: home page, Chapter 1 link and text, Chapter 2 navigation, Time
estimates page with its embedded screenshot, and scrolling the image. The
runtime log identifies GuideME 21.1.19 and `ae2craftingtime:guide`. Exact JAR and
screenshot hashes, profile, log, start/cleanup receipts, and manual verdict are
archived as `20260920-guide-21.1.19` under the same archive root.
The client exited normally and its exact scheduled task was removed and checked
absent. No other client ran concurrently.

The disposable options copy failed its legacy key migration and reset the
attempted keyboard binding; ordinary book use succeeded. The log also contains
VM audio initialization failure and recoverable missing Mekanism blocks from
the broader fixture world. These did not prevent the scoped book check and are
not claimed as GuideME regressions. No repository fixes were made.

## Timing

Campaign durations include staging, builds/reuse, loading, assertions, and
cleanup; build timings overlap those totals. The manual interval includes
first-run UI handling, page review, and cleanup rather than only load time.

| Part of the smoke UI testing task | Time | Why it took that long |
|---|---:|---|
| VM reuse and client staging separately | not measured | Existing guest; disposable runtime per run |
| Four automated focused campaigns | 619.757 s | Sequential loading and plan/sort/tooltip checks |
| Builds within automated campaigns | Forge JEI 15 s; Fabric JEI 31 s; Neo Vitae 1 s | NeoForge JEI build time not separately recorded here |
| GuideME host bundle build | 48 s | Separate host Gradle bundle, before the manual client interval |
| GuideME launch through cleanup | 509.848 s | Loading, first-run UI, book navigation/image/scroll review, and task cleanup |
| Manual setup/cleanup retries separately | not measured | Reset key binding; cleanup retried after process exit and with process-scoped execution policy |
| Automated screenshot review separately | not measured | All 20 full PNGs and sidecars inspected |
| Loading, assertions, cleanup individually | not measured | Included in the measured campaign/manual intervals |
| **Total measured run/build time** | **1177.605 s** | Excludes unmeasured staging/review; not total task wall time |
