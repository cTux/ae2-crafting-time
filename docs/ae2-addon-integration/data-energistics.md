# Data Energistics optional dependency

Status: in-progress; focused runtime checks passed, combined graph pending

Scope: Optional dependency metadata and qualification for NeoForge 1.21.1.

Issue: [#518](https://github.com/cTux/ae2-crafting-time/issues/518).

Planning: [requirements](spec.md#data-energistics-qualification),
[design](technical-design.md#data-energistics-qualification), and
[implementation plan](implementation-plan.md#data-energistics-qualification).

The user authorized implementation and prepared-client qualification on
2026-10-18. Implementation and focused runtime evidence are in
[PR #741](https://github.com/cTux/ae2-crafting-time/pull/741).

Ordinary crafting passed with the addon absent and present, the adaptive-provider
fixture passed, Status controls survived a client restart, and Crafting Tree
1.21.1-1.1.1 passed its screen checks. The largest compatible graph containing
Data Energistics still needs its final lifecycle run. Automatic screenshot gates
remain `REVIEW_REQUIRED` without qualified baselines; every captured PNG receives
manual review instead.

AE2 Lightning Tech 2.1.0-beta.5 and Thunderbolt Core 2.0.0-beta.4 explicitly
declare all Data Energistics versions incompatible. A combined launch reproduced
that loader rejection. Preserve their existing primary profile and qualify Data
Energistics separately; never strip the declarations. Coexistence is tracked in
[#742](https://github.com/cTux/ae2-crafting-time/issues/742).

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
the culprit or prove this optional dependency supported. The qualified scope is
ordinary AE2 CPUs and the Adaptive Pattern Provider's native-provider mode with
vanilla processing. Trinity CPUs and other provider modes remain unqualified
([#740](https://github.com/cTux/ae2-crafting-time/issues/740)).
