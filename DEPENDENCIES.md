# Dependencies

AE2 Crafting Time ships a separate JAR for each supported Minecraft version and
loader. Pick the one that matches your instance. Optional integrations bring
the same TTC and bottleneck hints into a few other AE2 screens and resource
types.

## Strict Dependencies

These are required. The mod will not load unless your instance matches the row
for its JAR.

| Target jar | Required dependencies |
| --- | --- |
| `1.20.1 Forge` | Minecraft `[1.20.1,1.21)`, Forge `[47.1.3,)`, Applied Energistics 2 `[15.0.10,16.0.0)` |
| `1.20.1 Fabric` | Minecraft `1.20.1`, Fabric Loader `>=0.14.21`, Fabric API `>=0.83.1`, Java `>=17`, Applied Energistics 2 `>=15.0.10 <16.0.0` |
| `1.21.1 NeoForge` | Minecraft `[1.21.1]`, NeoForge `[21.1.1,)`, Applied Energistics 2 `[19.0.24,20.0.0)` |
| `26.1.2 NeoForge` | Minecraft `[26.1.2]`, NeoForge `[26.1.2.71,)`, Java `>=25`, Applied Energistics 2 `[26.1.10-beta,27.0.0)` |

## Optional Dependencies

These are picked up automatically when installed. You do not need them to run
AE2 Crafting Time.

| Dependency | Targets | Version range | What it enables |
| --- | --- | --- | --- |
| Applied Botanics (original) (`appbot`) | `1.20.1 Forge`, `1.20.1 Fabric`, `1.21.1 NeoForge` | 1.20.1: `>=1.5.0 <1.6.0`; 1.21.1: `>=1.6.0-alpha.3 <1.7.0` | Raw mana amounts and mana unit labels through its native AE2 key. Existing milli-pool histories are converted on load. |
| AE2: Crafting Tree (`ae2ct`) | `1.20.1 Forge`, `1.20.1 Fabric`, `1.21.1 NeoForge` | `>=1.0.1` | TTC lines and details in AE2: Crafting Tree UI. |
| Applied Mekanistics (`appmek`) | `1.20.1 Forge`, `1.21.1 NeoForge` | Forge: `>=1.4.3 <2.0.0`; NeoForge: `>=1.6.2 <2.0.0` | TTC profiling and Crafting Plan display for chemicals stored in the ME network through Applied Mekanistics' native AE2 key. |
| AdvancedAE (`advanced_ae`) | `1.21.1 NeoForge`, `26.1.2 NeoForge` | 1.21.1: `>=1.6.11`; 26.1.2: `>=26.1.7` | TTC profiling for AdvancedAE crafting CPUs, including the Quantum Computer. |
| ME Requester (`merequester`) | `1.20.1 Forge`, `1.20.1 Fabric`, `1.21.1 NeoForge` | Forge/Fabric: `>=1.20.1`; NeoForge: `>=1.21.1` | TTC row labels and total TTC hints in ME Requester screens. |
| Neo ECO AE Extension (`neoecoae`) | `1.20.1 Forge` | `>=20.3.0 <20.4.0` | TTC profiling for C-series ECO crafting CPUs, including normal and FastPath dispatch. NeoEco 20.4.1 and 20.4.2 have an upstream AE2 mixin crash. |
| BloodMagic AE2 Addition (`bmaddon`) | `1.20.1 Forge`, `1.21.1 NeoForge`, `26.1.2 NeoForge` | Forge: `>=1.0.4 <1.1.0`; 1.21.1: `>=1.21.1-beta1`; 26.1.2: `>=1.0.2 <1.1.0` | TTC profiling and Crafting Plan display for crafts handled by the Blood Assembler. |
| Crazy AE2 Addons (`crazyae2addons`) | `1.20.1 Forge` | `>=3.2.4 <4.0.0` | TTC profiling and Crafting Plan display with Crazy AE2 Addons CPU priorities. |
| AE2 WCWT (`wcwt`) | `1.20.1 Forge`, `1.21.1 NeoForge` | Forge: `>=1.20.1.10 <1.21`; NeoForge: `>=1.3.8 <2.0.0` | One-item TTC in craftable entries of the wireless comprehensive work terminal. Crafting still uses AE2's normal CPU detection and Crafting Plan. |
| AE2 Wireless Terminals (`ae2wtlib`) | All supported targets | 1.20.1 Forge: `>=15.3.3 <16.0.0`; Fabric: `>=15.2.1 <16.0.0`; 1.21.1: `>=19.5.1 <20.0.0`; 26.1.2: `>=26.1.1-beta <27.0.0` | One-item TTC in craftable entries of its wireless crafting terminal. Crafting still uses AE2's normal CPU detection and Crafting Plan. |
| AE2 Lightning Tech (`ae2lightningtech`) | `1.20.1 Forge` | `>=2.1.0-beta.2` | TTC profiling for Tianshu time-wheel crafting CPUs. |
| OmniSequence: Transfinite (`molecularmanipulator`) | `1.20.1 Forge`, `1.21.1 NeoForge` | Forge: `>=1.3.9-forge`; NeoForge: `>=1.3.9-hotfix` | TTC profiling for Omni-Computation Core crafting CPUs through AE2's native execution path. |
| ExtendedAE-Plus (`extendedae_plus`) | `1.20.1 Forge`, `1.21.1 NeoForge` | Forge: `>=1.5.5 <1.6.0`; NeoForge: `>=1.6.2` | TTC profiling and UI coexistence with ExtendedAE-Plus crafting changes. |
| MEGA Cells (`megacells`) | `1.20.1 Forge`, `1.20.1 Fabric`, `1.21.1 NeoForge` | Forge/Fabric: `>=2.4.6 <3.0.0`; NeoForge: `>=4.11.0 <5.0.0` | TTC profiling and Crafting Plan display for MEGA crafting CPUs through AE2's native execution path. |
| OMNI Cells (`ae2omnicells`) | `1.20.1 Forge`, `1.21.1 NeoForge`, `26.1.2 NeoForge` | 1.20.1/1.21.1: `>=1.1.6 <1.2.0`; 26.1.2: `>=1.1.7 <1.2.0` | TTC profiling and Crafting Plan display for OMNI crafting CPUs through AE2's native execution path. |
| ProjectCell (`projectcell`) | `1.20.1 Forge`, `1.21.1 NeoForge` | `>=1.0.0 <2.0.0` | TTC profiling and Crafting Plan display when ProjectCell supplies ingredients from ProjectE EMC through AE2's native storage and crafting paths. |
| AppliedE / AppliedE TPS Fix (`appliede`) | `1.20.1 Forge`, `1.21.1 NeoForge` | Forge: `>=0.14.0 <1.0.0`; NeoForge: `>=1.0.0-beta <2.0.0` | TTC profiling and Crafting Plan display when AppliedE supplies ingredients from ProjectE EMC through AE2's native key and crafting paths. Install either AppliedE or its TPS Fix fork, not both. |
| Applied Flux (`appflux`) | `1.20.1 Forge`, `1.21.1 NeoForge`, `26.1.2 NeoForge` | 1.20.1: `>=1.3.7 <2.0.0`; 1.21.1: `>=2.1.4 <3.0.0`; 26.1.2: `>=1.0.1 <2.0.0` | TTC profiling and Crafting Plan display for FE stored in the ME network through Applied Flux's native AE2 energy key. |
| Modern AE2 Additions (`mae2`) | `1.20.1 Forge` | `>=1.1.0 <3.0.0` | TTC profiling and Crafting Plan display for CPUs using Modern AE2 Additions dense co-processors through AE2's native crafting path. |

