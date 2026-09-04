# Faster optional-integration development

These changes would shorten the time needed to add an optional dependency and
prove it through the Forge 1.20.1 UI smoke driver. They are ordered by expected
value. Every P0 and P1 item is implemented; the P2 items remain planned.

| Priority | Optimization | Concrete change | Expected benefit | Safety rule |
| --- | --- | --- | --- | --- |
| P0 | Share one runtime between scenarios | Keep the runtime at `build/ui-smoke/1.20.1-forge/<profile>/runtime`; keep results under scenario-specific directories. `scripts/run-ui-smoke.ps1` currently creates a separate runtime per scenario. | Could eliminate the cold-cache timeout and retry seen when adding a new scenario. | Run only one smoke scenario per workspace at a time. |
| P0 | Add a focused development profile | Load AE2, AE2 Crafting Time, the selected integration, the driver, and recursively required dependencies. | The AE2 Wireless Terminals smoke loaded 36 JARs and produced a 1.17 MB log with 179 warning and 19 error lines. A focused graph should materially reduce startup and review time. | Run the full compatible profile once before merge to catch cross-addon conflicts. |
| P0 | Avoid automatic rebases and duplicate smoke | Rebase only when explicitly requested; a rebase alone does not require full smoke. | Avoids repeating smoke without a relevant change. The duplicate AE2 Wireless Terminals run took 3 minutes 16 seconds. | Choose verification from the actual changes and task scope. |
| P1 | Automate version compatibility discovery | Add `scripts/audit-optional-integration.ps1 -ProjectId <id>`. Query every supported game and loader, download official artifacts, inspect embedded metadata, and print selected version IDs and ranges. | Replaces four manual searches plus artifact and metadata inspection with one command. | Report unsupported rows instead of selecting merely downloadable artifacts. |
| P1 | Derive release tests from the matrix | Replace the four literal dependency-list assertions in `scripts/test-deploy-changed.ps1` with assertions generated from `scripts/release-matrix.json`. | Removes four required test edits for every new dependency and prevents stale expectations. | Keep order and duplicate validation. |
| P1 | Add one consistency validator | Cross-check `scripts/run-client-versions.json`, `scripts/release-matrix.json`, loader metadata, and `docs/dependencies.md`. | Finds missing rows, mismatched version IDs, and forgotten metadata before commit. | Validate the existing sources instead of generating every TOML and JSON file from a new abstraction. |
| P1 | Preserve the existing fixture extension points | CPU integrations add only an `AddonCpuFixture`; wireless integrations add only a `WirelessTerminalFixture` subclass and a compile-only dependency. | Most future driver work stays inside one small fixture without new runner branches or state machines. | Add a new UI flow only when the add-on genuinely differs. |
| P2 | Split preparation from client execution | Give dependency resolution and build their own phase and timeout, then start Minecraft with a shorter scenario timeout. Reuse `run-client.ps1 -ResolveOnly`. | A cold cache no longer consumes the whole gameplay timeout and forces a full retry. | Both phases must use the exact same pinned profile. |
| P2 | Combine the two Gradle launches | Make runtime preparation depend on `testDriverJar`, copy the driver, and start `runClient` in one Gradle invocation. | Removes one Gradle configuration and daemon startup per smoke. | Benchmark after the P0 changes because this is likely a smaller saving. |
| P2 | Baseline third-party warnings | Store normalized known-warning fingerprints per profile and show only new warnings and errors while retaining the complete log. | Reduces manual review to the relevant warning delta. | Never suppress new or repository-owned warnings. |
| P2 | Overlap independent gates | After the commit creates the PR, let GitHub CI run while the VM smoke and PowerShell self-checks run locally. | Wall time becomes roughly the slowest gate instead of the sum of every gate. | Do not run multiple Minecraft clients against the shared runtime concurrently. |

The complete compatible profile remains the mandatory pre-merge gate. Use the
focused profile for iteration, then run the full profile once for the final
result. Do not rebase automatically or repeat full smoke solely because of a rebase.
