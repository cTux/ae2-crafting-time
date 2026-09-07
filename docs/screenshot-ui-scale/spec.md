# Screenshot UI Scale Spec

## Goal

Make every new UI-smoke screenshot use as much of the available framebuffer as
Minecraft safely allows, so text, tooltips, controls, and layout details are
larger and easier to review without clipping the tested screen.

Tracking issue: [#319](https://github.com/cTux/ae2-crafting-time/issues/319).

## Current behavior

Prepared clients start with a fixed GUI scale before the test driver maximizes
the Minecraft window. The framebuffer grows, but the UI keeps the smaller fixed
scale and leaves unnecessary empty space around the tested screen. Named
modpack runs maximize the window too, but their staged instance can retain any
GUI scale supplied by the pack or an earlier launch.

## Requirements

- **SUI-01:** Every prepared client and every eligible named-modpack smoke run
  must select the largest Minecraft GUI scale that keeps the complete tested
  screen visible in the actual screenshot framebuffer.
- **SUI-02:** Scale selection must adapt after the exact Minecraft window is
  maximized. It must not assume that the initial launch dimensions are the
  final screenshot dimensions.
- **SUI-03:** The tested screen, item cells, buttons, text, and required
  tooltips must remain fully visible. Containment and readability take priority
  over making the UI larger.
- **SUI-04:** The behavior must cover all four prepared targets, compatible and
  latest profiles, focused scenarios, single-launch suites, and named modpacks
  launched through the Prism workflow.
- **SUI-05:** Named-modpack setup may change the GUI scale only in the
  disposable guest-local staged instance. It must preserve the managed source
  instance and every unrelated option.
- **SUI-06:** Existing fixed language, fixture, cursor-position, scenario,
  target, profile, and evidence-isolation rules remain unchanged.
- **SUI-07:** Each screenshot sidecar must retain the effective GUI scale,
  scaled dimensions, and owned GUI bounds needed to prove containment.
- **SUI-08:** Automated runner checks must reject a prepared client that is not
  configured for adaptive scaling. Named-modpack preflight must read the staged
  option back before launch.
- **SUI-09:** Runtime verification must compare representative before-and-after
  screenshots at the same maximized framebuffer and confirm that the new image
  is clearer without a layout regression.

## Compatibility

The change applies only to development and smoke-test clients. It does not alter
production mod code, packets, worlds, saved data, server behavior, translations,
or player settings.

All supported Minecraft and loader rows remain in scope:

| Minecraft | Loader | Prepared target |
| --- | --- | --- |
| 1.20.1 | Forge | `1.20.1-forge` |
| 1.20.1 | Fabric | `1.20.1-fabric` |
| 1.21.1 | NeoForge | `1.21.1-neoforge` |
| 26.1.2 | NeoForge | `26.1.2-neoforge` |

An effective scale does not have to be the same across targets, screens, or VM
display sizes. A smaller scale is correct when it is the largest value that
keeps the tested screen and tooltip visible.

## Non-goals

- Cropping, resizing, or sharpening PNG files after capture.
- Changing AE2 or AE2 Crafting Time screen dimensions.
- Changing the CodexVM display resolution or forcing fullscreen mode.
- Replacing semantic assertions with screenshot comparison.
- Rewriting historical screenshot archives or documentation images.
- Modifying a user's normal Prism instance or global Prism settings.

## Acceptance criteria

- **SUI-A1:** Every prepared target and profile receives adaptive GUI scaling
  through the shared runner before Minecraft starts.
- **SUI-A2:** The named-modpack workflow applies and verifies the same behavior
  only in the exact disposable staged instance.
- **SUI-A3:** Runner tests fail when prepared-client adaptive scaling is absent,
  fixed, or malformed.
- **SUI-A4:** Runtime sidecars report a positive effective scale, scaled
  dimensions, and GUI bounds wholly inside those dimensions.
- **SUI-A5:** At least one prepared client and one named modpack are captured at
  the same maximized framebuffer before and after the change. When the viewport
  permits a larger scale, both after images use it and show less unused space.
- **SUI-A6:** Every required screen, tooltip, item cell, button, and text region
  remains readable and unclipped in the reviewed after images.
