# Mod automation coverage

The [dependency matrix](dependencies.md#dependency-and-integration-matrix) owns
required and optional versions, integration details, project links, and per-target
TestDriver availability. This file records client setup, modpack campaigns, and
what their evidence does and does not prove.

## Client setup and exclusions

- Forge compatible/latest full clients select Applied Botanics Fork `1.5.2`.
  The original shares its mod ID and filename, so they cannot coexist. Focused
  `-ProjectId 545hUrw9` selects original Applied Botanics; `-ProjectId 1605404`
  selects the fork. Fabric and focused Forge runs pin the original `1.5.2`.
  Both Forge artifacts use Botania 455 and the same raw-mana integration.
- Original Applied Botanics 1.21.1 requires an unavailable published Botania
  snapshot; it is not runtime-pinned or smoke-verified. See the exact coordinate
  and support boundary in [dependencies](dependencies.md#compatibility-boundaries).
- AE2 Things NeoForge `1.4.2-beta` remains compile-checked without a compatible
  runtime pin until a NeoForge smoke run promotes it.
- Expanded AE is excluded from the full compatible graph because of its
  Applied Flux/OmniSequence conflict. Forge tests use the latest focused profile;
  no coexistence with OmniSequence is claimed. The Forge client explicitly
  resolves AdvancedAE (required by an unguarded released mixin), ExtendedAE, and
  AE2 Wireless Terminals (declared under malformed upstream TOML dependency
  tables). Modrinth does not list these prerequisites.

## Modpack and prepared-client evidence

Project Infinity 0.1 (`0.0.51.3 HOTFIX`) also uses Crazy AE2 Addons `2.6.2`,
WCWT `1.20.1.7-hotfix`, NeoEco `20.4.0`, and OmniSequence
`1.3.8-hotfix-forge`. Forge metadata admits these pack versions; the existing
scenario fixtures remain applicable. NeoEco's expected-output hook accepts
both the 20.3 accounting object and the 20.4 batched-dispatch signature.
The prepared-client pins in [the dependency matrix](dependencies.md#dependency-and-integration-matrix)
are unchanged. CodexVM passed dependency
validation but Minecraft's square texture probe lowered its detected limit to
`8192`, preventing the pack's `16384x8192` atlas. A guest GL allocation probe later
confirmed that SVGA3D supports that rectangular atlas; the opt-in test-driver
workaround is documented in the test-driver design. A user-approved host-GPU rerun in a disposable
copy passed the dedicated scenarios for all four exact versions above, including
fresh CPU profiling samples and visible TTC for the three CPU integrations and
terminal tooltip/plan TTC for WCWT. The original pack's mods were not upgraded
or removed. These scenario results do not imply full modpack gameplay coverage.

The subsequent CodexVM campaign `20260902T084023Z-suite` passed all 23 installed
integration scenarios in one Minecraft process, with a fresh disposable world
per case, 31 visually inspected checkpoint screenshots, and exit code 0. It used
the exact pack above with Forge `47.4.20` and the opt-in rectangular atlas probe.
All 358 third-party JAR hashes and the original source instance were unchanged.
The earlier failed VM attempt is retained separately; the successful retry
includes fixes for capture-before-render and double initialization of AE2 nodes.
Per-mod results, screenshots, shared logs, and checkpoint mappings are archived
under the campaign ID using the [smoke evidence layout](ui-smoke-evidence.md).

The proposed pack update passed CodexVM campaign `20260902T100314Z-suite`:
NeoEco `20.4.2`, OmniSequence `1.3.9-forge`, WCWT `1.20.1.10`, and LightningTech
`2.1.0-beta.2`, with its required Thunderbolt `2.0.0-beta.2`. All four scenarios
passed in one Minecraft process with five visually inspected screenshots.
The three CPU cases produced fresh profiling samples and post-craft TTC;
WCWT covered its terminal tooltip and Crafting Plan. The earlier attempt
`20260902T095012Z-suite` is retained as failed: the driver missed the estimate
appended after the new tooltip label. The corrected observer traverses appended
text without treating missing-data text as a resolved estimate.
Only those requested dependencies changed in the disposable pack copy; the
prepared-client pins in [the dependency matrix](dependencies.md#dependency-and-integration-matrix)
are unchanged. This baseline smoke does not reproduce
the separately reported LightningTech Crimson Ingot job or verify its controller
layout, and it does not imply full modpack gameplay coverage.

The author's `Project Infinity-TEST 0.0.52.0 TEST.zip` includes LightningTech
`2.1.0-beta.2` and Thunderbolt `2.0.0-beta.2`. The time-wheel integration records
completed waiting demand, including standalone final outputs sent to ME storage.
Those outputs return zero from the CPU's storage-insertion API, so using that
return value missed their samples. The Forge driver now checks that the Tianshu
pool actually receives its smooth-stone smelting job before waiting for a fresh
sample and visible TTC. A real furnace and hopper return the output through an
ME interface. This covers ordinary final-output accounting; it does not claim
coverage of every Botanical Extra Machinery recipe or the controller's layout.

The prepared Forge campaign `20260902T110131Z-suite` completed all 25 scenarios
in one JVM with 47 loaded mods, 33 inspected screenshots, and launcher exit 0.
All automated checks passed, but the visual audit found ME Requester's row TTC
partly covered by its configured item icon; its header total remained readable.
Treat this as 24 visual scenario passes and one visual failure, not a clean full
UI pass. The source is the badge/item render order in `MERequesterScreenMixin`;
the current draw-call and rectangle checks do not detect that occlusion.
The earlier campaign `20260902T104212Z-suite` is retained after its blank
AEInfinityBooster captures were rejected. The rerun's corrected terminal/plan
captures show their contents. Production classes and every third-party JAR were
unchanged between attempts; only the driver changed. The complete dependency
inventory, hashes, images, shared logs, and timing reports use the archive layout
in [UI smoke evidence](ui-smoke-evidence.md). Crafting Tree has no dedicated UI
scenario, and the known compatible-profile exclusions remain in effect.

The prepared Forge suite also waits for rendered item tooltips in range-only
wireless terminals and rendered plan TTC before capture; populated menu data
alone is not screenshot readiness.
