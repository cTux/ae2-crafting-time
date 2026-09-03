# Running a Development Client

Build all production and test-driver JARs on the host. CodexVM only runs the
client and smoke checks using copied artifacts. Keep the live Minecraft runtime
on guest-local NTFS, give it an 8 GiB maximum heap, and maximize the exact
Minecraft window before a visual check.

## Host build and VM staging

1. Keep `JAVA_HOME_17`, `JAVA_HOME_21`, and `JAVA_HOME_25` set on both the host
   and CodexVM, pointing to each machine's own installed JDK directories.
   Verify each `bin/java.exe -version` before using it. Use
   `JAVA_HOME_17` or `JAVA_HOME_21` for the Gradle process; the version modules
   select Java 17, 21, or 25 toolchains. Do not start the multi-project build on
   Java 25. If the variables are missing, locate and verify the installed JDKs
   before setting them; do not guess paths.
2. Make the changes in the session worktree. Build the exact release-matrix
   row's `distMod` task and, when needed, `testDriverJar` on the host. Pass the
   installed toolchain paths explicitly when Gradle does not discover them:

   ```powershell
   $env:JAVA_HOME = $env:JAVA_HOME_21
   $jdkPaths = "$env:JAVA_HOME_17,$env:JAVA_HOME_21,$env:JAVA_HOME_25"
   # Example: Forge 1.20.1 production and test-driver artifacts.
   .\gradlew.bat :mc_1_20_1_forge:distMod :mc_1_20_1_forge:testDriverJar "-Porg.gradle.java.installations.paths=$jdkPaths"
   ```

   Read the actual module name from `scripts/release-matrix.json`. Rebuild after
   source changes; reuse the resulting artifact across matching clients in the
   same campaign. A user-supplied exact JAR does not need rebuilding.
3. Add the session worktree directory as a CodexVM shared folder through VMware's
   shared-folder controls. Reuse a share only when it already points to that
   exact worktree. Keep unrelated shares, reuse the running VM, and verify the
   guest can read the built files. Do not edit a running VM's VMX or restart it
   just to add a share.
4. Copy/replace the exact production and matching test-driver JARs in the needed
   guest-local client or modpack. Remove older enabled copies of our replaced
   JARs, leave one of each required artifact, and compare their SHA-256 hashes
   with the host files. Keep the existing client and dependencies when they
   already match the requested target and profile.
5. Launch the installed client with Java 17 for Minecraft 1.20.1, Java 21 for
   1.21.1, or Java 25 for 26.1.2. Run the requested smoke, review screenshots,
   return logs and evidence through the share, and stop only the tested client.

The PowerShell launchers resolve these variables from the current process,
then saved user settings, then machine settings. They reject missing executables
and wrong Java versions. `scripts/get-java-home.ps1 -Major 21` returns the
verified directory for a direct Java 21 launch on either machine. The Bash
launcher expects all three variables exported in its shell.

Host Gradle client launchers use these three variables as their toolchain
inventory, with automatic discovery and downloads disabled. Minecraft 26.1.2
uses `JAVA_HOME_25` for its client toolchain and `JAVA_HOME_21` for Gradle.

For Prism, inspect and use only the **Codex** group. If it lacks the exact
requested modpack release, download and install that release into **Codex**.
A matching pack elsewhere is not eligible: do not launch, copy, move, or modify
it. Keep temporary guest-local test instances in **Codex** as well.

Before opening Prism for a test, run this on the machine that will run the client:

```powershell
.\scripts\set-prism-java.ps1 -InstanceDirectory '<exact Codex instance directory>'
```

It checks Codex membership and the installed Minecraft version, resolves that
machine's matching `JAVA_HOME_*`, and sets the instance's Java executable with
automatic Java selection disabled. Run it again after changing a variable or
moving the instance between machines. For a staged guest-local instance, copy
its Codex group membership too. Do not edit settings while Prism or that client
is running. Prism needs the resolved executable path, not a literal environment
variable in its Java-path field.

## Prepared-client launcher limitation

The current `invoke-ui-smoke-codexvm.ps1` dispatcher reaches
`run-ui-smoke-codexvm.ps1`, `run-ui-smoke.ps1`, and `run-client.ps1`, which build
with Gradle and launch the production mod from a source set. The `run-*` wrappers
also reach that build path, including `run-client.ps1 -ResolveOnly`, which still
builds the driver. They are not artifact-only VM launchers. Do not run these
paths in CodexVM until they support launching host-built JARs without guest
builds. A missing artifact-only prepared launcher is a tooling gap, not permission
to build in the VM, launch the UI on the host, or substitute a Prism modpack.

The existing scenario contracts, fixture preparation, screenshot archive, and
result checks still apply to smoke runs. Keep the requested target, profile,
and scenario. During integration development, a focused dependency graph may
load only that integration and its required dependencies. Before merge, use the
complete compatible profile. Fetch and rebase onto `origin/master` immediately
before that final run; rerun after any later base change to production, build,
dependency, fixture, or driver code.

Provision VM transports with `prepare-codexvm-ui-smoke.ps1` on the host and
`setup-codexvm-ui-smoke.ps1` in elevated guest PowerShell when needed. Transport
setup does not authorize guest builds.

## Dependency profiles

Ordinary clients use a complete version lock: loader, AE2, Fabric API where
needed, the maximum mutually compatible addon set, every required library, and
JEI. These are the lowest full-stack versions currently kept as the working
compatibility baseline, not the oldest file from each project.

Latest clients ignore that lock and resolve every project again. A dependency
conflict or startup failure is expected evidence; the launcher does not hide it
or fall back to the compatible version. Latest clients use `run-latest`, so
their mods, configs, and worlds do not touch the ordinary `run` sandbox.

Update every client in one place: `scripts/run-client-versions.json`. Change
`projects` when the candidate set changes. Change `compatible` and its exact
`versions` map only after the full target is known to work together. The latest
profile resolves Modrinth and loader releases at launch. CurseForge-only files
have explicit `compatible` and `latest` records because CurseForge has no
anonymous version API; update their file IDs, names, and hashes in the matrix.

Before adding a Modrinth integration, audit every supported row from its
official artifacts:

```powershell
.\scripts\audit-optional-integration.ps1 -ProjectId <id>
```

The command selects the oldest stable artifact whose embedded Minecraft,
loader, and AE2 ranges admit the pinned row. It falls back to a compatible beta
when that target has no stable artifact, but never selects alpha files. Rows
without a compatible official artifact are reported as `UNSUPPORTED`.

The same matrix is the known-issues list. Keep incompatible candidates in
`projects`, set `compatible` to `false`, and record the concrete `reason`.
When issues exist, add `issue_url` for this repository and
`upstream_issue_url` for the dependency. Promote a latest version into
`compatible` only after the whole target starts and its requested smoke checks
pass together.

ExtendedAE-Plus is included in the Forge 1.20.1 and NeoForge 1.21.1 compatible
profiles. Expanded AE stays excluded because its Applied Flux pattern-provider
mixin conflicts with other candidates in both rows.
