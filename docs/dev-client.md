# Running a Development Client

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

`1.21.1-neoforge` excludes ExtendedAE-Plus only from the compatible profile:
all of its available releases collide with Expanded AE's Applied Flux mixin.
The matching latest client includes it so that upstream conflict remains
visible and reproducible.
