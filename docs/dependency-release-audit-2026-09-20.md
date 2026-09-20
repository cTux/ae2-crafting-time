# Dependency release audit, 2026-09-20

Tracking: [#452](https://github.com/cTux/ae2-crafting-time/issues/452).

Modrinth official version metadata was retrieved on 2026-09-19; CurseForge files were checked live on 2026-09-20. Exact artifact IDs determine changes; display-version spelling alone does not. The pinned-release column records the pre-audit baseline. Only rows with passing focused runtime and visual evidence are promoted. Tooling qualification is isolated in PR #465; loader qualification is isolated in PR #464.

| Addon | Target | Pinned release | Latest published artifact | Result |
| --- | --- | --- | --- | --- |
| Applied Botanics | 1.20.1-forge | 1.5.2 | [1.5.2-forge](https://modrinth.com/mod/545hUrw9/version/ByiqRpj3) | Same artifact |
| AE2 Crafting Tree | 1.20.1-forge | 1.0.1 | [1.0.1](https://modrinth.com/mod/a1RwDz90/version/flhDmaU7) | Same artifact |
| Applied Mekanistics | 1.20.1-forge | 1.4.3 | [1.4.3](https://modrinth.com/mod/IiATswDj/version/9n9p68Qq) | Same artifact |
| ME Requester | 1.20.1-forge | 1.20.1-1.2.1+forge | [1.20.1-1.2.1+forge](https://modrinth.com/mod/E6BFl96N/version/69N8Y7WD) | Same artifact |
| NeoEco AE | 1.20.1-forge | 20.3.0 | [20.4.2](https://modrinth.com/mod/udZtKfzP/version/Qm3GW128) | Focused scenario and manual visual review PASS |
| AdvancedAE | 1.20.1-forge | 1.3.6-1.20.1 | [1.3.6-1.20.1](https://modrinth.com/mod/rxYaglEe/version/d83Wdhdn) | Same artifact |
| ExtendedAE | 1.20.1-forge | 1.20-1.4.18-forge | [1.20-1.4.18-forge](https://modrinth.com/mod/JiOqfoFM/version/uq3lO4ER) | Same artifact |
| ExtendedAE-Plus | 1.20.1-forge | 1.5.5 | [1.6.2](https://modrinth.com/mod/xr109llC/version/A3uRgnwT) | Retained 1.5.5; prerequisites and CPU fixture follow-up [#461](https://github.com/cTux/ae2-crafting-time/issues/461) |
| BM Addon | 1.20.1-forge | 1.0.4 | [1.0.4](https://modrinth.com/mod/qPydPwtX/version/oTrjvD49) | Same artifact |
| Crazy AE2 Addons | 1.20.1-forge | 2.6.2 | [3.2.5](https://modrinth.com/mod/anaGQD2Q/version/DdUx8yRT) | Retained 2.6.2; missing prerequisites [#463](https://github.com/cTux/ae2-crafting-time/issues/463) |
| AE2 WCWT | 1.20.1-forge | 1.20.1.10 | [1.20.1.10](https://modrinth.com/mod/4inoel9g/version/RFT2nmVb) | Same artifact |
| AE2 Wireless Terminals | 1.20.1-forge | 15.3.3-forge | [15.3.3-forge](https://modrinth.com/mod/pNabrMMw/version/z8QXeyI0) | Same artifact |
| MEGA Cells | 1.20.1-forge | forge-2.4.6 | [forge-2.4.6](https://modrinth.com/mod/jjuIRIVr/version/SH2D1n3s) | Same artifact |
| OMNI Cells | 1.20.1-forge | 1.1.6-1.20.1-forge | [1.1.6-1.20.1-forge](https://modrinth.com/mod/RYE1pYyr/version/c2s3iMw8) | Same artifact |
| ProjectCell | 1.20.1-forge | 1.0.1 | [1.0.1](https://modrinth.com/mod/IZPmgTLT/version/wCGFfeun) | Same artifact |
| Applied Flux | 1.20.1-forge | 1.20-1.3.7-forge | [1.20-1.3.7-forge](https://modrinth.com/mod/oMgZ004U/version/cAcdjzEn) | Same artifact |
| Modern AE2 Additions | 1.20.1-forge | 2.0.1 | [2.0.1](https://modrinth.com/mod/5G4fpXXj/version/5vXTRu3V) | Same artifact |
| AE2 Import Export Card | 1.20.1-forge | 1.20.1-1.3.0 | [1.20.1-1.3.0](https://modrinth.com/mod/qelfSMnn/version/v8c3El4q) | Same artifact |
| AEInfinityBooster | 1.20.1-forge | 1.20.1-1.0.0+20 | [1.20.1-1.0.0+20](https://modrinth.com/mod/VQhDBNs8/version/cTJwfNfV) | Same artifact |
| Advanced Peripherals | 1.20.1-forge | 1.20.1-0.7.48r | [1.20.1-0.7.48r](https://modrinth.com/mod/SOw6jD6x/version/mIP0ApJY) | Same artifact |
| Expanded AE | 1.20.1-forge | 1.2.2 | [1.2.2](https://modrinth.com/mod/ayN3DZKb/version/KXyq9wjS) | Same artifact |
| GuideME | 1.20.1-forge | 20.1.15 | [20.1.15](https://modrinth.com/mod/Ck4E7v7R/version/i7Tp1AHw) | Same artifact |
| JEI | 1.20.1-forge | 15.56.0.204 | [15.59.0.212](https://modrinth.com/mod/u6dRKJwZ/version/IrfJO7PN) | Separate qualification in [tooling PR #465](https://github.com/cTux/ae2-crafting-time/pull/465) |
| Applied Botanics | 1.20.1-fabric | 1.5.2 | [1.5.2-fabric](https://modrinth.com/mod/545hUrw9/version/xyBemygB) | Same artifact |
| ME Requester | 1.20.1-fabric | 1.20.1-1.1.4+fabric | [1.20.1-1.1.4+fabric](https://modrinth.com/mod/E6BFl96N/version/NmhR6jIY) | Same artifact |
| ExtendedAE | 1.20.1-fabric | 1.20-1.0.2-fabric | [1.20-1.0.2-fabric](https://modrinth.com/mod/JiOqfoFM/version/Wmmw6SrO) | Same artifact |
| AE2 Wireless Terminals | 1.20.1-fabric | 15.2.1-fabric | [15.2.1-fabric](https://modrinth.com/mod/pNabrMMw/version/QewbGM3G) | Same artifact |
| MEGA Cells | 1.20.1-fabric | fabric-2.4.6 | [fabric-2.4.6](https://modrinth.com/mod/jjuIRIVr/version/NINqmdmJ) | Same artifact |
| AE2 Things | 1.20.1-fabric | 1.3.2 | [1.3.2](https://modrinth.com/mod/veunMwU3/version/SKanB27c) | Same artifact |
| JEI | 1.20.1-fabric | 15.56.0.204 | [15.59.0.212](https://modrinth.com/mod/u6dRKJwZ/version/l4UV8hII) | Separate qualification in [tooling PR #465](https://github.com/cTux/ae2-crafting-time/pull/465) |
| AE2 Crafting Tree | 1.21.1-neoforge | 1.0.1 | [1.0.1](https://modrinth.com/mod/a1RwDz90/version/35O4yt0D) | Same artifact |
| Applied Mekanistics | 1.21.1-neoforge | 1.6.3 | [1.6.3](https://modrinth.com/mod/IiATswDj/version/TpUCzFaW) | Same artifact |
| AdvancedAE | 1.21.1-neoforge | 1.6.12-1.21.1 | [1.6.12-1.21.1](https://modrinth.com/mod/rxYaglEe/version/ablTMAjP) | Same artifact |
| ME Requester | 1.21.1-neoforge | 1.21.1-1.4.3+neoforge | [1.21.1-1.5.0+neoforge](https://modrinth.com/mod/E6BFl96N/version/hLs5MFnR) | Focused scenario and manual visual review PASS |
| NeoEco AE | 1.21.1-neoforge | 21.1.1 | [21.2.0-beta4](https://modrinth.com/mod/udZtKfzP/version/c1xX1P0i) | Retained 21.1.1; breaking API and new prerequisites [#457](https://github.com/cTux/ae2-crafting-time/issues/457) |
| ExtendedAE | 1.21.1-neoforge | 1.21-2.2.35-neoforge | [1.21-2.2.35-neoforge](https://modrinth.com/mod/JiOqfoFM/version/bC302UUP) | Same artifact |
| ExtendedAE-Plus | 1.21.1-neoforge | 1.6.2 | [1.6.2](https://modrinth.com/mod/xr109llC/version/o8U8Ekro) | Same artifact |
| BM Addon | 1.21.1-neoforge | BMAddon1.21.1-beta1 | [BMAddon1.21.1-beta1](https://modrinth.com/mod/qPydPwtX/version/kKLZdIPS) | Same artifact |
| AE2 WCWT | 1.21.1-neoforge | 1.3.8 | [1.3.9](https://modrinth.com/mod/4inoel9g/version/a7skAXQ1) | Retained 1.3.8; terminal clipped at automatic scale [#468](https://github.com/cTux/ae2-crafting-time/issues/468) |
| AE2 Wireless Terminals | 1.21.1-neoforge | 19.5.1 | [19.5.1](https://modrinth.com/mod/pNabrMMw/version/CxSEpEnO) | Same artifact |
| MEGA Cells | 1.21.1-neoforge | 4.11.0 | [4.11.0](https://modrinth.com/mod/jjuIRIVr/version/RPG4EriK) | Same artifact |
| OMNI Cells | 1.21.1-neoforge | 1.1.6-1.21.1-neoforge | [1.1.6-1.21.1-neoforge](https://modrinth.com/mod/RYE1pYyr/version/k444ySGT) | Same artifact |
| ProjectCell | 1.21.1-neoforge | 1.0.3 | [1.0.3](https://modrinth.com/mod/IZPmgTLT/version/nd0U5eJF) | Same artifact |
| AppliedE | 1.21.1-neoforge | 1.0.1-beta | [1.0.8-beta](https://modrinth.com/mod/SyKS54UY/version/R5MXisky) | Retained 1.0.1-beta; focused prerequisite graph blocked [#467](https://github.com/cTux/ae2-crafting-time/issues/467) |
| Applied Flux | 1.21.1-neoforge | 1.21-2.1.5-neoforge | [1.21-2.1.5-neoforge](https://modrinth.com/mod/oMgZ004U/version/4x40hq9D) | Same artifact |
| AE2 Import Export Card | 1.21.1-neoforge | 1.21.1-1.6.0 | [1.21.1-1.9.0](https://modrinth.com/mod/qelfSMnn/version/SnCFnDzn) | Focused scenario and manual visual review PASS |
| AEInfinityBooster | 1.21.1-neoforge | 1.21.1-1.0.0.58 | [1.21.1-1.0.0.58](https://modrinth.com/mod/VQhDBNs8/version/qpbQk2Iq) | Same artifact |
| Advanced Peripherals | 1.21.1-neoforge | 1.21.1-0.8.0a | [1.21.1-0.8.1a](https://modrinth.com/mod/SOw6jD6x/version/1rbqTjbS) | Focused scenario and manual visual review PASS |
| Expanded AE | 1.21.1-neoforge | 2.1.1 | [2.1.1](https://modrinth.com/mod/ayN3DZKb/version/Iw2gl7Lb) | Same artifact |
| GuideME | 1.21.1-neoforge | 21.1.17 | [21.1.19](https://modrinth.com/mod/Ck4E7v7R/version/hFpGwC6q) | Separate qualification in [tooling PR #465](https://github.com/cTux/ae2-crafting-time/pull/465) |
| JEI | 1.21.1-neoforge | 19.51.0.417 | [19.56.0.441](https://modrinth.com/mod/u6dRKJwZ/version/ZWGz5dZX) | Retained 19.51.0.417; new MezzConfig prerequisite [#462](https://github.com/cTux/ae2-crafting-time/issues/462) |
| AdvancedAE | 26.1.2-neoforge | 26.1.7 | [26.1.7](https://modrinth.com/mod/rxYaglEe/version/hPJMJOBd) | Same artifact |
| ExtendedAE | 26.1.2-neoforge | 26.1-1.0.3-neoforge | [26.1-1.0.3-neoforge](https://modrinth.com/mod/JiOqfoFM/version/KpNzac5y) | Same artifact |
| BM Addon | 26.1.2-neoforge | beta3 | [beta3](https://modrinth.com/mod/qPydPwtX/version/lNUOSgAt) | Same artifact |
| Neo Vitae | 26.1.2-neoforge | 26.1.2-1.0.27 | [26.1.2-1.1.26](https://modrinth.com/mod/rvaW4C93/version/oDNiS7Ki) | Separate qualification in [tooling PR #465](https://github.com/cTux/ae2-crafting-time/pull/465) |
| AE2 Wireless Terminals | 26.1.2-neoforge | 26.1.1-beta | [26.1.1-beta](https://modrinth.com/mod/pNabrMMw/version/ncEqrp7o) | Same artifact |
| OMNI Cells | 26.1.2-neoforge | 1.1.7-26.1.2-neoforge | [1.1.7-26.1.2-neoforge](https://modrinth.com/mod/RYE1pYyr/version/wv0rLPsF) | Same artifact |
| Applied Flux | 26.1.2-neoforge | 26.1-1.0.1-neoforge | [26.1-1.0.1-neoforge](https://modrinth.com/mod/oMgZ004U/version/yrw1WDVE) | Same artifact |
| AE2 Import Export Card | 26.1.2-neoforge | 26.1.2-2.1.0 | [26.1.2-2.3.0](https://modrinth.com/mod/qelfSMnn/version/DMPhIJZQ) | Focused scenario and manual visual review PASS |
| AEInfinityBooster | 26.1.2-neoforge | 26.1.2-1.0.0.57 | [26.1.2-1.0.0.57](https://modrinth.com/mod/VQhDBNs8/version/qUOmPSfe) | Same artifact |
| GuideME | 26.1.2-neoforge | 26.1.12-beta | [26.1.12-beta](https://modrinth.com/mod/Ck4E7v7R/version/ZkJhP9xE) | Same artifact |
| JEI | 26.1.2-neoforge | 29.34.0.88 | [29.37.0.100](https://modrinth.com/mod/u6dRKJwZ/version/AM5ddBPW) | Separate qualification in [tooling PR #465](https://github.com/cTux/ae2-crafting-time/pull/465) |

## Scope and interpretation

A maximum supported version here is the newest exact release verified with this project's integration. Optional loader metadata has a minimum and no upper cap. The audit changes runtime test pins and their documented verified maximum, preserving compile/minimum dependencies and all integration code. Focused verification does not prove the combined addon graph.

## Target additions and candidate dependencies

- ME Requester now publishes `26.1.2-1.5.1+neoforge` for Minecraft 26.1.2 / NeoForge: #454. Existing 1.20.1 and 1.21.1 support is preserved; no 26.1.2 implementation was attempted.
- Applied Extended Crafting is a new candidate: #455. Exact official 1.2.0 artifacts exist for Forge 1.20.1 and NeoForge 1.21.1 / 26.1.2, and its assembler automation gives a concrete crafting-time integration surface.
- Applied Botanics 1.21.1 alpha and AE2 Things 1.21.1 beta were already documented targets. Their unpinned/unverified status is not a newly published target. Original Applied Botanics and its Forge fork are distinct identities.
- Existing research #439 (JEI++), #440 (Just Enough Crafting Tree), #441 (AE2 Crafting Optimizer), and #442 (AE2 Crafting Priority) already cover other discovered candidates. No duplicate issues were created.

## Breaking updates and unavailable sources

- NeoEco 21.2.0-beta4 relocates `cn.dancingsnow.neoecoae.api.me.ECOCraftingCPULogic` to `cn.dancingsnow.neoecoae.crafting.execution.ECOCraftingCPULogic`; the current adapter and test driver refer to the older contract. It also requires LDLib2. #457 tracks the separate newer adapter and retained 21.1 support.
- OmniSequence 2.0.4 adds required AppliedEnhancements dependencies on both targets and LDLib2 on NeoForge. Exact metadata is in #459. A file-ID-only bump would not resolve its runtime prerequisites; older verified pins remain.
- Lightning Tech's former Modrinth project is removed and the known CurseForge page returned a live 404. #460 tracks finding an authoritative source. This audit cannot claim its pinned files are newest.
- The runtime's Crafting Tree project on Modrinth is **AE2: Crafting Tree Refreshed** 1.0.1. The original CurseForge Crafting Tree 1.1.1 is a different fork, not a higher release of the same dependency. Neither source established a new Fabric target.

## Supporting old and new addon APIs

This is possible within each supported Minecraft/loader target, with one installed addon version. `IntegrationSelection` chooses the first matching adapter from the ordered `IntegrationCatalog`, once at startup. Newer explicit API contracts belong before older contracts. Existing Tree layout/helper and Forge NeoEco batched/pending contracts demonstrate the approach.

A breaking update should add a separately gated adapter and retain the older adapter, dependency minimum, and old contract checks. Probe the required classes/methods/fields before mixin application. Unknown contracts should disable only that integration family with diagnostics. Do not apply an older adapter after a newer one has already failed during execution. This does not make two versions of the same mod load together, or bridge incompatible Minecraft binaries.

No new adapter was implemented in this research. #457 is a concrete follow-up for the old/new NeoEco case.

## Test-runner finding

#458 records a confirmed cache identity defect: two focused addon selections at the same commit reused the first addon's bundle. The invalid ExtendedAE-Plus and Crazy AE2 attempts are excluded from addon compatibility conclusions. The audit preserves each old cache under its task evidence directory and resolves a fresh bundle before each later focused run. No tracked runner code was changed.

## Focused addon evidence

Tested source: `b6c1b9dafba82b09b49376401e89897c348b30d8`, based on
`ad6fb664ea9863313f09f8f86f3e67d15063fe87`. Later changes restore
unqualified pins and document results; they do not change integration code or
the resolved graph of a passing focused run. Each run used only its selected
addon and required prerequisites, with the baseline loader.

All five promoted rows have semantic, archive, and cleanup PASS. Automatic
visual results remain REVIEW_REQUIRED because no qualified baseline exists.
The screenshots were manually reviewed at automatic GUI scale 4, a 640 x 274
scaled viewport, and a 2558 x 1093 framebuffer. Plan panels fit at
(201,34), 238 x 206; item badges and total TTC labels do not overlap icons or
buttons. Requester's panel fits at (222,29), 195 x 215, with its TTC row below
the fields. Both Import Export Card terminal tooltips are readable and the
terminal fits. Generic full-viewport terminal sidecars alone are not visual
proof; the images were inspected.

Campaign IDs locate the archived selection, resolved mods, hashes, status,
sidecars, images, result, and gate under the task's UI-smoke evidence.

| Dependency and target | Campaign | Whole campaign | Host build (included) |
| --- | --- | ---: | ---: |
| NeoEco 20.4.2, Forge 1.20.1 | 20260920T051832781Z | 219.2 s | 2 s |
| ME Requester 1.5.0, NeoForge 1.21.1 | 20260920T051019441Z | 195.4 s | 1 s |
| Import Export Card 1.9.0, NeoForge 1.21.1 | 20260920T055110055Z | 172.6 s | 1 s |
| Advanced Peripherals 0.8.1a, NeoForge 1.21.1 | 20260920T055404707Z | 177.7 s | 1 s |
| Import Export Card 2.3.0, NeoForge 26.1.2 | 20260920T055704303Z | 58.3 s | 9 s |

Campaign totals include resolution/staging, host build, game loading, world
and UI assertions, archive and cleanup. These phases overlap and are not
added twice. Individual loading/assertion/review durations were not measured
separately. Most 1.20.1/1.21.1 elapsed time was client loading; the 26.1.2
client completed substantially faster.

Unsuccessful work also counts: Requester's VM-off setup attempt took 67.6 s;
ExtendedAE-Plus's missing-prerequisite run took 359.7 s and the complete-graph
CPU-fixture retry took 245.4 s; WCWT's semantically passing but visually
clipped run took 175.5 s. The cache-contaminated Crazy attempt took 164.4 s
and is invalid evidence. The other invalid cache attempt, fresh Crazy
prerequisite failure, VM preparation, metadata research, and manual review
durations were not measured separately. No total task-time estimate is
presented as a measurement.

## Follow-up boundaries

- [#461](https://github.com/cTux/ae2-crafting-time/issues/461),
  [#463](https://github.com/cTux/ae2-crafting-time/issues/463), and
  [#467](https://github.com/cTux/ae2-crafting-time/issues/467) need focused
  prerequisite/fixture work before qualification. These failures do not by
  themselves establish an addon API regression.
- [#468](https://github.com/cTux/ae2-crafting-time/issues/468) records WCWT
  clipping at automatic scale. Compare the old version before attributing
  the defect to 1.3.9. Lowering the scale was not used to qualify it.
- [#466](https://github.com/cTux/ae2-crafting-time/issues/466) records the
  multi-project PowerShell dispatch defect. Later tests use single-project
  selection only. No runner fixes are included.

## Loader sources

Latest target-specific loader candidates were checked against official
[Forge metadata](https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml),
[Fabric metadata](https://meta.fabricmc.net/v2/versions/loader), and
[NeoForge metadata](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml).
Forge 47.4.23, Fabric 0.19.5, NeoForge 21.1.251 and 26.1.2.109 are isolated
in [PR #464](https://github.com/cTux/ae2-crafting-time/pull/464), which records
their qualification independently of this addon audit.
