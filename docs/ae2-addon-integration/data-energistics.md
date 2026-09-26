# Data Energistics optional dependency

Status: draft

Scope: Optional dependency metadata and qualification for NeoForge 1.21.1.

Issue: [#518](https://github.com/cTux/ae2-crafting-time/issues/518).

Hold: The issue is marked `do-not-implement-yet`. This document does not lift it.

## Requested support

List Data Energistics as optional for the 1.21.1 NeoForge release, and document
the supported combination in [the dependency inventory](../dependencies.md).
The issue identifies this target only; do not advertise other loaders or versions.

Optional means Crafting Time must continue to start and provide its normal
crafting-time behavior without the addon. An installed dependency is not proof
of compatible profiling, UI, or crafting behavior.

## Qualification work

1. Resolve the exact addon artifact and official mod ID, supported loader,
   required dependencies, and version constraints before editing metadata.
   Use the existing release-matrix and optional-dependency conventions.
2. Compare ordinary AE2 crafts on the same target with the addon absent and
   present. Record versions, launch logs, profile samples, Plan estimates,
   Status updates, and completion behavior.
3. Exercise the Adaptive Pattern Provider and any other feature that changes
   pattern dispatch or output returns. Trace the existing shared profiler and
   integration selection before proposing a compatibility hook.
4. Add a hook only for a demonstrated gap. If needed, document its API contract,
   failure isolation, and tests before implementation; otherwise retain the
   existing integration path.
5. Update applicable loader/publishing metadata and dependency documentation
   together, retaining required AE2 dependencies and absent-addon startup.

## Completion evidence and boundaries

Record exact source/artifact identities, with/without results, logs, and reviewed
UI captures. Run the existing optional-integration metadata checks and applicable
CI after the implementation PR exists. A passing metadata check cannot replace
runtime qualification.

[#534](https://github.com/cTux/ae2-crafting-time/issues/534) separately investigates
missing UI in Applied Journey. Its report does not establish Data Energistics as
the culprit or prove this optional dependency supported. This page records
qualification requirements, not a completed compatibility result.
