# Potential Dependency Integrations

Start with AE2's own CPU, key, and UI contracts. Add addon-specific code only
when source inspection proves that an addon bypasses them. This covers the
largest useful group with the fewest optional mixins, while keeping the gaps
honest.

The full requirements and rollout are in [the addon integration spec],
[technical design], and [implementation plan].

## Chosen approach

| # | Approach | What changes | Addons covered |
| --- | --- | --- | --- |
| 0 | AE2 CPU execution hook | Already in `CraftingCpuLogicMixin`. Covers inherited AE2 methods; an override bypasses only the method it replaces. | Vanilla AE2 and addons that reuse its execution logic, including OmniSequence after conflict-safe verification |
| 1 | AE2 service observation | **Planned**, after a proof-of-coverage spike. Observe `CraftingService.submitJob`, `getCpus`, and busy-state changes. This can cover job lifecycle only for CPUs the service actually exposes; it cannot produce output-throughput samples by itself. | AE2 service-visible CPUs |
| 2 | Small custom CPU adapter | Mirror `AdvancedCraftingCpuLogicMixin` only for a custom or overridden crafting loop. Call the existing `ProfilerBridge` methods at the real dispatch, insertion, finish, and capacity points. | AdvancedAE (done), NeoEco, AE2 Lightning Tech |
| 3 | Native `AEKey` contract | `AeKeyAmounts` already normalizes every `AEKey` through `getAmountPerUnit()`. Verify a new key type before adding code; use `AEKeyType`, not addon class checks. | MEGA Cells, OMNI Cells, ProjectCell, AppliedE, Applied Flux, Applied Mekanistics |
| 4 | AE2 table/UI hook | The concrete craft-confirm and crafting-status renderer mixins cover screens that use them or inherit their hooked methods. `AbstractTableRendererMixin` only decorates TTC lines. **Planned:** add a shared `AEBaseScreen` hook only if one common method can expose TTC without screen-specific assumptions. | ExtendedAE, ExtendedAE-Plus, BM Addon, Crazy AE2, Modern AE2 Additions, Applied Botanics, AE2 Import Export Card, AE2 Things, Expanded AE |
| 5 | Bespoke UI or API mixin | Per-addon `@Pseudo` mixin (existing pattern). | ae2ct, ME Requester, AE2 WCWT, AE2 Wireless Terminals, AE2 Import Export Card, Advanced Peripherals |

A Mixin plugin or automatic `ICraftingCPU` subclass discovery is intentionally
skipped. `ICraftingCPU` exposes status, capacity, and cancellation, but not the
dispatch and accepted-output events needed for throughput samples. Discovery
therefore cannot replace layer 2.

## Candidate addons

