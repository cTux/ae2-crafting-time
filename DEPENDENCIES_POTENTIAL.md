# Potential Dependency Integrations

AE2 addons either reuse AE2 base classes (covered by general hooks) or ship
their own CPU logic and/or custom UI (needs targeted work). The approach below
collapses ~25 planned integrations into a few general hooks plus a small residue
of custom UI mixins, so we stop adding one integration per addon.

## Chosen approach

| # | Approach | What changes | Addons covered |
| --- | --- | --- | --- |
| 0 | Base CPU mixin (inheritance) | Already in `CraftingCpuLogicMixin`; verify each CUSTOM_CPU addon extends AE2 `CraftingCpuLogic`. No new code. | Any CUSTOM_CPU that subclasses AE2 logic |
| 1 | Universal job hook | **New** `CraftingServiceMixin` (mcCommon/neoforge) on `submitJob` + CPU registry. Captures job start/finish + every `ICraftingCPU` for basic TTC, zero per-addon code. | All CPUs (vanilla + every CUSTOM_CPU) |
| 2 | Optional per-addon CPU mixin (fidelity) | Mirror `AdvancedCraftingCpuLogicMixin`, call `ProfilerBridge.start/complete/startJob/finishJob/updateCapacity`. Only where full bottleneck detail is wanted. | AdvancedAE (done), NeoEco, OmniSequence, AE2 Lightning Tech |
| 3 | Generic KeyType handler | Centralize AEKey normalization in `AeKeyAmounts`/`ProfilerBridge.key` at the `AEKey`/`KeyType` boundary. | MEGA Cells, OMNI Cells, ProjectCell, AppliedE, Applied Flux, Applied Mekanistics |
| 4 | Generic table/UI mixin | `AbstractTableRendererMixin` (exists) + optional `AEBaseScreenMixin` for addon terminals reusing AE2 widgets. | ExtendedAE, ExtendedAE-Plus, BM Addon, Crazy AE2, Modern AE2 Additions, AE Additions, Applied Botanics |
| 5 | Bespoke UI mixin | Per-addon `@Pseudo` mixin (existing pattern). | ae2ct, ME Requester, Wireless Terminals (WCWT, Wireless Terminals), AE2 Import Export Card |

A Mixin plugin / auto-discovery of `ICraftingCPU` subclasses is intentionally
skipped: it cannot derive each addon's crafting-loop method, so it only repeats
layer 0 without removing layer-2 work.

## Candidate addons

