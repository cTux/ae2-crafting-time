# Documentation conformance audit — 2026-10-18

Scope: Reconcile the ten documentation mismatches found by this run of [#372](https://github.com/cTux/ae2-crafting-time/issues/372).

Audited revision: [`2a4726ecf05cdd990ee68bc0363b5b79ded71ab9`](https://github.com/cTux/ae2-crafting-time/tree/2a4726ecf05cdd990ee68bc0363b5b79ded71ab9).

Planning: [Evidence and design](technical-design.md), [implementation plan](implementation-plan.md).

The static audit found reference prose that disagrees with newer requirements
and current code. Correct those references; no executable defect was established
and no behavior change is included. Delivery, review, CI and merge evidence belong
in the #372 per-run comment. This dated audit report is not a new runtime
qualification campaign and carries no mutable feature-completion status.

## Acceptance

| ID | Required correction |
| --- | --- |
| D1 | Current references describe completion intervals, same-tick aggregation and the raw interval delay baseline, not completed whole-order windows. |
| D2 | The development code map describes remaining dependency critical-path TTC and partial confidence. |
| D3 | Persistence references distinguish live trackers from remembered status fallback, explain incremental updates and configured retention, and cover all four loader adapters. |
| D4 | The network overview describes full-list TTC collection, priority requests, merged CPU entries and mode-dependent expiry. |
| D5 | Current wire documentation matches Forge 29, both NeoForge registrars 28 and Fabric row channels v5/v14. Historical protocol transitions stay historical. |
| D6 | Optional-addon absence does not imply absence of native AE2 Plan/Status TTC. |
| D7 | Configuration guidance distinguishes live Done from external-file reload, includes current default-Off exceptions, and preserves the badge background default. Historical tooltip evidence stays scoped to its baseline. |
| D8 | Source-layer and integration references include Forge's shared AdvancedAE mixin subset and its supported targets. |
| D9 | Resource fixture documentation distinguishes implemented production mode from still-incomplete production/visual qualification. |
| D10 | Contributor guidance consistently requires PR creation before local tests and permits applicable post-PR checks. |
| D11 | The change contains only documentation; local links and source claims are checked, current-head CI is reported separately, and historical/deferred scopes are not declared complete. |

## Boundaries

All four release targets are included in the static audit: Forge/Fabric 1.20.1,
NeoForge 1.21.1 and NeoForge 26.1.2. Do not change runtime code, configuration
values, packets, saves, metadata, dependency pins, translations or release state.
Do not promote an unfinished feature because its implementation exists.

The audit inspected server profiling and lifecycle, persistence, row/CPU request
boundaries, UI state/formatting, configuration, optional adapters and peers,
build/release/CI wiring, and test-driver automation. It did not execute clients,
benchmarks, a release or external wiki checks, and does not prove every runtime
path conforms. The [recurring procedure](../audits/implementation-conformance.md)
still governs the final report and leaving #372 open.
