# Millisecond TTC implementation verification

The original campaign below is the baseline for the initial feature. The
per-tick throughput follow-up has separate evidence at the end of this report.

The behavior in [the spec](spec.md) is implemented in [PR #694](https://github.com/cTux/ae2-crafting-time/pull/694). Runtime evidence was captured on `e47ede5914bad6e2772b7f11f3f719d4e9eebfe2`; later verification-document changes do not change those tested JARs. This is implementation evidence, not a release or full four-target compatibility qualification.

## Automated checks and review

All local tests, GuideME resource validation, the shared 100% line/branch coverage gate, and four production distribution builds passed. Requester and graph-depth regression checks passed after review fixes. GitHub Build all mod JARs and Gradle tests passed for the tested runtime commit. Automated review completed without new findings; the two earlier confidence findings were fixed and resolved.

| Criteria | Evidence |
| --- | --- |
| MS-1–MS-3 | TimeEstimate tests cover 50/500/1000ms, upward fractional rounding, sample tick conversion, positive 5ms minimum, and unchanged retained observations. Native compact-hover captures show samples in ms. |
| MS-4–MS-7 | Component and renderer tests cover strict Instant eligibility, confidence/status precedence, default-on/persisted option, Show emoji, and numerical fallback. Native compact-hover assertions cover reliable Instant, low confidence and option restoration. English/Ukrainian resources, GuideME and wiki updated. |
| MS-8 | Four targets compile/test. Three compatible catalogues pass semantically; focused 1.21.1 core evidence passes. Native chat shows a readable plain Unicode medal and green Instant at automatic GUI scale 4. Full 1.21.1 addon proof and scale-1 readability remain limited as described below. |
| MS-9 | Job estimate tests cover unknown/unreliable off-path rows, ambiguous edges, cycles, disconnected components and depth limits. Requester tests exclude fulfilled rows but retain uncertain pending rows. CPU tests retain duration and confidence. |
| MS-10 | Two-recipient component tests and each target's vanilla component serialization round trip preserve client-local settings, confidence and metadata. Native chat exercises refreshed Instant/numeric text. |
| MS-11 | Bounded estimate codec and loader packet round-trip tests preserve fractional ms and reliability; coordinated packet version changes prevent old seconds readers. Native CPU-list checks cover expiry, reconnect and restart. |

## Native results

All captures use English in the dedicated VM and matching host-built production/test-driver JARs. Clients run sequentially and their exact processes are confirmed stopped. PNG hashes, dimensions, effective GUI scale and contained GUI bounds were checked. Every REVIEW_REQUIRED checkpoint was manually inspected in full-frame contact sheets; Instant feature captures were also inspected individually. No qualified visual baseline exists, so automatic gates remain REVIEW_REQUIRED rather than an invented automatic visual PASS.

| Target | Scope and observed result |
| --- | --- |
| Forge 1.20.1 | Change-selected compatible catalogue: 43 unique scenarios in five graphs, semantic PASS. 296 captures reviewed. |
| Fabric 1.20.1 | Change-selected compatible catalogue: 25 unique scenarios in three graphs, semantic PASS. 248 captures reviewed. |
| NeoForge 26.1.2 | Independent compatible catalogue: 28 unique scenarios in four graphs, semantic PASS. 267 captures reviewed. |
| NeoForge 1.21.1 | Compatible primary crashed in NeoEco AE2 1.1.1 after standard-plan-controls passed. Full catalogue FAIL; remaining addon graphs not run. Independent BaseOnly compact-hover-numbers passed (24 captures). CPU-list retry with explicit background-On setup passed (28 captures), including reconnect and two distinct process identities. Seven pre-crash completed-case captures were also reviewed. |

The known NeoEco renderer failure is [#627](https://github.com/cTux/ae2-crafting-time/issues/627#issuecomment-6128845981): RenderChunkRegion index -21, then FixedBlockEntityRenderers.render. No dependency substitution or upstream workaround was made. A focused core pass cannot repair that full-catalogue failure.

Small TTC badges lose text strokes at GUI scale 1. Automatic scale 4 and scale 2 are readable. This separate limitation is tracked in [#695](https://github.com/cTux/ae2-crafting-time/issues/695); no before-change runtime comparison was performed, so this report does not claim it is proven pre-existing. Scale-1 screenshots are recorded as LIMITATION in the manual ledger.

The client and server require matching updated packet builds. Saved sample formats are unchanged. No merge or release was performed.

The first isolated 1.21.1 CPU case failed because its layout assertion requires background bounds while badge backgrounds default to Off. This independent fixture dependency is [#696](https://github.com/cTux/ae2-crafting-time/issues/696). Its raw FAIL receipt is retained. The retry uses an explicit test-only `badgeBackground=true` setup; no production default or assertion was changed.

## Evidence receipts

The immutable local archive root is `.codex-test-results/ui-smoke/clients/` beneath the configured game-instance directory. Each archive retains selection, result, gate, original PNG/JSON pairs, manifest hashes and visual contracts:

- `20261012T154033692Z-e4bdf19f`: earlier Forge failure. Its tree observer rejected a valid question mark; the observer and regression test were corrected before the runtime commit above.
- `20261012T165658058Z-4096bc46`: Forge/Fabric pass and the raw NeoEco failure. Overall FAIL, archive and cleanup PASS.
- `20261012T171157776Z-ca8b721c`: NeoForge 26.1.2 semantic/archive/cleanup PASS, automatic visual REVIEW_REQUIRED.
- `20261012T171624796Z-47b83587`: focused NeoForge 1.21.1 short-time case semantic/archive/cleanup PASS, automatic visual REVIEW_REQUIRED.
- `20261012T171943108Z-76941c78`: isolated CPU fixture setup failure before restart, raw FAIL retained.
- `20261012T173408439Z-72ce55a0`: isolated CPU retry semantic/archive/cleanup PASS, automatic visual REVIEW_REQUIRED. Test-only saved TTC config restored to its original absent state after both processes exited.

The worktree's ignored `build/ttc-visual-review/reviewed.json` holds individual manual verdicts. Full PNGs remain in the archives; contact sheets are review aids only. Local logs include `ttc-tests.log`, `ttc-verification.log`, `ttc-requester-checks.log`, `ttc-tree-checks.log` and the campaign logs. CI is recorded separately from local checks.

## Smoke timing

Times come from host run receipts and guest status/case timestamps. Preparation combines host build, dependency resolution and staging because they lack separate receipt boundaries. Single-case restart runs combine loading and UI assertions. Gate/archive timings are already inside the campaign wrapper; review overlaps execution and must not be added again.

| Part of the smoke UI testing task | Time | Why it took that long |
| --- | ---: | --- |
| VM setup and first cancelled staging | not measured | Reused the running VM; initial staging was cancelled for a review fix. No reliable phase timer was retained. |
| Earlier Forge retry | 10m 5s | Preparation 103s, native execution 499s, cleanup/return 3s. Tree observer then rejected the new `?` label. Native first load 105s, completed assertions 363s, case resets 17s; remaining time includes startup/exit and failing case. |
| Forge successful selected catalogue | 38m 24s | Five native graphs, including CPU expiry and two process-restart scenarios. Sum: preparation 255s, native 2028s, cleanup/return 21s. |
| Fabric successful selected catalogue | 23m 50s | Three graphs with CPU expiry and restarts. Sum: preparation 116s, native 1303s, cleanup/return 12s. |
| NeoForge 1.21.1 failed compatible primary | 4m 48s | Preparation 70s, native 218s, cleanup/return under 1s. Initial mod/world load 101s, completed case 105s, then upstream NeoEco renderer crash. |
| Four-target wrapper gate/archive | 20s | Validation 14.385s and archive copy 5.502s, outside the individual graph times above; overall FAIL retained. |
| NeoForge 26.1.2 successful catalogue | 11m 52s | Four graphs. Preparation 124s, native 571s, cleanup/return 17s. Primary initial load 13s and assertions 259s; isolated addon initial load 9s and assertions 33s. |
| NeoForge 26.1.2 gate/archive | 10s, inside wrapper | Validation 7.547s and archive copy 2.296s. |
| NeoForge 1.21.1 focused short-time check | 3m 58s | Preparation 23s, native loading and UI assertions 212s, cleanup/return 3s. Its gate/archive took another 1.002s. |
| NeoForge 1.21.1 isolated CPU failed attempt | 2m 56s | Preparation 8s, native 168s, cleanup/return under 1s. Default-Off badge setup failed the fixture assertion before restart; gate/archive another 0.174s. |
| NeoForge 1.21.1 isolated CPU successful retry | 6m 38s | Preparation 8s, native loading/UI/reconnect/restart 388s, cleanup/return 3s. Gate/archive another 1.224s. |
| Screenshot review | overlapping, not separately measured | Full-frame manual review, individual Instant inspection, hash/scale/bounds checks, and scale-1 follow-up. |
| Total measured execution window | 2h 3m 41s | From first retained attempt at 15:30:28.288 UTC through final runtime archive at 17:34:09 UTC. Includes failed attempts, source corrections, diagnosis, sequential runs and overlapping review. Additional setup and final documentation/review time are not measured. Successful target execution totals above remain separate. |

## Per-tick throughput follow-up (MS-12)

Runtime commit `5579df6612d27a854565b038646049be3bb8d40b` removes visible `/t` and
`/т`. Hover tooltips and timed chat keep per-second throughput beside sample or
average/latest ms times. Rate-only displays use amounts/ms and amounts/s. The
conversion divides the stored per-tick value by 50; internal stats and packets
are unchanged. Both GuideME locales and wiki detail pages were updated.

All four target tests, GuideME validation and shared 100% line/branch coverage
passed (`gradlew test jacocoTestReport checkGuideResources`, 1m 37s). Boundary
tests cover valid/absent/invalid samples, both languages and resource units,
compact/regular hover, full chat precision and the two chat argument shapes.
GitHub build/tests passed and automatic review completed without new findings
on this runtime commit. Later commits change documentation only.

Focused BaseOnly `compact-hover-numbers` native runs passed on NeoForge 26.1.2
and Fabric 1.20.1. Each captured 24 full PNG/JSON pairs. All 48 were manually
reviewed with PNG hash, dimensions, effective scale and contained-GUI checks.
The screenshots show per-second throughput beside ms samples and no redundant
per-tick or per-ms rate. Both exact clients exited normally. Semantic, archive
and cleanup gates are PASS; automatic visual gates remain REVIEW_REQUIRED
because no qualified baseline exists. This follow-up does not requalify the
complete four-target addon catalogue or replace the baseline's known limits.

Immutable archives: `20261012T182825729Z-3af59319` (NeoForge 26.1.2) and
`20261012T183310915Z-416b3684` (Fabric). The ignored worktree ledger is
`build/ttc-throughput-reviews.json`; logs are `ttc-throughput-tests.log`,
`ttc-throughput-smoke-26.log` and `ttc-throughput-smoke-fabric.log`.

| Part of the smoke UI testing task | Time | Why it took that long |
| --- | ---: | --- |
| VM setup | not measured | Reused the already running dedicated VM and installed clients. |
| NeoForge 26.1.2 build/dependency resolution/staging | 24.8s | Existing native runtime and cached dependencies; changed host JARs were rebuilt. |
| NeoForge 26.1.2 Minecraft loading and UI assertions | 56.2s | One compact-hover leaf checks Plan, Status, chat and regular hover across six rate fixtures. No separate loading/assertion timestamps exist for this leaf. |
| NeoForge 26.1.2 cleanup/return and gate/archive | 2.7s + 1.045s | Exact process exit, report validation and immutable archive copy. |
| Fabric build/dependency resolution/staging | 28.6s | Built and staged the older loader's matching production/test-driver JARs. |
| Fabric Minecraft loading and UI assertions | 222.5s | Older Minecraft/mod/world loading plus the same native UI assertions. No separate loading/assertion timestamps exist. |
| Fabric cleanup/return and gate/archive | 2.7s + 1.018s | Exact process exit, report validation and immutable archive copy. |
| Screenshot review | overlapping, not measured separately | Manual full-frame review of all 48 captures while clients ran sequentially. |
| Failed attempts/retries | none | Both focused native runs passed on their first execution. |
| Total measured execution window | 6m 10s | From first run receipt at 18:27:01.237 UTC through final archive at 18:33:11 UTC; includes dispatch gaps and overlapping review. Earlier setup and final documentation are not measured. |
