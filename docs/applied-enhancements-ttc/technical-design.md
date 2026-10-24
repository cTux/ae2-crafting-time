# Implementation design

Scope and acceptance: [spec](spec.md). Upstream evidence: [research](research.md).

Use an optional `aelis-plan-114` IntegrationCatalog candidate for the Forge client.
Check the screen, row record, layout and column record methods/fields before any
hooks apply. String-target mixins avoid compile/runtime addon dependencies.
Gate the paired width/reservation/render hooks after bytecode inspection, and use
require=0 presentation injections so drift keeps the upstream screen usable.

Reserve 70 scaled pixels by widening the existing panel's cap from 360 to 430.
Only widen when the viewport can preserve the upstream 300-pixel minimum table
plus the TTC column and margins. Shift the existing column right boundary left
by 70 inside widened layouts; original quantities and name clipping remain
coherent. Upstream table and scrollbar use the resulting layout. Draw the TTC
column's row backgrounds and text in the reserved strip, and the total below the
storage summary. Tree mode receives neither extra column nor misleading total.

Read row key/craft/missing/cyclic quantities through existing IntegrationRead.
Reuse ClientStatsRequests with visible priority for drawn rows and background
priority for whole-plan totals. The existing menu/screen/connection context owns
invalidation. ProfileAmounts units remain authoritative. Normalize BigInteger
quantities exactly before checking long range; no narrowing or intermediate
long multiplication overflow. Unsupported totals are partial, never silently
clamped. Shared AelisPlanTtc owns layout decisions, normalization and aggregation.

Use existing options, text, color and badge helpers. Extend the existing driver
with a focused aelis-plan leaf, StandardCraftFixture and real enabled-AELIS menu.
Keep pure arithmetic/geometry tests in shared; ASM boundaries prove contracts
and optional hook shape. Runtime screenshots and original/native view controls
prove presentation; they do not establish arbitrary planner compatibility.