| Mod | Category | Approach | Issue | Links | Status |
| --- | --- | --- | --- | --- | --- |
| AdvancedAE | CUSTOM_CPU (Quantum Computer) | 2 | #67 | [CF][advancedae-cf] / [MR][advancedae-mr] | Implemented (verify) |
| NeoEco AE | CUSTOM_CPU (C4/C6/C9 + F4/F6/F9) | 2 | #66 | [CF][neoecoae-cf] / [MR][neoecoae-mr] | Implemented (verify) |
| OmniSequence: Transfinite | CUSTOM_CPU (Omni-Computation Core) | 0 (conflict-safe verification) | #71 | [CF][omnisequence-cf] | Implemented (verify) |
| AE2 Lightning Tech | CUSTOM_CPU (Tianshu + Matter Warping) | 2 | #72 | [CF][ae2lt-cf] / [MR][ae2lt-mr] | Implemented (verify) |
| ExtendedAE | ASSEMBLER + PROVIDER | 4 | #73 | [CF][extendedae-cf] | Not started |
| ExtendedAE-Plus | PROVIDER / QOL | 4 | #74 | [CF][extendedaeplus-cf] / [MR][extendedaeplus-mr] | Verified coexistence |
| BM Addon | PROVIDER (Ara Vitae Assembler) | 4 | #75 | [CF][bmaddon-cf] / [MR][bmaddon-mr] | Not started |
| Crazy AE2 Addons | PROVIDER / QOL | 4 | #76 | [CF][crazyae2-cf] / [MR][crazyae2-mr] | Verified coexistence |
| AE2 WCWT | WIRELESS_TERMINAL | 5 | #77 | [CF][ae2wcwt-cf] / [MR][ae2wcwt-mr] | Direct TTC tooltip verified |
| AE2 Wireless Terminals | WIRELESS_TERMINAL | 5 | #78 | [CF][wireless-cf] / [MR][wireless-mr] | Direct TTC tooltip verified |
| MEGA Cells | STORAGE_CELL / KEY | 3 | #79 | [CF][megacells-cf] / [MR][megacells-mr] | Not started |
| OMNI Cells | STORAGE_CELL | 3 | #80 | [CF][omnicells-cf] | Not started |
| ProjectCell | STORAGE_CELL / EMC_KEY | 3 | #81 | [CF][projectcell-cf] / [MR][projectcell-mr] | Verified on Forge 1.20.1 |
| AppliedE / AppliedE TPS Fix | EMC_KEY | 3 | #82 | [CF][appliede-cf] / [MR][appliede-mr] | Verified on Forge 1.20.1 |
| Applied Flux | STORAGE_CELL / energy KEY | 3 | #83 | [CF][appliedflux-cf] / [MR][appliedflux-mr] | Verified on Forge 1.20.1 |
| Modern AE2 Additions | QOL (AE Additions) | 4 | #84 | [CF][modernae2-cf] / [MR][modernae2-mr] | Verified on Forge 1.20.1 |
| Applied Mekanistics | CHEMICAL_KEY | 3 | #68 | [CF][appmek-cf] / [MR][appmek-mr] | Verified on Forge 1.20.1 |
| ae2ct (Crafting Tree) | CRAFTING_UI | 5 | #69 | [CF][ae2ct-cf] | Implemented |
| ME Requester | QOL / requester | 5 | #70 | [CF][merequester-cf] / [MR][merequester-mr] | Implemented |
| AE2 Import Export Card | QOL | 4/5 | #85 | [CF][ae2iec-cf] / [MR][ae2iec-mr] | Not started |
| AE2 Network Analyser | QOL (visual tool) | `GuiAnalyser` compatibility smoke; no TTC hook | #86 | [CF][ae2na-cf] | Integrated |
| AEInfinityBooster | QOL (range) | none | #87 | [CF][aeinfinity-cf] / [MR][aeinfinity-mr] | Not started |
| Applied Botanics (Fork) | QOL (Botania) | 4 | #88 | [CF][appliedbotanics-cf] | Not started |
| Advanced Peripherals | QOL (ME Bridge) | 5 (peripheral API, if useful) | #89 | [CF][advancedperipherals-cf] / [MR][advancedperipherals-mr] | Not started |
| AE2 Things | QOL (Inscriber / Crystal Growth) | 4/5 | — | [MR][ae2things-mr] | Not started |
| Expanded AE | PROVIDER / QOL | 4 | — | [CF][expandedae-cf] / [MR][expandedae-mr] | Not started |

## CPU-detection mixin summary (#24 fix path)

- Layer 0 covers inherited `CraftingCpuLogic` methods. Layer 1 is separate
  because a service-visible CPU may implement its own execution logic. The
  service can observe submission and busy-state changes, but layer 2 is still
  required for accurate output throughput when the addon bypasses layer 0.
- Only add a `@Pseudo` layer-2 mixin per addon that ships its own logic and
  overrides the hooked methods, mirroring `AdvancedCraftingCpuLogicMixin`:
  - NeoEco: `cn.dancingsnow.neoecoae.api.me.ECOCraftingCPULogic`
  - AE2 Lightning Tech: `com.moakiee.ae2lt.crafting.timewheel.Ae2LtTimeWheelCraftingCpuLogic`
- OmniSequence reuses `CraftingCpuLogic`, so it stays in layer 0. Its own
  redirect on `executeCrafting` must not collide with ours.
- `getGrid()` and `getLevel()` are concrete CPU methods, not part of
  `ICraftingCPU`. A layer-2 adapter may use them only after checking the addon
  class that supplies them.

## Selection rule

Prefer integrations that reuse `ClientStats`, `TimeEstimate`, and
`AeKeyAmounts`. Add a compile-time dependency only when AE2 APIs or an optional
string-target mixin cannot reach the screen or key type.

[the addon integration spec]: docs/ae2-addon-integration/spec.md
[technical design]: docs/ae2-addon-integration/technical-design.md
[implementation plan]: docs/ae2-addon-integration/implementation-plan.md

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
[appliede-mr]: https://modrinth.com/mod/appliede
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
