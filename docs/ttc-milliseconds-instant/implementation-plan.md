# Millisecond TTC implementation plan

See the [canonical status and requirements](spec.md) and [design](technical-design.md).
This plan does not authorize implementation or remove the issue's backlog hold.

1. Resolve the fractional-ms rounding and Instant option default/label/group
   decisions listed in the spec. Review the issue and all three
   documents together, then mark the scope ready-to-implement. Keep backlog
   until the user authorizes implementation.
2. Trace every `TimeEstimate` caller, total/CPU duration field and packet unit.
   Replace TTC carriers with the design's optional finite-double millisecond
   estimate plus reliability, including sum/critical-path calculations, caches,
   sorting/colors, both codecs and four loader adapters. Apply the documented
   protocol/ID version changes and capability gates without clamping source data.
   Carry graph completeness from dependency construction, including ambiguous
   crafted-input choices. Test aggregate confidence and unknown/overflow handling. Cover MS-1 through
   MS-5, MS-9 and MS-11 in formatter, total, CPU and packet tests.
   Migrate accuracy predicted-second fields to double at the explicit
   `durationMs / 1000.0` boundary, including tracker records, snapshots/codecs
   and latest-job display. Verify subsecond recording, ratios, signed error and
   packet round trips with the design's 50ms/100ms case.
3. Update tooltip and chat tick-unit translations together, including normalized
   average/latest fields and sample lists. Retain full precision in calculations
   and existing output-unit normalization. Cover MS-2, MS-3 and MS-6.
4. Add the Instant status with the plain 🥇 emoji through the existing text/symbol
   path at existing presentation boundaries, after higher-priority
   states and only for confident estimates. Add the client option, defaults,
   persistence/reset behavior, English/Ukrainian strings and Show emoji behavior.
   Replace the server-selected chat estimate with the design's semantic TTC
   component using invariant string metadata and client-local selection/refresh.
   Round-trip it through each target's vanilla component serializer before
   rendering in tests. Cover MS-4 through MS-7 and
   MS-10 with component, row, configuration and two-recipient chat tests.
5. Update GuideME and the GitHub wiki with ms examples, the 5ms floor, confidence
   precedence and the Instant control. Reconcile the older profiling examples,
   compact-hover boundary and symbol palette linked from the spec.
6. Follow repository commit/PR ordering: create the implementation PR through the
   hook before running local checks. Run relevant core, component, chat, renderer,
   configuration and locale checks; build all four supported targets. Record
   GitHub CI separately. Capture focused native/addon client evidence for small
   numeric times, Instant, low confidence, option toggles, and narrow layouts.

Completion requires MS-1 through MS-11, reviewed implementation, passing required
checks, verified readable medal/status rendering, and updated player guides.
A merged documentation PR alone does not satisfy this gate.
