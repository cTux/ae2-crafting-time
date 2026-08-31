# Running a Development Client

Run UI and startup checks inside CodexVM. The VM's read/write `projects` share
contains this checkout; keep `-RuntimeDirectory` on guest-local NTFS so Minecraft
does not use VMware's shared filesystem for its live runtime. Each client has an
8 GiB maximum heap. Maximize the exact Minecraft window before a visual check.

For the fast Forge 1.20.1 UI smoke, run this on the host:

```powershell
.\scripts\invoke-ui-smoke-codexvm.ps1
```

It uses OpenSSH by default. Add `-Transport Vmrun` to use VMware guest
execution, or `-Stop` to terminate only the PID tree recorded by the current
run. Both transports dispatch into the logged-in Codex desktop so Minecraft is
still visible. The guest checkout, Gradle output, resolved mods, and runtime
stay warm under `C:\Users\Public\Documents\AE2CraftingTimeSmoke`; only per-run evidence and
the disposable world are replaced.

Progress does not require VNC. Read `status.json`, `launcher.stdout.log`, and
`launcher.stderr.log` under
`build\ui-smoke\1.20.1-forge\<profile>`. The status records the phase, exact
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

The same matrix is the known-issues list. Keep incompatible candidates in
`projects`, set `compatible` to `false`, and record the concrete `reason`.
When issues exist, add `issue_url` for this repository and
`upstream_issue_url` for the dependency. Promote a latest version into
`compatible` only after the whole target starts and its requested smoke checks
pass together.

`1.21.1-neoforge` excludes ExtendedAE-Plus only from the compatible profile:
all of its available releases collide with Expanded AE's Applied Flux mixin.
The matching latest client includes it so that upstream conflict remains
visible and reproducible.
