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
| AE2: Crafting Tree (`ae2ct`) | `1.20.1 Forge`, `1.20.1 Fabric`, `1.21.1 NeoForge` | `>=1.0.1` | TTC lines and details in AE2: Crafting Tree UI. |
| Applied Mekanistics (`appmek`) | `1.20.1 Forge`, `1.21.1 NeoForge` | Forge: `>=1.4.0`; NeoForge: `>=1.6.0` | TTC profiling and display for Applied Mekanistics chemical keys. |
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

Applied Mekanistics is not declared for the Fabric jar because the supported
Applied Mekanistics releases for these targets are Forge/NeoForge.

## Build Tools

If you are building the project yourself, the 1.20.1 modules use Java 17, the
1.21.1 NeoForge module uses Java 21, and the 26.1.2 NeoForge module uses Java 25.
