# AppliedE investigation and correction gates

Deliver the [spec](spec.md) from the verified [design](technical-design.md).
This plan first restores a controlled reproduction. It does not authorize a
speculative production fix or treat startup repair as issue completion.

## Ordered work

| Order | Work and check | Criteria |
|---|---|---|
| 1 | Merge the reviewed documentation-only change. Preserve the claim and keep the issue open. | All |
| 2 | Recheck CodexVM, SSH, Java 17, Prism's Codex group, exact managed pack metadata, fixture preparation and archive access. Resolve the missing exact dependency artifacts and compare their hashes to the design and full archived graph. | AP-01, AP-07 |
| 3 | Stage one new marked guest-local copy through the named-modpack workflow. Build current Forge production/driver JARs on the host after repository PR ordering permits checks, record their SHA/hash, and verify the complete staged graph and configuration. | AP-01 |
| 4 | Establish bounded world entry and a feasible read-only trace method. Diagnose the existing STARTING timeout from loading/world/driver evidence before choosing a lifecycle or deadline change. | AP-02, AP-03 |
| 5 | Run the focused enabled/disabled comparison described below with fresh calculations and native safety intact. Retain actual field values and result boundaries. | AP-03, AP-04, AP-07 |
| 6 | Review causation and correction ownership. Amend and merge these three documents with the proven correction and exact regression/verification plan before production implementation. | AP-05 |
| 7 | Implement and verify only that documented correction, including exact requested output/EMC accounting and existing AppliedE profiling checks. | AP-06, AP-07 |

Ordered-work item 4 is a prerequisite gate. If existing facilities cannot provide usable world
entry or AP-03 traces, stop dependent execution and document the smallest concrete
in-repository prerequisite, its callers, branch coverage and runtime proof before
changing code. Obtain scope authorization for substantial new infrastructure or
third-party changes. Do not run the old staging script against its stale worktree
or locally built dependency references.

## Exact-pack comparison

Use Minecraft 1.20.1 / Forge 47.4.20 / Java 17 and the reported Project Infinity
0.0.52.0 graph, with scenario `appliede-cpu`. Reuse its module, knowledge, EMC and
native CPU fixture. Prepared `compatible` or `latest` clients do not substitute
for this graph. The result's `compatible` profile string is not pack provenance.

Follow the named-modpack and VM skills: host-only builds, guest-local NTFS,
marked disposable world, an 8 GiB client, maximized window, `guiScale:0`, and
`pauseOnLostFocus:false`. Preserve the managed source. Validate absolute
forward-slash filesystem properties in Prism before launch and verify the exact
active world and fixture marker before actions.

For each control, record:

- the native request identity, output key and requested amount;
- available pattern class, definition and full output keys/amounts;
- node identity, quantity at request entry, quantity used to rebuild the pattern,
  first child-build order and whether later calls reuse the same processes;
- effective automatic-AELIS and long-range settings, native-safety application,
  selected calculation path, outcome and exception boundary;
- accepted amount, furnace return, EMC before/after, job completion, profile
  sample and TTC when the request reaches crafting.

Run automatic AELIS enabled first, then disabled in a fresh calculation after
restarting from equivalent pristine fixture/player/profiler state. Preserve every
other mod and gameplay setting; use the same harness settings in both controls.
If loading or world entry fails, label that attempt as a
prerequisite failure; no recipe verdict follows. Toggling AELIS without recording
quantity/build ordering may isolate a dependency interaction but cannot prove
the specific cached-zero hypothesis. Use a further focused causal control only
when the observed trace identifies it; document any needed code change first.

## Time and evidence

Initially budget two focused pack launches and 45 minutes per attempt, including
startup, one scenario, evidence capture and cleanup. These are planning ceilings,
not permission to extend the driver's existing deadline. No reliable isolated
cold-start measurement is available: the original archived 16m26s process also
ran twenty preceding scenarios. Measure actual phase times on the first attempt.

Inspect progress at least once per minute. When an existing watchdog expires or
loading/world progress stops, retain logs and the exact screen, diagnose the last
completed checkpoint, then request normal quit of only the identified client.
Do not spend another full launch replaying proven phases without a new hypothesis.
A resumed or instrumented attempt is diagnostic evidence; final correction proof
requires a clean focused run with the documented released dependencies.

Use the [archive and timing contract](../ui-smoke-evidence.md). Keep inventory,
configuration, production/driver identities, trace events, original result files,
screenshots/sidecars, process exit and per-attempt timings. Review required visual
evidence without presenting a screenshot as proof of server state. Exclude account
data and private machine/server paths from published documents and comments.

## Checks and completion

Before the documentation PR, review links, referenced paths, hashes and criterion
coverage; do not run repository tests. Documentation needs static validation, not
new executable tests. After the hook creates the PR, inspect its applicable CI.

Any later executable prerequisite needs the existing focused regression and
unchanged 100% line/branch coverage requirements. Trace all shared callers and
the separate 26.1.2 implementation before choosing checks. A shared startup-policy
change must account for Forge/Fabric 1.20.1 and NeoForge 1.21.1 consumers; it must
not silently extend standard/status deadlines or the independent 26.1.2 policy.
Name exact compile targets and scenarios in the amendment, then run those checks
only after its implementation PR exists. No full release matrix is implied.

The investigation completes when AP-01 through AP-05 have retained evidence and
a reviewed correction decision. If causation remains unresolved, report the exact
missing evidence and retain an open issue. Closing #420 additionally requires the
documented correction, current-head checks, AP-06 functional proof and AP-07
readback; a successful startup or unverified workaround is insufficient.
