# JEI++ optional client qualification

Status: draft

Scope: [#439](https://github.com/cTux/ae2-crafting-time/issues/439).

Hold: `backlog` remains in force.

## Requested support and boundaries

The issue identifies JEI++ (JEI Plus), CurseForge project 1645653, and version
1.0.5 files for Forge 1.20.1 and NeoForge 1.21.1. Recheck exact official artifacts
and prerequisites before choosing pins; these issue-recorded versions are not
a claim about the newest release.

Keep JEI++ optional and client-only. Do not add Fabric 1.20.1 or NeoForge 26.1.2
profiles without a matching upstream file. Its recipe tree, crafting assistant,
grouping, and navigation are not automatically TTC integrations.

## Qualification sequence

1. Resolve compatible JEI++/JEI/AE2 versions and required dependencies for each
   requested target. Record exact file identities and installation sides.
2. Add the applicable optional inventory entries and development-client pins
   only as part of the authorized implementation. Preserve startup without JEI++.
3. Run focused checks with JEI++, JEI, AE2, and Crafting Time together. Open the
   recipe tree and assistant, navigate recipes, and inspect ordinary Plan/Status
   estimates for screen, mixin, and interaction errors.
4. Trace whether those screens reuse normal JEI/AE2 paths. Add a TTC hook only
   for a demonstrated display gap; otherwise document why no hook is needed.
   A startup-only pass does not settle that question.
5. Record verified versions and behavior in [dependencies](../dependencies.md).
   Keep metadata explicit about optional/client-only ownership.

## Completion

Both requested targets must resolve, launch, and pass the focused UI scenario.
Retain source commit, resolved versions, logs, reviewed captures, and the
hook/no-hook finding. Link relevant metadata checks and current-head CI after the
implementation PR exists. Mark unsupported and untested combinations separately.
This draft does not install the addon or certify compatibility.
