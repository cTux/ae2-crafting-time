# Project Infinity 0.0.52.0: one-run smoke measurement

On 2026-09-08, all **36 selected smoke cases passed in one client and one loaded
world**, in **15m 6.975s from launch to exit**. The driver saved 87 distinct
screenshots without agent input after launch. This is one observed run, not a
cold/warm benchmark or a claim of full automatic visual qualification.

Tracked in [#347](https://github.com/cTux/ae2-crafting-time/issues/347), implemented
in [#349](https://github.com/cTux/ae2-crafting-time/pull/349). See the
[specification](spec.md#one-loaded-world-per-graph),
[technical design](technical-design.md), and [implementation plan](implementation-plan.md).

## What changed

The suite loads a marked disposable world once. Before the first case, it captures
the bounded test fixture using Minecraft's native structure template. Between
cases it restores blocks and block entities, removes test entities, restores
player inventory and position and AE2 Crafting Time profiler state, then clears
client caches and observations. It waits for server completion before advancing.
It does not claim to restore arbitrary third-party global state outside the fixture.

The old suite kept one JVM but loaded a different world for every case. That
repeated expensive mod and JEI initialization. The default schema-2 suite removes
those reloads; `-FreshWorldPerCase` retains schema 1 for isolation diagnostics.
Existing assertions, screenshot checkpoints and timeout limits remain in place.
The [driver specification](../test-driver/spec.md#single-launch-suites) describes
preparation and launch. For Prism, prepare the selected leaves once with
`scripts/prepare-ui-smoke-suite.ps1`, launch with the returned world and output
properties, and validate/archive after that exact process exits.

## Exact run

| Field | Recorded value |
|---|---|
| Pack | Project Infinity 0.0.52.0 |
| Minecraft / loader / AE2 | 1.20.1 / Forge 47.4.20 / AE2 15.4.10 |
| Installed graph | 363 top-level JARs; 386 loaded mods including nested metadata |
| Guest | Windows 11, 16 logical processors, 16 GiB RAM, 8 GiB client heap |
| Renderer | VMware SVGA3D, OpenGL 4.3, Mesa 24.1.0 |
| Capture | 2558 x 1093, English, automatic GUI scale resolving to 4 |
| Runtime source | `81f53a5735b07ba1127b547e071fa9306b0d4cc9` |
| Driver SHA-256 | `2b3134cca951669700ce7348095e919d535e0654f0c3a92b19b2fb93d5fb6003` |
| Production SHA-256 | `e1f7108142f00505b5d34ce6c43ad320729e2cf92a9d6037106317c045581c9b` |
| Graph SHA-256 | `47842dbee09ed973d141f73191c7c82834f54c0b860e79b403545d85e941099b` |
| Launch / exit UTC | 10:45:02.3907516 / 11:00:09.3655455 |
| Java process | PID 7628; started 10:45:13.2186518 UTC; exit code 0; absence verified |
| World | `ae2ct-dbda43fc35ba47c5832a9da58571f722`, one schema-2 world for all cases |
| Controller decisions during execution | 0; read-only monitoring and display inspection only |

A later build-only change routes TypeTools metadata to Maven Central after
repeated mirror HTTP 502 responses. It retains resolved version 0.6.3 and does not
change the tested runtime classes. Final host validation also fixes repeated
references to the same checkpoint file: it validates that file once while keeping
case identity, hash, dimensions and cross-file identity checks. Runtime evidence
was revalidated without editing its results or PNGs.

## Coverage and evidence

The 36 cases comprise six standard AE2 leaves, craft plan, crafting tree,
17 CPU integration leaves, three wireless terminal integrations, ME Requester,
ME Network Analyser, and six dispatch/storage status leaves. The exact ordered
case list, checks, per-case timing and screenshot names are in the archived suite
plan and results. All 36 passed independent semantic validation.

The final archive is `20260908T111040712Z-e9ac1b5d` under the configured
`ui-smoke/modpacks/project-infinity-0.0.52.0` archive root. It contains the exact
JARs, their hashes, logs, suite plan, raw results, PNG/JSON pairs, timing breakdown,
render identity, manual visual review, gate and archive manifest.

The main agent inspected all 87 screenshots after execution. TTC panels, labels,
sort controls, tooltips, status transitions and selected integration screens were
visible. The existing base AE2 elapsed-title anomaly reproduced and remains
tracked in [#350](https://github.com/cTux/ae2-crafting-time/issues/350).
No claim is made that the complete modpack UI is defect-free.

The final gate records semantic, archive and client-exit checks as `PASS` and
visual qualification as `REVIEW_REQUIRED`. The checked-in baseline catalogue is
empty. Manual inspection does not silently promote screenshots or rewrite this
gate to `PASS`. Qualified visual references and four-target/newest-adapter runtime
qualification remain separate work; this exact-pack run does not replace them.

Two failed host finalizations are preserved alongside the final archive. First,
the ad hoc pack collector incorrectly reused the newest adapter expectation
`tree-layout`. Independent bytecode inspection confirmed that this pack's
AE2 Crafting Tree 1.1.1 correctly uses `tree-helper`. Second, duplicate references
to five saved checkpoints triggered the validator's global identity check. The
host fix has a failing-first regression test passing under PowerShell 5 and 7.
Neither correction reran Minecraft or changed the raw captured evidence.

## Comparison limits

The previous separate-world attempt stopped after 11 passing cases, with case 12
still running, at **1h 6m 36.048s**. It was interrupted to implement the requested
one-world approach; it is not a completed baseline. Later cases in that attempt
spent roughly 230-237 seconds in world startup, versus 1.383-2.794 seconds for
fixture resets in the new run. The VM also changed from 8 to 16 logical processors,
so these observations must not be presented as a controlled speedup ratio.

The 35 resets totalled **60.028s**, averaging **1.715s**. They are already included
in the case-execution interval below, not additional time. The next major runtime
cost is initial modpack startup; there are no per-case agent pauses left to remove.

| Part of the smoke UI testing task | Time | Why it took that long |
|---|---:|---|
| Launch to first world-ready observation | 10m 24.742s | Prism/Java, mod loading and the single initial world load |
| First world ready to final case completion | 4m 34.917s | All 36 cases, 87 saved screenshots and 35 fixture resets |
| Final case completion to process exit | 7.316s | World save, mod shutdown and process exit |
| **Total successful client run** | **15m 6.975s** | One launch, one loaded world, clean exit |
| Final host validation and archive | 4.994s | 2.415s validation plus 2.579s archive; transfer excluded |
| Research, earlier attempts, VM recovery, staging, transfer, manual review and delivery | not measured as one continuous interval | These span interrupted attempts and implementation work |
| **Total complete task time** | **not measured** | The successful client runtime above is not total task elapsed time |
