# Screenshot UI Scale Technical Design

This design implements [SUI-01 through SUI-09](spec.md#requirements) by using
Minecraft's native automatic GUI scale. It adds no custom scale calculator and
does not change screenshot pixels after capture.

## Current seams

- [`scripts/run-ui-smoke.ps1`](../../scripts/run-ui-smoke.ps1) writes a fresh
  `options.txt` for every prepared runtime with `guiScale:2`, then launches the
  selected native client.
- The shared and 26.1.2 test-driver runtimes maximize the GLFW window before
  scenario work begins.
- Both `UiObservationStore` implementations already record the effective GUI
  scale, scaled width and height, and owned GUI rectangle in every semantic
  screenshot sidecar.
- The named-modpack workflow stages an eligible Prism instance on guest-local
  NTFS before launch, but it does not currently normalize the staged GUI scale.

## Selected behavior

Set Minecraft's stored GUI-scale option to automatic (`guiScale:0`) before
launch. Minecraft then owns the cross-version scale calculation and recalculates
it when the driver or operator maximizes the window. Scenario stability waits
remain the capture boundary, so screenshots are taken only after the resized
screen has settled.

The configured value and the effective value are different evidence:

- `options.txt` proves automatic scaling was requested;
- the screenshot sidecar's numeric scale and dimensions prove what the running
  client actually used;
- the PNG and GUI bounds prove the required screen stayed readable and
  unclipped.

Do not duplicate Minecraft's minimum-dimension or Unicode-even-scale rules in
PowerShell. Those rules belong to each supported Minecraft version.

## Prepared-client flow

```text
shared UI-smoke runner
  -> write isolated runtime options with guiScale:0
  -> launch the selected prepared target/profile
  -> test driver maximizes the exact GLFW window
  -> Minecraft recalculates its effective scale
  -> scenario waits for stable frames
  -> PNG + semantic sidecar
  -> runner validates sidecar shape and GUI containment
```

Change the existing `guiScale:2` entry in `scripts/run-ui-smoke.ps1`; do not
introduce a second options writer. All four targets, both profiles, focused
cases, and suites already pass through this seam.

Extend `scripts/test-run-ui-smoke.ps1` so its fake client rejects an options
file that does not contain exactly one automatic GUI-scale entry. Keep the
existing accessibility-option assertion.

The result validators should require a finite positive effective scale, positive
scaled dimensions, and GUI bounds contained within those dimensions. They do
not need to prove Minecraft's value is mathematically maximal because that would
reimplement version-owned behavior.

## Named-modpack flow

```text
verified Prism instance in Codex group
  -> copy to marked guest-local staging root
  -> resolve the staged Minecraft runtime directory
  -> replace or add one guiScale entry with automatic mode
  -> read back exactly one guiScale:0 entry
  -> launch the staged instance and maximize its exact window
  -> PNG + semantic sidecar + visual review
  -> archive evidence, restore Prism root, remove staged copy
```

Update the named-modpack skill and its `launch-and-verify.md` reference to make
this a required preflight step. The edit is limited to the staged instance's
`options.txt`. Preserve its encoding, line order where practical, and every
unrelated option. If the file has duplicate GUI-scale entries, normalize them to
one entry and verify the readback before launch. Never edit the managed source
instance, another Prism group, or global Prism settings.

The prepared-client skill should state that automatic scale comes from the
shared runner and must be confirmed in captured evidence. No Prism behavior is
added to the prepared-client route.

## Evidence and failures

Update `docs/ui-smoke-evidence.md` so a visual review records the maximized
framebuffer, effective scale, scaled dimensions, and containment result for the
representative prepared-client and named-modpack captures.

Prepared setup fails before launch when the generated options file does not
request automatic scale. Named-modpack setup fails before launch when the exact
staged runtime cannot be resolved, its options cannot be updated, or readback
does not contain exactly one automatic entry.

A runtime capture fails when its sidecar lacks a positive effective scale or
dimensions, its GUI rectangle falls outside the scaled screen, or required UI
is clipped in the PNG. A smaller-than-expected scale is not automatically a
failure when it is needed to fit the current screen or tooltip; record the
viewport and inspect the image instead.

Historical archives stay unchanged. A diagnostic rerun uses a new attempt
directory and cannot overwrite the failed evidence.

## Requirement coverage

| Requirements | Design seam |
| --- | --- |
| SUI-01, SUI-02, SUI-04 | Shared prepared options seam and native automatic scaling |
| SUI-03, SUI-07 | Existing sidecar scale, dimensions, bounds, and PNG review |
| SUI-05 | Staged named-modpack `options.txt` preflight and readback |
| SUI-06 | Existing runner and evidence inputs remain unchanged |
| SUI-08 | Prepared fake-client assertion and named-modpack preflight |
| SUI-09 | Same-framebuffer prepared and modpack before/after review |

## Rejected alternatives

- A larger fixed value would still be wrong for another framebuffer or a tall
  screen and could clip required UI.
- A repository-owned scale calculator would duplicate version-specific
  Minecraft behavior and need unnecessary cross-version maintenance.
- Cropping screenshots would hide context and would not make the live UI easier
  to inspect.
- Changing the VM resolution would affect every desktop workflow and still
  would not adapt to other displays.
