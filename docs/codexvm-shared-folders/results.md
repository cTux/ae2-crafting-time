# CodexVM shared-folder recovery results

The 2026-09-12 HGFS failure is **not reproduced; cause unresolved**. Later
read-only checks could read existing shares and hash the retained #353 bundle
through HGFS without changing the launcher, VM configuration, shares or
services. That proves the provider was working during those checks, not why it
previously returned system error 67 or that it recovered because of a specific
action.

There is no demonstrated repository or launcher defect to fix. Keep the current
dispatcher and working VM state unchanged unless a repeated failure identifies
a cause with a matching failure/recovery observation. The
[specification](spec.md), [design](technical-design.md), and
[implementation plan](implementation-plan.md) define that boundary and the
remaining verification.

## Evidence status

| Criterion | Status | Evidence and remaining proof |
|---|---|---|
| SF-01 — Reproducible observations | PARTIAL | The historical report records system error 67 from key-based SSH and desktop session 1. The later inspection records successful HGFS root, share and file reads with 30 ms, 2 ms and 3 ms elapsed times. A fresh structured snapshot with the exact operation, account/session, mapping, provider state, UTC timestamp, native exit and error is still required. |
| SF-02 — Transfer proof | PARTIAL | Host and guest HGFS hashes matched for all three retained #353 bundle files listed below. Copying into guest-local NTFS, returning a unique report through HGFS, and proving both dispatch and interactive contexts are still required. |
| SF-03 — Preserve other work | NOT STARTED | No recovery mutation was made, so there is nothing to roll back. Before any future mutation, retain the exact VM configuration and share inventory, check active work, and define rollback. Transfer and smoke cleanup must remove only artifacts owned by this run. |
| SF-04 — Cause before correction | PASS FOR NO-CHANGE DECISION | Successful reads coexist with old-share log warnings, so those warnings do not establish the historical cause. No correction or launcher change is claimed. The missing causal evidence is the same HGFS file operation failing and then succeeding while its mapping, provider and request context are captured. |
| SF-05 — Normal staging smoke | NOT RUN | The required clean HGFS-dispatched `1.20.1-forge`, `compatible`, Forge `47.4.10`, Java 17 `craft-lifecycle` smoke must wait until the implementation PR exists. It still requires 12 semantic checks, 13 required captures and sidecars, visual review, unchanged fixture hashes, returned evidence and exact-client exit. |
| SF-06 — Honest provenance | PARTIAL | The read-only investigation is bound to `63e3441ac5159b95eb984eb8963be95479deb024`; this results record starts from documentation merge `d94b6eeb17b7c36eeb7fb1a0eb0d83b10b80ba7e`. The reused bundle keeps its #353 identity and hashes below. Its recorded source revision and all current-head transfer/smoke artifacts remain to be retained; GitHub CI is separate. |

## Verified read hashes

These are the existing #353 bundle files read through HGFS during the later
inspection. Guest hashing took 105 ms, 81 ms and 272 ms respectively. They are
diagnostic reads, not a current-head build, guest-local copy, returned report or
smoke result.

| Bundle-relative file | Host and guest SHA-256 |
|---|---|
| `profile.json` | `358E4D41CC8D023591900E367E8EA4CABD0351F2A972E2E65847AFF05445DDFB` |
| `mods/ae2-crafting-time-1.2.5-forge-1.20.1.jar` | `2240B3E1E8FC7EC1DF3558EB265667B8E7074C9C9DC9FFFB59DEB43FF9E67A44` |
| `mods/ae2-crafting-time-1.2.5-forge-1.20.1-test-driver.jar` | `41C99D08B616BEB58C0DE84FD638986BA27D5E780E8D59E902528CDBB486D78F` |

## Completion gate

This record does not close the issue. After the PR exists, follow the plan in
order: capture the fresh structured state, complete the owned round trip in
both guest contexts, then run the one representative smoke. Update this file
with retained artifact identifiers, hashes, measured timings, cleanup and the
actual tested PR head. If HGFS remains healthy, keep the cause unresolved and
report only the checks that passed.
