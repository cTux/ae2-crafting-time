# AELIS plan TTC

Status: in-progress. Issue: [#756](https://github.com/cTux/ae2-crafting-time/issues/756).
The user authorized implementation of the research recommendation on October 24, 2026.

On Forge 1.20.1 with Applied Enhancements 1.1.4, add one TTC column for the
material table's To craft quantity and an approximate total for the whole plan.
Reuse Plan row TTC and Plan total switches, profiling permissions, learned
history, colors, confidence markers and optional badge backgrounds. Stored-only
rows show no remaining time. Unknown history shows collecting data; unsupported
quantities show unknown. Missing/cyclic plans retain a visibly partial total.
Filtering must not change that total. Craft Tree remains unchanged.

Preserve existing amounts, names, search, filters, scrolling, tooltips, CPU
selection, submission and AE2-view switching. Reserve space for the column only
where the existing minimum table width plus the new column fits; otherwise keep
the upstream table. Support must remain optional on clients and absent on servers,
other targets, absent addons and unknown API contracts. No new packets or saves.

Acceptance: covered shared width/quantity/total boundaries; released-contract,
missing-member, wrong-target and server selection tests; remapped hook inspection;
real enabled-AELIS plan screenshots with history and without history, options,
filters, cyclic rows, scrolling, narrow layout and AE2-view switching; existing
native plan checks and absent-addon startup. Do not report unrun cells as passed.
