# Trinity implementation and verification plan

Scope and delivery status: [specification](spec.md). Hook semantics and source
contracts: [technical design](technical-design.md).

## Two implementation phases

Merge these planning documents before executable work. Repository policy forbids
local tests before the hook-created PR, while TR-01 requires runtime reproduction
before adding the adapter. Use one implementation PR in two deliberate phases:

1. Step 6 implements only the native fixture, independent observations and their
   small checks. Steps 7-8 review and commit that candidate; the post-commit hook
   opens the implementation PR. Do not claim the feature complete.
2. Step 9 runs the fixture-only baseline on that exact head and records actual
   Trinity completion with missing profiling. A normal positive profiling check
   is expected to fail here; identify it as diagnostic baseline evidence, not a
   passing implementation test. If the gap is not reproduced, stop adapter work
   and investigate the observed path.
3. Return to Step 10 for the justified adapter follow-up, related tests and docs.
   Repeat review and affected verification on the new head. Only the final clean
   supported scenario and current-head CI can satisfy the completion gate.

The issue already explicitly authorizes the disposable fixture. It extends the
existing driver and launcher; do not introduce a separate runner or silently
expand into general Trinity infrastructure.

## Work and criterion mapping

| Order | Changes | Checks / criteria |
| --- | --- | --- |
| 1 | Implement `DataEnergisticsTrinityFixture` in `versions/1.21.1-neoforge/src/testDriver/java/.../testdriver`. Register `data-energistics-trinity-cpu` in `shared/src/testDriver1201/java/.../AddonCpuFixture.java`. Compute and check the rotated main/CPU/crafting union before placement. Reuse shared scenario state, adding native observations needed for the baseline. | Real formation, published CPU, worker job, dispatch/output/completion independent of profiler; owned-volume cleanup. TR-01. |
| 2 | Extend `TestDriverCoreTest` and target driver mixin/resources only for those observations. Update `scripts/ui-smoke-groups.json`, coverage metadata and the applicable suite selection without dropping the existing adaptive-provider scenario. | Registry, scenario selection, output identity and driver-only packaging. TR-01, TR-06. |
| 3 | Commit fixture phase through the normal hook and run the reproduction checkpoint. Retain baseline head, production/driver hashes, graph manifest, native observations, missing sample and screenshots. | Required pre-adapter gate. TR-01. |
| 4 | Add target-local optional mixins/accessors, the checked candidate in `shared/src/main/java/.../core/IntegrationCatalog.java`, and only necessary `compileOnly` configuration in `versions/1.21.1-neoforge/build.gradle`. Add pure accounting logic/tests and the smallest profiler withdrawal boundary if needed. | Submission/commit/return/finish ordering; counted, exact, dynamic and reusable accounting; no mutation of native crafting results. TR-02 through TR-06. |
| 5 | Reuse estimate recovery for Trinity job NBT; add the integration option and both scope mappings. Inspect all consumers of `OptionFeature`, options wire/config encoding and recovery invalidation before editing. Update `en_us.json` and `uk_ua.json` together. | Cancellation, replacement, partial return, load, option off/on, stale recovery, quantity boundaries and scope isolation. TR-04, TR-05, TR-07. |
| 6 | Extend `IntegrationCatalogTest`, `IntegrationSelectionTest`, `IntegrationBoundaryTest`, nearest profiler/recovery/options tests and driver boundary checks. | Every new pure branch/line covered; absent/wrong target/missing signature rejected; production artifact excludes driver/addon classes. TR-02 through TR-07. |
| 7 | Run focused runtime cases, inspect native UI and fix any required presentation boundary. | Clean current-head native Trinity, learned Plan/Status TTC, counted/reusable, cancellation/restart/capacity, absent/present regression. TR-08 and semantic criteria. |
| 8 | Update `docs/dependencies.md`, `docs/ae2-addon-integration/data-energistics.md`, `docs/mod-automation-coverage.md`, test-driver docs and this spec with the exact verified boundary/evidence. Update GuideME English/Ukrainian integration guidance and the corresponding GitHub wiki source through the existing documentation workflow. | Truthful claims, matching option documentation, exact evidence links; no universal Trinity claim. TR-06 through TR-08. |

