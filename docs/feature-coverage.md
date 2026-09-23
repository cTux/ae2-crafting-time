# Feature Documentation

This page maps every shipped feature to the document that defines what it does
and how it works. A single document can serve as both the feature spec and the
technical design when it already covers player behavior, boundaries, data flow,
and verification.

Read each linked specification for its [scope status and evidence](documentation-status.md).
Shipped behavior can still have unfinished verification or separately planned additions.

## Shipped Features

| Feature | Spec and technical design |
| --- | --- |
| Learned throughput, retained samples, confidence, and outlier filtering | [Profiling and diagnostics spec](profiling-and-diagnostics/spec.md) and [technical design](profiling-and-diagnostics/technical-design.md) |
| Craft-plan and crafting-status row estimates and totals | [Time To Craft lines](time-to-craft-plan.md) |
| Remaining total TTC in every visible Crafting CPU card | [CPU-list total TTC spec](cpu-list-total-ttc/spec.md) and [technical design](cpu-list-total-ttc/technical-design.md) |
| Running-job total based on the remaining dependency critical path | [Time To Craft lines](time-to-craft-plan.md#estimate-formula) |
| Prediction-accuracy history | [Profiling and diagnostics spec](profiling-and-diagnostics/spec.md) and [technical design](profiling-and-diagnostics/technical-design.md#prediction-accuracy) |
| Delayed-output warnings and bottleneck hints | [Profiling and diagnostics spec](profiling-and-diagnostics/spec.md) and [technical design](profiling-and-diagnostics/technical-design.md#delayed-output-diagnostics) |
| Clickable delayed warnings that locate the provider | [Provider locate spec](provider-locate/spec.md) and [technical design](provider-locate/technical-design.md) |
| Network-scoped server snapshots, packet limits, and privacy boundaries | [Server-owned stats](server-client-stats.md) |
| World-save persistence | [World-save persistence](world-save-persistence.md) |
| Fast-to-slow TTC colors | [TTC colors](ttc-colored-text.md) |
| Craft-plan and crafting-status TTC sorting | [TTC sorting](ttc-sorting.md) |
| TTC badges, totals, tooltips, and screen layout | [Player controls and integrations spec](player-controls-and-integrations/spec.md) and [technical design](player-controls-and-integrations/technical-design.md#presentation) |
| Ctrl-click details and Ctrl-Alt-click reset | [Player controls and integrations spec](player-controls-and-integrations/spec.md#details-and-reset) and [technical design](player-controls-and-integrations/technical-design.md#details-and-reset-flow) |
| Common configuration | [Player controls and integrations spec](player-controls-and-integrations/spec.md#configuration) and [technical design](player-controls-and-integrations/technical-design.md#configuration) |
| AE2: Crafting Tree and ME Requester UI | [Player controls and integrations spec](player-controls-and-integrations/spec.md#optional-integrations) and [technical design](player-controls-and-integrations/technical-design.md#optional-ui-adapters) |
| Applied Mekanistics key support and AdvancedAE, NeoEco, and AE2 Lightning Tech CPU profiling | [AE2 addon integration](ae2-addon-integration/spec.md), [technical design](ae2-addon-integration/technical-design.md), and [player controls and integrations](player-controls-and-integrations/technical-design.md#other-addon-boundaries) |
| English and Ukrainian player text | [Player controls and integrations spec](player-controls-and-integrations/spec.md#compatibility-and-failure-rules) and [technical design](player-controls-and-integrations/technical-design.md#localization) |
| Craftable bilingual guide with an introduction and first-estimate chapter on every supported target | [Guide book spec](guideme-guide/spec.md), [1.20.1 extension](guideme-guide/1.20.1/spec.md), and [technical design](guideme-guide/1.20.1/technical-design.md) |
| Four supported Minecraft/loader targets | [Architecture](architecture.md#supported-targets) |
| Builds, development clients, release automation, automatic PR setup, CI, and review | [Building](building.md), [development client](dev-client.md), [release](release.md), [working with this project](working-with-project.md), and [repository workflow](../AGENTS.md) |

## Other feature and verification scopes

These links include proposals, implementation in progress, and completed verification
work. Their specifications own the current status; this index does not duplicate it.

- [AE2 Addon Integration Spec](ae2-addon-integration/spec.md)
- [AE2 Ponder Guides Spec](ae2-ponder-guides/spec.md)
- [AppliedE planner compatibility](appliede-planner-compatibility/spec.md)
- [Automated UI Testing Spec](automated-ui-testing/spec.md)
- [CodexVM MCP Evaluation Spec](codexvm-mcp-evaluation/spec.md)
- [CodexVM shared-folder recovery](codexvm-shared-folders/spec.md)
- [Collecting Data Status Spec](collecting-data-status/spec.md)
- [Compact crafting-status amounts](crafting-status-amounts/spec.md)
- [Completed Craft Total TTC Specification](completed-status-total-ttc/spec.md)
- [In-game Configuration Screen Specification](configuration-screen/spec.md)
- [Total TTC in the Crafting CPU list](cpu-list-total-ttc/spec.md)
- [Exact ingredient mismatch diagnostics](exact-ingredient-mismatch/spec.md)
- [Minecraft 1.20.1 Guide Book Specification](guideme-guide/1.20.1/spec.md)
- [Feature Chapter Specification](guideme-guide/feature-chapter/spec.md)
- [AE2 Crafting Time Guide Book Specification](guideme-guide/spec.md)
- [Status Chapter Specification](guideme-guide/status-chapter/spec.md)
- [Initial Crafting Status ETA](initial-status-eta/spec.md)
- [NeoForge Early-Display Config Smoke Spec](neoforge-early-display-config/spec.md)
- [NeoForge Suite Shutdown Spec](neoforge-suite-shutdown/spec.md)
- [No Power Status](no-power-status/spec.md)
- [No Provider Status](no-provider-status/spec.md)
- [No Space Status](no-space-status/spec.md)
- [Finite Outlier Configuration and TTC](nonfinite-outlier-multiplier/spec.md)
- [Player Controls And Integrations Spec](player-controls-and-integrations/spec.md)
- [Profiling And Diagnostics Spec](profiling-and-diagnostics/spec.md)
- [Project Infinity 0.1 Full UI Smoke Spec](project-infinity-0-1-smoke/spec.md)
- [NO CHANNEL status](provider-dispatch-statuses/no-channel/spec.md)
- [Provider dispatch statuses](provider-dispatch-statuses/spec.md)
- [Delayed resource icons](provider-locate/resource-icons/spec.md)
- [Provider Locate Spec](provider-locate/spec.md)
- [Recurrent ingredients in the crafting plan](recurrent-crafting-status/spec.md)
- [Screenshot UI Scale Spec](screenshot-ui-scale/spec.md)
- [Startup Integration Diagnostics Spec](startup-integration-diagnostics/spec.md)
- [Connected resource lifecycle fixtures](test-driver/connected-resource-fixtures/spec.md)
- [AE2 Crafting Time Test Driver Spec](test-driver/spec.md)
- [TTC sorting for active crafting orders](ttc-sorting/cpu-list/spec.md)
- [TTC Sorting Specification](ttc-sorting/spec.md)
- [Vortex Spec](vortex/spec.md)
- [Waiting To Start Spec](waiting-to-start/spec.md)
- [CPU-bound craft-time stats](cpu-bound-stats/index.md)
