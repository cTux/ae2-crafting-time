# Mod Text Shadow Design

Lifecycle and requirements: [specification](spec.md).

## Current flow

Investigation baseline: `f5191db5154bfb0356806e612896f7faf606f9b6`.

`OptionFeature` defines client switches. `FeatureOptions` defaults them on except compact amounts; `ClientConfigFile` iterates client-owned features for TOML reads/writes. `OptionsSession` edits a copy, applies it through `ClientOptionsRuntime` only on save, and resets the client model for Reset all. These paths already provide persistence and draft behavior.

`OptionsScreen` builds feature rows by group, but Appearance currently takes a separate colors/opacity-only path. Its row count, index mapping, input validation, and section reset must account for the new toggle. Merely adding an Appearance enum value would not display or reset it.

Both `shared/src/mc1201/.../mixin/AbstractTableRendererMixin.java` and the `mc2612` counterpart identify mod lines by the `text.ae2craftingtime.` translation prefix. Their normal draw uses `shadow || isAe2CraftingTime`; horizontally scaled badges pass `true`. Thus every recognized mod line is shadowed, including compact amounts emitted by the common Plan and Status table mixins.

Other owned draw calls pass `true` directly:

| Surface | Adapter under `com/ctux/ae2craftingtime/mc1201` | Source family |
| --- | --- | --- |
| Plan total | `mixin/CraftConfirmScreenMixin.java` | `mc1201`, `mc2612` |
| Status header total | `mixin/CraftingCPUScreenMixin.java` | `mc1201`, `mc2612` |
| CPU-card total | `mixin/CPUSelectionListMixin.java` | `mc1201`, `mc2612` |
| Crafting Tree TTC | `CraftingTreeTtc.java` | `mc1201` |
| ME Requester TTC | `mixin/MERequesterScreenMixin.java` | `mc1201` |

The first three targets consume `mc1201`; 26.1.2 consumes `mc2612`, whose draw API is `GuiGraphics.text` rather than `drawString`. Optional integrations use their existing availability rules.

## Change

Add client-owned `TEXT_SHADOW` in Appearance with key `textShadow`. Reuse the feature model and persistence. Expose its current client value through the existing runtime class; do not use the general `ClientOptionsRuntime.enabled` method, which suppresses most features when server profiling is disabled. Cosmetic preferences must also work on compact amount lines when profiling is off.

Include Appearance features in the existing row layout, then offset color/opacity indices by the feature-row count. Reset the group's features as well as its colors and opacity. Preserve unsaved input validation before navigation or rebuilding the screen, including when toggling the new feature. Avoid a new widget framework.

For table text, select the configured value when the line belongs to this mod and otherwise preserve the original `shadow` value. Apply the same client setting to the scaled branch and every direct call listed above. Keep recognition at the current mod translation boundary; do not match arbitrary native text or change tooltip/chat rendering. Put any new pure shadow-selection decision in the existing shared core model and test it there, keeping Minecraft adapters thin.

## Failure and compatibility boundaries

- Missing or malformed persisted values fall back to On through the existing parser/model.
- Save failure retains existing runtime state; retain the existing save-before-apply ordering.
- Both native shadow inputs must survive unchanged even when the option is Off.
- Default On preserves the shipped appearance without migration or a server handshake.
- The new enum is client-owned; server wire encoding must continue to include server features only.
- No new dependency, renderer abstraction, runtime fixture, or smoke infrastructure is needed.

The four module source sets and existing JUnit test homes are present. Gradle's configured Foojay resolver supplies toolchains for Java 17, 21, and 25 where provisioning is needed; successful local provisioning has not been claimed. This run will not launch Minecraft or CodexVM, and automated checks do not establish GUI-scale readability.
