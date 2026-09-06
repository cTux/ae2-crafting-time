# Project Infinity 0.1 Full UI Smoke Technical Design

This design implements [PI-01 through PI-12](spec.md#requirements) by composing
the existing named-modpack, TestDriver, suite, and evidence paths. It adds no
new runtime or test framework.

## Campaign shape

```text
official CurseForge file 8664964
  -> Prism Codex-group instance and metadata checks
  -> enabled JAR metadata inventory, including nested JARs
  -> release row 1.20.1-forge
  -> host production and test-driver builds
  -> guest-local staged instance with verified hashes
  -> inventory-derived flat suite and fresh worlds
  -> one sequential Prism launch
  -> semantic results + reviewed screenshots + current logs
  -> immutable archive, exact-client shutdown, targeted cleanup
```

The official CurseForge file is the release identity. Prism display text is
not enough: `instance.cfg` and `mmc-pack.json` must agree with it. Enabled JAR
metadata is authoritative for AE2 and integration presence; filenames are not.

## Preflight and staging

Use the named-modpack Prism workflow and CodexVM. Reuse an instance only when it
is in **Codex** and its metadata matches project `1266680`, file `8664964`,
Minecraft `1.20.1`, and Forge. Otherwise install that exact CurseForge file
through Prism into **Codex**. Blocked-mod downloads must finish with every row
green; skipping files creates an invalid graph.

After AE2 eligibility succeeds, select the `1.20.1-forge` row from
`scripts/release-matrix.json`. Build its `distMod` and `testDriverJar` tasks on
the host. Stage the managed instance on guest-local NTFS, run
`scripts/set-prism-java.ps1` against that exact staged directory, replace only
old enabled AE2 Crafting Time production/driver copies, and compare SHA-256
hashes with the host artifacts before launch.

## Suite selection

The suite selector starts with the fixed core list from PI-04. It expands
`standard-ae2` through `scripts/expand-ui-smoke-groups.ps1`, preserving the
declared leaf order.

For integrations, normalize the enabled mod inventory to provider project ID,
mod ID, version, and containing JAR. Join that inventory to the
`1.20.1-forge` entries in `scripts/ui-smoke-coverage.json`, then retain a
scenario only when all of these are true:

1. the project is present and enabled in the staged pack;
2. the coverage disposition is `DIRECT_UI` or `DIRECT_BEHAVIOR`;
3. the scenario exists in `scripts/ui-smoke-forge-suite.json`;
4. artifact inspection maps the installed version to a supported adapter; and
5. the scenario's required fixture can be built without changing the pack graph.

An enabled mapped project that fails conditions 3 through 5 blocks preflight as
missing or contradictory coverage. Do not silently downgrade it to coexistence
or skip it. Deduplicate valid scenarios while preserving Forge-suite order.
Feed the resulting group/case list to `scripts/prepare-ui-smoke-suite.ps1` with
target `1.20.1-forge`; do not pass `standard-ae2` to the raw JVM property. Save
both the requested groups and expanded leaves. Unknown AE2 addons receive a
recorded coexistence or unsupported disposition rather than a guessed scenario.

If the pack selects an older supported adapter, run that pack scenario only
when its contract supports it and record the exact adapter ID. Schedule the
newest implemented adapter in a separate prepared fixture as required by
SP-01 through SP-04. Never replace a pack dependency for that purpose.

## Execution and observation

The prepared suite owns one fresh disposable world per leaf. Prism starts one
client with the suite scenario property; the driver advances cases sequentially
and writes per-case results. The 40-minute existing suite timeout remains in
force unless a measured run demonstrates that a separate reviewed change is
needed.

Before visual inspection, maximize the exact Minecraft window. Review every
checkpoint required by `scripts/ui-smoke-groups.json`, the selected scenario
contracts, and `docs/ui-smoke-evidence.md`. A single image may support several
checks only when each is visible. Server-only observations remain semantic
assertions paired with the resulting UI image.

Do not retry automatically. A diagnostic rerun gets a new attempt directory
and cannot overwrite or reclassify the original campaign.

## Evidence model

The archive root identifies the pack project/file, instance ID, target,
selection mode `manual`, commit, artifacts, start/end time, and outcome. It
contains:

- the raw and normalized enabled-mod inventory;
- production and driver filenames plus host/guest SHA-256 hashes;
- requested groups, expanded suite plan, coverage joins, exclusions, and
  selected adapter IDs;
- one directory per leaf with semantic JSON, required PNG files, and outcome;
- the current `latest.log`, Prism console output, and crash report when present;
- an inspected `report.md`, cleanup result, and measured timing table.

The archive excludes credentials, account identity, tokens, unrelated worlds,
server addresses, and stale logs from earlier launches.

## Failure and cleanup

Fail before launch for wrong pack identity, missing AE2, no matching release
row, incomplete downloads, wrong Java, mismatched hashes, duplicate test JARs,
invalid or contradictory suite selection, or missing fixtures. During the run,
preserve the first failing case and current screen/log evidence. Later cases
remain `NOT_RUN` when the driver cannot advance safely.

Close normally when possible. If stuck, use Prism's selected-instance **Kill**
for that client only. Confirm Prism no longer marks it running, synchronize the
archive and logs, restore Prism's normal shared root, then remove only the
marked temporary guest-local copy.

## Requirement coverage

| Requirements | Design seam |
| --- | --- |
| PI-01, PI-02 | CurseForge/Prism identity and JAR metadata preflight |
| PI-03, PI-07 | Release row, host build, verified guest-local staging and Java |
| PI-04, PI-05, PI-06 | Core list, inventory join, group expansion and single-launch suite |
| PI-08, PI-10 | Driver assertions, visual review and immutable evidence archive |
| PI-09 | Separate prepared newest-adapter fixtures |
| PI-11, PI-12 | Failure retention, exact-client shutdown and targeted cleanup |

## Rejected alternatives

- Running the prepared Forge suite instead of the pack would not test Project
  Infinity's real graph.
- Adding missing addons would change the graph and invalidate the pack claim.
- Relaunching once per case would add load time and lose the existing suite's
  attribution without improving isolation; fresh worlds already provide it.
- Inferring mods from filenames would miss nested JARs and renamed artifacts.
