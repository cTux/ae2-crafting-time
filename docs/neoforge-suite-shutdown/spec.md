# NeoForge Suite Shutdown Spec

## Goal

Make a completed shared-world UI smoke suite restore its disposable fixture
before Minecraft shuts down, so the integrated server can save and exit without
retaining the final case's addon blocks, jobs, or entities.

Tracking issue: [#362](https://github.com/cTux/ae2-crafting-time/issues/362).

## Current behavior

Schema-2 suites restore the captured fixture between cases. After the final case
passes, `TestDriverRuntime` writes the completed suite result and immediately
calls `Minecraft.stop()` instead. The last case's mutations remain live during
server shutdown.

The failing NeoForge 1.21.1 campaign ended with the AdvancedAE status graph. All
four semantic cases passed, but the server then spent more than four minutes in
chunk-unload scheduling while Minecraft displayed `Saving worlds`.

## Requirements

- **NSS-01:** A passing schema-2 suite must restore the same captured fixture
  after its final case that it restores between cases.
- **NSS-02:** Normal client shutdown must start only after that server-thread
  restore completes and the existing two-tick readiness boundary passes.
- **NSS-03:** A restore failure or timeout must remain a suite failure. It must
  not be converted into a pass or hidden by forced termination.
- **NSS-04:** Intermediate case transitions, schema-1 isolation suites,
  single-case runs, semantic assertions, screenshots, and result files must keep
  their current behavior.
- **NSS-05:** The full compatible NeoForge 1.21.1 suite must finish every planned
  case and its native process must exit normally.

## Compatibility

The change belongs to the shared 1.20.1/1.21.1 test-driver runtime. It affects
development smoke suites only. Production mod code, player worlds, packets,
saved-data formats, and published artifacts are unchanged.

## Acceptance criteria

- **NSS-A1:** The final passing schema-2 case enters the existing bounded
  fixture-reset phase instead of stopping Minecraft immediately.
- **NSS-A2:** The fixture restore completes on the integrated-server thread and
  shutdown is requested after the existing post-restore tick wait.
- **NSS-A3:** Existing transition tests remain green and a regression check
  covers the final-cleanup decision.
- **NSS-A4:** A full compatible `1.21.1-neoforge` smoke suite passes and the
  launcher records a normal native exit without forced cleanup.
