# Running a Development Client

Run UI and startup checks inside CodexVM. The VM's read/write `projects` share
contains this checkout; keep `-RuntimeDirectory` on guest-local NTFS so Minecraft
does not use VMware's shared filesystem for its live runtime. Each client has an
8 GiB maximum heap. Maximize the exact Minecraft window before a visual check.

For the fast Forge 1.20.1 UI smoke, run this on the host:

```powershell
.\scripts\invoke-ui-smoke-codexvm.ps1
```

During integration development, add `-ProjectId <id>` to load only that
integration and its required dependencies. Before merge, rerun the scenario
without `-ProjectId` to prove it against the complete compatible profile. Fetch
and rebase onto `origin/master` immediately before that final full-profile run;
rerun it after any later base change to production, build, dependency, fixture,
or driver code.

It uses OpenSSH by default. Add `-Transport Vmrun` to use VMware guest
execution, or `-Stop` to terminate only the PID tree recorded by the current
run. Both transports dispatch into the logged-in Codex desktop so Minecraft is
still visible. The guest checkout, Gradle output, resolved mods, and runtime
stay warm under `C:\Users\Public\Documents\AE2CraftingTimeSmoke`; only per-run evidence and
the disposable world are replaced.

Progress does not require VNC. Read `status.json`, `launcher.stdout.log`, and
`launcher.stderr.log` under
`build\ui-smoke\1.20.1-forge\<profile>\<scenario>`. Each profile keeps one warm
runtime and separates results by scenario. The runner rejects concurrent smoke
scenarios that would share that runtime. The status records the phase, exact
PID, Java home, result, and evidence path. Use VNC only for the final maximized
Minecraft visual check.

Provision a fresh VM once by running `prepare-codexvm-ui-smoke.ps1` on the host,
then run `setup-codexvm-ui-smoke.ps1` in an elevated guest PowerShell with the
one-time JSON path printed by the host command. The guest deletes that JSON
after installing OpenSSH, adding the dedicated `CodexSmoke` vmrun account, and
installing the host public key. The reusable vmrun credential is encrypted for
the current host user and is never stored in the repository.

Use an ordinary script for the pinned compatible sandbox:

```powershell
.\run-1.20.1-forge.bat
.\run-1.20.1-fabric.bat
.\run-1.21.1-neoforge.bat
.\run-26.1.2-neoforge.bat
```

Use the matching latest script to expose new upstream incompatibilities:

```powershell
.\run-1.20.1-forge-latest.bat
.\run-1.20.1-fabric-latest.bat
.\run-1.21.1-neoforge-latest.bat
.\run-26.1.2-neoforge-latest.bat
```

The `.sh` wrappers have the same names and behavior.

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
loader, and AE2 ranges admit the pinned row. Rows without one are reported as
`UNSUPPORTED` instead of accepting any downloadable file.

The same matrix is the known-issues list. Keep incompatible candidates in
`projects`, set `compatible` to `false`, and record the concrete `reason`.
When issues exist, add `issue_url` for this repository and
`upstream_issue_url` for the dependency. Promote a latest version into
`compatible` only after the whole target starts and its requested smoke checks
pass together.

ExtendedAE-Plus is included in the Forge 1.20.1 and NeoForge 1.21.1 compatible
profiles. Expanded AE stays excluded because its Applied Flux pattern-provider
mixin conflicts with other candidates in both rows.
