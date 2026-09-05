# Startup Integration Diagnostics Implementation Plan

Implement [issue #193](https://github.com/cTux/ae2-crafting-time/issues/193) from the
[specification](spec.md) and [technical design](technical-design.md). This document
retains the implementation sequence. Completed checks and remaining limits are
recorded in [runtime evidence](../ui-smoke-evidence.md#startup-integration-diagnostics-2026-09-05).

## 1. Fixed reporting model and inventory

Create `shared/src/main/java/com/ctux/ae2craftingtime/core/IntegrationDiagnostics.java`
and focused tests under the existing `shared/src/test/java/.../core` package.
Use the 26 integration rows from the design, fixed capability IDs, target masks,
physical/logical side rules, and alternative mod-ID lookup. Represent native
support as shared-hook dependencies; do not copy the client dependency graph.

Implement the exact aggregate precedence, finite transitions, one-time summary,
thread-safe observation, metadata sanitization, and bounded output rules. Include
`phase`, `integration`, `mod`, `version`, `capability`, `state`, `mode`, `reason`,
and failure `action` in stable log fields. Optional summary counts exclude core.

Before marking the inventory complete, compare every row and alias with release
metadata, packaged mixin lists, existing fixtures, and the dependency table.
Assert specifically that Forge AdvancedAE is a shipped adapter, NeoEco/Lightning
Tech are not supported merely because the pre-26 config or run profile mentions
them, and original/fork aliases are not double-counted. Confirm the installed
ExtendedAE metadata ID rather than using its display name as a lookup key.

Tests cover all five aggregates; partial with pending and confirmed siblings;
no-applicable-capability skips; absent/config/side/target precedence; aliases;
unknown versions; control characters and length limits; duplicate/concurrent
callbacks; confirmation followed by failure; terminal disable; config re-enable;
one summary; and no repeated records after a simulated world change. Keep the
repository's 100% shared line/branch coverage gate intact.

## 2. Loader context and registration evidence

Add `IntegrationLog` in `shared/src/mcCommon/java/.../mc1201`. Keep the state model
independent of SLF4J and pass only metadata values into it. Bind actual runtime
metadata in all four `Ae2CraftingTime` entrypoints. Report context before setup
and `phase=entrypoint_checks` once after available common registration work.

Instrument successful returns of config/network registration, deferred NeoForge
payload registration, and each target's existing client key-registration method.
Pending event work stays pending. Report required setup failures with their cause
and propagate; do not add a top-level exception net. Do not move world-data loading
or infer successful persistence from registering a server-start callback.

Boundary checks verify actual metadata conversion, listener-versus-registration
distinction, no premature client imports on dedicated servers, no lookup of absent
optional classes, and independent process-local reports. Fabric client networking
and NeoForge event-driven networking must both appear in their own confirmations.

## 3. Observed hook capabilities

Add one-time observations at existing successful operation boundaries:

- `mcCommon` core CPU, plan/status row and tooltip mixins;
- both `mc1201` and `mc2612` plan/status screen total, sort, and control routes;
- AdvancedAE's shared mixin, including Forge's extra configuration;
- NeoEco's normal/FastPath dispatch and other CPU events;
- Lightning Tech's CPU events and successful capacity reflection;
- both Crafting Tree variants and ME Requester;
- both wireless tooltip variants, attributed to matching installed IDs;
- shared `AeKeyAmounts` and `StatsRequestContext`.

Preserve original method results, redirects, cancellation and event order. Do not
inject extra callbacks just to make an unused hook pass. Keep every `require`,
shadow, mixin side, config required flag, and refmap unchanged. A success marker
belongs after the existing operation, not at method entry or after mod detection.
For native integrations, report only the confirmed shared-hook scope and retain
`addon_job_not_verified`; no generic callback proves a particular addon's job.

Exercise each capability independently in tests/scenarios: firing a render hook
must leave tooltip/click pending, and a successful dispatch must leave completion
pending. Include no-data/disabled-config/empty-screen paths. Audit the hot-path
code so repeated callbacks do no repeated formatting, allocation of diagnostic
records, mod-list traversal, or logging after confirmation.

## 4. Recover only at proven read-only boundaries

Replace silent reflection failures in `CraftingTreeTtc`, both widget callers,
ME Requester's reads, and AdvancedAE's optional selected-CPU lookup with the
design's explicit failure/result distinction. Put Java-only reflection resolution,
result validation, and failure classification in covered shared code; keep GUI
changes as thin adapters. Reuse the existing helper rather than create a general
reflection framework. All read callers must preserve valid null/no-data behavior.

Disable only the capability groups listed in the design. Stage reads and results
before rendering/tooltip/click mutations. Verify old-tree spacing restoration,
cache clearing, original tooltip invocation exactly once, no failed SHOW/RESET
dispatch, no host-click cancellation, and atomic omission of the requester overlay.
The AdvancedAE fallback retains the grid and returns null selected CPU.

Use plain Java fixtures with missing/private/inaccessible members, wrong return
types, null results, incompatible/ambiguous method arguments, an invoked nonfatal
exception, and an invoked `Error`. Assert that the first unexpected failure retains
its cause and later calls skip reflection. Include the field-lookup fallback that
must succeed without warning. Do not alter Lightning Tech's stateful capacity
failure, CPU exception handling, save data, or normal AE2 profiling.

## 5. Cross-target runtime evidence

Use the prepared-client smoke workflow and the existing Forge test driver; extend
its scenarios/log assertions where needed. For targets without that driver, use
packaged manual runs and retain logs/screenshots with an explicit manual label.
Do not copy the entire driver to new loaders just for logging verification.
Use isolated disposable worlds/profiles for intentionally incompatible fixtures.

| Campaign on each of F, B, N, X | Required evidence |
| --- | --- |
| Core-only client and dedicated server | Eight startup logs total: correct context, complete inventory, INFO-only expected absence, no optional/client class-loading regression. |
| Compatible installed set, client plus applicable server mods | Actual mod versions; correct target/side skips; pending at startup; common registrations confirmed; payload/key events confirmed only when executed. Client-only addons need not be installed server-side. |
| Deferred activity | Normal AE2 craft, plan/status UI, and every available supported custom adapter/UI path. Capture first capability transitions, reopen screens, run a second job, and re-enter a world to prove no duplicates. Verify absence of remote-server claims in a multiplayer client. |
| Recoverable contract failure | Deliberately incompatible read-only fixture on each build that owns the boundary: tree/requester on F/B/N, AdvancedAE selected-CPU lookup on F/N/X. Confirm one WARN, exact lost capability, original screen/grid behavior, and successful core crafting afterward. |
| Unrecoverable failure | On each loader family, a disposable required-hook or loader-dependency mismatch must retain its original diagnostic and fail the run; on both NeoForge versions check packaged startup separately. Never lower injection requirements to make this case continue. |

Runtime evidence must name exact fixture/artifact versions and target, expected
outcome, actual excerpt, and pass/fail. Test a real transformed target for the
incompatible reflection case; a pure unit fixture alone is insufficient smoke
proof. Keep fixtures in test/development sources and verify they are absent from
production JARs. No user-facing fault-injection setting is added.

Unpinned integrations remain declared with explicitly unverified runtime evidence;
do not fabricate a pass or silently drop their report row. A pending hook that was
never exercised is correct runtime behavior but not a completed positive test.
If an applicable artifact cannot be obtained, record the blocked matrix cell and
leave implementation verification incomplete.

## 6. Review, delivery, and completion

Follow `AGENTS.md`: one feature branch and conventional commit; the post-commit
hook creates/updates the PR before local repository tests. During implementation,
follow the development, prepared-client smoke, and test-driver skills for their
respective checks. For this documentation-only planning PR, check Markdown links,
paths, requirement consistency, and `git diff --check`; do not run gameplay tests.

For the implementation PR, inspect GitHub's `test jacocoTestReport` result and
retain the 100% shared coverage gate. After the PR exists, run required boundary
checks and a four-target packaged build through `scripts/build-all-versions.ps1`.
Complete the runtime matrix above. Report GitHub CI separately from local checks;
never label pending CI as passed. No merge or release is authorized by this plan.

Update the existing player-controls technical design's stale AdvancedAE/wireless
descriptions and link this feature from the architecture/dependency docs when
behavior ships. Update dependency documentation for the precise diagnostic scope,
without raising minima or promoting development candidates. Record actual smoke
evidence in the existing coverage/evidence documents, not in invented success logs.

| Acceptance | Implementation and verification ownership |
| --- | --- |
| AC-01 | Steps 1-2: metadata, counts, lifecycle tests; step 5's eight baseline starts. |
| AC-02 | Steps 1-2: target/side/presence checks; step 5 core-only packaged runs. |
| AC-03 | Step 3: independent real callback evidence and correct pending states; step 5 deferred activity. |
| AC-04 | Steps 1 and 3: inventory/alias/package checks and shared-hook scope wording. |
| AC-05 | Step 4: reflection and UI rollback tests; step 5 incompatible UI and subsequent successful craft. |
| AC-06 | Step 4: AdvancedAE null-CPU/grid boundary; step 5 dedicated/integrated fallback evidence. |
| AC-07 | Steps 2 and 4: cause propagation; step 5 original loader/Mixin failure excerpts. |
| AC-08 | Steps 1 and 3: finite transitions and repeated-callback tests; step 5 reopen/world-change logs. |
| AC-09 | All steps: coverage, packaged target matrix, actual logs and honest blocked cells. |

Done means every acceptance check has evidence, all applicable matrix cells have
passed, every first failure has an actionable cause and disposition, no diagnostic
claims more than was observed, no absent optional class was loaded by diagnostics,
and no recovery changes authoritative samples or host behavior. Research and these
documents alone do not meet the implementation completion gate.
