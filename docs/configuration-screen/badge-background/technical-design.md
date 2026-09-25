# Badge Background Design

Lifecycle and requirements: [specification](spec.md).

## Current flow

Investigation baseline: `dd3f7b468dbb31757fb98930d458857e6ed5601d`.

`OptionFeature` defines client switches. `FeatureOptions` defaults Appearance
switches On. `ClientConfig` copies and resets those features, while
`ClientConfigFile` reads and writes client-owned keys generically. `OptionsScreen`
already includes Appearance feature rows before colors and opacity, and its
section reset includes those features. `OptionsSession` keeps an editable copy;
`ClientOptionsRuntime.apply` saves successfully before replacing runtime state.

`ClientOptionsRuntime.badgeBackground()` composes the saved opacity and RGB into
`TtcBadge.BACKGROUND` on initialization and apply. Both `TtcBadge` implementations
unconditionally emit three rectangles. There is no independent visibility value.
All production callers pass `TtcBadge.BACKGROUND`:

| Surface | Caller under `com/ctux/ae2craftingtime/mc1201` | Source family |
| --- | --- | --- |
| Plan/Status row badges, normal and horizontally scaled | `mixin/AbstractTableRendererMixin.java` | `mc1201`, `mc2612` |
| Plan total | `mixin/CraftConfirmScreenMixin.java` | Both |
| Status header total | `mixin/CraftingCPUScreenMixin.java` | Both |
| CPU-card total | `mixin/CPUSelectionListMixin.java` | Both |
| Crafting Tree TTC | `CraftingTreeTtc.java` | `mc1201` |
| ME Requester TTC | `mixin/MERequesterScreenMixin.java` | `mc1201` |

Text drawing follows each background call independently. Native AE2 window,
row, and tooltip backgrounds do not call this helper. The first three release
targets use `mc1201`; 26.1.2 uses `mc2612` and `GuiGraphicsExtractor`.

## Change

Add `BADGE_BACKGROUND` / `badgeBackground` as CLIENT/APPEARANCE in `OptionFeature`.
Reuse generic UI, persistence, draft, copy, and reset behavior. Keep the existing
opacity and color fields untouched when toggling.

Skip fills at the two `TtcBadge.fillRoundedRect` entry points when the client
switch is Off. Keep shared pure policy in the existing core model with coverage;
keep each rendering adapter limited to the guard and existing API calls. Do not
scatter guards through callers or hide text/layout along with the rectangle.
Do not use `ClientOptionsRuntime.enabled` for this cosmetic choice: it suppresses
most features when server profiling is disabled, including situations where
compact amount text still renders. Read the client preference directly.

Keep `BACKGROUND` composed from the user's saved values. No change to the
foreground or shadow selection is needed, and no new rendering framework,
configuration library, or dependency is needed.

## Boundaries and failure cases

- Missing or invalid boolean values use On through the existing parser/model.
- An unsuccessful save must not update runtime appearance; retain save-before-apply.
- Off/On preserves opacity at `0`, `255`, and intermediate values and any valid RGB.
- Resetting Appearance or all options restores their existing color/opacity defaults
  as well as the new switch; this differs intentionally from merely toggling it.
- Adding a client enum does not change `ServerOptionsWire`: its bit index advances
  only for SERVER features. No wire or persisted world format changes.
- Existing Appearance indexing is generic, but its model test currently uses one
  feature row. Verify the new two-toggle layout and the color/opacity page boundary.

## Verification infrastructure

Host Java 17.0.19, 21.0.11, and 25.0.4.1 executables were verified during
investigation. The release matrix, compatible dependency inventory, dispatcher,
guest launcher, and source fixture worlds exist. Forge and both NeoForge source
markers exist. `prepare-ui-smoke-suite.ps1` intentionally selects the Forge
source for Fabric; `FabricBaseFixture` supplies its native fixture path.

`StandardAe2Scenario` already drives Plan/Status screens and Options widgets;
`CpuListTtcScenario` and the optional `crafting-tree-screen` and
`merequester-screen` cases cover the other callers. Reuse these flows and their
checkpoint/capture machinery for both background states. Existing cases do not
yet prove this switch. Small focused checkpoints are in scope; a new runner,
fixture system, or broad configuration-screen test project is not.

CodexVM was off and was started only for read-only preflight. Its VMX uses
localhost VNC and `vmrun list` confirms the exact VM running, but guest IP queries
reported VMware Tools unavailable. Guest Java 17/21/25, prepared `launch.json`
files, installed loader identities, and SSH access therefore remain unverified.
Resolve this prerequisite using the existing VM workflow before runtime testing;
do not silently replace native clients or expand infrastructure.
