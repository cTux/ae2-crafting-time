# CodexVM shared-folder design

The [specification](spec.md) restores an existing staging contract. Inspection
at `63e3441ac5159b95eb984eb8963be95479deb024` found working HGFS reads and no
demonstrated launcher defect. Keep configuration unchanged until evidence
identifies a correction.

## Current flow and callers

| Component | Behavior and boundary |
|---|---|
| [`run-ui-smoke.ps1`](../../scripts/run-ui-smoke.ps1) | Host invocation delegates to the matrix. Guest invocation owns disposable fixtures, runtime lock, launch, results and cleanup. |
| [`run-ui-smoke-matrix.ps1`](../../scripts/run-ui-smoke-matrix.ps1) | Plans targets, builds or reuses sealed host bundles, records hashes and dispatches sequentially. Calls the dispatcher for normal execution and timeout stop. |
| [`invoke-ui-smoke-codexvm.ps1`](../../scripts/invoke-ui-smoke-codexvm.ps1) | With no bundle, delegates to the matrix. Otherwise hashes the exact worktree path into a share name, tries `setSharedFolderState`, adds the share when that fails, and enables shares. An explicit `-GuestSourceRoot` bypasses registration. |
| Dispatcher path translation | Converts the runner, bundle, report and resume paths to the selected shared UNC. Resolves the exact VM's IP with `vmrun`, then invokes the shared guest script through encoded PowerShell over key-based SSH. |
| [`run-ui-smoke-codexvm.ps1`](../../scripts/run-ui-smoke-codexvm.ps1) | Reads the bundle profile, selects the exact-version or generic prepared manifest, resolves guest Java and mirrors source into guest-local storage. Runs the local runner and returns reports through HGFS in `finally`. |
| [`prepare-ui-smoke-launch.ps1`](../../scripts/prepare-ui-smoke-launch.ps1) | Validates target/profile/Java/loader and runtime ownership. Copies manifest-selected JARs and checks every source/destination hash. Preserves native classpath/assets while replacing test properties and disposable-world arguments. |

These are all callers of the repository's share-registration implementation.
The matrix and direct dispatcher share the same route, including stop. An HGFS
failure can prevent the guest script from loading, block source/bundle reads,
or lose report return after execution. SSH reachability alone does not prove
any of those operations. Minecraft classloading begins after staging.

The guest runner verifies the source fixture's `SOURCE_ONLY` marker, takes a
runtime lock and creates a disposable world. It verifies the source tree hash
again during cleanup. Scheduled Java uses the existing interactive guest user.
Stop validates the recorded PID, command and creation time before stopping it.
Preserve these boundaries and the [evidence contract](../ui-smoke-evidence.md).

## Evidence on 2026-09-12

