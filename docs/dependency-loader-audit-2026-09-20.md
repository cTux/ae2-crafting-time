# Loader release audit, 2026-09-20

Part of [#452](https://github.com/cTux/ae2-crafting-time/issues/452).
Only the highest verified loader pins and their documentation change. Minimum
requirements, Minecraft targets, AE2, optional addons, and integration code stay
unchanged. These are focused base-client checks, not a full addon-suite pass.

Official release metadata was checked on September 19:
[Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html),
[Fabric](https://meta.fabricmc.net/v2/versions/loader), and
[NeoForge](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml).

## Verification

All four CodexVM clients ran the `craft-plan` scenario with the base dependency
graph. Each passed semantic assertions, evidence archival, and process cleanup.
The plan, three sort states, and tooltip were individually reviewed: readable
TTC badges, controls, and tooltip; no clipping or overlap. The automatic visual
gate remains `REVIEW_REQUIRED` because no qualified baseline exists; manual
review is separately PASS. Framebuffer: 2558 x 1093, automatic GUI scale 4,
logical screen 640 x 274. The scenario verifies the plan UI, not craft execution.

Runtime source commit: `0b2e5d1678b8b71b820dfb75f3d052892d414508`.
Subsequent changes only document these results and verified maxima.

| Client | Previous maximum | Verified maximum | Campaign | Seconds |
|---|---|---|---|---:|
| 1.20.1 Forge | 47.4.10 | 47.4.23 | `20260920T055804642Z` | 259.522 |
| 1.20.1 Fabric | 0.19.4 | 0.19.5 | `20260920T061918185Z` | 202.624 |
| 1.21.1 NeoForge | 21.1.238 | 21.1.251 | `20260920T062243008Z` | 160.558 |
| 26.1.2 NeoForge | 26.1.2.99 | 26.1.2.109 | `20260920T060434829Z` | 97.036 |

Receipts: `build/ui-smoke/campaigns/<campaign>/compatible/{result,gate}.json`.
Screenshots, sidecars, resolved mods, hashes, logs, and selection are retained in
each campaign and the archive named by its gate under
`E:/games/mc-instances/.codex-test-results/ui-smoke/clients/`.

Initial Fabric and 1.21.1 NeoForge attempts failed before Minecraft launched:
the task-created native launch manifests serialized an argument array as a
PowerShell object. Only those disposable manifests were corrected; repository
code was not changed. A retry also failed at the archive write precheck under
the restricted shell. The successful retries reused the exact unchanged base
bundles and ran through the authorized archive path.

## Timing

Campaign durations include staging, build or bundle reuse, loading, assertions,
and cleanup. Build times overlap those totals and must not be added again.

| Part of the smoke UI testing task | Time | Why it took that long |
|---|---:|---|
| Native loader installation and manifest setup | not measured | Separate native installations for four targets; task-only manifest repair |
| Four successful focused campaigns | 719.740 s | Sequential client loading and five plan checkpoints per target |
| Builds within those campaigns | Forge 50 s; NeoForge 26.1.2 52 s | Fabric and NeoForge 1.21.1 reused bundles built by their initial attempts |
| Initial Fabric / NeoForge 1.21.1 setup failures | 59.9 s / 65.0 s | Launch manifest shape rejected before Minecraft started |
| Restricted-shell retry | 1.7 s | Archive write precheck failed; no client started |
| Staging, loading, assertions, cleanup individually | not measured | Included in campaign durations; receipts do not isolate every phase |
| Manual screenshot review | not measured | All 20 full screenshots and metadata inspected |
| **Total measured campaign/failed-attempt time** | **846.3 s** | Excludes separately unmeasured setup and review; not total task wall time |