The abbreviated Java paths above retain the repository's existing
`com/ctux/ae2craftingtime` package layout. Target-specific code stays on 1.21.1;
shared core/options changes must preserve every other loader's behavior.

## Verification cases

Pure tests must cover committed count greater than one; partial, zero, rejected
and simulated returns; repeated completion; two workers/networks with the same
output; cancelled pending suffix; reusable rollback and duplicate settlement;
returned tools; job replacement; and final-output/finish ordering. Cover zero,
negative and maximum conversion inputs, overflow in aggregated quantities, and
one-time fail-closed diagnostics. Include item/fluid normalization boundaries.
Use existing recovery tests for valid and mismatched identity, restart, disabled
integration and a late first output; extend only where Trinity adds behavior.

Use the exact pinned addon artifact and existing focused compatible graph with
NeoForge `21.1.251`, Java 21 and an 8 GiB maximized English client in CodexVM.
Environment preflight on 2026-10-19 verified a matching prepared `launch.json`,
Java 21 and working VM access; no new installation is required. The VM was
cleanly stopped after preflight. Recheck the manifest before launch.

After the implementation PR exists, use the prepared-smoke skill and its changed
selection workflow. Inspect selection reasons. The focused invocation shape is
`scripts/run-ui-smoke-matrix.ps1 -Target 1.21.1-neoforge -ProjectId 1565514 -Scenario data-energistics-trinity-cpu`.
Use the existing launcher/suite mechanism for lifecycle variants; confirm its
actual options before executing. Preserve the compatible Data graph's documented
Lightning Tech/Thunderbolt exclusions. No Prism or full release matrix is needed.

| Runtime case | Required observations |
| --- | --- |
| Fixture-only baseline | Native structure and worker identity; real dispatch, accepted output, completion and inventory delta; absent new server sample. |
| Clean Trinity craft | Native CPU selection and Plan; active/completed Status; native quantities equal profiler quantities; new sample and reopened learned TTC. |
| Counted/reusable | Form the crafting child and load vanilla `book_cloning` into its mounted pattern core: one generation-zero written book plus multiple writable books. Observe more than one committed logical craft and actual products, with the original retained/returned but excluded from production. Cancelled suffix and repeated settlement produce no phantom samples. Use the native route described in the design, not a custom recipe or fake host. |
| Lifecycle/capacity | Cancellation then fresh craft on the same worker; insufficient CPU storage and exhausted provider capacity; no samples from rejected work. |
| Restart/options | Save/reload an active worker and verify matching estimate recovery without fabricated timing; complete a fresh post-restart craft; disable/re-enable integration and verify stale work stays discarded. |
| Regression | `data-energistics-cpu` still learns through the native CPU/adaptive provider; addon-absent `craft-lifecycle` still passes. |

Batch related cases in the existing disposable-world suite where supported.
Keep builds on the host and launch sequentially. Record launch count and measured
wall time; use progress checkpoints for diagnosis, then one clean final run on
the immutable head. Retain logs, graph/artifact hashes, server observations,
screenshots/sidecars and individual manual reviews. Stop only the launched
client, then shut down the configured CodexVM and verify it is absent from the
VM list, including on failure.

Run required local/static/boundary checks after PR creation. GitHub's Gradle
tests/coverage and configured review workflow are separate current-head gates;
never substitute runtime evidence for them. Broad additional smoke is warranted
only if actual shared changes introduce another materially different path.

## Issue wording and completion

When delivering these docs, replace only the stale final deferred paragraph of
#740 with:

> Active follow-up to #518. That issue qualifies ordinary native AE2 CPUs with the Adaptive Pattern Provider; this issue will qualify Trinity CPU execution on NeoForge 1.21.1. Build and run the disposable native Trinity fixture before adding production hooks; retain the baseline evidence and document any deliberately unsupported mode.

Preserve the original report, source evidence and work items. No runtime support
claim changes until the corresponding checks pass. Mark the spec finished only
after the reviewed implementation and required evidence satisfy every criterion;
link the final tested head and merge evidence.