| Mod | Category | Approach | Issue | Links | Status |
| --- | --- | --- | --- | --- | --- |
| AdvancedAE | CUSTOM_CPU (Quantum Computer) | 0+1+2 | #67 | [CF][advancedae-cf] / [MR][advancedae-mr] | Implemented (verify) |
| NeoEco AE | CUSTOM_CPU (C4/C6/C9 + F4/F6/F9) | 1+2 | #66 | [CF][neoecoae-cf] / [MR][neoecoae-mr] | Not started |
| OmniSequence: Transfinite | CUSTOM_CPU (Omni-Computation Core) | 0+1 (gate existing hooks) | #71 | [CF][omnisequence-cf] | Not started |
| AE2 Lightning Tech | CUSTOM_CPU (Tianshu + Matter Warping) | 1+2 | #72 | [CF][ae2lt-cf] / [MR][ae2lt-mr] | Not started |
| ExtendedAE | ASSEMBLER + PROVIDER | 4 | #73 | [CF][extendedae-cf] | Not started |
| ExtendedAE-Plus | PROVIDER / QOL | 4 | #74 | [CF][extendedaeplus-cf] / [MR][extendedaeplus-mr] | Not started |
| BloodMagic AE2 Addition | PROVIDER (Ara Vitae Assembler) | 4 | #75 | [CF][bmaddon-cf] / [MR][bmaddon-mr] | Not started |
| Crazy AE2 Addons | PROVIDER / QOL | 4 | #76 | [CF][crazyae2-cf] / [MR][crazyae2-mr] | Not started |
| AE2 WCWT | WIRELESS_TERMINAL | 5 | #77 | [CF][ae2wcwt-cf] / [MR][ae2wcwt-mr] | Not started |
| AE2 Wireless Terminals | WIRELESS_TERMINAL | 5 | #78 | [CF][wireless-cf] / [MR][wireless-mr] | Not started |
| MEGA Cells | STORAGE_CELL / KEY | 3 | #79 | [CF][megacells-cf] / [MR][megacells-mr] | Not started |
| OMNI Cells | STORAGE_CELL | 3 | #80 | [CF][omnicells-cf] | Not started |
| ProjectCell | STORAGE_CELL / EMC_KEY | 3 | #81 | [CF][projectcell-cf] / [MR][projectcell-mr] | Not started |
| AppliedE TPS Fix | EMC_KEY | 3 | #82 | [CF][appliede-cf] | Not started |
| Applied Flux | STORAGE_CELL / energy KEY | 3 | #83 | [CF][appliedflux-cf] / [MR][appliedflux-mr] | Not started |
| Modern AE2 Additions | QOL (AE Additions) | 4 | #84 | [CF][modernae2-cf] / [MR][modernae2-mr] | Not started |
| Applied Mekanistics | CHEMICAL_KEY | 3 | #68 | [CF][appmek-cf] / [MR][appmek-mr] | Implemented |
| ae2ct (Crafting Tree) | CRAFTING_UI | 5 | #69 | [CF][ae2ct-cf] | Implemented |
| ME Requester | QOL / requester | 5 | #70 | [CF][merequester-cf] / [MR][merequester-mr] | Implemented |
| AE2 Import Export Card | QOL | 4/5 | #85 | [CF][ae2iec-cf] / [MR][ae2iec-mr] | Not started |
| AE2 Network Analyser | QOL (visual tool) | none | #86 | [CF][ae2na-cf] | Not started |
| AEInfinityBooster | QOL (range) | none | #87 | [CF][aeinfinity-cf] / [MR][aeinfinity-mr] | Not started |
| Applied Botanics (Fork) | QOL (Botania) | 4 | #88 | [CF][appliedbotanics-cf] | Not started |
| Advanced Peripherals | QOL (ME Bridge) | 4/5 | #89 | [CF][advancedperipherals-cf] / [MR][advancedperipherals-mr] | Not started |
| AE2 Things | QOL (Inscriber / Crystal Growth) | 4/5 | — | [MR][ae2things-mr] | Not started |
| Expanded AE | PROVIDER / QOL | 4 | — | [CF][expandedae-cf] / [MR][expandedae-mr] | Not started |

## CPU-detection mixin summary (#24 fix path)

- Layer 0/1 already covers addons that reuse AE2 `CraftingCpuLogic` or submit
  through the grid `CraftingService`.
- Only add a `@Pseudo` layer-2 mixin per addon that ships its own logic and
  overrides the hooked methods, mirroring `AdvancedCraftingCpuLogicMixin`:
  - NeoEco: `cn.dancingsnow.neoecoae.api.me.ECOCraftingCPULogic`
  - AE2 Lightning Tech: `com.moakiee.ae2lt.crafting.timewheel.Ae2LtTimeWheelCraftingCpuLogic`
  - OmniSequence: gates the existing `CraftingCpuLogic` hooks (no new class).
- `grid`/`level`/`gameTime` come from the addon's `ICraftingCPU` object
  (`getGrid()` / `getLevel()` / `level.getGameTime()`).

## Selection rule

Prefer integrations that reuse `ClientStats`, `TimeEstimate`, and
`AeKeyAmounts`. Add a compile-time dependency only when AE2 APIs or an optional
string-target mixin cannot reach the screen or key type.

