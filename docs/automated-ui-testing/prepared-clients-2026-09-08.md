# Prepared-client single-world smoke, 2026-09-08

All four prepared targets completed their full selected coverage: **122 passing cases across eight required dependency graphs**. Each graph used one native client process and one loaded disposable world. The driver performed every known action without agent screenshot decisions between cases. Separate graphs are required where dependency versions cannot coexist.

Tracking: [#351](https://github.com/cTux/ae2-crafting-time/issues/351). Fixes: [#360](https://github.com/cTux/ae2-crafting-time/pull/360). This measures the prepared repository clients; the earlier [Project Infinity 0.0.52.0 run](project-infinity-0.0.52.0.md) is a separate environment.

## Completed full runs

| Target | Cases | Client launches | Total wrapper time | Captured source | Campaign |
|---|---:|---:|---:|---|---|
| 1.20.1-forge | 42 | 3 | 941.745s | `26cecdd9` | `20260908T153842637Z` |
| 1.20.1-fabric | 19 | 1 | 359.421s | `26cecdd9` | `20260908T155424970Z` |
| 1.21.1-neoforge | 36 | 2 | 621.347s | `0420da0d` | `20260908T152728902Z` |
| 26.1.2-neoforge | 25 | 2 | 256.93s | `a495343d` | `20260908T150758501Z` |

Combined successful full-run wrapper time: **2179.444s**. This excludes failed attempts, implementation, subsequent focused checks and manual review. It is not total task time or a matched cold/warm benchmark.

## Time by phase

All values are seconds. Capture writes are included in UI time, not an additional cost. Preparation combines host builds/dependency work and guest staging; those steps lack separate reliable timer boundaries. Loading includes native client startup, resources and the first world-ready observation.

| Target / graph | Preparation | Loading | UI incl. captures | Capture writes | Fixture resets | Exit / collection | Graph total |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1.20.1-forge / primary | 26.938 | 260.211 | 203.904 | 10.555 | 33.125 | 9.564 | 533.742 |
| 1.20.1-forge / newest NeoEco | 17.503 | 155.999 | 10.856 | 0.354 | 1.218 | 5.933 | 191.51 |
| 1.20.1-forge / AdvancedAE | 17.467 | 170.32 | 16.067 | 1.269 | 2.64 | 6.265 | 212.758 |
| 1.20.1-fabric / primary | 18.447 | 158.337 | 164.979 | 8.368 | 6.266 | 8.44 | 356.47 |
| 1.21.1-neoforge / primary | 25.744 | 222.62 | 202.382 | 10.167 | 3.631 | 7.684 | 462.061 |
| 1.21.1-neoforge / AdvancedAE | 10.34 | 125.736 | 13.627 | 1.158 | 0.645 | 5.169 | 155.517 |
| 26.1.2-neoforge / primary | 23.446 | 18.692 | 165.252 | 8.088 | 2.413 | 5.007 | 214.81 |
| 26.1.2-neoforge / AdvancedAE | 11.277 | 15.223 | 9.911 | 0.876 | 0.358 | 3.316 | 40.086 |

Wrapper overhead outside the graph timers includes planning, dispatch gaps and campaign archiving: 1.20.1-forge: 3.736s; 1.20.1-fabric: 2.951s; 1.21.1-neoforge: 3.769s; 26.1.2-neoforge: 2.034s.

Forge restores a nonempty native structure between cases. The NeoForge fixture regions are empty and restore by clearing. Every run retained the schema-2 plan, shared world ID, process ID, ordered results, fixture hashes, JAR hashes and original PNG/JSON captures. Native process exit and source-fixture integrity were checked separately from semantic case completion.

## Source and validation boundaries

- Forge and Fabric full runs used `26cecdd9289a204e97998a4898b4abcfba38ef4e`.
- NeoForge 1.21.1 full coverage used `0420da0d765d2fde307b2057f78ff8fc7d9a506a`.
- NeoForge 26.1.2 full coverage used `a495343d255082449fe4d86e6b5bc198378d98f1`. All 25 native cases passed and both clients exited normally. Its original primary host verdict remains FAIL: the old validator treated JSON property order as significant. The unchanged evidence passed the complete corrected validation block at `0420da0d`, including exact check membership, identities, adapters, screenshots, bounds, managed driver and fatal-log checks. A separate receipt and hashes preserve the original failure rather than rewriting it.
- The final two-line fixture change establishes physical LOW before the pulse through a real neighbor update. Focused NeoForge lock checks verify that delta separately; they do not replace either full-suite result or its timings.
- Known title issue [#350](https://github.com/cTux/ae2-crafting-time/issues/350) remains outside approved visual references. Subsequent clean NeoForge 1.21.1 runs do not establish root-cause fixes for the earlier startup [#357](https://github.com/cTux/ae2-crafting-time/issues/357) or shutdown [#362](https://github.com/cTux/ae2-crafting-time/issues/362) failures.

## Reproduce

Run `./scripts/run-ui-smoke-matrix.ps1` on the host with the four native clients prepared in CodexVM. Use `-Target 1.20.1-forge` or another supported target to run only that target and its required adapter graphs. Java runtimes are 17, 17, 21 and 25 respectively; builds stay on the host. Keep source and dependency inputs unchanged while each target plan executes. A changed plan is rejected rather than silently running mixed artifacts.

Screenshots are captured during execution and reviewed afterward. Review can overlap the next client; it never determines the next known UI action. No visual baseline was added or automatically approved. Automatic visual status remains `REVIEW_REQUIRED` until the separate reference-qualification work is complete.

## Fixes verified by these runs

The suite now clears empty fixture templates while retaining fatal checks for failed nonempty restoration. Fabric provider injection matches the AE2 owner and method name across named and intermediary mappings, with required injection counts and bytecode regression checks preserved. Terminal opening and amount submission occur once per transition.

Provider fixtures use a bounded craft quantity, sufficient CPU capacity, exact pending-input drainage and a two-item recipe with one free item slot to test partial insertion. Fabric fixture completion waits for its native grid and drive to become ready within the existing timeout. Lock testing establishes an actual LOW neighbor state before the later rising edge. AdvancedAE check sets describe their required CPU checks, and the host compares exact case-sensitive check membership without depending on JSON key order.

The separate prepared Forge 47.4.23 installation also needed its raw Minecraft JAR in the loader ignore list to prevent duplicate modules. That installation correction is separate from the source patch.
## Attempt ledger

A failed attempt is not a full-smoke benchmark. Setup-only failures did not launch a client. Durations below are target-wrapper wall time unless stated otherwise; captures and review are not added twice.

| Campaign or attempt | Target | Wall time | Outcome |
|---|---|---:|---|
| `20260908T130929875Z` | Forge 1.20.1 | 758.073s | Primary 10 passes then BM failure; newest NeoEco startup failure; AdvancedAE input failure |
| `20260908T132242115Z` | Fabric 1.20.1 | 117.888s | Packaged provider mixin injection failure, no cases |
| `20260908T132513130Z` | NeoForge 1.21.1 | 268.723s | Primary early-display startup failure; AdvancedAE empty-fixture reset failure |
| `20260908T133041114Z` | NeoForge 26.1.2 | 85.297s | Both graphs stopped after first case at empty-fixture reset |
| `20260908T134904096Z` | NeoForge 26.1.2 | 3.484s | Stale plan, no launch |
| `20260908T135006167Z` | NeoForge 26.1.2 | 354.218s | Primary craft-plan timeout; AdvancedAE input timeout |
| `20260908T140001125Z` | Forge 1.20.1 | 1250.262s | Primary 34 passes then input stall and exact-process stop; newest NeoEco 2/2; AdvancedAE stale plan |
| Focused request at `14:22:59Z` | NeoForge 26.1.2 | 0.092s | Invalid suite/project combination, no launch |
| `20260908T142331879Z` | NeoForge 26.1.2 | 39.896s | Input fixture driver pass; missing host AdvancedAE contract, subsequently validated |
| `20260908T143105224Z` | All-target command | 964.586s | Forge primary 32 passes then oversized default no-power job; newest NeoEco 2/2; AdvancedAE 4/4; remaining targets rejected stale plans without launch |
| `20260908T144837306Z` | Fabric 1.20.1 | 310.433s | 7/19 then fixture Optional exception |
| `20260908T145348240Z` | NeoForge 1.21.1 | 849.834s | Primary 30/32 then fixture CPU_TOO_SMALL; AdvancedAE 4/4 semantics but shutdown spin required process stop |
| `20260908T150758501Z` | NeoForge 26.1.2 | 256.930s | All 25 native cases; primary host property-order rejection corrected through preserved-evidence revalidation |
| `20260908T151215908Z` | Forge 1.20.1 | 21.336s | Stale plan, no launch |

The 1.21.1 AdvancedAE exit stall is tracked in [#362](https://github.com/cTux/ae2-crafting-time/issues/362). Two thread dumps showed the server repeatedly processing chunk unload continuations while the render thread waited for disconnect. The four semantic passes do not establish a clean full-graph pass. That attempt spent 294.841s after its last case in shutdown, diagnosis and collection.

Preparation combines host build/dependency work and guest staging because the wrapper does not record separate reliable boundaries for those parts. It is not pure build time. Initial loading includes resources, client startup and first-world readiness. UI time includes capture writes; resets are separate. Capture durations come from each sidecar's monotonic timer. All phase intervals use timestamped evidence, not estimated frame counts.


The next Fabric attempt, `20260908T152124211Z`, took **363.741s** and passed 18/19 cases before pulse recovery failed. Its 73 original captures and normal process exit are retained. The subsequent complete 19-case run is the successful measurement above.


## Final focused verification

The final fixture delta was checked at `26cecdd9289a204e97998a4898b4abcfba38ef4e` using `-Scenario locked-status` on both NeoForge targets. Each selected primary and AdvancedAE graph passed and exited normally. These checks are additional validation, not full-smoke timing samples.

| Target | Campaign | Wrapper time |
|---|---|---:|
| 26.1.2-neoforge | `20260908T160025175Z` | 80.995s |
| 1.21.1-neoforge | `20260908T160146590Z` | 374.606s |

## Evidence and review

Original campaign directories are retained below `build/ui-smoke/campaigns/<campaign>/compatible`; immutable campaign archives are retained by the existing archive runner. They contain selections, artifact hashes, ordered results, launcher logs, process-exit records and original screenshots with capture sidecars. The 26.1.2 correction additionally has a separate `20260908T153455159Z-neo26-revalidation` archive containing the corrected receipt, validator, hashes and review records. The original rejected verdict is unchanged.

The completed full runs contain 107 Forge, 74 Fabric, 101 NeoForge 1.21.1 and 86 NeoForge 26.1.2 original screenshots. Each original was independently inspected by two reviewers after capture. Hash, dimension and GUI-bound checks passed. Review does not grant visual-baseline approval, and the known title anomaly remains separately tracked.

| Full-run review | Reviewer A window / elapsed | Reviewer B window / elapsed |
|---|---|---|
| Fabric | 16:01:32–16:06:35 UTC / 303s | 16:02:03–16:05:55 UTC / 232s |
| Forge | 15:50:38–15:56:36 UTC / 358s | 15:50:57–16:01:45 UTC / 648s |
| NeoForge 1.21.1 | 15:36:16–15:42:15 UTC / 359s | 15:36:33–15:41:57 UTC / 324s |
| NeoForge 26.1.2 | 15:22:04–15:27:48 UTC / 344s | 15:22:29–15:27:02 UTC / 273s |

Review windows overlap each other and client execution; some include waiting for later captures. They are elapsed review windows, not additive client time or measured reviewer CPU time. Focused-check review records are retained with the campaign qualification evidence.

Host validation and archive timings are recorded in each gate receipt and are contained in campaign time; they must not be added again to the wrapper total. Work before the task timer was started was not measured. Failed attempts, repair work, focused checks, local verification, CI and review are explicitly outside the 36m 19.44s successful-full-run total.

## Qualification status

The requested prepared-client coverage is complete. [#351](https://github.com/cTux/ae2-crafting-time/issues/351) remains open for qualified automatic references and matched cold/warm measurements. These results provide observed timings for the current prepared installations, not a controlled speedup claim or a promise that every future run has the same duration.

## Additional timing detail

Focused checks have one case, so no between-case fixture reset. UI ends at the logged result-written phase; capture writes are included in that interval.

| Focused target / graph | Preparation, s | Loading, s | UI, s | Capture writes, s | Exit / collection, s |
|---|---:|---:|---:|---:|---:|
| 26.1.2-neoforge / primary | 17.653 | 19.748 | 9.33 | 0.351 | 3.159 |
| 26.1.2-neoforge / rxYaglEe | 9.949 | 14.628 | 1.889 | 0.157 | 2.963 |
| 1.21.1-neoforge / primary | 21.634 | 192.179 | 12.917 | 0.39 | 4.693 |
| 1.21.1-neoforge / rxYaglEe | 9.341 | 121.548 | 5.744 | 0.13 | 4.812 |

Gate receipt costs below are milliseconds and are already included in the campaign wrapper. The original NeoForge 26.1.2 receipt remains rejected; its separate correction receipt is described above.

| Campaign | Gate validation, ms | Archive, ms | Immutable archive ID |
|---|---:|---:|---|
| `20260908T153842637Z` | 1987 | 873 | `20260908T155422690Z-45bb9094` |
| `20260908T155424970Z` | 1495 | 528 | `20260908T160022983Z-bed72b31` |
| `20260908T152728902Z` | 1776 | 712 | `20260908T153748308Z-67bd4dc1` |
| `20260908T150758501Z` | 486 | 616 | `20260908T151213930Z-cf3ca08f` |
| `20260908T160025175Z` | 394 | 273 | `20260908T160144941Z-482cbef5` |
| `20260908T160146590Z` | 387 | 392 | `20260908T160759894Z-ca170d47` |
