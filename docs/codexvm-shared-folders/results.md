# CodexVM shared-folder recovery results

The 2026-09-12 HGFS failure is **not reproduced; cause unresolved**. Verification
on runtime source `baf6af0f1f38adff39e536811566d5f6ad56eaea` completed the
planned shared-folder round trip and one representative prepared-client smoke
without a launcher, VM, service or recovery change. The passing result proves
the route worked during this run; it does not explain the earlier system error
67 or justify claiming a repair.

The [specification](spec.md), [design](technical-design.md), and
[implementation plan](implementation-plan.md) define the no-change boundary and
the checks recorded here.

## Evidence status

| Criterion | Status | Evidence |
|---|---|---|
| SF-01 — Reproducible observations | PASS | The exact running CodexVM and interactive host context were checked before mutation. Workstation `26.0.0 build-25388281`, guest build 26200, Tools `13.1.0`, running `vmhgfs`, `vmci` and Tools services, and the registered HGFS provider were captured together. Between `2026-09-12T21:36:35.473Z` and `21:36:35.571Z`, HGFS root, exact share and file hash operations exited 0 with null errors in 9 ms, 6 ms and 84 ms. |
| SF-02 — Transfer proof | PASS | The retained #353 bundle still matched on the host and through HGFS. A new 72-byte input with SHA-256 `6DC5C99C5CDBE06125E453D90695B9029FDA8382B9DFCDE05BDE8EC7CD55CBE2` copied through the exact worktree share into guest-local storage in 7 ms. SSH dispatch session 0 and a disposable interactive session 1 both read the same bytes and hash and returned reports through HGFS with exit 0. |
| SF-03 — Preserve other work | PASS | No Java client was active before the run. The dispatcher added only the exact session-worktree share, changing the inventory from 54 to 55 entries; the existing 54 definitions were preserved. No VM, service, provider, launcher, credential or unrelated share was changed. The disposable transfer task and guest directory, smoke task, client process and disposable world were removed or confirmed absent afterward. |
| SF-04 — Cause before correction | PASS FOR NO-CHANGE DECISION | All current probes and the normal smoke dispatch passed without recovery. The historical failure remains **not reproduced; cause unresolved**. No correction or launcher change is claimed. A causal claim would still require the same HGFS operation to fail and recover while its mapping, provider and request context are captured. |
| SF-05 — Normal staging smoke | PASS | One clean `1.20.1-forge`, compatible/base-only Forge `47.4.10`, AE2 `15.4.10`, Java 17 `craft-lifecycle` run passed. All 12 semantic checks, 13 required PNG files and sidecars, fixture equality, report return, archive and cleanup gates passed. PID 3980 exited 0 and was confirmed absent. The automatic visual result remains `REVIEW_REQUIRED`; manual review found every required image readable and unclipped. |
| SF-06 — Honest provenance | PASS | Transfer diagnostics retain their actual #353 bundle identity. The new bundle was freshly sealed for `baf6af0f1f38adff39e536811566d5f6ad56eaea`; captures, semantic results, fixture evidence and the archive bind to that runtime SHA. This later evidence-only documentation commit was not executed as Minecraft code. GitHub CI remains separate from local verification. |

## Shared-folder and transfer record

The exact session worktree was registered through the existing dispatcher as
`ae2ct-9c4ca584d792`. Before registration, the VM had 54 configured shares.
Afterward it had the same 54 entries plus this enabled readable/writable mapping.
No recovery backup was needed because no failed component or corrective mutation
was demonstrated.

The retained #353 bundle stayed separate from current-head proof:

| Bundle-relative file | Host and guest SHA-256 | Guest hash time |
|---|---|---:|
| `profile.json` | `358E4D41CC8D023591900E367E8EA4CABD0351F2A972E2E65847AFF05445DDFB` | 4 ms |
| `mods/ae2-crafting-time-1.2.5-forge-1.20.1.jar` | `2240B3E1E8FC7EC1DF3558EB265667B8E7074C9C9DC9FFFB59DEB43FF9E67A44` | 76 ms |
| `mods/ae2-crafting-time-1.2.5-forge-1.20.1-test-driver.jar` | `41C99D08B616BEB58C0DE84FD638986BA27D5E780E8D59E902528CDBB486D78F` | 271 ms |

The task-owned transfer evidence is retained under
`build/issue-400-verification/20260912T2133Z`:

| File | SHA-256 |
|---|---|
| `inbound.txt` | `6DC5C99C5CDBE06125E453D90695B9029FDA8382B9DFCDE05BDE8EC7CD55CBE2` |
| `dispatch-report.json` | `004D1463EE4C8BD3DE798B77478514606E46EB41AE51B1AD57917CEE73B7872E` |
| `interactive-report.json` | `8AB82F1435C7CE80AFE5177CE2391144E1FE23AD9D2A1A94055E6D5C20A26210` |
| `prelaunch-vnc.png` | `BA069473E2525A25D166960A55807E2EA2ABBBD18877344516984B72A9E6373B` |
| `postcleanup-vnc.png` | `82013B4A4464EB9B4B1F9D4E078BF13C1E2B70C44729B081263A0EFCE5C8C076` |

## Representative smoke

Plan-only selection required exactly the compatible, base-only primary graph for
`1.20.1-forge` and the `craft-lifecycle` case. Forge 47.4.23, other targets and
the rest of the suite were not selected. The current-head bundle was freshly
built, not reused:

