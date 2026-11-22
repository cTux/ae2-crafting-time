# NeoEco 20.5 Forge implementation plan

[Scope and acceptance criteria](spec.md) · [Technical design and research](technical-design.md)

1. Use the prepared Forge 1.20.1 client and Java 17. Pin the primary disposable
   graph to [NeoEco 20.5.10](https://modrinth.com/mod/neoecoae/version/HhvCyNDd)
   and use [20.5.9](https://modrinth.com/mod/neoecoae/version/8Hm2te4S) for the
   confirmed compatibility boundary. Verify the resolved JARs against Modrinth's
   published SHA-512. Choose fixture versions of Forge, AE2, GuideME and LDLib
   that satisfy the [tagged addon metadata][metadata]. Keep the production
   minimums and compatible-client pins unchanged.
2. Reuse the `neoeco-cpu` and `neoeco-fastpath-cpu` scenario intent from the
   [prepared-client workflow](../../dev-client.md). The current NeoEco fixture
   and FastPath observation mixin reference legacy classes. Qualify the 20.5
   scenario against the real active CPU and its new dispatch accounting before
   using it as proof. Keep the legacy paths usable. First reproduce the gap
   with the 1.3.1 baseline: a real craft succeeds while the optional caller
   cannot bind and the status panel has no Crafting Time estimate.
3. Add the six-method Forge API and the necessary strictly scoped profiler
   completion path described in the design. Reuse the existing bridge for
   identity, normalization, plan estimates, suspension and cleanup. Use
   production JAR inspection or reflection to verify the exact public static
   signatures, including the `OptionalDouble` return type. Run the existing
   integration catalog contract tests.
4. Add focused tests for two same-key CPUs on one network, including a receipt
   when only the other CPU has a pending sample. Cover partial returns,
   repeated or excess reports after the scoped amount is exhausted, finish
   and cancellation, and disabling while a job is live. Check that a fresh
   job starts cleanly after each transition. Bind evidence to the same PR head.
5. On 20.5.10, run a real normal and FastPath craft. Observe dispatch, receipt
   and completion independently of the profiler. Verify that a subsequent
   job uses learned TTC and the NeoEco status page shows the synced estimate
   for the selected running CPU. Verify pause/resume, cancellation/new job,
   post-restart empty live state, disabled integration, and an exact order
   without TTC activity. Repeat a focused real craft and status check on
   20.5.9. Retain exact addon/loader artifacts and the reviewed status capture.
6. Run a focused Forge legacy craft on each retained contract, using 20.3.0
   and 20.4.2, and a Forge base-only craft. Check the 1.21.1 NeoForge
   `batched-int` selection and unchanged packaging using the catalog tests.
   Observe the documented [renderer quarantine](../../neoeco-renderer-crash/spec.md)
   when choosing NeoForge verification. Update the dependency and integration
   documentation with the versions actually qualified and link the evidence.

| Criterion | Evidence |
| --- | --- |
| NE760-1 | Exact production API binding, same-key isolation test, and real counted dispatch and receipt on both stable releases. |
| NE760-2 | Reviewed NeoEco status evidence for known/unknown estimates and job termination, plus a fresh server session without restored active TTC. |
| NE760-3 | Settings and lifecycle checks, pause/resume and an exact order whose native behavior still works. |
| NE760-4 | Legacy Forge runs, NeoForge catalog/packaging checks, addon-absent Forge run, and unchanged version metadata. |

The official releases and prepared Forge launch workflow are available. The
20.5-specific fixture and resolved dependency graph still need preparation in
the implementation branch. Keep unresolved runtime evidence separate from
source findings and from the docs PR's checks.

[metadata]: https://github.com/DancingSnow0517/NeoECOAEExtension/blob/f7dd0efeba093a80621c8564bb8e8082c53fdcc7/src/main/resources/META-INF/mods.toml#L34-L75
