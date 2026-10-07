# Missing UI in Applied Journey

Tracking: [#534](https://github.com/cTux/ae2-crafting-time/issues/534).

This investigation has confirmed a composition obstruction in the resumed
Applied Journey graph's Plan description and tooltip methods. The user's
original UI report and full-pack acceptance remain open; the finding does not
establish that one addon explains every missing row or every graph. Data
Energistics and OmniSequence: Transfinite are not the early cancellation shown
by the transformed Plan methods.

## Reproduction boundary

The historical export identifies Applied Journey 0.0.1, Minecraft 1.21.1,
NeoForge 21.1.249, and 200 manifest entries. Its relevant CurseForge IDs are:

| Mod | Project | File |
| --- | ---: | ---: |
| AE2 Crafting Time | 1591476 | 8955379 |
| AE2 | 223794 | 7027323 |
| Data Energistics | 1565514 | 8841396 |
| OmniSequence: Transfinite | 1624558 | 8941493 |

The export contains no runtime logs or crash reports. Obtain the pack through
an authorized source; the unpublished archive is not part of this repository.
Keep private server details and account information out of retained evidence.

The historical archive's SHA-256 is
`015d222a3577cfd8f746812db1fc3e734d530fc893d7111d89a21605802bb344`.
It also bundles `overrides/mods/ae2_extended-1.0-SNAPSHOT.jar`, outside the
200 manifest downloads. Its metadata identifies `ae2_extended` version
`1.0-SNAPSHOT`; its required mixins include `MixinCraftingCPUCluster` and
storage/network hooks. Include it in the exact graph. This observation does
not establish a conflict.

The resumed investigation uses a newer user-supplied export of Applied Journey
0.0.1 on Minecraft 1.21.1, NeoForge 21.1.252, with 197 manifest entries.
Its SHA-256 is
`073c2a514a6a52f58d8aed734278cbeb99762a0d77104e58108b54739c23063f`.
Its relevant CurseForge file IDs are Crafting Time `9016846`, AE2 `8992605`,
Data Energistics `8841396`, and OmniSequence `9043224`, under the same projects
listed above. It retains the bundled `ae2_extended` override.

Installed JAR metadata identifies Crafting Time 1.2.12, AE2 19.2.18, Data
Energistics 3.2.2, and OmniSequence 2.0.7 (`molecularmanipulator`). The installed
1.2.12 artifact is the resumed baseline, not the historical 1.2.9 artifact.
The user reports entering a world in this newer graph; the available log shows
a normal shutdown. This does not verify the reported crafting UI behavior.

Apply the full-pack criteria to this newer graph while retaining the historical
archive boundary. A pass here does not reproduce or explain the historical
report. If the old archive or artifact is unavailable, record that comparison
as blocked; do not substitute the newer graph for historical evidence.

## Confirmed Plan composition obstruction

On the resumed graph, the transformed `CraftConfirmTableRenderer` bytecode
shows AppliedEnhancements 1.1.0 cancelling `getEntryDescription` after it
builds the vanilla and addon lines, before the later Data Energistics return
modifier, Crafting Time return injection, and test-driver observer. The
transformed `getEntryTooltip` has the same ordering. This is a confirmed reason
those later return handlers do not see these Plan results; it is not evidence
that AppliedEnhancements alone explains every missing UI element.

Thunderbolt's conditional HEAD handler can return even earlier for entries
with ExactPlanReports amounts. The retained export does not establish which
live entries take that branch. Data Energistics runs after the observed
AppliedEnhancements cancellation, so a Data replacement is not the cause of
this observed obstruction. The earlier Data hypothesis is superseded by the
transformed-method evidence.

With OC2 off, both the installed Crafting Time 1.2.12 baseline and the
current-source artifact reached Plan. The footer displayed an estimate of
about seven seconds while TTC row descriptions were absent. `0/3` was the
smoke case-pass count, not a footer count or elapsed-time measurement. The
separate OC2 startup hypothesis remains unconfirmed; this renderer result does
not establish whether OC2 caused any startup failure.

In a controlled standard-plan-controls comparison, removing AppliedEnhancements
1.1.0 together with its required OmniSequence 2.0.7 dependency passed the
tested Plan controls. That comparison alone does not identify which member
caused its result; bytecode separately confirms AppliedEnhancements' early
Plan return cancellation. It does not pass full-pack acceptance. Case two
disconnected in the pair-off runtime when Thunderbolt's
`CPUSelectionListStorageMixin` could not inject at `formatStorage`. Stock AE2
19.2.18 lacks that method, and the removed mods do not supply it. This separate
target mismatch does not explain the Plan row descriptions; full-graph CPU
class loading was not tested, and case three did not run. The exact full graph,
historical graph, and remaining comparisons are blocked or unrun, so none has
a pass result. Thunderbolt may also cancel the Plan method earlier for entries
with ExactPlanReports amounts; that per-entry branch remains unknown.

The issue's [acceptance criteria](spec.md#applied-journey-investigation),
[design](technical-design.md#applied-journey-investigation), and
[execution plan](implementation-plan.md#applied-journey-investigation) also
cover the requested full pack smoke.

## Investigation sequence

1. Reproduce in the exact pack on a normal AE2 CPU. Record the screen, expected
   and missing elements, actual loaded versions, configuration, logs, and image.
   Distinguish absent row text from missing history or disabled profiling.
2. Compare AE2 plus Crafting Time alone, then Data Energistics, then
   OmniSequence with its required dependencies, then both addons, then the pack.
   Keep the recipe, CPU, options, and screen consistent between comparisons.
3. Trace the transformed Plan description and tooltip order using the retained
   class and disassembly evidence identified in the technical design. Compare
   the first return modifier, return cancellation, Crafting Time decoration,
   and driver observation. Check the Thunderbolt ExactPlanReports condition per row. Then
   inspect the shared Plan/Status hooks, `TtcText` rendering path, options,
   snapshots, and `IntegrationCatalog`/`IntegrationSelection` diagnostics;
   startup history alone does not prove an addon conflict.
   Record the negotiated Crafting Time channel and client/server options:
   an unavailable channel or disabled server profiling intentionally hides UI.
4. Follow the reviewed [technical design](technical-design.md#applied-journey-investigation)
   for the proposed shared Plan/Status wrapper and final-result driver
   observation. Do not implement before the plan is approved or add an addon
   adapter merely because an addon appears in the pack.

## Full pack smoke

Install the supplied export through Prism into its **Codex** group, then stage
a disposable guest-local copy in CodexVM. Preserve the original exported mod
graph and configuration for the historical baseline; record the one Crafting
Time artifact replacement for current-source verification. Inventory enabled
JAR metadata, nested mods, file hashes and overrides before selecting cases.

Use `prepare-ui-smoke-suite.ps1` and its `standard-ae2` expansion, plus every
applicable installed integration and general status case from the NeoForge
suite. Record each selected case and every absent or unsupported case with its
reason. Do not add mods just to satisfy the prepared-client suite. Run one
shared-world suite per installed graph, restoring fixture, player, profiler
and client cache state between cases. Review screenshots as well as assertions;
startup alone cannot satisfy this request.

Keep controlled addon comparisons separate from the unchanged full graph.
Record any temporary diagnostic mod/config change and restore the exact graph
for the final full run. A failing dependency graph or VM prerequisite is a
reported blocker, not permission to call a reduced graph a full-pack pass.

## Completion evidence

### Continuation after partial renderer delivery

[PR #669](https://github.com/cTux/ae2-crafting-time/pull/669) merged the renderer
composition correction as `dd535d61c0e3fa82a100acc883fbf9e4df5270f3`, with the
same source tree as tested `17d7561e30fa3945dfd9eec4c5d226fc6721edf2`.
Five controlled Plan graphs passed. See the [canonical AJ-04 status and
acceptance boundary](spec.md#remaining-prerequisite-qualification).
Core Status controls passed without its separate persistence check, Data Status
hit its required missing `formatStorage` target, and Omni/Both Status remain
unverified. A disposable full graph with the one-class Thunderbolt DEV correction
passed Plan, then its Status startup crashed in Architectury event dispatch.

Qualify official Data 3.3.4 and the retained upstream-derived Thunderbolt DEV
separately, following the [continuation gates](implementation-plan.md#applied-journey-continuation-gates).
Prove the Architectury collection mechanism deterministically before reviewing
any disposable correction; the listener-list writer remains unknown. Use
existing fixtures for ordinary native CPU rows, tooltips, controls and settings
persistence. Inspect finite/infinite Data and Thunderbolt formatting handlers
statically; native Trinity row, tooltip, hit-test and crafting behavior remain
unverified without a prepared fixture. Retain every replacement hash and source
revision. This changes the diagnostic graph, not the managed pack or historical
baseline. See the canonical [AJ-04 status and acceptance
boundary](spec.md#remaining-prerequisite-qualification).
Historical NeoForge 21.1.249 evidence remains separately unavailable/unverified.

### Required final record

Record the tested commit, dependency graph, configuration, logs, and reviewed
screenshots for the failing baseline and corrected case. Ordinary CPU estimates
and controls must work with and without the implicated addons on NeoForge
1.21.1. Mark unrun graphs explicitly; a successful launch is not a UI check.

[#459](https://github.com/cTux/ae2-crafting-time/issues/459) covers qualification
of a newer OmniSequence graph; [#518](https://github.com/cTux/ae2-crafting-time/issues/518)
covers Data Energistics metadata. Neither proves this regression fixed.
Update the [integration specification](spec.md) and dependency guidance only
after the behavior is verified. This documentation change does not close #534.
