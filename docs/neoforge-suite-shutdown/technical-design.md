# NeoForge Suite Shutdown Technical Design

This design restores the final shared-world fixture through the same path used
between cases. It adds no NeoForge-specific shutdown hook and does not weaken
the launcher's exit validation.

## Root cause

`TestDriverRuntime.tick()` currently has two completion paths. When
`SuiteProgress.finish(...)` reports another case, the runtime sets `switching`
and `switchCase()` restores the captured `SuiteFixture` on the server thread.
When it reports the final case, the runtime sets `finished` and calls
`Minecraft.stop()` immediately.

That asymmetry leaves the final scenario's blocks, block entities, entities,
jobs, and addon lifecycle state in the loaded world while the integrated server
saves and unloads chunks. In the retained failure, the final graph was
AdvancedAE and both thread samples showed the server repeatedly scheduling
chunk unloads. The observed stack does not establish an AdvancedAE defect; the
driver's missing final cleanup is the shared lifecycle defect it can correct.

## Selected behavior

Keep the existing `SuiteFixture.restore(...)` operation and its server-thread
future. Add one final-cleanup state to `TestDriverRuntime`:

```text
final case PASS
  -> atomically write completed suite progress
  -> enter the existing schema-2 fixture reset
  -> await restore future and two server ticks
  -> clear the existing client caches and observations
  -> request normal Minecraft shutdown
```

Intermediate transitions still construct the next `CraftPlanScenario` after
cleanup. Final cleanup stops instead. Schema-1 suites retain their current
world-close behavior because they do not reuse a loaded fixture.

The restore timeout and exception handling remain unchanged. A failure before
the shutdown request still aborts the runtime and cannot produce a synthetic
successful exit.

## Test boundary

Keep the lifecycle decision small and Minecraft-free so `SuitePlanTest` can
cover both outcomes: another case continues after reset, while the final
successful schema-2 case resets and then stops. Runtime verification remains
the authoritative boundary for server-thread restore, addon teardown, world
save, and native process exit.

## Requirement coverage

| Requirements | Design seam |
| --- | --- |
| NSS-01, NSS-02 | Existing schema-2 reset future plus final-cleanup state |
| NSS-03 | Existing reset deadline and exception path |
| NSS-04 | Final-case branch only; schema-1 and single-case paths unchanged |
| NSS-05 | Full compatible NeoForge 1.21.1 smoke campaign |

## Rejected alternatives

- Killing the client after the suite would preserve the hang and misreport the
  required native exit.
- Adding an AdvancedAE-specific teardown would patch one final graph while
  leaving every other final case outside the shared fixture contract.
- Saving and reloading the world would add work without improving the existing
  bounded restore operation.
