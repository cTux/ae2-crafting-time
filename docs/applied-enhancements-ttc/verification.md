# Applied Enhancements 1.1.4-forge verification

October 24, 2026. [Issue #754](https://github.com/cTux/ae2-crafting-time/issues/754),
[PR #755](https://github.com/cTux/ae2-crafting-time/pull/755).

## Result and limits

Focused native AE2 coexistence passed. No production compatibility correction
was needed. This is not a full addon-graph or enabled-AELIS planner qualification.
The later custom-table implementation is covered separately by the
[implementation verification](implementation-verification.md) for #756.

Baseline source: dd8615375deae602dc5d3946e86752e33eaae1de. This dated report
predates the AELIS implementation; it does not qualify later executable changes.

## Exact runtime

- Minecraft 1.20.1; Forge 47.4.23; AE2 15.4.10; GuideME 20.1.15.
- Applied Enhancements 1.1.4-forge beta, CurseForge project 1665696/file 9301183,
  published October 23 and retrieved October 24, 2026.
- Beta SHA-512: `06e1979668e72e187ea6df6fab712beed689cc486a75cc9b939ec0fed0fe1c7190079604ac60aedb1d8b0afcc46bc754aa1ef2f6d2962077853162bc60a1208d`.
- Crafting Time 1.2.19 production and matching test-driver JARs built on the host.
  Artifact SHA-256 manifests and loaded-mod receipts are retained per campaign.
- Guest Java 17.0.20.101, disposable offline worlds; English, effective GUI scale 4,
  framebuffer 2558x1093 and scaled dimensions 640x274.
- Guest configuration: `enable_automatic_planner=false`, `enable_plan_screen=true`,
  `enable_big_integer_planning=true`. No request for an enormous exact quantity
  was made. A temporary custom calculation screen appeared during startup;
  completed plan/status evidence covers native AE2.

## Checks and reviewed evidence

`test-ui-smoke-coverage.ps1` and `validate-optional-integrations.ps1` passed.
GitHub Gradle tests and Build all mod JARs passed on tested source dd861537.
Local `distMod` and `testDriverJar` completed successfully in 1m16s.

`standard-plan-controls` passed all nine assertions: plan, sorting, missing-first,
tooltip, details, reset, total TTC, layout and item resolution. All seven captures
were manually reviewed: default, three sort modes, tooltip, details and reset.
The total drops from about seven seconds to two after clearing stone history.
The panel and tooltip fit the frame and the missing material remains first.
Native plan GUI bounds: x201/y34, width238/height206.

`running-status` passed submission, running, progress, header and layout checks.
Both captures were manually reviewed. The first shows an active stone step and
waiting smooth-stone step; the second shows returned progress and remaining
smooth-stone work. CPU list and header TTC remain visible. Native status bounds:
x201/y45, width238/height184. This does not qualify craft completion, all status
controls, every provider warning, or arbitrary custom CPUs.

The automatic gates retain REVIEW_REQUIRED because this render environment has
no qualified visual baseline. Manual review passed all nine captures; the gate
files were not rewritten to pretend an automatic visual pass. Both gates record
semantic PASS, archive PASS and cleanup PASS. Exact clients 3228 and 9548 exited
with code 0 and passed status receipts before another client was launched.

The initial preflight failed because the new project's smoke coverage declaration
was missing. It ran no client. The declaration was added and its validator passed
before both successful runs. Logs retain offline profile-key and unavailable
OpenAL-device errors, absent optional-addon class warnings, old fixture recipe
warnings and occasional slow server ticks. No integration assertion failed.

## Retained local evidence

Paths are relative to this worktree; keep the worktree until ignored evidence is
archived elsewhere. No evidence was exported outside the authorized worktree.

- `build/ui-smoke/campaigns/20261024T122703086Z/compatible`: original setup failure.
- `build/ui-smoke/campaigns/20261024T122722630Z/compatible`: native plan campaign,
  selection, bundles, hashes, status, screenshots and gate.
- `build/ui-smoke/campaigns/20261024T123411487Z/compatible`: active-crafting campaign.
- `build/applied-enhancements/archive/20261024T122703219Z-5c976776`: failed preflight archive.
- `build/applied-enhancements/archive/20261024T123403728Z-ace9b915`: native plan archive.
- `build/applied-enhancements/archive/20261024T123822335Z-50824d49`: running-status archive.
- `build/applied-enhancements/plan-visual-review.json` and `status-visual-review.json`:
  manual review receipts.

## Timing

Measured from the first smoke preflight at 12:27:03.137 UTC to the final campaign
archive at 12:38:22.335 UTC. Research ran alongside the campaigns. VM boot began
before that interval and was not measured separately. Each successful campaign
includes its build/cache preparation, guest staging, loading, world generation,
assertions and client exit. Screenshot review continued after the interval.

| Part of the smoke UI testing task | Time | Why it took that long |
|---|---:|---|
| Initial failed preflight | 0.06s | Missing coverage declaration; no client launched |
| Correction and restart gap | 19.48s | Added declaration, checked it and updated the PR |
| Native plan campaign | 6m40.52s | Host build 1m16s; fresh VM mod loading and world generation dominated the guest run |
| Between campaigns | 8.35s | Archive completed and the next focused run was dispatched |
| Active-crafting campaign | 4m10.43s | Reused built bundle; mod loading and a fresh disposable world dominated |
| Final archive | 0.36s | Retained the status run and evidence gate |
| Manual screenshot review | not measured | Reviewed all seven plan and two status images after their captures; some work overlapped the next run |
| VM boot | not measured | Started before the first smoke command; no isolated timing receipt |
| Total measured smoke interval | 11m19.20s | Includes the failed attempt and both successful campaigns; excludes later final review |