The complete [issue #400](https://github.com/cTux/ae2-crafting-time/issues/400)
and its timeline had no comments or open prerequisite. Linked
[PR #399](https://github.com/cTux/ae2-crafting-time/pull/399) was merged to the
inspected base and closed #353. Its successful SSH-staged smoke does not prove
HGFS recovery. The earlier test-driver documents concern Tomcat, not transport.

Read-only checks around 20:54–21:00 UTC observed:

- Workstation `26.0.0 build-25388281`, the exact VM running, Tools
  `13.1.0 build-25218885`, and successful existing key-based SSH with strict
  host-key checking. Localhost VNC listened on `127.0.0.1:5905`.
- Guest PowerShell `5.1.26100.9444`; `vmhgfs` and `vmci` running. HGFS DLL and
  driver both reported `11.0.49.0`. The registered provider points to
  `System32\vmhgfs.dll`, names `\Device\hgfs`, and appears first in provider
  order. No Java client was observed.
- The sandbox reported zero running VMs; the actual interactive host account
  returned the exact running VM. Resolve that context before starting anything.
- `Get-Item` succeeded for the HGFS root, an existing general share, and the
  previous task's exact worktree file in 30, 2 and 3 ms respectively. Reading
  the current worktree through an existing parent mapping also succeeded; a
  dedicated mapping for this worktree had not yet been registered.
- `net view` against the VMware host name returned error 53. That SMB
  enumeration result did not contradict successful HGFS reads. Explicit JSON
  output was needed because mixed PowerShell table output hid some properties.
- The VM retained 54 configured shares; 14 host directories were absent.
  Hypervisor logs contained `HgfsPlatformScanvdir` errors for old entries.
  Valid reads succeeded with those entries still present. Their presence alone
  therefore does not establish the cause of total HGFS failure.

The existing #353 bundle was hashed on the host and through HGFS with identical
results. Paths below are relative to its retained `build/issue-353/bundle`:

| File | SHA-256 |
|---|---|
| `profile.json` | `358E4D41CC8D023591900E367E8EA4CABD0351F2A972E2E65847AFF05445DDFB` |
| `mods/ae2-crafting-time-1.2.5-forge-1.20.1.jar` | `2240B3E1E8FC7EC1DF3558EB265667B8E7074C9C9DC9FFFB59DEB43FF9E67A44` |
| `mods/ae2-crafting-time-1.2.5-forge-1.20.1-test-driver.jar` | `41C99D08B616BEB58C0DE84FD638986BA27D5E780E8D59E902528CDBB486D78F` |

Guest hashing took 105, 81 and 272 ms. These are individual read timings, not
copy, recovery or smoke measurements. No configuration, files or shares were
changed, and no new Minecraft run occurred during this investigation.

The current [`craft-lifecycle` catalogue](../../scripts/ui-smoke-groups.json)
requires 12 checks and 13 screenshots. The prior #353 run retained 14 images;
that historical total does not add a mandatory checkpoint to the current case.

## Hypotheses and limits

| Hypothesis | Evidence and next distinguishing observation |
|---|---|
| Missing/disabled provider or Tools | Current running state plus successful file hashes rules out persistent absence now. Capture provider and operation state together if failure recurs. |
| Wrong worktree mapping | Prior exact mapping and current files through an existing mapping read successfully. Compare registered target, requested UNC and a known file before blaming registration. |
| Old shares poison all HGFS access | Not demonstrated: valid reads coexist with old-entry log errors. Do not remove shares to turn correlation into a claimed cause. |
| Transient initialization or request-context failure | Still plausible, unproven. Retain the actual failed command, context, timestamp and provider state; compare with the same successful operation. |

The earlier report says both SSH and desktop session 1 failed with error 67.
That is historical evidence, not a current reproduction. Current desktop-session
reads, writes back to the host, sustained access and normal smoke dispatch are
still unverified. Do not retrofit a causal story from the later success.

## Feasibility and recovery boundaries

Guest Java 17/21/25 installations and prepared manifests for all four targets
exist, with an additional Forge 47.4.23 manifest. Java 17 executed as
`17.0.20.1+1`. The representative generic manifest and existing bundle both use
Forge 47.4.10; the bundle records AE2 15.4.10, Java 17 and catalogue mode.
The host Forge fixture has its valid source-only marker and saved world.

One historical version-JAR entry among the manifest's 94 classpath entries was
absent. The prior successful smoke used this installation; an unused nonexistent
classpath entry is not by itself proof of a broken loader. Keep native launch
as the decisive check and investigate a concrete loader failure if it occurs.
Do not silently substitute another Forge version or install a new profile.

No new runner, fixture or control service appears necessary. Register/reuse the
exact worktree only through the existing documented path when execution is
authorized. An old bundle can prove diagnostic transport integrity but cannot
be labelled as newly built from another SHA. Final smoke inputs must pass the
normal provenance rules at their actual source revision.

Before any cause-supported recovery, retain the exact affected configuration
and inventory outside published documents, with hashes and a rollback record.
Check active guest work. Never edit a running VMX, remove unrelated shares,
restart another VM, stop Java broadly or use the password fallback. A required
restart must use the VM skill's exact-target lifecycle seam and preserve active
work. If all probes pass, leave the working state alone and report the cause
as unresolved. The existing [MCP evaluation](../codexvm-mcp-evaluation/results.md)
still rules out the rejected credential-bearing control paths.
