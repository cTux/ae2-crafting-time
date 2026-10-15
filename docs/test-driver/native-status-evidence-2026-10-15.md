# Fresh native Status evidence, 2026-10-15

This report verifies [issue #671](https://github.com/cTux/ae2-crafting-time/issues/671)
against implementation commit `787113fd78c65c99aea061649bb65cb8e9f7c476` in
[PR #713](https://github.com/cTux/ae2-crafting-time/pull/713). It covers the
development-only `standard-status-controls` scenario. Both runs used English,
the prepared compatible clients, the base AE2 dependency graph, and one JVM at
a time. This focused graph exercises the shared Forge and native NeoForge status
observers; it is not a full addon-compatibility qualification.

| Target | Campaign (UTC) | Time in Kyiv (UTC+3) | Result | Evidence archive |
| --- | --- | --- | --- | --- |
| NeoForge 26.1.2.109, Java 25 | `20261015T102355214Z` | 13:23:55–13:26:13 (2m 18s) | PASS, two launches, 14/14 checks | `20261015T102615259Z-a1f06f96` |
| Forge 1.20.1-47.4.23, Java 17 | `20261015T103932737Z` | 13:39:32–13:46:34 (7m 2s) | PASS, two launches, 14/14 checks | `20261015T104635863Z-be63b6d6` |

| Part of the smoke UI testing task | Time in Kyiv (UTC+3) | Why it took that long |
| --- | --- | --- |
| NeoForge first client | 13:24:14–13:25:44 (90s) | Client startup and first scenario phase. |
| NeoForge restart | 13:25:46–13:26:08 (22s) | Second launch and restart assertions. |
| NeoForge total wall time | 13:23:55–13:26:13 (2m 18s) | Includes setup, both launches and evidence finalization. |
| Forge first client | 13:39:50–13:43:45 (3m 55s) | Client startup and first scenario phase. |
| Forge restart | 13:43:47–13:46:29 (2m 42s) | Second launch and restart assertions. |
| Forge total wall time | 13:39:32–13:46:34 (7m 2s) | Includes setup, both launches and evidence finalization. |
| Host build and focused local checks | One observed build: 23s; local Gradle checks: 1m 8s and 44s | Prepared tested JARs and checked the focused driver/capture behavior. These operations occurred at different stages; no combined wall timer was retained. |
| VM recovery and Windows update | Not measured separately | VM startup/recovery and Windows update interrupted earlier attempts. |
| Diagnostic attempts and retries | Individual durations in the ledger below | Failures, an interrupted controller and process-overlap diagnosis preceded the clean campaigns; some attempts overlap. |
| Manual image review and VM cleanup | Not measured separately | Reviewed 48 PNGs and sidecars per target, then shut down the exact VM. |
| Retained campaign window | 12:47:42–13:46:34 (58m 52s) | From first retained attempt to final clean Forge receipt; later visual review and cleanup were not measured separately. |

Phase durations are contained in each campaign's total wall time; they are not
additional time to add to it. Diagnostic attempts below partly overlap, so
their durations are likewise not summed.

The sealed relaunch receipts bind the tested production and driver JARs to
these SHA-256 pairs (production, driver): NeoForge
`4a163e5d3ef8cbf60a387d28b72ed4028a7df07dbabcff5c93d5fb6c283cda1d`,
`e9fc182372faa655d19f88f4cc867dc43cff6ed8772a9764d255945f37628db3`;
Forge `cebcf7d540aba7fc936a19425f7fbac76c6fac33c923c48c53c464037496a50c`,
`63147a2381c637384205346225899b6fb59d3fdaf1654c6bd584e124e998edc3`.
The NeoForge bundle's `appliedenergistics2-26.1.10-beta.jar` SHA-256 is
`8412a2208e56989b90e783e0a594c91a955ab8785d7d020fa4d8f342346110f2`,
the same AE2 binary inspected for the original report.

The run reports and screenshot sidecars are in the matching local
`build/ui-smoke/campaigns/<campaign>/compatible/<target>/primary/run/evidence/`
directories. Each target retained 48 PNGs, their 48 capture sidecars and 12
other JSON evidence files. Manual review
of all 48 images per target found the expected title, row amounts, tooltip,
options, sort, scales, reset and restart states with the GUI contained in the
frame. All 48 PNG SHA-256 values per target match their capture sidecars; all
sidecar GUI rectangles fit their scaled screens. The semantic gate is `PASS`.
The visual gate remains `REVIEW_REQUIRED` because no qualified image baseline
exists; manual review does not create one.

The zero-progress and measured-progress captures keep the same selected job,
CPU and output within each run. Both sidecars identify synthetic status input
with elapsed time 10,000,000,000 ns and start count 100. Zero progress has
remaining count 100, completed count 0 and native title `00:16:40`; measured
progress has remaining count 50, completed count 50 and title `00:00:10`. The
Forge job UUID is `d608a7ee-e4bd-4db8-a745-8cc6ca700046`; the NeoForge job
UUID is `4ab649a5-e9b6-422a-8196-8a0bed75dad3`. The separate TTC CPU-card
estimate remains around five seconds. These captures demonstrate that the
native title responds to status progress, not to the TTC card time.

The final `status-server-profiling-off.png` checkpoint reacquires a native
status after the synthetic probes and a rendered frame with the same selected
job and CPU. Both clients show native title `2562047:47:16` while the TTC title
is disabled. Their native status has start and remaining counts `2147483647`,
completed count 0 and progress 0; the recorded status and CPU elapsed times
are about 59.35 s on NeoForge and 46.61 s on Forge. The title is therefore
neither elapsed time nor TTC's estimate. The evidence attributes the display
to AE2's native title path receiving **fresh native**, zero-progress status
counts, not synthetic fixture input. AE2's `ElapsedTimeTracker` starts with an
`Integer.MAX_VALUE` quantity and its native remaining-time calculation scales
elapsed time by remaining / max(1, completed). With completed count zero and
remaining count `2147483647`, that path produces the huge title. The test-driver
defect was a stale status observation after the synthetic probes; the menu
reopen and freshness gate repair that observation. The native zero-progress
input still exists, and the smoke attributes the remaining huge title to AE2's
native fallback without prescribing an upstream fix. TTC's production code
does not change those status counts, so this finding does not justify changing
its estimator. The [approved issue evidence comment](https://github.com/cTux/ae2-crafting-time/issues/671#issuecomment-6189995868)
records the native observation and its limit.

| Capture | PNG SHA-256 | Sidecar SHA-256 |
| --- | --- | --- |
| NeoForge fresh native | `995076978bedfb3075f6bdbf702df75e199ebce9b03642142de8fd9d8e83f636` | `2c61acb59a86d9fd2d78ccf41924cbafdbc9426e7b6feee8b35f83d44e336068` |
| Forge fresh native | `8108480f117d9b32efcaf54ee19a03031e0e1b986bbd171a4f6b85c7dc6e61f4` | `acb5d656826ff0c68b631d925f32d23e1ec66e9017f319666f821920a8e387ca` |

The original reported capture, sidecar and runtime JAR hashes remain in the
[technical design](technical-design.md#fresh-native-status-evidence-671); they
are historical identities, not visual baselines. Focused driver/capture checks
and all four test-driver JAR builds passed locally. The local Gradle run used
ignored `build/issue671/current-driver-first.gradle` to place the current
test-driver JAR ahead of a stale cached transformed 1.2.2 JAR. Clean GitHub CI
passed without that overlay. The tested implementation commit's
[Build](https://github.com/cTux/ae2-crafting-time/actions/runs/38719965801)
and [Tests](https://github.com/cTux/ae2-crafting-time/actions/runs/38719965842)
checks passed separately. The exact CodexVM shut down after visual review;
`vmrun list` returned zero running VMs.

Earlier attempts are retained as diagnostics. Unknown means no final receipt
established the runtime or client count. `FAIL_SETUP` means no client launched.
In the ledger, "Total runs" counts observed or attempted client launches, not
repeat campaigns.
The extra Java process in the diagnostic Forge pass prevented it from counting
as a clean qualifying run; the strict rerun had one JVM in each launch.

| Scenario | Run | Total runs | Time in Kyiv (UTC+3) | Duration | Result |
| --- | --- | --- | --- | --- | --- |
| Forge | `20261015T094742888Z`, `75fb3a1` | 1 observed; incomplete | Started 12:47:42 | Unknown | Interrupted during UI/resource reload; no final receipt. |
| Forge | `20261015T100448612Z`, `75fb3a1` | 1 attempted | 13:04:48–13:06:54 | 2m 6s | Failed: first launcher exited -1. |
| Forge | `20261015T100741857Z`, `75fb3a1` | Unknown | 13:07:41–13:10:14 | 2m 33s | Failed: file not found; client count uncertain. |
| NeoForge | `20261015T100841026Z`, `75fb3a1` | 0 | 13:08:41–13:09:03 | 22s | FAIL_SETUP: SSH dispatch rejected while Forge client active; no native client. |
| NeoForge | `20261015T101030792Z`, `75fb3a1` | 1 attempted | 13:10:30–13:13:36 | 3m 6s | Failed: first phase had no checkpoint. |
| NeoForge | `20261015T101919893Z`, `7b5ab448` | 1 attempted | 13:19:19–13:22:19 | 3m | Failed: first phase had no checkpoint. |
| NeoForge | `20261015T102355214Z`, `787113fd` | 2 | 13:23:55–13:26:13 | 2m 18s | PASS, 14/14 checks; qualifying. |
| Forge | `20261015T102623099Z`, `787113fd` | Unknown | Started 13:26:23 | Unknown | Interrupted; build artifacts only, no final receipt. |
| Forge | `20261015T102930060Z`, `787113fd` | 2 | 13:29:30–13:37:23 | 7m 53s | Diagnostic pass; extra first-phase Java process. |
| Forge | `20261015T103755617Z`, `787113fd` | 0 | 13:37:55–13:38:02 | 7s | FAIL_SETUP: malformed UNC argument, no client. |
| Forge | `20261015T103932737Z`, `787113fd` | 2 | 13:39:32–13:46:34 | 7m 2s | PASS, 14/14 checks; qualifying. |
