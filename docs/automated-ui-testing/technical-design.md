# Automated UI Testing Technical Design

## Planned smoke-policy enforcement

Implement [SP-01 through SP-04](spec.md#smoke-policy) without changing product
translation support. Resolve the newest implemented adapter per dependency and
target before scheduling direct cases, then assert the runtime-selected ID
against it. The integration selector's immutable catalogue/snapshot owns this
identity; do not infer it by sorting dependency versions or duplicate its order
in a runner. Until that selector exists, explicitly record the adapter/contract
established by artifact inspection in the campaign evidence.

Preserve compatible/latest graph identities. Where a compatible pin selects
an older variant, omit its direct adapter case and schedule a focused fixture
for the newest variant. Record the omitted case as an older-adapter policy skip,
not PASS, failure, or removed support. Latest-profile diagnostics do not waive
the required newest-adapter case. A requested named pack is never upgraded to
meet this policy.

Set `en_us` before the first observed frame and verify it for each scenario.
Remove Ukrainian reload/capture states from the shared and 26.1.2 driver
counterparts and update checkpoint/result validators together. Keep distinct
English behavior checkpoints and both-language static resource checks. Historical
campaigns retain their original languages, filenames, and results.

## Execution path

The public unattended command is `scripts/run-ui-smoke.ps1` with no target.
It delegates to `run-ui-smoke-matrix.ps1`, which selects release-matrix targets
in order. `-Latest` selects a separate diagnostic campaign. A focused run uses
`run-ui-smoke-matrix.ps1 -Target <id> -Scenario <scenario>`.

```text
release-matrix.json + run-client-versions.json + ui-smoke-coverage.json
  -> host run-client.ps1 -ResolveOnly -Packaged
  -> immutable production / driver / dependency bundle
  -> invoke-ui-smoke-codexvm.ps1
  -> guest-local disposable worlds and native loader
  -> driver observations, screenshots, logs and campaign result
```

JAR builds and dependency resolution stay on the host. The guest uses its
installed loader's `launch.json`; target, Java major and resolved loader must
match. `prepare-ui-smoke-launch.ps1` preserves that installation's native
classpath and assets, replaces its game directory and test properties, and
uses an 8 GB heap. It copies the bundle's exact manifest into the owned runtime
and verifies every copied JAR hash. Bundles are immutable for each campaign;
a later build cannot replace files while a guest reads them.

## Ownership

| Component | Responsibility |
| --- | --- |
| `run-ui-smoke-matrix.ps1` | Target order, host preparation, VM dispatch, campaign evidence and exit status |
| `get-ui-smoke-coverage.ps1` | Matrix parity, explicit project dispositions, exclusions and required scenarios |
| `run-ui-smoke-codexvm.ps1` | Interactive guest task, worktree staging and recorded-process stop |
| `run-ui-smoke.ps1` | Disposable fixtures, runtime lock, native launch, result validation and cleanup |
| `prepare-ui-smoke-suite.ps1` | One fresh marked world for each scenario in a single client session |
| Driver Java | Bounded scenarios, final UI observations, server outcome assertions and atomic results |
| Target adapters | Loader activation, Minecraft input APIs, fixture APIs and framebuffer capture |

All four targets have separate development-only driver artifacts under
`build/test-driver`. Their version must match the installed production mod.
Existing artifact checks reject driver classes and mod IDs in production JARs;
release discovery accepts only production filenames. Explicit test properties
and a valid disposable-world marker are required for activation.

## Standard scenario

`StandardAe2Scenario` creates a native AE2 grid in the copied world. Two actual
vanilla furnaces process cobblestone into stone, then smooth stone. Known
retained samples give the two rows distinct estimates for sorting assertions.
Fixture code supplies fuel and imports only actual furnace output through the
AE2 storage API. Completion requires exactly one smooth stone in storage and
an idle crafting CPU.

The client opens the terminal face, selects the craftable output, clicks the
amount confirmation and Crafting Plan buttons, then opens Crafting Status.
It exercises all TTC sort modes, hovers the visible item, sends modifier-click
input and checks returned chat plus server reset state. Fuel is withheld to
observe waiting, running and delayed states, then restored to complete the job.

Driver observations inspect final screen rows, rendered translation components,
widget bounds, item cells and tooltips. Assertions use those observations and
the server's actual job/output state. Each input phase waits for stable frames;
phase timestamps and the last fixture checkpoint make timeouts attributable.
Modifier keys are released on both success and failure.
The Windows VM receives native key events through Minecraft's existing JNA
library; input does not depend on AWT's headless setting. Before pressing keys,
the driver verifies native foreground ownership and transfers focus from the
previous desktop window when necessary. It waits for Minecraft to observe the
modifiers before clicking the row.

After completion, the driver saves the last job view, returns to the terminal
and reopens Crafting Status through its buttons. The fresh view must be empty.
Older AE2 builds can retain a final incremental row in the already-open view;
`status-finished-job.png` preserves that observation instead of hiding it.
Each screenshot also has a JSON snapshot with transformed screen-space bounds.

## Coverage and results

`ui-smoke-coverage.json` contains project IDs, dispositions and required
scenario IDs, without dependency versions. Its keys must exactly match the
client matrix. Compatible exclusions require a reason; replacements remain
explicit. A missing standard or direct scenario fails before launch. Startup
alone never promotes coexistence to direct integration coverage.

The driver atomically writes `result.json` with schema, driver, target, profile,
scenario, `complete`, result, required checks, screenshots and optional failure.
The runner independently validates these fields, screenshot existence, process
exit and fatal-log absence. Each suite case has its own evidence directory;
shared logs and the resolved-mod manifest belong to the containing run.

Campaigns are stored under
`build/ui-smoke/campaigns/<UTC-run-id>/<profile>/<target>/`. They preserve the
host bundle, coverage ledger and copied guest run. Compatible failures produce
a nonzero exit after all selected targets have been attempted. Latest failures
are retained as `DIAGNOSTIC_FAILURE` and do not change compatible results.
A focused campaign leaves unrelated coverage `NOT_RUN`.
Completed case outcomes remain in the ledger even when another part of the run
fails. An unconfirmed client exit stops the campaign before another launch.

## Fixture and process safety

The runner hashes the marked source fixture before copying and verifies it
again during cleanup. Only uniquely named copies inside the owned runtime may
be opened or removed. Each suite case receives a separate copy. Runtime locks
prevent concurrent use of the same target/profile directory.

Fabric keeps the marked Forge block layout but copies `level.dat` from the
tracked Fabric world. This removes the Forge metadata dependency on Blood Magic
dimensions without changing either checked-in world. Both source layout and
target metadata hashes are recorded before and after the run.

The native Java PID, process creation time and argument-file path identify the
launched client. Normal driver completion requests shutdown. Timeout cleanup
can terminate only that recorded process tree; explicit stop also verifies its
creation time and command line to reject a reused PID. No broad Java process
selection is used. Failed runs retain available evidence before later attempts.

Interactive diagnosis is single-target only. It forwards explicit interactive
mode and the existing per-run token environment to the driver. The bounded
loopback endpoint and its existing allowlist remain separate from unattended
release-facing runs.

Screenshots require visual review, not pixel equality. The permanent archive
and timing contract is documented in [UI smoke evidence](../ui-smoke-evidence.md).
