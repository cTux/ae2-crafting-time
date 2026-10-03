# Final issue text for approval

This is the exact replacement title/body proposed for issue #647. It has not
been applied. The planning workflow requires explicit approval before replacement.

## Title

Support crafting suspension for replacement addon CPUs on Forge 1.20.1

## Body

Forge 1.20.1 currently exposes Suspend/Resume only for the exact standard AE2
CPU and logic classes. Extend the same player behavior to AdvancedAE Quantum
Computer CPUs, NeoEco ECO CPUs and LightningTech time-wheel CPUs through their
existing optional integrations.

Keep the finished standard-CPU backport from #631 and its NeoEco FastPath fix
from #641. Fabric and newer NeoForge targets are outside this extension.

### Resulting behavior

- Suspend stops new dispatch, keeps the same job and reserved ingredients, and
  accepts in-flight outputs. Resume continues that job. Cancel and completion
  remain available, and suspension survives a clean server restart.
- Reuse persistent native player-pause state where available. AdvancedAE needs
  a state/persistence backport. NeoEco Resume must never clear its separate
  internal suspension condition.
- The existing world-owned `craftingSuspension` option governs the integrated
  player-pause state, including native addon pause actions. Disabling it clears
  player pause on the next loaded logic tick without clearing internal engine
  conditions. Profiling and addon diagnostic switches do not govern Resume.
- Use the current menu/context/UUID validation and per-menu synchronization.
  Show one control per supported screen, and preserve optional-peer behavior.
- Hide suspension for missing/incompatible adapters or unknown replacements;
  preserve otherwise working profiling support.

### Acceptance

1. Real replacement CPUs from AdvancedAE, both retained Forge NeoEco API
   families and LightningTech can be selected, suspended and resumed.
2. Normal, batched, FastPath and budgeted time-wheel dispatch stop while paused.
   In-flight returns remain valid, another CPU finishes through shared
   providers, and the same resumed job conserves exact input/output counts.
3. Repeated cycles, cancellation/soft cancellation, final in-flight completion,
   failed submissions, CPU switches and stale replacement-job requests are safe.
4. Two real clients agree before and after reopen and clean dedicated restart;
   duplicate or forged requests cannot mutate another job.
5. Live and startup-file disable recovery work with profiling/diagnostics off.
   Native pause actions update TTC state, and NeoEco internal suspension survives.
6. Paused rows, totals, cards, warnings, automatic highlights and accuracy use
   the actual addon profiler scope and leave another active job unaffected.
7. Core coverage, contract/boundary checks, transformed production hooks,
   reviewed focused runtime evidence, current-head CI, English/Ukrainian
   GuideME/wiki and dependency documentation agree.

### Boundaries and risks

Do not replace addon schedulers, stop external machines, release CPUs or
reservations, enable arbitrary subclasses, change the packet layout, or raise
dependency minimums. Native APIs alone do not prove that every dispatch entrance
or selected-menu path is safe; verify those paths on exact artifacts.

LightningTech #460 tracks missing current release-source identity. Use the
recorded, hash-verified artifact for this work; do not claim it is the latest
release. No other known issue blocks the defined planning scope.

### Planning documents

- `docs/crafting-suspension/spec.md`, replacement addon CPU scope, ACS-1–ACS-7.
- `docs/crafting-suspension/technical-design.md`, addon CPU extension.
- `docs/crafting-suspension/implementation-plan.md`, addon CPU extension.
- `docs/crafting-suspension/addon-evidence.md`, inspected artifact/API evidence.

Keep the issue open until the implementation is delivered and all gates above
are verified. A planning PR alone does not deliver addon suspension.