[advancedae-cf]: https://www.curseforge.com/minecraft/mc-mods/advancedae
[advancedae-mr]: https://modrinth.com/mod/advancedae
[neoecoae-cf]: https://www.curseforge.com/minecraft/mc-mods/neo-eco-ae-extension
[neoecoae-mr]: https://modrinth.com/mod/neoecoae
[omnisequence-cf]: https://www.curseforge.com/minecraft/mc-mods/omnisequence-transfinite
[ae2lt-cf]: https://www.curseforge.com/minecraft/mc-mods/ae2-lightning-tech
[ae2lt-mr]: https://modrinth.com/mod/ae2-lightning-tech
[extendedae-cf]: https://www.curseforge.com/minecraft/mc-mods/ex-pattern-provider
[extendedaeplus-cf]: https://www.curseforge.com/minecraft/mc-mods/extendedae-plus
[extendedaeplus-mr]: https://modrinth.com/mod/extendedae-plus
[bmaddon-cf]: https://www.curseforge.com/minecraft/mc-mods/bmaddon
[bmaddon-mr]: https://modrinth.com/mod/bmaddon
[crazyae2-cf]: https://www.curseforge.com/minecraft/mc-mods/crazy-ae2-addons
[crazyae2-mr]: https://modrinth.com/mod/crazy-ae2-addons
[ae2wcwt-cf]: https://www.curseforge.com/minecraft/mc-mods/ae2-wcwt
[ae2wcwt-mr]: https://modrinth.com/mod/ae2-wcwt
[wireless-cf]: https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2-wireless-terminals
[wireless-mr]: https://modrinth.com/mod/applied-energistics-2-wireless-terminals
[megacells-cf]: https://www.curseforge.com/minecraft/mc-mods/mega-cells
[megacells-mr]: https://modrinth.com/mod/mega
[omnicells-cf]: https://www.curseforge.com/minecraft/mc-mods/omni-cells
[projectcell-cf]: https://www.curseforge.com/minecraft/mc-mods/projectcell
[projectcell-mr]: https://modrinth.com/mod/projectcell
[appliede-cf]: https://www.curseforge.com/minecraft/mc-mods/appliede-tps-fix
[appliedflux-cf]: https://www.curseforge.com/minecraft/mc-mods/applied-flux
[appliedflux-mr]: https://modrinth.com/mod/applied-flux
[modernae2-cf]: https://www.curseforge.com/minecraft/mc-mods/modern-ae2-additions
[modernae2-mr]: https://modrinth.com/mod/modern-ae2-additions
[appmek-cf]: https://www.curseforge.com/minecraft/mc-mods/applied-mekanistics
[appmek-mr]: https://modrinth.com/mod/applied-mekanistics
[ae2ct-cf]: https://www.curseforge.com/minecraft/mc-mods/ae2-crafting-tree
[merequester-cf]: https://www.curseforge.com/minecraft/mc-mods/merequester
[merequester-mr]: https://modrinth.com/mod/merequester
[ae2iec-cf]: https://www.curseforge.com/minecraft/mc-mods/ae2-import-export-card
[ae2iec-mr]: https://modrinth.com/mod/ae2-import-export-card
[ae2na-cf]: https://www.curseforge.com/minecraft/mc-mods/ae2-network-analyser
[aeinfinity-cf]: https://www.curseforge.com/minecraft/mc-mods/aeinfinitybooster
[aeinfinity-mr]: https://modrinth.com/mod/aeinfinitybooster
[appliedbotanics-cf]: https://www.curseforge.com/minecraft/mc-mods/applied-botanics-fork
[advancedperipherals-cf]: https://www.curseforge.com/minecraft/mc-mods/advancedperipherals
[advancedperipherals-mr]: https://modrinth.com/mod/advancedperipherals
[ae2things-mr]: https://modrinth.com/mod/ae2things
[expandedae-cf]: https://www.curseforge.com/minecraft/mc-mods/expanded-ae
[expandedae-mr]: https://modrinth.com/mod/expanded-ae
