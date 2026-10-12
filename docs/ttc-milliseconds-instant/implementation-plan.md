# Millisecond TTC implementation plan

See the [canonical status and requirements](spec.md) and [design](technical-design.md).
This plan does not authorize implementation or remove the issue's backlog hold.

1. Resolve the fractional-ms rounding and Instant option default/label
   decisions listed in the spec. Review the issue and all three
   documents together, then mark the scope ready-to-implement. Keep backlog
   until the user authorizes implementation.
2. Trace every `TimeEstimate` caller, total/CPU duration field and packet unit.
   Record where whole-second rounding occurs. Change the shared presentation
   path and any necessary precision carriers without clamping source data.
   Cover MS-1 through MS-5 in formatter and total tests.
3. Update tooltip and chat tick-unit translations together, including normalized
   average/latest fields and sample lists. Retain full precision in calculations
   and existing output-unit normalization. Cover MS-2, MS-3 and MS-6.
4. Add the Instant status with the plain 🥇 emoji through the existing text/symbol
   path at existing presentation boundaries, after higher-priority
   states and only for confident estimates. Add the client option, defaults,
   persistence/reset behavior, English/Ukrainian strings and Show emoji behavior.
   Cover MS-4 through MS-7 with component, row and configuration tests.
5. Update GuideME and the GitHub wiki with ms examples, the 5ms floor, confidence
   precedence and the Instant control. Reconcile the older profiling examples,
   compact-hover boundary and symbol palette linked from the spec.
6. Follow repository commit/PR ordering: create the implementation PR through the
   hook before running local checks. Run relevant core, component, chat, renderer,
   configuration and locale checks; build all four supported targets. Record
   GitHub CI separately. Capture focused native/addon client evidence for small
   numeric times, Instant, low confidence, option toggles, and narrow layouts.

Completion requires MS-1 through MS-8, reviewed implementation, passing required
checks, verified readable medal/status rendering, and updated player guides.
A merged documentation PR alone does not satisfy this gate.