| Artifact | SHA-256 |
|---|---|
| Complete bundle | `309C638D0202835520B92CCCD2DE2F3A78FD7D5BBC3F7C3EFAB0699905AF48FF` |
| Production JAR | `C10B71951FE6CC9F590A9E642E84A980D1FAFC40DB98A4F54BE49BFC53E45CC9` |
| Test-driver JAR | `1462971C41BEE45E2F94461430D5E46ADB52F9B40C0F97098A5D1939D31460D7` |
| Applied Energistics 2 15.4.10 | `FBFEE05C6674CB6B00FE425E945CA4818D41EB34069B60DCB4A36221A0390FCC` |
| GuideME 20.1.15 | `CF8052AB3DA121012EFC59A20E731C24BFE7488DFA556C9B88809F42DEA3B24A` |

All 12 semantic checks passed: `plan`, `submitted`, `status`, `profile-sample`,
`total-cleared`, `completed`, `output`, `plan-no-data`, `plan-partial`,
`accuracy-full`, `accuracy-partial` and `details-chat`.

The 13 required PNG paths and 13 screenshot sidecars are present. They contain 12
content-distinct frames because `details-chat.png` and `job-accuracy-full.png`
use the same frame, which visibly supports both checks. Every image was manually
reviewed. The maximized captures were readable and unclipped; all sidecars
reported effective GUI scale 4, scaled dimensions 640 by 274, and contained GUI
bounds. This manual pass does not change the automatic `REVIEW_REQUIRED` result
caused by missing qualified baselines.

The source fixture hash before and after was
`EA4DC57046695FAD8174737A887DA4543719755FFDB87293CC4159D13FDAF6B3`.
Fixture metadata before and after was
`6B51BEC088FE2A3F5B39F99479D8933D9D168FAA0B5086C2AF090FF6481C65D5`.
The disposable world was removed. The only Minecraft process, PID 3980, started
at `2026-09-12T21:40:05.8955304Z`, exited 0 at
`2026-09-12T21:44:03.8779812Z`, and was absent during final cleanup.

The archive is
`E:/games/mc-instances/.codex-test-results/ui-smoke/clients/20260912T214407923Z-4d80bd62`:

| Archive file | SHA-256 |
|---|---|
| `manifest.json` | `12E20544E72B8EE5FE14F49AC37D34AC361CC52118266D042DEDA974730B6026` |
| `report.md` | `37C6A479273CB7905290D92F662946E138D02B8C8139335DF943055942BBDFC1` |
| `gate.json` | `9177F0147EAE9A6B519F78B9963809204BE266BA54DB82DCFB2383C8BA8F3895` |
| `selection.json` | `5BE832A3B31DAE2A447452B965D1221D3C9840334D0CE56AF3E882764EBF811A` |
| Runtime `status.json` | `5CB0B42B99021AC9D4647256C6084C7C2BF07D74FE3962C380A1D9F2ED7FE179` |
| Semantic `result.json` | `7F367BC41E9B36F40BB37C2FB8857E7F17EB28CE7CC8B0EB7D1C79AEB40D992A` |

The semantic, archive and cleanup gates are `PASS`. The overall automatic gate
is `REVIEW_REQUIRED`, solely because the 13 checkpoints lack qualified visual
baselines.

## Measured timing

Overlapping phases are shown separately and must not be added together.

| Part | Time | Why it took that long |
|---|---:|---|
| Plan-only selection | 1.9 s | Selected one compatible Forge graph and case. |
| VM, account and provider snapshot | 6.3 s | Checked the interactive host and SSH guest contexts before mutation. |
| Dispatcher share-registration probe | 9.7 s | Added the exact worktree mapping and confirmed no prior smoke was active. |
| HGFS round trip | 8.5 s | Read the root/share/file, hashed both bundles, copied into guest-local storage and returned both reports. |
| Controller preflight retries and VNC | 21.1 s | Corrected command quoting, execution-policy invocation and native-stderr capture before launch. No Minecraft retry occurred. |
| Host build | 53 s | Built the production and test-driver artifacts. |
| Complete campaign | 5m 09.486s | Included build, staging, one native run, report return and gates. |
| Guest runner | 4m 01.034s | Prepared the local runtime, ran the scenario and retained its report. |
| Minecraft process | 3m 57.982s | One Java launch through clean exit. |
| Startup through world join | 2m 46.160s | Forge/mod loading and disposable fixture-world load. |
| UI assertions through quit | 1m 04.599s | Ran the complete and partial craft lifecycles and captured evidence. |
| Client shutdown | 7.223 s | Saved the disposable world and confirmed exact-process exit. |
| Automatic visual validation | 831 ms | Evaluated all 13 checkpoint contracts. |
| Archive copy | 159 ms | Copied the retained campaign. |
| Manual visual review | not independently measured | Reviewed all 13 required images in three batches. |
| Task-owned guest cleanup | 6.7 s | Removed the disposable transfer directory and confirmed both tasks and PID 3980 were absent. |
| Retained diagnostic-to-cleanup window | 14m 53.197s | Measured from `2026-09-12T21:31:46.108Z` to `2026-09-12T21:46:39.305Z`. |

## Completion gate

SF-01 through SF-06 are complete for the documented no-change outcome on
runtime source `baf6af0f1f38adff39e536811566d5f6ad56eaea`. This record does
not claim the historical cause was found or repaired. The evidence-only commit
that contains this final record is newer than the runtime SHA; rerunning
Minecraft for that documentation-only change would not add runtime coverage.
GitHub CI and merge readiness remain separate workflow gates.
