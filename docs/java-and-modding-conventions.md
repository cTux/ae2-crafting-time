# Java and Minecraft Modding Conventions

These are the Java and Minecraft modding conventions we should adopt in this
repository. They combine current primary-source guidance with the boundaries
that already exist in AE2 Crafting Time.

The goal is consistent, safe code. It is not a reason to reformat unrelated
files or add tooling before it solves a real problem.

## Java conventions

| Priority | Convention | How it applies here |
| --- | --- | --- |
| Required | Use UTF-8, spaces only, braces for every control block, and one statement per line. | UTF-8 is already configured. Keep the existing four-space indentation instead of reformatting the repository to Google's two-space style. |
| Required | Keep lines near 100 characters and treat 120 as the normal upper bound. | Wrap code when it improves readability. Long imports, URLs, generated signatures, and identifiers are reasonable exceptions. |
| Required | Use explicit, sorted imports and remove unused imports. Never use wildcard imports. | Automate this when a formatter is added instead of spending review time on import order. |
| Required | Use `UpperCamelCase` for types, `lowerCamelCase` for methods and fields, and `UPPER_SNAKE_CASE` only for deeply immutable constants. | Follow the [Google Java naming rules](https://google.github.io/styleguide/javaguide.html#s5-naming). |
| Required | Use the narrowest useful visibility. Make implementation classes `final` unless extension is intentional. | Do not create speculative public APIs, interfaces, factories, or extension points. |
| Required | Use records for genuine immutable data carriers. | Records are available in the shared Java 17 baseline. Do not use them when a Minecraft, loader, serialization, or mixin API requires another class shape. See [Oracle's record documentation](https://docs.oracle.com/en/java/javase/17/language/records.html). |
| Required | Use `var` only when the type remains obvious at the declaration. | Avoid it when the initializer hides an important Minecraft, AE2, or loader type. |
| Required | Validate constructor and public-boundary invariants immediately. | Prefer a clear `IllegalArgumentException`, `Objects.requireNonNull`, or an existing domain type over a delayed failure. |
| Required | Catch the narrowest exception and preserve the cause. | Never silently swallow failures unless an operation is explicitly best-effort. Preserve interruption and use [try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) for `AutoCloseable` resources. |
| Required | Use SLF4J parameterized messages. | Use `LOGGER.debug("Loaded {} samples", count)`, not string concatenation or `System.out`. Do not add a logging backend. See the [SLF4J manual](https://www.slf4j.org/manual.html). |
| Required | Comments explain why, invariants, compatibility constraints, or non-obvious API behavior. | Do not narrate straightforward code. Add Javadoc for useful contracts, not every private method. |
| Required | Do not use preview Java features. | Shared code stays Java 17-compatible. Code used only by Minecraft 1.21.1 may use Java 21, and code used only by Minecraft 26.1.2 may use Java 25. |
| Required | Test observable behavior and boundaries instead of implementation details. | Use [parameterized tests](https://docs.junit.org/5.14.1/writing-tests/parameterized-classes-and-tests.html) when one contract has several inputs. Changed executable behavior keeps 100% line and branch coverage. |

## Minecraft mod conventions

| Priority | Convention | How it applies here |
| --- | --- | --- |
| Critical | The logical server owns gameplay state; the client owns input and presentation. | Keep using packets in singleplayer. The client must not read profiler or world-save state directly. See [NeoForge's sides guide](https://docs.neoforged.net/docs/1.21.1/concepts/sides/). |
| Critical | Keep `net.minecraft.client` references out of common and server-loadable classes. | Isolate client entrypoints and rendering code. Any client/common boundary change needs a dedicated-server startup smoke test. |
| Critical | Treat every client-to-server packet as hostile input. | Bound collection and string sizes. Validate IDs, state, sender, permissions, and rate limits before mutation. Never load arbitrary chunks from client-provided positions. See [Forge's packet security guidance](https://docs.minecraftforge.net/en/1.20.x/networking/simpleimpl/). |
| Critical | Touch game and world state only on the correct main thread. | Use `enqueueWork` or the loader's documented payload context. Decode first, then mutate state on the game thread. |
| Critical | Treat packet and saved-data layouts as compatibility contracts. | Version incompatible changes, migrate intentionally, update every affected loader, and test maximum and malformed inputs. |
| Required | Prefer loader or mod events over mixins when an equivalent event exists. | Fabric notes that events commonly replace mixins. See [Fabric's event guide](https://docs.fabricmc.net/develop/events). |
| Required | When a mixin is necessary, use the least invasive injector. | Prefer `@Inject` or argument modification over `@Redirect`. Avoid `@Overwrite`; Sponge documents its conflict and maintenance risks in the [Mixin overwrite guide](https://github.com/SpongePowered/Mixin/wiki/Introduction-to-Mixins---Overwriting-Methods). |
| Required | Mandatory mixins fail when their target disappears. | Keep the normal nonzero `require`. Use `require = 0` only for genuinely optional or version-tolerant integrations, with presence-and-absence smoke coverage. |
| Required | Choose stable and precisely constrained injection points. | Prefer semantic calls or `HEAD`/`RETURN` when correct. Document why an ordinal or fragile target is safe, then verify it when updating AE2 or Minecraft. |
| Required | Use namespaced IDs and loader-supported deferred registration. | Do not query registries while registration is running. See [NeoForge's registry guide](https://docs.neoforged.net/docs/1.21.1/concepts/registries/). |
| Required | Share code at the highest compatible source-set level. | Put pure Java in `shared/src/main/java`, common Minecraft code in `mcCommon`, API-generation code in `mc1201` or `mc2612`, NeoForge-shared code in `neoforge`, and loader glue in the matching version module. |
| Required | Keep optional integrations optional. | Prefer compile-only dependencies and string-target mixins where appropriate. An absent optional mod must not prevent class loading. |
| Required | Avoid expensive work in ticks, rendering, and packet handlers. | Do not repeatedly scan registries, allocate unbounded collections, perform disk I/O, block, or spam logs. Add caching only after measuring a need. |
| Required | Use generated data only when the amount of data justifies it. | Do not add a data-generation framework for a handful of static files. If content grows, enable strict validation. |
| Required | Test at the nearest useful boundary. | Pure decisions get unit tests; codecs and NBT get round-trip and limit tests; client separation, mixins, and optional integrations get prepared-client or dedicated-server smoke tests. |

## Adoption plan

| Order | Change | Decision |
| ---: | --- | --- |
| 1 | Keep this page as the code-convention reference. | Link to it from contributor instructions when those instructions next need editing. Do not duplicate the rules in several files. |
| 2 | Add Spotless with `googleJavaFormat().aosp()`, unused-import cleanup, wildcard-import rejection, and `ratchetFrom 'origin/master'`. | Ratcheting formats changed files without a repository-wide formatting commit. Spotless documents this workflow in its [Gradle plugin guide](https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md). |
| 3 | Run `spotlessCheck` in pull-request CI. | Ensure checkout history contains `origin/master`, which the formatting ratchet needs. |
| 4 | Review changes for side ownership, packet trust, mixin stability, compatibility boundaries, and source-set placement. | These Minecraft-specific checks matter more than a large generic lint configuration. |
| 5 | Audit existing `require = 0` and `@Redirect` uses separately. | Do not rewrite them mechanically. Confirm which hooks are optional and which have the required smoke coverage. |
| Skip initially | Checkstyle, PMD, Error Prone, generated Javadoc, and full-repository formatting. | Add another tool only when repeated defects show that formatting, compiler warnings, tests, and review rules are not enough. |

## Current baseline

The repository currently has 165 Java files, no wildcard imports, no
tab-indented Java lines, and 32 lines over 120 characters. CI runs Gradle tests
and JaCoCo coverage, but it does not enforce source formatting. The first useful
automation change is therefore a ratcheted formatter, not a mass rewrite.