Applied Mekanistics is not declared for the Fabric jar because the supported
Applied Mekanistics releases for these targets are Forge/NeoForge.

Applied Flux is not declared for the Fabric jar because it has no stable Fabric
1.20.1 release.

AEInfinityBooster is covered as an optional range-only addon on Forge 1.20.1
(`1.20.1-1.0.0+20`), NeoForge 1.21.1 (`1.21.1-1.0.0.58`), and NeoForge 26.1.2
(`26.1.2-1.0.0.57`). It uses AE2's normal Crafting Plan; no extra TTC hook is
needed. There is no Fabric release for our supported target.

Applied Botanics (Fork) 1.5.2 is an alternative to the original on Forge 1.20.1.
It shares the `appbot` ID, version and mana key; install one, not both. The full
Forge compatible/latest clients use the fork, while a focused `-ProjectId 545hUrw9`
selects the original and `-ProjectId 1605404` selects the fork.
Both use the same raw-mana integration and Botania 455. The fork has no
published Fabric, NeoForge 1.21.1, or NeoForge 26.1.2 artifact.

Original Applied Botanics 1.5.2 remains pinned for Fabric 1.20.1 and focused
Forge runs. The 1.21.1 alpha requires Botania `454-SNAPSHOT` (upstream
build: `vazkii.botania:botania-neoforge-1.21.1:454-20260621.181850-47`), which
has no matching published Modrinth release; that client is not runtime-pinned
or smoke-verified. No original Applied Botanics release exists for 26.1.2.
The Forge driver verifies native mana storage and normal crafting together,
not a mana-generation recipe.

Advanced Peripherals is covered as an optional native-crafting integration on
Forge 1.20.1 (`0.7.48r`) and NeoForge 1.21.1 (`0.8.0a`, alpha). Its ME Bridge
submits through AE2's normal crafting service, so existing profiling and TTC
apply without another production hook. The Forge driver calls `craftItem`
with a real attached CC:Tweaked computer and checks the result in AE2's UI;
this does not add a ComputerCraft TTC API or display. There are no published
Fabric 1.20.1 or NeoForge 26.1.2 versions.

AE2 Things is covered through native item storage and crafting: the original
Fabric 1.20.1 release (`1.3.2`) and the separately published Forge port
(`1.2.1` for 1.20.1; `1.4.2-beta` for NeoForge 1.21.1). No 26.1.2 artifact
exists. The Forge driver mounts a real DISK, supplies the craft ingredients
through it, and checks a fresh profiling sample and TTC. The Forge/NeoForge
editions no longer contain the old Inscriber or Crystal Growth Chamber;
this integration does not add TTC overlays to the Fabric-only machine UIs.
NeoForge support is compile-checked, but its new beta runtime has not been
promoted into the compatible client profile without a NeoForge smoke run.

Expanded AE is supported on Forge 1.20.1 (`1.2.2`) and NeoForge 1.21.1
(`2.1.1`) through the native AE2 CPU and Crafting Plan path. The Forge driver
builds a CPU with its two-thread accelerator and checks a real craft, sample
and TTC. There are no Fabric 1.20.1 or NeoForge 26.1.2 artifacts. Its existing
Applied Flux/OmniSequence conflict remains: Expanded AE is excluded from the
full compatible profile and tested separately with the latest focused profile.
No coexistence with OmniSequence is claimed.

## Build Tools

If you are building the project yourself, the 1.20.1 modules use Java 17, the
1.21.1 NeoForge module uses Java 21, and the 26.1.2 NeoForge module uses Java 25.
