# Missing UI in Applied Journey

Tracking: [#534](https://github.com/cTux/ae2-crafting-time/issues/534).

This is an investigation plan, not a reproduced compatibility diagnosis. The
report says Crafting Time UI and estimates disappear on ordinary AE2 CPUs in
Applied Journey on NeoForge 1.21.1. Neither Data Energistics nor OmniSequence:
Transfinite has been established as the cause.

## Reproduction boundary

The supplied export identifies Applied Journey 0.0.1, Minecraft 1.21.1,
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

## Investigation sequence

1. Reproduce in the exact pack on a normal AE2 CPU. Record the screen, expected
   and missing elements, actual loaded versions, configuration, logs, and image.
   Distinguish absent row text from missing history or disabled profiling.
2. Compare AE2 plus Crafting Time alone, then Data Energistics, then
   OmniSequence with its required dependencies, then both addons, then the pack.
   Keep the recipe, CPU, options, and screen consistent between comparisons.
3. Inspect the shared Crafting Plan/Status mixins and `TtcText` rendering path,
   client options, server snapshots, and `IntegrationCatalog`/`IntegrationSelection`
   diagnostics. Use the first differing runtime evidence to narrow the fault;
   update history alone does not prove an addon conflict.
4. Fix the confirmed shared seam, or retain a minimal reproducer and link an
   external follow-up when the defect belongs upstream. Do not add an adapter
   merely because an addon appears in the pack.

## Completion evidence

Record the tested commit, dependency graph, configuration, logs, and reviewed
screenshots for the failing baseline and corrected case. Ordinary CPU estimates
and controls must work with and without the implicated addons on NeoForge
1.21.1. Mark unrun graphs explicitly; a successful launch is not a UI check.

[#459](https://github.com/cTux/ae2-crafting-time/issues/459) covers qualification
of a newer OmniSequence graph; [#518](https://github.com/cTux/ae2-crafting-time/issues/518)
covers Data Energistics metadata. Neither proves this regression fixed.
Update the [integration specification](spec.md) and dependency guidance only
after the behavior is verified. This documentation change does not close #534.
