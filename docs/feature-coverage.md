# Feature Documentation

This page maps every shipped feature to the document that defines what it does
and how it works. A single document can serve as both the feature spec and the
technical design when it already covers player behavior, boundaries, data flow,
and verification.

## Shipped Features

| Feature | Spec and technical design |
| --- | --- |
| Learned throughput, retained samples, confidence, and outlier filtering | [Profiling and diagnostics spec](profiling-and-diagnostics/spec.md) and [technical design](profiling-and-diagnostics/technical-design.md) |
| Craft-plan and crafting-status row estimates and totals | [Time To Craft lines](time-to-craft-plan.md) |
| Running-job total based on elapsed progress | [Time To Craft lines](time-to-craft-plan.md#estimate-formula) |
| Prediction-accuracy history | [Profiling and diagnostics spec](profiling-and-diagnostics/spec.md) and [technical design](profiling-and-diagnostics/technical-design.md#prediction-accuracy) |
| Delayed-output warnings and bottleneck hints | [Profiling and diagnostics spec](profiling-and-diagnostics/spec.md) and [technical design](profiling-and-diagnostics/technical-design.md#delayed-output-diagnostics) |
| Network-scoped server snapshots, packet limits, and privacy boundaries | [Server-owned stats](server-client-stats.md) |
| World-save persistence | [World-save persistence](world-save-persistence.md) |
| Fast-to-slow TTC colors | [TTC colors](ttc-colored-text.md) |
| Craft-plan and crafting-status TTC sorting | [TTC sorting](ttc-sorting.md) |
| TTC badges, totals, tooltips, and screen layout | [Player controls and integrations spec](player-controls-and-integrations/spec.md) and [technical design](player-controls-and-integrations/technical-design.md#presentation) |
| Ctrl-click details and Ctrl-Alt-click reset | [Player controls and integrations spec](player-controls-and-integrations/spec.md#details-and-reset) and [technical design](player-controls-and-integrations/technical-design.md#details-and-reset-flow) |
| Common configuration | [Player controls and integrations spec](player-controls-and-integrations/spec.md#configuration) and [technical design](player-controls-and-integrations/technical-design.md#configuration) |
| AE2: Crafting Tree and ME Requester UI | [Player controls and integrations spec](player-controls-and-integrations/spec.md#optional-integrations) and [technical design](player-controls-and-integrations/technical-design.md#optional-ui-adapters) |
| Applied Mekanistics key support and AdvancedAE CPU profiling | [AE2 addon integration](ae2-addon-integration/spec.md), [technical design](ae2-addon-integration/technical-design.md), and [player controls and integrations](player-controls-and-integrations/technical-design.md#other-addon-boundaries) |
| English and Ukrainian player text | [Player controls and integrations spec](player-controls-and-integrations/spec.md#compatibility-and-failure-rules) and [technical design](player-controls-and-integrations/technical-design.md#localization) |
| Four supported Minecraft/loader targets | [Architecture](architecture.md#supported-targets) |
| Builds, development clients, release automation, automatic PR setup, CI, and review | [Building](building.md), [development client](dev-client.md), [release](release.md), [working with this project](working-with-project.md), and [repository workflow](../AGENTS.md) |

## Planned Features

These documents are proposals, not claims about the current JARs:

- [Collecting Data status](collecting-data-status/spec.md)
- [Waiting To Start status](waiting-to-start/spec.md)
- [CPU-bound craft-time stats](cpu-bound-stats/index.md)
- the unimplemented addon-CPU layers described by the
  [AE2 addon integration plan](ae2-addon-integration/implementation-plan.md)
