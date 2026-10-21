package com.ctux.ae2craftingtime.testdriver;

import com.ctux.ae2craftingtime.core.RequesterTtcLayout;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.net.URLClassLoader;
import java.math.BigInteger;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDriverCoreTest {
    @Test
    void trinityPhysicalSuffixRequiresTwoRealProductsAndKeepsOneUnitIsolated() {
        assertFalse(TrinityPhysicalSuffix.twoGlassReady(1, 0));
        assertTrue(TrinityPhysicalSuffix.twoGlassReady(2, 0));
        assertFalse(TrinityPhysicalSuffix.twoGlassReady(2, 1));
        assertEquals(TrinityPhysicalSuffix.Source.OUTPUT, TrinityPhysicalSuffix.source(0, 2, 0));
        assertEquals(TrinityPhysicalSuffix.Source.INPUT, TrinityPhysicalSuffix.source(1, 1, 0));
        assertThrows(IllegalStateException.class, () -> TrinityPhysicalSuffix.source(0, 2, 1));
        assertThrows(IllegalStateException.class, () -> TrinityPhysicalSuffix.source(-1, 3, 0));
        assertThrows(IllegalStateException.class, () -> TrinityPhysicalSuffix.source(2, 0, 0));
        assertThrows(IllegalStateException.class, () -> TrinityPhysicalSuffix.source(0, 1, 0));
        assertFalse(TrinityPhysicalSuffix.isolated(1, 0, 1));
        assertFalse(TrinityPhysicalSuffix.isolated(0, 1, 1));
        assertFalse(TrinityPhysicalSuffix.isolated(0, 0, 0));
        assertTrue(TrinityPhysicalSuffix.isolated(0, 0, 1));
    }

    @Test
    void trinityCoordinatorUsesNativeNameRatherThanWorkerOrMenuSerialZero() {
        var cards = List.of(
                new UiSnapshot.CpuCard(3, "Trinity Data Core #0", null, 0, 0, false,
                        null, null, null, null, null, null),
                new UiSnapshot.CpuCard(1, "CPU #1", null, 0, 0, false,
                        null, null, null, null, null, null),
                new UiSnapshot.CpuCard(2, "CPU #2", null, 0, 0, false,
                        null, null, null, null, null, null),
                new UiSnapshot.CpuCard(4, "Trinity Data Core #1", "minecraft:glass", 1, 0, true,
                        null, null, null, null, null, null));
        assertEquals(3, TrinityStatusCoordinator.card(cards, "Trinity Data Core #0").serial());
        assertNull(TrinityStatusCoordinator.card(cards, "other coordinator"));
        assertNull(TrinityStatusCoordinator.card(cards, ""));
        assertNull(TrinityStatusCoordinator.card(cards, null));
        assertNull(TrinityStatusCoordinator.card(List.of(cards.get(3)), "Trinity Data Core #1"));
        assertThrows(IllegalStateException.class, () -> TrinityStatusCoordinator.card(
                List.of(cards.get(0), cards.get(0)), "Trinity Data Core #0"));
    }

    @Test
    void trinityAlreadySelectedWorkerWaitsForFreshNativeStatusWithSameIdentityContext() {
        var freshness = new TrinityActiveStatusFreshness();
        var job = "e407354d-de75-4336-9a02-df12ef448fa0";
        var first = new UiSnapshot.StatusObservation(101, "native", 1, 1, 1, 0, 0,
                4, job, "minecraft:glass", 1L);
        var next = new UiSnapshot.StatusObservation(102, "native", 2, 1, 1, 0, 0,
                4, job, "minecraft:glass", 2L);
        assertFalse(freshness.observe(null, true, 4, job, "minecraft:glass", 10));
        assertFalse(freshness.observe(first, false, 4, job, "minecraft:glass", 10));
        assertFalse(freshness.observe(first, true, 3, job, "minecraft:glass", 10));
        assertFalse(freshness.observe(first, true, 4, "other-job", "minecraft:glass", 10));
        assertFalse(freshness.observe(first, true, 4, job, "minecraft:stone", 10));
        assertFalse(freshness.observe(new UiSnapshot.StatusObservation(101, "synthetic", 1, 1, 1,
                0, 0, 4, job, "minecraft:glass", 1L), true, 4, job, "minecraft:glass", 10));
        assertNull(freshness.baselineIdentity());

        assertFalse(freshness.observe(first, true, 4, job, "minecraft:glass", 10));
        assertEquals(101, freshness.baselineIdentity());
        assertNull(freshness.accepted());
        assertFalse(freshness.observe(first, true, 4, job, "minecraft:glass", 11));
        assertFalse(freshness.observe(next, true, 4, job, "minecraft:glass", 10));
        assertFalse(freshness.observe(new UiSnapshot.StatusObservation(103, "native", 2, 1, 1,
                0, 0, 4, "new-job", "minecraft:glass", 2L), true, 4,
                "new-job", "minecraft:glass", 11));
        assertEquals(101, freshness.baselineIdentity());
        assertTrue(freshness.observe(next, true, 4, job, "minecraft:glass", 11));
        assertEquals(new TrinityActiveStatusFreshness.Evidence(101, 10, 102, 11,
                4, job, "minecraft:glass"), freshness.accepted());
        freshness.reset();
        assertNull(freshness.baselineIdentity());
        assertNull(freshness.accepted());
        assertFalse(freshness.observe(next, true, 4, job, "minecraft:glass", 12));
    }

    @Test
    void trinitySmokeCatalogueMatchesDriverChecksAndNativeScreenshots() throws Exception {
        var root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("settings.gradle"))) root = root.getParent();
        assertNotNull(root, "repository root");
        var catalogue = com.google.gson.JsonParser.parseString(Files.readString(
                root.resolve("scripts/ui-smoke-groups.json"))).getAsJsonObject();
        var cases = catalogue.getAsJsonObject("cases");
        var normal = "data-energistics-trinity-cpu";
        var counted = "data-energistics-trinity-counted-cpu";
        var cancel = "data-energistics-trinity-cancel-cpu";
        var global = "data-energistics-trinity-global-option-cpu";
        var provider = "data-energistics-trinity-provider-cpu";
        var admission = "data-energistics-trinity-admission-cpu";
        var reload = "data-energistics-trinity-reload-cpu";
        var group = new ArrayList<String>();
        for (var entry : catalogue.getAsJsonObject("groups").getAsJsonArray("data-energistics-trinity"))
            group.add(entry.getAsString());
        assertEquals(List.of("data-energistics-cpu", normal, counted, cancel, global, provider, admission), group);
        for (var scenario : List.of(normal, counted, cancel, global, provider, admission, reload)) {
            var contract = cases.getAsJsonObject(scenario);
            assertNotNull(contract, scenario);
            var checks = new ArrayList<String>();
            for (var entry : contract.getAsJsonArray("checks")) checks.add(entry.getAsString());
            assertEquals(DriverResult.requiredChecks(scenario, false), checks, scenario);
            var screenshots = new ArrayList<String>();
            for (var entry : contract.getAsJsonArray("screenshots")) screenshots.add(entry.getAsString());
            var expected = new ArrayList<>(List.of("addon-native-plan.png", "trinity-selected-cpu-plan.png"));
            if (!scenario.equals(counted)) expected.addAll(List.of(
                    "trinity-active-status.png", "trinity-completed-status.png"));
            expected.addAll(List.of("trinity-plan-badge-off.png", "trinity-plan-badge-on.png",
                    scenario.replace("-cpu", "") + "-profiled-plan.png"));
            assertEquals(expected, screenshots, scenario);
        }
    }

    @Test
    void everyTrinityScenarioHasAnExplicitFixtureAndRequiredNativeChecks() {
        for (var scenario : List.of("data-energistics-trinity-cpu", "data-energistics-trinity-counted-cpu",
                "data-energistics-trinity-cancel-cpu", "data-energistics-trinity-global-option-cpu",
                "data-energistics-trinity-provider-cpu", "data-energistics-trinity-admission-cpu",
                "data-energistics-trinity-reload-cpu")) {
            assertTrue(AddonCpuFixture.supports(scenario), scenario);
            var checks = DriverResult.requiredChecks(scenario, false);
            assertTrue(checks.contains("job-accepted"), scenario);
            assertTrue(checks.contains("cpu-selected-visible"), scenario);
            assertTrue(checks.contains("profile-sample"), scenario);
            assertTrue(checks.contains("ttc-after-sample"), scenario);
        }
        assertTrue(DriverResult.requiredChecks("data-energistics-trinity-counted-cpu", false)
                .contains("returned-amount"));
        assertTrue(DriverResult.requiredChecks("data-energistics-trinity-reload-cpu", false)
                .contains("restored-graph-advanced"));
        assertFalse(AddonCpuFixture.supports("data-energistics-trinity-unknown-cpu"));
    }

    @Test
    void nativeCpuSelectionRequiresVisibleProofEvenBeforeTheCheckKeyExists() {
        var checks = new LinkedHashMap<String, Boolean>();
        assertTrue(CraftPlanScenario.needsVisibleCpuSelection(true, checks));
        assertFalse(CraftPlanScenario.needsVisibleCpuSelection(false, checks));
        checks.put("cpu-selected-visible", false);
        assertTrue(CraftPlanScenario.needsVisibleCpuSelection(true, checks));
        checks.put("cpu-selected-visible", true);
        assertFalse(CraftPlanScenario.needsVisibleCpuSelection(true, checks));
    }

    @Test
    void addonFlowAllowsResumeCheckpointAndFreshJobButRejectsShortcuts() {
        for (var path : List.of(
                List.of(ScenarioState.STARTING, ScenarioState.ADDON_CRAFT_SUBMITTED,
                        ScenarioState.QUIT_REQUESTED),
                List.of(ScenarioState.STARTING, ScenarioState.WORLD_READY, ScenarioState.TERMINAL_OPEN,
                        ScenarioState.PLAN_OPEN, ScenarioState.PLAN_STABLE, ScenarioState.ADDON_CPU_SELECTED,
                        ScenarioState.ADDON_CRAFT_SUBMITTED, ScenarioState.WORLD_READY,
                        ScenarioState.TERMINAL_OPEN, ScenarioState.PLAN_OPEN, ScenarioState.PLAN_STABLE,
                        ScenarioState.ADDON_CPU_SELECTED, ScenarioState.ADDON_CRAFT_SUBMITTED,
                        ScenarioState.ADDON_SAMPLE_RECORDED, ScenarioState.ADDON_PLAN_OPEN,
                        ScenarioState.RESULT_WRITTEN, ScenarioState.QUIT_REQUESTED))) {
            for (int i = 1; i < path.size(); i++)
                assertTrue(ScenarioFlow.allows(path.get(i - 1), path.get(i)),
                        path.get(i - 1) + " -> " + path.get(i));
        }
        assertFalse(ScenarioFlow.allows(ScenarioState.STARTING, ScenarioState.ADDON_PLAN_OPEN));
        assertFalse(ScenarioFlow.allows(ScenarioState.ADDON_CRAFT_SUBMITTED, ScenarioState.RESULT_WRITTEN));
        assertFalse(ScenarioFlow.allows(ScenarioState.QUIT_REQUESTED, ScenarioState.WORLD_READY));
    }

    @Test
    void freshAddonTransitionClearsEveryFutureAndRearmsSelection() throws Exception {
        var scenario = new CraftPlanScenario(null,
                new DriverOptions("craft-plan", "compatible", "test", Path.of("."), false), "driver");
        var state = CraftPlanScenario.class.getDeclaredField("state");
        state.setAccessible(true);
        state.set(scenario, ScenarioState.ADDON_CRAFT_SUBMITTED);
        for (var name : List.of("cpuCheck", "nativePlanCheck", "submitCheck", "sampleCheck",
                "restartCheck", "resumeCheck", "cancelCheck", "providerCheck")) {
            var field = CraftPlanScenario.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(scenario, CompletableFuture.completedFuture(null));
        }
        var nativeSelection = CraftPlanScenario.class.getDeclaredField("clientCpuSelectionSent");
        nativeSelection.setAccessible(true);
        nativeSelection.setBoolean(scenario, true);
        var advance = CraftPlanScenario.class.getDeclaredMethod("advance", ScenarioState.class);
        advance.setAccessible(true);
        advance.invoke(scenario, ScenarioState.WORLD_READY);
        assertEquals(ScenarioState.WORLD_READY, state.get(scenario));
        for (var name : List.of("cpuCheck", "nativePlanCheck", "submitCheck", "sampleCheck",
                "restartCheck", "resumeCheck", "cancelCheck", "providerCheck")) {
            var field = CraftPlanScenario.class.getDeclaredField(name);
            field.setAccessible(true);
            assertNull(field.get(scenario), name + " would reuse the previous job's server result");
        }
        assertFalse(nativeSelection.getBoolean(scenario), "a fresh job must send its native CPU action again");
        var checks = new LinkedHashMap<String, Boolean>();
        checks.put("cpu-selected", true);
        checks.put("cpu-selected-visible", true);
        checks.put("profile-sample", true);
        CraftPlanScenario.resetAddonSelectionChecks(checks);
        assertFalse(checks.get("cpu-selected"));
        assertFalse(checks.get("cpu-selected-visible"));
        assertTrue(checks.get("profile-sample"));

        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream(
                "/com/ctux/ae2craftingtime/testdriver/CraftPlanScenario.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        assertTrue(node.methods.stream().anyMatch(method -> {
            var calls = java.util.Arrays.stream(method.instructions.toArray())
                    .filter(MethodInsnNode.class::isInstance).map(MethodInsnNode.class::cast).toList();
            int watch = java.util.stream.IntStream.range(0, calls.size())
                    .filter(i -> calls.get(i).owner.equals("com/ctux/ae2craftingtime/testdriver/DispatchObservation")
                            && calls.get(i).name.equals("watch")).findFirst().orElse(-1);
            int start = java.util.stream.IntStream.range(0, calls.size())
                    .filter(i -> calls.get(i).owner.equals("com/ctux/ae2craftingtime/testdriver/AddonCpuFixture")
                            && calls.get(i).name.equals("startCraft")).findFirst().orElse(-1);
            return watch >= 0 && start > watch;
        }), "each submission must rearm the dispatch watcher before native startJob");
    }

    @Test
    void addonPassRequiresNativeTerminalWhenFixtureOptsInAndRunnerUsesTheGate() throws Exception {
        var terminal = new AtomicInteger();
        class Fixture extends AddonCpuFixture<Object> {
            private final boolean nativeRequired;
            Fixture(boolean nativeRequired) { this.nativeRequired = nativeRequired; }
            @Override protected Object place(net.minecraft.server.level.ServerPlayer player, FixtureMarker marker) { return null; }
            @Override protected boolean finish(net.minecraft.server.level.ServerPlayer player, Object placement) { return true; }
            @Override protected appeng.api.networking.crafting.ICraftingCPU cpu(
                    net.minecraft.server.level.ServerPlayer player, Object placement,
                    appeng.api.networking.IGrid grid) { return null; }
            @Override boolean dispatchReady(DispatchObservation.Snapshot snapshot) {
                return snapshot.completedExactlyOnce();
            }
            @Override boolean requiresNativeCompletion() { return nativeRequired; }
            @Override boolean nativeCompleted() { return terminal.get() == 1; }
        }
        var successfulProfiler = new DispatchObservation.Snapshot(3, 3, 3, 1, 1,
                true, 0, "native-worker", 1);
        var incompleteProfiler = new DispatchObservation.Snapshot(3, 3, 2, 1, 1,
                true, 0, "native-worker", 1);
        var trinity = new Fixture(true);
        assertFalse(trinity.readyForPass(successfulProfiler),
                "a completed profiler callback cannot stand in for native job terminal evidence");
        assertTrue(new Fixture(false).readyForPass(successfulProfiler),
                "existing addon fixtures keep their dispatch-based gate");
        terminal.set(1);
        assertTrue(trinity.readyForPass(successfulProfiler));
        assertFalse(trinity.readyForPass(incompleteProfiler));

        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream(
                "/com/ctux/ae2craftingtime/testdriver/CraftPlanScenario.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        var await = node.methods.stream().filter(method -> method.name.equals("awaitAddonSample"))
                .findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(await.instructions.toArray())
                .filter(MethodInsnNode.class::isInstance).map(MethodInsnNode.class::cast)
                .anyMatch(call -> call.owner.equals("com/ctux/ae2craftingtime/testdriver/AddonCpuFixture")
                        && call.name.equals("readyForPass")),
                "the real profiler-to-PASS path must consume the native terminal gate");
    }

    @Test
    void instantaneousCountedCraftNeedsCapturedNativeTerminalAndExactBookConservation() {
        var job = UUID.randomUUID();
        var other = UUID.randomUUID();
        assertTrue(new TrinityCountedCompletion(job, job, 1, true, true, true,
                false, true, false, 3, 3, 3, 1, 1).complete());
        assertTrue(new TrinityCountedCompletion(job, job, 1, true, true, true,
                true, false, false, 3, 3, 3, 1, 1).complete());
        assertFalse(new TrinityCountedCompletion(job, other, 1, true, true, true,
                false, true, false, 3, 3, 3, 1, 1).complete());
        assertFalse(new TrinityCountedCompletion(job, job, 0, true, true, true,
                false, true, false, 3, 3, 3, 1, 1).complete());
        assertFalse(new TrinityCountedCompletion(job, job, 1, false, true, true,
                false, true, false, 3, 3, 3, 1, 1).complete());
        assertFalse(new TrinityCountedCompletion(job, job, 1, true, false, true,
                false, true, false, 3, 3, 3, 1, 1).complete());
        assertFalse(new TrinityCountedCompletion(job, job, 1, true, true, false,
                false, true, false, 3, 3, 3, 1, 1).complete());
        assertFalse(new TrinityCountedCompletion(job, job, 1, true, true, true,
                false, false, false, 3, 3, 3, 1, 1).complete());
        assertFalse(new TrinityCountedCompletion(job, job, 1, true, true, true,
                false, true, true, 3, 3, 3, 1, 1).complete());
        assertFalse(new TrinityCountedCompletion(job, job, 1, true, true, true,
                false, true, false, 2, 3, 3, 1, 1).complete());
        assertFalse(new TrinityCountedCompletion(job, job, 1, true, true, true,
                false, true, false, 3, 2, 3, 1, 1).complete());
        assertFalse(new TrinityCountedCompletion(job, job, 1, true, true, true,
                false, true, false, 3, 3, 2, 1, 1).complete());
        assertFalse(new TrinityCountedCompletion(job, job, 1, true, true, true,
                false, true, false, 3, 3, 3, 1, 0).complete());
    }

    @Test
    void nativePlanRequiresExactRenderedScreenAndExecutableServerResult() throws Exception {
        var trinity = AddonCpuFixture.TRINITY_CONFIRM_SCREEN;
        var ae2 = "appeng.client.gui.me.crafting.CraftConfirmScreen";
        assertTrue(UiObservationStore.isNativeTrinityConfirm(trinity));
        assertFalse(UiObservationStore.isNativeTrinityConfirm(ae2));
        assertFalse(UiObservationStore.isNativeTrinityConfirm(trinity + "Other"));
        assertTrue(CraftPlanScenario.nativePlanReady(trinity, trinity, trinity, true, true));
        assertFalse(CraftPlanScenario.nativePlanReady(trinity, ae2, trinity, true, true));
        assertFalse(CraftPlanScenario.nativePlanReady(trinity, trinity, ae2, true, true));
        assertFalse(CraftPlanScenario.nativePlanReady(trinity, trinity, trinity, false, true));
        assertFalse(CraftPlanScenario.nativePlanReady(trinity, trinity, trinity, true, false));
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream(
                "/com/ctux/ae2craftingtime/testdriver/UiObservationStore.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        var begin = node.methods.stream().filter(method -> method.name.equals("begin")).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(begin.instructions.toArray())
                .filter(MethodInsnNode.class::isInstance).map(MethodInsnNode.class::cast)
                .anyMatch(call -> call.owner.equals("com/ctux/ae2craftingtime/testdriver/UiObservationStore")
                        && call.name.equals("isNativeTrinityConfirm")),
                "the render observation producer must admit the native screen before the frame gate");
    }

    @Test
    void addonSetupCheckpointShowsPlacementAndNativeStage() {
        var placement = new CompletableFuture<Object>();
        assertEquals("not-started", AddonCpuFixture.setupCheckpoint(null, "ready"));
        assertEquals("placing", AddonCpuFixture.setupCheckpoint(placement, "ready"));
        placement.complete(new Object());
        assertEquals("main-formation", AddonCpuFixture.setupCheckpoint(placement, "main-formation"));
    }

    @Test
    void trinitySetupStageDeadlineResetsOnProgressAndExpiresAfterFortyFiveSeconds() {
        var progress = new SetupStageProgress();
        assertEquals("placing", progress.stage());
        assertFalse(progress.observe("main-formation", 100));
        assertEquals("main-formation", progress.stage());
        long deadline = 100 + Duration.ofSeconds(45).toNanos();
        assertFalse(progress.observe("main-formation", deadline));
        assertFalse(progress.observe("cpu-formation", deadline + 1));
        assertEquals("cpu-formation", progress.stage());
        assertFalse(progress.observe("cpu-formation", deadline + Duration.ofSeconds(45).toNanos() + 1));
        assertEquals("Trinity fixture setup stalled at cpu-formation", assertThrows(
                IllegalStateException.class, () -> progress.observe("cpu-formation",
                        deadline + Duration.ofSeconds(45).toNanos() + 2)).getMessage());
    }

    @Test
    void trinityNativeCompletionRequiresSuccessfulExactJobAndInventory() {
        var job = UUID.randomUUID();
        var other = UUID.randomUUID();
        var one = BigInteger.ONE;
        var zero = BigInteger.ZERO;
        assertTrue(new TrinityNativeCompletion(job, job, true, true, true, true, false, false,
                1, 1, 1, one, one).complete());
        assertTrue(new TrinityNativeCompletion(job, job, true, true, true, false, true, false,
                1, 1, 1, one, one).complete()); // standalone link has no nexus to mark done
        assertFalse(new TrinityNativeCompletion(job, job, true, true, true, false, true, true,
                2, 2, 1, BigInteger.TWO, BigInteger.TWO).complete()); // partial output followed by cancellation
        assertFalse(new TrinityNativeCompletion(job, job, true, true, true, true, false, true,
                1, 1, 1, one, one).complete());
        assertFalse(new TrinityNativeCompletion(job, job, true, true, true, false, false, false,
                1, 1, 1, one, one).complete());
        assertFalse(new TrinityNativeCompletion(job, job, true, true, true, true, false, false,
                1, 0, 1, one, one).complete());
        assertFalse(new TrinityNativeCompletion(job, other, true, true, true, true, false, false,
                1, 1, 1, one, one).complete());
        assertFalse(new TrinityNativeCompletion(job, job, true, false, true, true, false, false,
                1, 1, 1, one, one).complete());
        assertFalse(new TrinityNativeCompletion(job, job, false, true, true, true, false, false,
                1, 1, 1, one, one).complete());
        assertFalse(new TrinityNativeCompletion(job, job, true, true, true, true, false, false,
                1, 1, 0, one, one).complete());
        assertFalse(new TrinityNativeCompletion(job, job, true, true, true, true, false, false,
                1, 1, 1, zero, one).complete());
        assertFalse(new TrinityNativeCompletion(job, job, true, true, true, true, false, false,
                1, 1, 1, one, zero).complete());
        assertFalse(new TrinityNativeCompletion(null, job, true, true, true, true, false, false,
                1, 1, 1, one, one).complete());
        assertFalse(new TrinityNativeCompletion(job, job, true, true, false, true, false, false,
                1, 1, 1, one, one).complete());
        assertFalse(new TrinityNativeCompletion(job, job, true, true, true, true, false, false,
                1, 1, 1, BigInteger.TWO, one).complete());
        assertFalse(new TrinityNativeCompletion(job, job, true, true, true, true, false, false,
                0, 0, 0, zero, zero).complete());
    }

    @Test
    void nativeStatusFreshnessRequiresReplacementSameSelectedJobAndLaterFrame() {
        String job = "00000000-0000-0000-0000-000000000001";
        var measured = new UiSnapshot.StatusObservation(2, "native", 10_000_000_000L,
                100, 50, 50, 0.5, 4, job, "minecraft:stone", 11_000_000_000L);
        assertFalse(StandardAe2Scenario.freshNativeStatus(null, 4, job, "minecraft:stone", 11, 10, true));
        assertFalse(StandardAe2Scenario.freshNativeStatus(new UiSnapshot.StatusObservation(1, "synthetic",
                10_000_000_000L, 100, 100, 0, 0, 4, job, "minecraft:stone", 0L),
                4, job, "minecraft:stone", 11, 10, true));
        assertFalse(StandardAe2Scenario.freshNativeStatus(measured, 4, job, "minecraft:stone", 11, 10, false));
        assertFalse(StandardAe2Scenario.freshNativeStatus(measured, 4, job, "minecraft:stone", 10, 10, true));
        assertFalse(StandardAe2Scenario.freshNativeStatus(measured, 5, job, "minecraft:stone", 11, 10, true));
        assertFalse(StandardAe2Scenario.freshNativeStatus(measured, 4, null, "minecraft:stone", 11, 10, true));
        assertFalse(StandardAe2Scenario.freshNativeStatus(measured, 4, "", "minecraft:stone", 11, 10, true));
        assertFalse(StandardAe2Scenario.freshNativeStatus(measured, 4,
                "00000000-0000-0000-0000-000000000002", "minecraft:stone", 11, 10, true));
        assertFalse(StandardAe2Scenario.freshNativeStatus(measured, 4, job, null, 11, 10, true));
        assertFalse(StandardAe2Scenario.freshNativeStatus(measured, 4, job, "minecraft:glass", 11, 10, true));
        assertFalse(StandardAe2Scenario.freshNativeStatus(new UiSnapshot.StatusObservation(2, "native",
                10_000_000_000L, 100, 50, 50, 0.5, null, job, "minecraft:stone", 11_000_000_000L),
                4, job, "minecraft:stone", 11, 10, true));
        assertFalse(StandardAe2Scenario.freshNativeStatus(new UiSnapshot.StatusObservation(2, "native",
                10_000_000_000L, 100, 50, 50, 0.5, 4, null, "minecraft:stone", 11_000_000_000L),
                4, job, "minecraft:stone", 11, 10, true));
        assertFalse(StandardAe2Scenario.freshNativeStatus(new UiSnapshot.StatusObservation(2, "native",
                10_000_000_000L, 100, 50, 50, 0.5, 4, job, "minecraft:stone", null),
                4, job, "minecraft:stone", 11, 10, true));
        assertTrue(StandardAe2Scenario.freshNativeStatus(measured, 4, job, "minecraft:stone", 11, 10, true));
    }

    @Test
    void nativeStatusRefreshUsesMenuReopenInsteadOfCoalescedSelection() throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream(
                "/com/ctux/ae2craftingtime/testdriver/StandardAe2Scenario.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        var tick = node.methods.stream().filter(method -> method.name.equals("tick")).findFirst().orElseThrow();
        var instructions = java.util.Arrays.asList(tick.instructions.toArray());
        var calls = instructions.stream().filter(MethodInsnNode.class::isInstance)
                .map(MethodInsnNode.class::cast).toList();
        assertEquals(0, calls.stream().filter(call -> call.name.equals("selectCpu")).count(),
                "selecting away and back in one client tick can coalesce before native status refresh");
        assertTrue(calls.stream().filter(call -> call.name.equals("ae2craftingtime_test_driver$statusButton")).count() >= 3,
                "the refresh must reopen Status through AE2's real menu button");
        assertTrue(instructions.stream().filter(org.objectweb.asm.tree.LdcInsnNode.class::isInstance)
                .map(org.objectweb.asm.tree.LdcInsnNode.class::cast)
                .anyMatch(value -> "waiting-for-storage".equals(value.cst)));
        assertTrue(instructions.stream().filter(org.objectweb.asm.tree.LdcInsnNode.class::isInstance)
                .map(org.objectweb.asm.tree.LdcInsnNode.class::cast)
                .anyMatch(value -> "waiting-for-status".equals(value.cst)));
        int reopen = java.util.stream.IntStream.range(0, instructions.size())
                .filter(i -> instructions.get(i) instanceof org.objectweb.asm.tree.LdcInsnNode value
                        && "waiting-for-status".equals(value.cst)).findFirst().orElseThrow();
        int firstSnapshot = java.util.stream.IntStream.range(0, instructions.size())
                .filter(i -> instructions.get(i) instanceof MethodInsnNode call
                        && call.owner.equals("com/ctux/ae2craftingtime/testdriver/UiObservationStore")
                        && call.name.equals("latest")).findFirst().orElseThrow();
        assertTrue(reopen < firstSnapshot,
                "storage screens have no observation frame, so refresh navigation must precede the frame gate");
    }

    @Test
    void nativeEtaAssertionsUsePositiveElapsedWithZeroAndMeasuredProgress() {
        var zeroInput = new UiSnapshot.StatusObservation(1, "synthetic", 10_000_000_000L,
                100, 100, 0, 0, null, null, null, null);
        var measuredInput = new UiSnapshot.StatusObservation(2, "synthetic", 10_000_000_000L,
                100, 50, 50, 0.5, null, null, null, null);
        var format = "mm:ss";
        assertTrue(StandardAe2Scenario.nativeEtaMatches(zeroInput, "Status - 16:40", "Status", format));
        assertTrue(StandardAe2Scenario.nativeEtaMatches(measuredInput, "Status - 00:10", "Status", format));
        assertFalse(StandardAe2Scenario.nativeEtaMatches(null, "Status", "Status", format));
        assertFalse(StandardAe2Scenario.nativeEtaMatches(zeroInput, null, "Status", format));
        assertFalse(StandardAe2Scenario.nativeEtaMatches(zeroInput, "Status - wrong", "Status", format));
        assertFalse(StandardAe2Scenario.nativeEtaMatches(new UiSnapshot.StatusObservation(3, "synthetic",
                0, 100, 100, 0, 0, null, null, null, null), "Status", "Status", format));
    }

    @Test
    void statusRelaunchParksOnceBeforeEachRenderedCapture(@TempDir Path output) {
        var scenario = new StandardAe2Scenario("standard-plan-controls", "world", output, false);
        var moves = new ArrayList<String>();
        BiConsumer<Integer, Integer> moveMouse = (x, y) -> moves.add(x + "," + y);

        assertFalse(scenario.statusCaptureReady(1, 10, moveMouse));
        assertFalse(scenario.statusCaptureReady(1, 10, moveMouse));
        assertTrue(scenario.statusCaptureReady(1, 11, moveMouse));
        assertFalse(scenario.statusCaptureReady(2, 12, moveMouse));
        assertFalse(scenario.statusCaptureReady(2, 12, moveMouse));
        assertTrue(scenario.statusCaptureReady(2, 13, moveMouse));
        assertEquals(List.of("0,0", "0,0"), moves);
    }

    @Test
    void compactHoverRestoresOptionsOnCompletionFailureCancellationAndDoubleClose() {
        var feature = com.ctux.ae2craftingtime.core.OptionFeature.COMPACT_HOVER_NUMBERS;
        var options = com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime.current().features();
        boolean initial = options.enabled(feature);
        try {
            for (boolean original : new boolean[] {false, true}) {
                for (String outcome : List.of("success", "failed-capture", "cancellation")) {
                    options.setEnabled(feature, original);
                    var scenario = new CompactHoverNumbersScenario();
                    try {
                        options.setEnabled(feature, !original);
                        if (!outcome.equals("success")) throw new IllegalStateException(outcome);
                    } catch (IllegalStateException expected) {
                        assertEquals(outcome, expected.getMessage());
                    } finally {
                        scenario.close();
                    }
                    assertEquals(original, options.enabled(feature));
                    scenario.close();
                    assertEquals(original, options.enabled(feature));
                }
            }
            assertTrue(StandardAe2Scenario.supports("compact-hover-numbers"));
            assertEquals(CompactHoverNumbersScenario.CHECKS, StandardAe2Scenario.CHECKS.get("compact-hover-numbers"));
        } finally {
            options.setEnabled(feature, initial);
        }
    }

    @Test
    void fullChatContractRejectsWrongNodeAmountRateAndMissingFields() {
        var timed = Component.translatable("text.ae2craftingtime.chat.summary", "minecraft:stone", 1, "5ms")
                .append(Component.translatable("text.ae2craftingtime.chat.details.ms", 1, "item", "5", "item", "5", "200", "items"));
        assertTrue(CraftingTreeScenario.fullChatRates(timed, "minecraft:stone", 1, "0.2", "200"));
        assertFalse(CraftingTreeScenario.fullChatRates(timed, "minecraft:stone", 1, "0.2", "20"));
        var malformed = Component.translatable("text.ae2craftingtime.chat.summary", "minecraft:stone", 1, "5ms")
                .append(Component.translatable("text.ae2craftingtime.chat.details.ms", 1, "item", "5"));
        assertFalse(CraftingTreeScenario.fullChatRates(malformed, "minecraft:stone", 1, "0.2", "200"));
        var details = Component.translatable("text.ae2craftingtime.chat.details.rate.ms", 1, "0.123456789",
                Component.literal("items"), "2.46913578", Component.literal("items"));
        var summary = Component.translatable("text.ae2craftingtime.chat.summary", "minecraft:stone",
                9_007_199_254_740_993L, "~1s").append(" | ").append(details);
        assertTrue(CraftingTreeScenario.fullChatRates(summary, "minecraft:stone", 9_007_199_254_740_993L,
                "0.123456789", "2.46913578"));
        assertTrue(CraftingTreeScenario.fullChatRates(Component.Serializer.fromJson(Component.Serializer.toJson(summary)),
                "minecraft:stone", 9_007_199_254_740_993L, "0.123456789", "2.46913578"));
        assertTrue(CraftingTreeScenario.fullChatRates(Component.translatable("chat.type.text", "player", summary),
                "minecraft:stone", 9_007_199_254_740_993L, "0.123456789", "2.46913578"));
        assertFalse(CraftingTreeScenario.fullChatRates(summary, "minecraft:glass", 9_007_199_254_740_993L,
                "0.123456789", "2.46913578"));
        assertFalse(CraftingTreeScenario.fullChatRates(summary, "minecraft:stone", 9_007_199_254_740_992L,
                "0.123456789", "2.46913578"));
        assertFalse(CraftingTreeScenario.fullChatRates(summary, "minecraft:stone", 9_007_199_254_740_993L,
                "0.12", "2.47"));
        assertFalse(CraftingTreeScenario.fullChatRates(Component.literal("stone x1: 2.47"), "minecraft:stone", 1,
                "0.123456789", "2.46913578"));
    }
    @Test
    void fixturePreparationSendsAbsoluteProviderFacingRotation() throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream(
                "/com/ctux/ae2craftingtime/testdriver/StandardCraftFixture.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        var prepare = node.methods.stream().filter(method -> method.name.equals("prepare")).findFirst().orElseThrow();
        var teleports = java.util.Arrays.stream(prepare.instructions.toArray())
                .filter(MethodInsnNode.class::isInstance).map(MethodInsnNode.class::cast)
                .filter(call -> call.name.startsWith("teleport")).toList();
        assertEquals(1, teleports.size());
        var teleport = teleports.get(0);
        assertEquals("net/minecraft/server/network/ServerGamePacketListenerImpl", teleport.owner);
        assertEquals("teleport", teleport.name);
        assertEquals("(DDDFF)V", teleport.desc, "position-only teleport preserves arbitrary spawn rotation");
        assertEquals(Opcodes.FCONST_2, teleport.getPrevious().getOpcode(), "fixed downward pitch");
        var yaw = assertInstanceOf(org.objectweb.asm.tree.LdcInsnNode.class, teleport.getPrevious().getPrevious());
        assertEquals(-53.13f, yaw.cst, "fixed yaw, not the player's previous orientation");
        assertEquals(-Math.toDegrees(Math.atan2(4, 3)), (float) yaw.cst, 0.01,
                "the camera must face the actual provider east of the terminal");
    }

    @Test
    void crazyPriorityProbeMutatesAndPersistsTheServerCluster() throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream(
                "/com/ctux/ae2craftingtime/testdriver/StandardCraftFixture.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        var method = node.methods.stream().filter(candidate -> candidate.name.equals("raiseCrazyPriority"))
                .findFirst().orElseThrow();
        var calls = java.util.Arrays.stream(method.instructions.toArray())
                .filter(MethodInsnNode.class::isInstance).map(MethodInsnNode.class::cast).toList();
        assertTrue(calls.stream().anyMatch(call -> call.name.equals("getMethod")));
        assertTrue(calls.stream().anyMatch(call -> call.name.equals("invoke")));
        assertTrue(calls.stream().anyMatch(call -> call.name.equals("markDirty")),
                "the live Crazy priority must be persisted on its server crafting cluster");
    }

    @Test
    void nativeCpuOrderKeepsCrazyPriorityAcrossAe2ModeTransitions() {
        var raw = List.of(3, 6, 4, 5, 8, 1);
        var prioritized = List.of(7, 3, 6, 4, 5, 8);
        assertTrue(CpuListTtcScenario.nativeOrderReady(false, raw, raw, 7));
        assertFalse(CpuListTtcScenario.nativeOrderReady(false, prioritized, raw, 7));
        assertTrue(CpuListTtcScenario.nativeOrderReady(true, prioritized, raw, 7));
        assertFalse(CpuListTtcScenario.nativeOrderReady(true, raw, raw, 7));
        assertFalse(CpuListTtcScenario.nativeOrderReady(true, List.of(), raw, 7));
    }

    @Test
    void cpuListFirstDrawProbeUsesTheStableTooltipCaller() throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream(
                "/com/ctux/ae2craftingtime/testdriver/mixin/CPUSelectionListObservationMixin.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        var method = node.methods.stream()
                .filter(candidate -> candidate.name.equals("ae2craftingtime_test_driver$inputBoundaries"))
                .findFirst().orElseThrow();
        var calls = java.util.Arrays.stream(method.instructions.toArray())
                .filter(MethodInsnNode.class::isInstance)
                .map(MethodInsnNode.class::cast)
                .filter(call -> call.getOpcode() == Opcodes.INVOKEVIRTUAL)
                .toList();
        assertEquals(1, calls.stream().filter(call -> call.name.equals("getTooltip")).count());
        assertEquals(2, calls.stream().filter(call -> call.name.equals("onMouseUp")).count());
        assertEquals(0, calls.stream().filter(call -> call.name.equals("hitTestCpu")).count(),
                "input probes must exercise stable production callers rather than the private helper");
    }

    @Test
    void recurrentConnectedControlAcceptsOnlyTheBoundedAlphaDirectory() throws Exception {
        var root = java.nio.file.Files.createTempDirectory("ae2ct-recurrent-control");
        var previous = System.getProperty("ae2craftingtime.test.control");
        try {
            System.setProperty("ae2craftingtime.test.control", root.toString());
            assertTrue(RecurrentPlanControl.directory("alpha").startsWith(root));
            assertThrows(IllegalArgumentException.class, () -> RecurrentPlanControl.directory("beta"));
            assertThrows(IllegalArgumentException.class, () -> RecurrentPlanControl.directory("other"));
        } finally {
            if (previous == null) System.clearProperty("ae2craftingtime.test.control");
            else System.setProperty("ae2craftingtime.test.control", previous);
        }
    }
    @Test
    void embeddedContextResolvesClassesThroughTheDriverLoader() throws Exception {
        var driverJar = Path.of(System.getProperty("ae2craftingtime.test.driverJar"));
        try (var driverLoader = new URLClassLoader(new java.net.URL[] {driverJar.toUri().toURL()},
                getClass().getClassLoader())) {
            TomcatURLStreamHandlerFactory.disable();
            var tomcat = new Tomcat();
            tomcat.setBaseDir(temporary.resolve("tomcat").toString());
            Context context = InteractiveMcpServer.addContext(tomcat, temporary.toString(), driverLoader);
            var inheritedParent = driverLoader.getParent();
            try {
                tomcat.start();
                var webappLoader = context.getLoader().getClassLoader();
                for (var suffix : List.of("authenticator.AuthenticatorBase", "valves.ValveBase",
                        "util.LifecycleBase", "session.ManagerBase")) {
                    var name = "com.ctux.ae2craftingtime.testdriver.lib.tomcat." + suffix;
                    var resource = name.replace('.', '/') + ".class";
                    assertThrows(ClassNotFoundException.class, () -> inheritedParent.loadClass(name));
                    assertNull(inheritedParent.getResource(resource));
                    assertSame(driverLoader, webappLoader.loadClass(name).getClassLoader());
                    try (var input = webappLoader.getResourceAsStream(resource)) {
                        assertTrue(input != null && input.readAllBytes().length > 0, resource);
                    }
                }
            } finally {
                tomcat.stop();
                tomcat.destroy();
            }
        }
    }

    @Test
    void driverProgressRetriesOnlyTransientWindowsReplaceFailures() throws Exception {
        var transientOutput = temporary.resolve("transient-progress");
        var transientAttempts = new AtomicInteger();
        new DriverProgress(transientOutput, "phase=ACTIVE", (source, target) -> {
            if (transientAttempts.incrementAndGet() <= 5) throw new AccessDeniedException(target.toString());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        });
        assertEquals(6, transientAttempts.get());
        assertEquals("phase=ACTIVE", com.google.gson.JsonParser.parseString(
                Files.readString(transientOutput.resolve("driver-progress.json")))
                .getAsJsonObject().get("checkpoint").getAsString());

        var persistentOutput = temporary.resolve("persistent-progress");
        var persistentAttempts = new AtomicInteger();
        assertThrows(AccessDeniedException.class, () -> new DriverProgress(persistentOutput, "phase=ACTIVE",
                (source, target) -> {
                    persistentAttempts.incrementAndGet();
                    throw new AccessDeniedException(target.toString());
                }));
        assertEquals(DriverProgress.MOVE_ATTEMPTS, persistentAttempts.get());
        var retained = Files.readString(persistentOutput.resolve("driver-progress.json.tmp"));
        assertTrue(retained.startsWith("{") && retained.endsWith("}"));
        assertEquals("phase=ACTIVE", com.google.gson.JsonParser.parseString(retained)
                .getAsJsonObject().get("checkpoint").getAsString());
    }

    @Test
    void cpuListControlRetriesOnlyTransientWindowsReplaceFailures() throws Exception {
        var values = new Properties();
        values.setProperty("phase", "first-grid");
        var transientState = temporary.resolve("transient-control/state.properties");
        var transientAttempts = new AtomicInteger();
        CpuListTtcControl.write(transientState, values, (source, target) -> {
            if (transientAttempts.incrementAndGet() <= 5) throw new AccessDeniedException(target.toString());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        });
        assertEquals(6, transientAttempts.get());
        var written = new Properties();
        try (var input = Files.newInputStream(transientState)) { written.load(input); }
        assertEquals("first-grid", written.getProperty("phase"));

        var persistentState = temporary.resolve("persistent-control/state.properties");
        var persistentAttempts = new AtomicInteger();
        var error = assertThrows(IllegalStateException.class, () -> CpuListTtcControl.write(
                persistentState, values, (source, target) -> {
                    persistentAttempts.incrementAndGet();
                    throw new AccessDeniedException(target.toString());
                }));
        assertTrue(error.getCause() instanceof AccessDeniedException);
        assertEquals(DriverProgress.MOVE_ATTEMPTS, persistentAttempts.get());
        var retained = new Properties();
        try (var input = Files.newInputStream(persistentState.resolveSibling("state.properties.tmp"))) {
            retained.load(input);
        }
        assertEquals("first-grid", retained.getProperty("phase"));
    }

    @Test
    void cpuListCompletionUsesTheUniqueFirstOutputAndPumpsItsProvider() {
        var jobs = StandardCraftFixture.cpuListJobs();
        assertEquals("minecraft:glass", jobs.get(0).itemId());
        assertEquals(4, jobs.get(0).amount());
        assertEquals(List.of("minecraft:smooth_stone", "minecraft:smooth_stone", "minecraft:smooth_stone",
                "minecraft:stone"), jobs.subList(1, 5).stream().map(StandardCraftFixture.CpuJob::itemId).toList());
        assertEquals(List.of(8L, 12L, 12L, 1L),
                jobs.subList(1, 5).stream().map(StandardCraftFixture.CpuJob::amount).toList());
        assertArrayEquals(new int[] { 4, 8, 12 }, StandardCraftFixture.pumpOffsets(true));
        assertArrayEquals(new int[] { 4, 8 }, StandardCraftFixture.pumpOffsets(false));
    }

    @Test
    void cpuListSelectionFollowsTheDerivedSerialWhenKnownCardsReorder() {
        var expected = new Rect(20, 30, 8, 6);
        var text = new UiSnapshot.ObservedText("ttc", "~1s", List.of(), new Rect(21, 31, 6, 4));
        var cards = List.of(
                new UiSnapshot.CpuCard(4, "Delta", "minecraft:smooth_stone", 16, 0, false,
                        null, null, null, null, null, new Rect(1, 2, 3, 4)),
                new UiSnapshot.CpuCard(2, "Beta", "minecraft:smooth_stone", 8, 0, false,
                        null, null, null, null, text, expected));
        assertEquals(expected, CpuListTtcScenario.selectionBounds(cards, 2, "minecraft:smooth_stone", 8));
        var unboxed = new UiSnapshot.CpuCard(2, "Beta", "minecraft:smooth_stone", 8, 0, false,
                null, null, null, null, text, null);
        assertEquals(text.bounds(), CpuListTtcScenario.selectionBounds(List.of(cards.get(0), unboxed), 2,
                "minecraft:smooth_stone", 8));
        assertThrows(IllegalStateException.class, () -> CpuListTtcScenario.selectionBounds(
                List.of(cards.get(0), new UiSnapshot.CpuCard(2, "Beta", "minecraft:glass", 8, 0, false,
                        null, null, null, null, text, expected)), 2, "minecraft:smooth_stone", 8));
        assertThrows(IllegalStateException.class, () -> CpuListTtcScenario.selectionBounds(
                List.of(unboxed), 2, "minecraft:smooth_stone", 9));
        assertThrows(IllegalStateException.class, () -> CpuListTtcScenario.selectionBounds(
                List.of(new UiSnapshot.CpuCard(2, "Beta", "minecraft:smooth_stone", 8, 0, false,
                        null, null, null, null, null, null)), 2, "minecraft:smooth_stone", 8));
        var missingBounds = cpuSnapshotWithText(new UiSnapshot.ObservedText("ttc", "~1s", List.of(), null), null);
        assertThrows(IllegalStateException.class, () -> CpuListTtcScenario.selectionBounds(
                missingBounds.cpuCards(), 4, "minecraft:smooth_stone", 8));
    }

    @Test
    void cpuListLayoutValidatesTtcTextAndOptionalBadge() {
        var text = new Rect(60, 20, 20, 8);
        var badge = new Rect(58, 18, 24, 12);
        CpuListTtcScenario.validateLayout(cpuSnapshot(text, null), false);
        CpuListTtcScenario.validateLayout(cpuSnapshot(text, badge), true);
        assertThrows(IllegalStateException.class, () -> CpuListTtcScenario.validateLayout(cpuSnapshot(text, null), true));
        assertThrows(IllegalStateException.class, () -> CpuListTtcScenario.validateLayout(cpuSnapshot(text, badge), false));
        var missingBounds = cpuSnapshotWithText(new UiSnapshot.ObservedText("ttc", "~1s", List.of(), null), null);
        assertThrows(IllegalStateException.class, () -> CpuListTtcScenario.validateLayout(missingBounds, false));
        for (var invalid : List.of(new Rect(105, 20, 20, 8), new Rect(20, 17, 20, 8),
                new Rect(15, 46, 20, 8), new Rect(65, 46, 20, 8))) {
            assertThrows(IllegalStateException.class,
                    () -> CpuListTtcScenario.validateLayout(cpuSnapshot(invalid, null), false));
        }
        for (var invalid : List.of(new Rect(58, 18, 54, 12), new Rect(58, 18, 12, 12),
                new Rect(12, 18, 70, 37), new Rect(58, 18, 24, 37), new Rect(12, 14, 70, 16))) {
            assertThrows(IllegalStateException.class,
                    () -> CpuListTtcScenario.validateLayout(cpuSnapshot(text, invalid), true));
        }
        var idle = cpuSnapshot(null, null);
        CpuListTtcScenario.validateLayout(idle, false);
        CpuListTtcScenario.validateLayout(idle, true);
    }

    @Test
    void cpuListScrollTracksTheActualFirstVisibleCard() {
        var cards = List.of(
                new UiSnapshot.CpuCard(7, "Alpha", "minecraft:glass", 4, 0, false,
                        null, null, null, null, null, null),
                new UiSnapshot.CpuCard(2, "Beta", "minecraft:smooth_stone", 8, 0, false,
                        null, null, null, null, null, null));
        assertEquals(7, CpuListTtcScenario.firstVisibleSerial(cards));
        assertTrue(CpuListTtcScenario.visibleWindowAt(List.of(3, 7, 2, 9), cards, 1));
        assertFalse(CpuListTtcScenario.visibleWindowAt(List.of(3, 7, 2, 9), cards, 0));
        assertFalse(CpuListTtcScenario.visibleWindowAt(List.of(3, 7), cards, 1));
        assertFalse(CpuListTtcScenario.visibleWindowAt(List.of(3, 7, 2), List.of(), 1));
    }

    @Test
    void cpuListTerminalOpenRetriesAnIgnoredReadyInteractionWithoutSpamming() {
        long attemptedAt = 1_000_000_000L;
        assertTrue(CpuListTtcScenario.shouldAttemptTerminalOpen(false, attemptedAt, attemptedAt));
        assertFalse(CpuListTtcScenario.shouldAttemptTerminalOpen(true, attemptedAt,
                attemptedAt + CpuListTtcScenario.OPEN_RETRY_NANOS - 1));
        assertTrue(CpuListTtcScenario.shouldAttemptTerminalOpen(true, attemptedAt,
                attemptedAt + CpuListTtcScenario.OPEN_RETRY_NANOS));
    }

    @Test
    void cpuListRequestCaptureRetainsBatchesAndDeduplicatesCoverage() {
        CpuTtcPacketControl.beginRequestCapture();
        CpuTtcPacketControl.observeRequest(new com.ctux.ae2craftingtime.mc1201.net.CpuTtcPacketCodec.Request(
                1, 2, 3, List.of(4, 5)));
        CpuTtcPacketControl.observeRequest(new com.ctux.ae2craftingtime.mc1201.net.CpuTtcPacketCodec.Request(
                1, 2, 4, List.of(5, 6)));
        var capture = CpuTtcPacketControl.requestCapture();
        assertEquals(List.of(List.of(4, 5), List.of(5, 6)),
                capture.batches().stream().map(CpuTtcPacketControl.ObservedRequest::serials).toList());
        assertEquals(List.of(4, 5, 6), capture.uniqueSerials());
    }

    @Test
    void cpuListRemovalTracksTheThirdFixtureCpuDespiteEqualJobAmounts() {
        var cards = List.of(
                new UiSnapshot.CpuCard(4, "Delta CPU", "minecraft:smooth_stone", 12, 0, false,
                        null, null, null, null, null, null),
                new UiSnapshot.CpuCard(5, "Gamma CPU", "minecraft:smooth_stone", 12, 0, false,
                        null, null, null, null, null, null),
                new UiSnapshot.CpuCard(2, "Beta", "minecraft:smooth_stone", 8, 0, false,
                        null, null, null, null, null, null));
        assertEquals(5, CpuListTtcScenario.removalSerial(cards));
        assertThrows(IllegalStateException.class, () -> CpuListTtcScenario.removalSerial(List.of()));
    }

    @Test
    void cpuListInputObservationsKeepTheFirstProbeAndResetBetweenCases() {
        CpuListInputControl.reset();
        assertNull(CpuListInputControl.noFirstDraw());
        CpuListInputControl.noFirstDraw(null);
        CpuListInputControl.noFirstDraw(3);
        assertTrue(CpuListInputControl.noFirstDraw());
        CpuListInputControl.armWheel(4);
        assertEquals(4, CpuListInputControl.wheelSerial());
        CpuListInputControl.wheelResult(4);
        assertEquals(4, CpuListInputControl.wheelResult());
        assertNull(CpuListInputControl.wheelSerial());
        CpuListInputControl.armStale(5);
        assertEquals(5, CpuListInputControl.staleSerial());
        CpuListInputControl.staleResult(null);
        assertTrue(CpuListInputControl.staleSuppressed());
        CpuListInputControl.armStale(5);
        CpuListInputControl.staleResult(6);
        assertFalse(CpuListInputControl.staleSuppressed());
        CpuListInputControl.reset();
        assertNull(CpuListInputControl.wheelResult());
        assertNull(CpuListInputControl.staleSerial());
        assertNull(CpuListInputControl.staleSuppressed());
        CpuListInputControl.noFirstDraw(3);
        assertFalse(CpuListInputControl.noFirstDraw());
        CpuListInputControl.reset();
    }

    @Test
    void blockedInputReservesExactlyTheProvidersPendingQueue() {
        for (long pending : new long[] { 0, 1, 64, 65, 128, 27 * 64 }) {
            long free = 0;
            for (int slot = 0; slot < 27; slot++) {
                int occupied = ProviderDispatchStatusScenario.occupiedSlotCount(slot, 27, pending);
                assertTrue(occupied >= 0 && occupied <= 64);
                free += 64 - occupied;
            }
            assertEquals(pending, free);
        }
        assertEquals(63, ProviderDispatchStatusScenario.occupiedSlotCount(1, 27, 65));
        assertThrows(IllegalArgumentException.class,
                () -> ProviderDispatchStatusScenario.occupiedSlotCount(0, 27, 27 * 64 + 1));
        assertThrows(IllegalArgumentException.class,
                () -> ProviderDispatchStatusScenario.occupiedSlotCount(0, 27, Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> ProviderDispatchStatusScenario.occupiedSlotCount(0, 27, -1));
    }

    @Test
    void standardResultCannotOmitAnyRequiredPlanStatusOrOutputCheck() {
        assertFalse(AddonCpuFixture.supports("standard-ae2"));
        assertEquals(12, StandardAe2Scenario.CHECKS.size());
        assertEquals(List.of("plan-on", "plan-off", "plan-restored", "status-on", "status-off", "status-restored"),
                StandardAe2Scenario.CHECKS.get("badge-background"));
        for (var entry : StandardAe2Scenario.CHECKS.entrySet()) {
            var scenario = entry.getKey();
            assertTrue(AddonCpuFixture.supports(scenario));
            assertNull(AddonCpuFixture.create(scenario));
            assertEquals(entry.getValue(), DriverResult.requiredChecks(scenario));
            assertTrue(new StandardAe2Scenario(scenario, "world", temporary, false).checkpoint().contains("PREPARE"));
            new StandardAe2Scenario(scenario, "world", temporary, false).releaseKeys();
            for (String missing : entry.getValue()) {
                var checks = new LinkedHashMap<String, Boolean>();
                entry.getValue().stream().filter(key -> !key.equals(missing)).forEach(key -> checks.put(key, true));
                assertThrows(IllegalArgumentException.class, () -> new DriverResult(1, true, "driver.jar",
                        "1.20.1-forge", "compatible", scenario, "PASS", "en_us", java.util.Map.of(), null, checks, List.of(), null));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> new StandardAe2Scenario("standard-ae2", "world", temporary, false));
    }

    @Test
    void chanceOutputScenarioUsesTheNativeDriver() {
        assertTrue(AddonCpuFixture.supports("chance-output-status"));
        assertNull(AddonCpuFixture.create("chance-output-status"));
        assertEquals(DriverResult.CHANCE_OUTPUT_CHECKS, DriverResult.requiredChecks("chance-output-status"));
    }

    @Test
    void cpuListScenarioRequiresEveryA5AndA7RuntimeTransition() {
        assertTrue(StandardAe2Scenario.supports("cpu-list-total-ttc"));
        assertEquals(CpuListTtcScenario.CHECKS, DriverResult.requiredChecks("cpu-list-total-ttc"));
        assertTrue(CpuListTtcScenario.CHECKS.containsAll(List.of("initial-distinct", "unknown-hidden", "idle-hidden",
                "badge-select", "selected-title", "tooltip", "scroll-down", "scroll-up", "partial", "stalled",
                "reordered", "finished", "cancelled", "replacement", "removed", "drop-expiry", "delayed-expiry",
                "close-reopen", "second-grid", "small-scale", "large-scale", "same-jvm-clear", "process-relaunch",
                "initial-mode", "cpu-sort-cycle", "raw-order", "stable-groups", "offscreen-promoted",
                "item-sort-cycle", "channel-fallback", "server-selection", "wheel-before-draw", "no-first-draw",
                "native-cancel", "stale-hit", "mode-switch-late", "ae2-expiry", "default-reopen",
                "background-coverage", "relaunch-progress", "reconnect", "layout")));
    }

    @Test
    void secondCpuGridObservesAllKnownIdentitiesAcrossItsTwoPages() {
        var scrollbar = new appeng.client.gui.widgets.Scrollbar();
        scrollbar.setRange(0, 1, 1);
        CpuListScrollControl.bind(scrollbar);
        var ttc = new UiSnapshot.ObservedText("text.ae2craftingtime.ttc", "~1:00", List.of("~1:00"),
                new Rect(0, 0, 1, 1));
        var all = java.util.stream.IntStream.range(0, 7)
                .mapToObj(serial -> new UiSnapshot.CpuCard(serial, "CPU", "minecraft:stone", 1, 1, false,
                        new Rect(0, 0, 1, 1), new Rect(0, 0, 1, 1), new Rect(0, 0, 1, 1),
                        new Rect(0, 0, 1, 1), serial == 0 || serial == 1 || serial == 5 || serial == 6 ? ttc : null, null))
                .toList();
        var observed = new java.util.LinkedHashSet<Integer>();
        var firstPage = new UiSnapshot("screen", "menu", new Rect(0, 0, 1, 1), 1, 1, 1, 1, 0,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), all.subList(0, 6));
        assertFalse(CpuListTtcScenario.secondScreenReady(firstPage, observed));
        assertEquals(java.util.Set.of(0, 1, 5), observed);
        assertEquals(1, scrollbar.getCurrentScroll());
        var secondPage = new UiSnapshot("screen", "menu", new Rect(0, 0, 1, 1), 1, 1, 1, 2, 1,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), all.subList(1, 7));
        assertFalse(CpuListTtcScenario.secondScreenReady(secondPage, observed));
        assertEquals(java.util.Set.of(0, 1, 5, 6), observed);
        assertEquals(0, scrollbar.getCurrentScroll());
        assertTrue(CpuListTtcScenario.secondScreenReady(firstPage, observed));
    }

    @Test
    void cpuListRequestEvidenceRequiresIncreasingSequencesAndOneSecondCadence() {
        var first = new CpuTtcPacketControl.ObservedRequest(1, 1_000, List.of(1));
        var next = new CpuTtcPacketControl.ObservedRequest(2, 2_000, List.of(2));
        assertTrue(CpuListTtcScenario.requestCadenceValid(List.of()));
        assertTrue(CpuListTtcScenario.requestCadenceValid(List.of(first)));
        assertTrue(CpuListTtcScenario.requestCadenceValid(List.of(first, next)));
        assertFalse(CpuListTtcScenario.requestCadenceValid(List.of(first,
                new CpuTtcPacketControl.ObservedRequest(1, 2_000, List.of(2)))));
        assertFalse(CpuListTtcScenario.requestCadenceValid(List.of(first,
                new CpuTtcPacketControl.ObservedRequest(2, 1_999, List.of(2)))));
    }

    @Test
    void nativeCancelOracleIgnoresOnlyTheSelectedCpu() {
        var selectedBefore = cpuState(1, "minecraft:stone", 8, true, 3);
        var selectedAfter = cpuState(1, null, 0, false, 0);
        var other = cpuState(2, "minecraft:glass", 4, true, 7);
        var before = serverState(selectedBefore, other);
        var after = serverState(selectedAfter, other);
        assertTrue(CpuListTtcScenario.otherJobsUnchanged(before, after, 1));
        assertFalse(CpuListTtcScenario.otherJobsUnchanged(null, after, 1));
        assertFalse(CpuListTtcScenario.otherJobsUnchanged(before, null, 1));
        assertFalse(CpuListTtcScenario.otherJobsUnchanged(before,
                serverState(selectedAfter, cpuState(2, "minecraft:glass", 5, true, 7)), 1));
    }

    @Test
    void lifecycleGuardRejectsRecursiveTicksAndReopensAfterExit() {
        var guard = new TestDriverLifecycleGuard();
        assertTrue(guard.enter());
        assertFalse(guard.enter());
        guard.exit();
        assertTrue(guard.enter());
    }

    @Test
    void containerCloseMixinRemapsTheMinecraftLifecycleMethod() throws Exception {
        var mixin = Class.forName("com.ctux.ae2craftingtime.mc1201.mixin.AbstractContainerScreenCpuTtcMixinSrg");
        var method = java.util.Arrays.stream(mixin.getDeclaredMethods())
                .filter(value -> value.getName().contains("clearCpuTtcOnClose"))
                .findFirst().orElseThrow();
        var inject = method.getAnnotation(org.spongepowered.asm.mixin.injection.Inject.class);
        assertTrue(inject.remap());
    }

    @Test
    void recurrentPlanSenderRemapsTheMinecraftBroadcastLifecycleMethod() throws Exception {
        var mixin = Class.forName("com.ctux.ae2craftingtime.mc1201.mixin.CraftConfirmMenuMixinSrg");
        var method = java.util.Arrays.stream(mixin.getDeclaredMethods())
                .filter(value -> value.getName().equals("send"))
                .findFirst().orElseThrow();
        var inject = method.getAnnotation(org.spongepowered.asm.mixin.injection.Inject.class);
        assertTrue(inject.remap(),
                "broadcastChanges must remap so the recurrent-plan sender targets production Minecraft bytecode");
    }

    @Test
    void reconnectPacketHoldTracksTheNewestOutstandingResponse() {
        var first = new com.ctux.ae2craftingtime.mc1201.net.CpuTtcPacketCodec.Snapshot(1, 1, List.of());
        var latest = new com.ctux.ae2craftingtime.mc1201.net.CpuTtcPacketCodec.Snapshot(1, 2, List.of());
        CpuTtcPacketControl.holdLatest();
        assertFalse(CpuTtcPacketControl.intercept(first));
        assertFalse(CpuTtcPacketControl.intercept(latest));
        assertEquals(2, CpuTtcPacketControl.heldSequence());
        CpuTtcPacketControl.resume();

        CpuTtcPacketControl.hold();
        assertFalse(CpuTtcPacketControl.intercept(first));
        assertFalse(CpuTtcPacketControl.intercept(latest));
        assertEquals(1, CpuTtcPacketControl.heldSequence());
        CpuTtcPacketControl.resume();
    }

    @Test
    void connectedServerRefusesSourceUnmarkedAndMismatchedFixtures() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> CpuListTtcControl.validateDisposableServer(temporary, "1.20.1-forge"));
        var marker = temporary.resolve(".ae2-crafting-time-dedicated-fixture.json");
        var valid = "{\"schema\":2,\"sourceFixtureId\":\"ae2-crafting-time\",\"role\":\"disposable\",\"target\":\"1.20.1-forge\"}";
        Files.writeString(marker, valid);
        CpuListTtcControl.validateDisposableServer(temporary, "1.20.1-forge");
        for (var invalid : List.of("null", valid.replace("schema\":2", "schema\":1"),
                valid.replace("ae2-crafting-time", "other"), valid.replace("disposable", "source"),
                valid.replace("1.20.1-forge", "1.20.1-fabric"))) {
            Files.writeString(marker, invalid);
            assertThrows(IllegalStateException.class,
                    () -> CpuListTtcControl.validateDisposableServer(temporary, "1.20.1-forge"));
        }
    }

    @Test
    void connectedControlResumesAboveRetainedAckAndRequiresTheExactEpochActionAndSequence() {
        assertEquals(8, CpuListTtcControl.nextSequence(0, 7));
        assertEquals(9, CpuListTtcControl.nextSequence(8, 7));
        var exact = new CpuListTtcControl.State(true, "campaign-a", 8, "reconnect", "second-grid",
                1, 2, 3, "4,5", "state");
        assertTrue(CpuListTtcControl.acknowledges(exact, "campaign-a", 8, "reconnect"));
        assertFalse(CpuListTtcControl.acknowledges(exact, "campaign-b", 8, "reconnect"));
        assertFalse(CpuListTtcControl.acknowledges(exact, "campaign-a", 7, "reconnect"));
        assertFalse(CpuListTtcControl.acknowledges(exact, "campaign-a", 8, "complete"));
    }

    @Test
    void relaunchContinuationRejectsAnotherWorldEpochOrIncompletePredecessor() throws Exception {
        var path = temporary.resolve("cpu-list-continuation.json");
        var world = "ae2ct-" + "a".repeat(32);
        var continuation = new CpuListContinuation(1, "relaunch-ready", world, "campaign-a", "server-state",
                1, 2, 3, List.of("initial-distinct", "same-jvm-clear"), List.of("before.png"), 8, 9);
        CpuListContinuation.write(path, continuation);
        assertEquals(continuation, CpuListContinuation.read(path, world, "campaign-a"));
        assertThrows(IllegalArgumentException.class,
                () -> CpuListContinuation.read(path, "ae2ct-" + "b".repeat(32), "campaign-a"));
        assertThrows(IllegalArgumentException.class,
                () -> CpuListContinuation.read(path, world, "campaign-b"));
        Files.writeString(path, Files.readString(path).replace("relaunch-ready", "running"));
        assertThrows(IllegalArgumentException.class, () -> CpuListContinuation.read(path, world, "campaign-a"));
    }

    @Test
    void relaunchIdentityIgnoresPhaseLocalSerialAndElapsedButNotPhysicalJobIdentity() {
        var firstCpu = new CpuListTtcControl.CpuState("1,2,3", 4, "minecraft:stone", 8,
                true, true, new com.ctux.ae2craftingtime.core.TtcEstimate(10L * 1000, true), 11, 12, "job-a");
        var nextCpu = new CpuListTtcControl.CpuState("1,2,3", 99, "minecraft:stone", 8,
                true, true, new com.ctux.ae2craftingtime.core.TtcEstimate(9L * 1000, true), 999, 13, "job-a");
        var before = new CpuListTtcControl.ServerState("network", 1, -1, true, true, List.of(firstCpu));
        var after = new CpuListTtcControl.ServerState("network", 77, -1, true, true, List.of(nextCpu));
        assertTrue(CpuListTtcScenario.samePhysicalJobs(before, after));
        assertFalse(CpuListTtcScenario.samePhysicalJobs(before,
                new CpuListTtcControl.ServerState("other", 77, -1, true, true, List.of(nextCpu))));
        assertFalse(CpuListTtcScenario.samePhysicalJobs(before,
                new CpuListTtcControl.ServerState("network", 77, -1, true, true, List.of(
                        new CpuListTtcControl.CpuState("1,2,4", 99, "minecraft:stone", 8,
                                true, true, new com.ctux.ae2craftingtime.core.TtcEstimate(9L * 1000, true), 999, 13)))));
        assertFalse(CpuListTtcScenario.samePhysicalJobs(before,
                new CpuListTtcControl.ServerState("network", 77, -1, true, true, List.of(
                        new CpuListTtcControl.CpuState("1,2,3", 99, "minecraft:stone", 8,
                                true, true, new com.ctux.ae2craftingtime.core.TtcEstimate(9L * 1000, true),
                                999, 13, "replacement-job")))));
        var saved = new CpuListTtcControl.ServerState("network", 1, -1, true, true,
                List.of(firstCpu), 4096, 256, 3, 4);
        assertFalse(CpuListTtcScenario.samePhysicalJobs(saved,
                new CpuListTtcControl.ServerState("network", 77, -1, true, true,
                        List.of(nextCpu), 4097, 256, 3, 4)));
        assertFalse(CpuListTtcScenario.samePhysicalJobs(saved,
                new CpuListTtcControl.ServerState("network", 77, -1, true, true,
                        List.of(nextCpu), 4096, 256, 3, 5)));
    }

    @Test
    void onlySavedCpuListGridsUsePhysicalRestartWiring() {
        var saved = new StandardCraftFixture();
        assertTrue(saved.persistentCpuListWiring);
        var temporaryLarge = saved.largeCpuGrid();
        assertTrue(temporaryLarge.cpuListScenario);
        assertFalse(temporaryLarge.persistentCpuListWiring);
    }

    @Test
    void relaunchWritesToAPhaseLocalCheckpointLedger() {
        assertEquals("cpu-list-checkpoints.jsonl", CpuListTtcScenario.checkpointFile(false));
        assertEquals("cpu-list-checkpoints.phase-2.jsonl", CpuListTtcScenario.checkpointFile(true));
    }

    @Test
    void resumedCpuListReturnsToItsPersistedTerminalBeforeOpening() throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream(
                "/com/ctux/ae2craftingtime/testdriver/CpuListTtcScenario.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        assertTrue(node.methods.stream().flatMap(method ->
                java.util.Arrays.stream(method.instructions.toArray()))
                .filter(MethodInsnNode.class::isInstance).map(MethodInsnNode.class::cast)
                .anyMatch(call -> call.owner.endsWith("/StandardCraftFixture")
                        && call.name.equals("viewTerminal")));
    }

    @Test
    void resumedCpuListStartsAtTheCpuOwnedActiveStage() throws Exception {
        var world = "ae2ct-" + "a".repeat(32);
        var path = temporary.resolve("cpu-list-continuation.json");
        CpuListContinuation.write(path, new CpuListContinuation(1, "relaunch-ready", world, "campaign-a",
                "server-state", 1, 2, 3, List.of("same-jvm-clear"), List.of("before.png"), 8, 9));
        System.setProperty("ae2craftingtime.test.continuation", path.toString());
        System.setProperty("ae2craftingtime.test.campaign", "campaign-a");
        try {
            assertTrue(new StandardAe2Scenario("cpu-list-total-ttc", world, temporary, false)
                    .checkpoint().startsWith("phase=ACTIVE "));
        } finally {
            System.clearProperty("ae2craftingtime.test.continuation");
            System.clearProperty("ae2craftingtime.test.campaign");
        }
    }

    @Test
    void resumedCpuListMergesContinuationScreenshotsIntoTheFinalResultList() throws Exception {
        var world = "ae2ct-" + "a".repeat(32);
        var path = temporary.resolve("cpu-list-continuation.json");
        CpuListContinuation.write(path, new CpuListContinuation(1, "relaunch-ready", world, "campaign-a",
                "server-state", 1, 2, 3, List.of("same-jvm-clear"),
                List.of("phase-1-first.png", "phase-1-last.png"), 8, 9));
        System.setProperty("ae2craftingtime.test.continuation", path.toString());
        System.setProperty("ae2craftingtime.test.campaign", "campaign-a");
        try {
            var resultScreenshots = new ArrayList<String>();
            new StandardAe2Scenario("cpu-list-total-ttc", world, temporary, false, resultScreenshots);
            resultScreenshots.add("phase-2.png");
            assertEquals(List.of("phase-1-first.png", "phase-1-last.png", "phase-2.png"), resultScreenshots);
        } finally {
            System.clearProperty("ae2craftingtime.test.continuation");
            System.clearProperty("ae2craftingtime.test.campaign");
        }
    }

    @Test
    void relaunchRequiresEveryKnownSavedJobAndKeepsUnknownControl() {
        var known = java.util.stream.IntStream.range(0, 4).mapToObj(index ->
                new CpuListTtcControl.CpuState("1,2," + index, index, "minecraft:smooth_stone", 8,
                        true, true, new com.ctux.ae2craftingtime.core.TtcEstimate(7000, true),
                        9, 10, "job-" + index)).toList();
        var unknown = new CpuListTtcControl.CpuState("1,2,5", 5, "minecraft:glass", 4,
                true, true, null, 9, 10, "unknown-job");
        var all = new java.util.ArrayList<>(known);
        assertFalse(CpuListTtcScenario.allKnownRecovered(serverState(known.toArray(CpuListTtcControl.CpuState[]::new))));
        all.add(unknown);
        assertTrue(CpuListTtcScenario.allKnownRecovered(serverState(all.toArray(CpuListTtcControl.CpuState[]::new))));
        for (int index = 0; index < 4; index++) {
            var missing = new java.util.ArrayList<>(all);
            var cpu = known.get(index);
            missing.set(index, new CpuListTtcControl.CpuState(cpu.position(), cpu.serial(), cpu.jobId(), cpu.amount(),
                    true, true, null, 9, 10, cpu.craftId()));
            assertFalse(CpuListTtcScenario.allKnownRecovered(
                    serverState(missing.toArray(CpuListTtcControl.CpuState[]::new))));
        }
        all.set(4, new CpuListTtcControl.CpuState("1,2,5", 5, "minecraft:glass", 4,
                true, true, new com.ctux.ae2craftingtime.core.TtcEstimate(7000, true), 9, 10, "unknown-job"));
        assertFalse(CpuListTtcScenario.allKnownRecovered(serverState(all.toArray(CpuListTtcControl.CpuState[]::new))));
    }

    @Test
    void dedicatedReconnectDropsStaleServerCpuIdentities() throws Exception {
        var fixture = new StandardCraftFixture();
        var identities = StandardCraftFixture.class.getDeclaredField("cpuListIdentities");
        identities.setAccessible(true);
        identities.set(fixture, new ArrayList<>());

        DedicatedCpuScenario.refreshCpuIdentitiesForReconnect(fixture);

        assertNull(identities.get(fixture));
    }

    @Test
    void completedPreambleObservationAdvancesRelaunchFreshWithoutAnotherObservation() {
        var cpus = new ArrayList<CpuListTtcControl.CpuState>();
        cpus.add(new CpuListTtcControl.CpuState("1,2,3", 4, "minecraft:smooth_stone", 8,
                true, true, new com.ctux.ae2craftingtime.core.TtcEstimate(3600L * 1000, true), 9, 10, "job-a"));
        for (int index = 0; index < 3; index++) cpus.add(new CpuListTtcControl.CpuState("1,2," + index,
                5 + index, "minecraft:stone", 8, true, true,
                new com.ctux.ae2craftingtime.core.TtcEstimate(1000, true), 9, 10, "job-" + index));
        cpus.add(new CpuListTtcControl.CpuState("1,2,9", 9, "minecraft:glass", 4,
                true, true, null, 9, 10, "unknown-job"));
        var state = new CpuListTtcControl.ServerState("network", 1, 4, true, true, cpus);
        var rendered = TtcText.ttc("~1:00:00").getString();
        var snapshot = selectedCpuSnapshot(new Rect(60, 20, 20, 8), null, rendered);
        assertEquals(CpuListTtcScenario.Stage.DONE,
                CpuListTtcScenario.afterRelaunchFreshObservation(snapshot, state, false));
        var boxed = selectedCpuSnapshot(new Rect(60, 20, 20, 8), new Rect(58, 18, 24, 12), rendered);
        assertEquals(CpuListTtcScenario.Stage.DONE,
                CpuListTtcScenario.afterRelaunchFreshObservation(boxed, state, true));
        assertThrows(IllegalStateException.class,
                () -> CpuListTtcScenario.afterRelaunchFreshObservation(snapshot, state, true));
        assertThrows(IllegalStateException.class,
                () -> CpuListTtcScenario.afterRelaunchFreshObservation(boxed, state, false));
        assertEquals(CpuListTtcScenario.Stage.RELAUNCH_FRESH,
                CpuListTtcScenario.afterRelaunchFreshObservation(cpuSnapshot(null, null), state, false));
        assertEquals(CpuListTtcScenario.Stage.RELAUNCH_FRESH,
                CpuListTtcScenario.afterRelaunchFreshObservation(cpuSnapshot(new Rect(60, 20, 20, 8), null), state, false));
    }

    @Test
    void restartCaptureWaitsForSelectedJobNativeRowsAndNewStatus() {
        var cpu = new CpuListTtcControl.CpuState("1,2,3", 4, "minecraft:smooth_stone", 8,
                true, true, new com.ctux.ae2craftingtime.core.TtcEstimate(7000, true), 9, 10, "job-a");
        var state = new CpuListTtcControl.ServerState("network", 1, 4, true, true, List.of(cpu));
        var base = selectedCpuSnapshot(new Rect(60, 20, 20, 8), null, "~7s");
        var row = new UiSnapshot.Row("minecraft:smooth_stone", 8, 0, new Rect(1, 1, 16, 16), List.of());
        var oldRow = new UiSnapshot.Row("minecraft:glass", 4, 0, new Rect(1, 1, 16, 16), List.of());
        var current = new UiSnapshot.StatusObservation(2, "native", 1, 1, 1, 0, 0, 4,
                "job-a", "minecraft:smooth_stone", 1L);
        var missingJob = new UiSnapshot.StatusObservation(2, "native", 1, 1, 1, 0, 0, 4,
                null, "minecraft:smooth_stone", 1L);
        var ready = new UiSnapshot(base.screen(), base.menu(), base.gui(), base.screenWidth(),
                base.screenHeight(), base.guiScale(), 2, base.scroll(), List.of(row), base.text(),
                base.badges(), base.widgets(), base.itemCells(), base.tooltip(), base.cpuCards(),
                base.rawCpuSerials(), current);
        assertTrue(CpuListTtcScenario.freshSelectedCpuStatus(ready, state, 1, 1));
        assertFalse(CpuListTtcScenario.freshSelectedCpuStatus(ready, state, 2, 1));
        assertFalse(CpuListTtcScenario.freshSelectedCpuStatus(ready, state, 1, 2));
        assertFalse(CpuListTtcScenario.freshSelectedCpuStatus(new UiSnapshot(ready.screen(), ready.menu(),
                ready.gui(), ready.screenWidth(), ready.screenHeight(), ready.guiScale(), ready.frame(),
                ready.scroll(), List.of(oldRow), ready.text(), ready.badges(), ready.widgets(),
                ready.itemCells(), ready.tooltip(), ready.cpuCards(), ready.rawCpuSerials(), current), state, 1, 1));
        assertFalse(CpuListTtcScenario.freshSelectedCpuStatus(new UiSnapshot(ready.screen(), ready.menu(),
                ready.gui(), ready.screenWidth(), ready.screenHeight(), ready.guiScale(), ready.frame(),
                ready.scroll(), ready.rows(), ready.text(), ready.badges(), ready.widgets(),
                ready.itemCells(), ready.tooltip(), ready.cpuCards(), ready.rawCpuSerials(), missingJob), state, 1, 1));
    }

    @Test
    void connectedRelaunchUsesTheTwoProcessScenarioBudget() {
        assertEquals(40, DedicatedCpuScenario.timeoutMinutes("cpu-list-total-ttc-connected"));
        assertEquals(5, DedicatedCpuScenario.timeoutMinutes("startup-only"));
    }

    @Test
    void smokeUsesThePackagedCataloguesOrderForEveryTarget() {
        for (var target : List.of("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge", "26.1.2-neoforge")) {
            SmokeAdapterCatalog.newest(target).forEach((dependency, variant) -> {
                assertTrue(dependency.matches("[a-z0-9_]+"), dependency);
                assertTrue(variant.matches("[a-z0-9-]+"), variant);
            });
        }
        assertEquals("batched-long", SmokeAdapterCatalog.newest("1.20.1-forge").get("neoecoae"));
        assertEquals("batched-int", SmokeAdapterCatalog.newest("1.21.1-neoforge").get("neoecoae"));
        assertEquals("tree-layout", SmokeAdapterCatalog.newest("1.20.1-fabric").get("ae2ct"));
        assertEquals(java.util.Map.of("advanced_ae", "advanced-cpu-26"), SmokeAdapterCatalog.newest("26.1.2-neoforge"));
        assertTrue(SmokeAdapterCatalog.newest("unknown").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> SmokeAdapterCatalog.main(new String[0]));
        SmokeAdapterCatalog.main(new String[] { "unknown" });
    }

    @Test
    void noSpaceRequiresTheRenderedWarningAdviceAndOrderedControls() {
        assertTrue(AddonCpuFixture.supports(NoSpaceScenario.SCENARIO));
        assertNull(AddonCpuFixture.create(NoSpaceScenario.SCENARIO));
        assertEquals(NoSpaceScenario.CHECKS, DriverResult.requiredChecks(NoSpaceScenario.SCENARIO));
        var tooltip = List.of(NoSpaceScenario.KEY, NoSpaceScenario.KEY + ".explanation",
                NoSpaceScenario.KEY + ".suggestion", "text.ae2craftingtime.locate_hint",
                "text.ae2craftingtime.details_hint", "text.ae2craftingtime.reset_hint").stream()
                .map(key -> new UiSnapshot.ObservedText(key, key, List.of(), null)).toList();
        assertTrue(NoSpaceScenario.tooltipReady(tooltip));
        for (int missing = 0; missing < tooltip.size(); missing++) {
            var incomplete = new java.util.ArrayList<>(tooltip);
            incomplete.remove(missing);
            assertFalse(NoSpaceScenario.tooltipReady(incomplete));
        }
        var duplicated = new java.util.ArrayList<>(tooltip);
        duplicated.add(3, tooltip.get(3));
        assertFalse(NoSpaceScenario.tooltipReady(duplicated));
        assertFalse(NoSpaceScenario.tooltipReady(List.of()));
    }

    @Test
    void providerWarningRequiresItsBodyFollowedByOneOrderedControlSection() {
        var scenario = new ProviderDispatchStatusScenario(ProviderDispatchStatusScenario.INPUT_BLOCKED);
        var keys = List.of("text.ae2craftingtime.input_blocked",
                "text.ae2craftingtime.input_blocked.explanation",
                "text.ae2craftingtime.input_blocked.suggestion",
                "text.ae2craftingtime.dispatch_status.scheduled_only",
                "text.ae2craftingtime.locate_hint",
                "text.ae2craftingtime.details_hint",
                "text.ae2craftingtime.reset_hint");
        var tooltip = keys.stream().map(key -> new UiSnapshot.ObservedText(key, key, List.of(), null)).toList();

        assertTrue(scenario.tooltipReady(tooltip));
        var previous = System.getProperty("ae2craftingtime.test.advancedStatus");
        try {
            System.setProperty("ae2craftingtime.test.advancedStatus", "true");
            assertTrue(scenario.tooltipReady(tooltip));
        } finally {
            if (previous == null) System.clearProperty("ae2craftingtime.test.advancedStatus");
            else System.setProperty("ae2craftingtime.test.advancedStatus", previous);
        }
        assertTrue(WarningTooltipChecks.hasControls(tooltip));
        for (int missing = 0; missing < tooltip.size(); missing++) {
            var incomplete = new java.util.ArrayList<>(tooltip);
            incomplete.remove(missing);
            assertFalse(scenario.tooltipReady(incomplete));
        }
        var reordered = new java.util.ArrayList<>(tooltip);
        java.util.Collections.swap(reordered, reordered.size() - 2, reordered.size() - 1);
        assertFalse(scenario.tooltipReady(reordered));
        assertFalse(WarningTooltipChecks.hasControls(reordered));
    }

    @Test
    void noProviderRequiresTheRenderedWarningAndBothAdviceLines() {
        assertTrue(AddonCpuFixture.supports(NoProviderScenario.SCENARIO));
        assertNull(AddonCpuFixture.create(NoProviderScenario.SCENARIO));
        assertEquals(NoProviderScenario.CHECKS, DriverResult.requiredChecks(NoProviderScenario.SCENARIO));
        var tooltip = List.of(NoProviderScenario.KEY, NoProviderScenario.KEY + ".explanation",
                NoProviderScenario.KEY + ".suggestion").stream()
                .map(key -> new UiSnapshot.ObservedText(key, key, List.of(), null)).toList();
        assertTrue(NoProviderScenario.tooltipReady(tooltip));
        for (int missing = 0; missing < tooltip.size(); missing++) {
            var incomplete = new java.util.ArrayList<>(tooltip);
            incomplete.remove(missing);
            assertFalse(NoProviderScenario.tooltipReady(incomplete));
        }
        assertFalse(NoProviderScenario.tooltipReady(List.of()));
    }

    @Test
    void providerStatusWaitsForTheSelectedCpuRowsBeforeMutatingTheFixture() {
        assertTrue(AddonCpuFixture.supports(ProviderDispatchStatusScenario.NO_CHANNEL));
        assertNull(AddonCpuFixture.create(ProviderDispatchStatusScenario.NO_CHANNEL));
        assertEquals(ProviderDispatchStatusScenario.checks(ProviderDispatchStatusScenario.NO_CHANNEL),
                DriverResult.requiredChecks(ProviderDispatchStatusScenario.NO_CHANNEL));
        assertTrue(DriverResult.requiredChecks(ProviderDispatchStatusScenario.NO_CHANNEL)
                .containsAll(List.of("channel-starved", "no-samples", "reboot-boundary", "job-completed")));
        assertFalse(ProviderDispatchStatusScenario.statusRowsReady(List.of()));
        assertTrue(ProviderDispatchStatusScenario.statusRowsReady(List.of(
                new UiSnapshot.Row("minecraft:diamond", 64, 0, null, List.of()))));
    }

    @Test
    void providerStatusDrainsCompletedServerStepsAfterTheUiStateChanges() throws Exception {
        var scenario = new ProviderDispatchStatusScenario(ProviderDispatchStatusScenario.LOCKED);
        var phaseField = ProviderDispatchStatusScenario.class.getDeclaredField("phase");
        var operationField = ProviderDispatchStatusScenario.class.getDeclaredField("operation");
        var fixtureField = ProviderDispatchStatusScenario.class.getDeclaredField("fixture");
        var providerPositionField = DispatchStatusFixture.class.getDeclaredField("providerPosition");
        var tickLocked = ProviderDispatchStatusScenario.class.getDeclaredMethod("tickLocked", Minecraft.class,
                UiSnapshot.class, Map.class, Consumer.class, BiConsumer.class);
        for (var field : List.of(phaseField, operationField, fixtureField, providerPositionField)) {
            field.setAccessible(true);
        }
        tickLocked.setAccessible(true);
        providerPositionField.set(fixtureField.get(scenario), net.minecraft.core.BlockPos.ZERO);

        var warning = new UiSnapshot.ObservedText("text.ae2craftingtime.locked", "locked", List.of(), null);
        var warningSnapshot = new UiSnapshot("screen", "menu", null, 100, 100, 1, 1, 0,
                List.of(), List.of(warning), List.of(), List.of(), List.of(), List.of());
        var recoveredSnapshot = new UiSnapshot("screen", "menu", null, 100, 100, 1, 1, 0,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        for (int phase = 4; phase <= 10; phase++) {
            phaseField.setInt(scenario, phase);
            operationField.set(scenario, CompletableFuture.completedFuture(true));
            var snapshot = phase % 2 == 0 ? recoveredSnapshot : warningSnapshot;
            var screenshots = new AtomicInteger();

            tickLocked.invoke(scenario, null, snapshot, new LinkedHashMap<String, Boolean>(),
                    (Consumer<String>) ignored -> screenshots.incrementAndGet(),
                    (BiConsumer<Integer, Integer>) (x, y) -> {});

            assertEquals(phase + 1, phaseField.getInt(scenario), "phase " + phase);
            assertNull(operationField.get(scenario), "phase " + phase);
            assertEquals(0, screenshots.get(), "phase " + phase + " must not capture a changed UI state");
        }
    }

    @Test
    void noPowerRequiresTheRenderedWarningAndBothAdviceLines() {
        assertTrue(AddonCpuFixture.supports(NoPowerScenario.SCENARIO));
        assertNull(AddonCpuFixture.create(NoPowerScenario.SCENARIO));
        assertEquals(NoPowerScenario.CHECKS, DriverResult.requiredChecks(NoPowerScenario.SCENARIO));
        var tooltip = List.of(NoPowerScenario.KEY, NoPowerScenario.KEY + ".explanation",
                NoPowerScenario.KEY + ".suggestion").stream()
                .map(key -> new UiSnapshot.ObservedText(key, key, List.of(), null)).toList();
        assertTrue(NoPowerScenario.tooltipReady(tooltip));
        for (int missing = 0; missing < tooltip.size(); missing++) {
            var incomplete = new java.util.ArrayList<>(tooltip);
            incomplete.remove(missing);
            assertFalse(NoPowerScenario.tooltipReady(incomplete));
        }
        assertFalse(NoPowerScenario.tooltipReady(List.of()));
    }

    @Test
    void standardPlanWaitsForBothSeededEstimatesBeforeCheckingSortOrder() {
        var ready = UiObservationStore.observed(List.of(TtcText.ttc("~2s")), null);
        var pending = UiObservationStore.observed(List.of(TtcText.ttcCollectingData()), null);
        var stone = new UiSnapshot.Row("minecraft:stone", 1, 0, null, ready);
        var smooth = new UiSnapshot.Row("minecraft:smooth_stone", 1, 0, null, ready);
        assertFalse(StandardAe2Scenario.planEstimatesReady(List.of()));
        assertFalse(StandardAe2Scenario.planEstimatesReady(List.of(stone)));
        for (var unresolved : List.of(pending, List.<UiSnapshot.ObservedText>of())) {
            assertFalse(StandardAe2Scenario.planEstimatesReady(List.of(smooth,
                    new UiSnapshot.Row("minecraft:stone", 1, 0, null, unresolved))));
            assertFalse(StandardAe2Scenario.planEstimatesReady(List.of(stone,
                    new UiSnapshot.Row("minecraft:smooth_stone", 1, 0, null, unresolved))));
        }
        assertTrue(StandardAe2Scenario.planEstimatesReady(List.of(smooth, stone)));
    }

    @Test
    void flattenedPlanFooterRetainsDrawnTotalAndRespectsBadgeSetting() {
        var gui = new Rect(20, 30, 238, 206);
        var bounds = new Rect(100, gui.y() + 178, 35, 9);
        var flattened = com.ctux.ae2craftingtime.mc1201.TtcComponents.rowSymbolsInTextColor(
                TtcText.totalTtc("~7s"), 0xFFFFFF);
        assertEquals("literal", UiObservationStore.observed(List.of(flattened), bounds).get(0).key());
        // JUnit has no en_us client language; this is the rendered footer observed in the smoke failure.
        var drawn = new UiSnapshot.ObservedText("literal", "TTC: ~7s", List.of(), bounds);
        var total = UiObservationStore.planTotalText("appeng.client.gui.me.crafting.CraftConfirmScreen", gui, drawn);
        assertEquals("text.ae2craftingtime.total_ttc", total.key());
        assertEquals(drawn.rendered(), total.rendered());
        assertEquals(bounds, total.bounds());
        assertNull(UiObservationStore.planTotalText("appeng.client.gui.me.crafting.CraftingStatusScreen", gui, drawn));
        assertNull(UiObservationStore.planTotalText("appeng.client.gui.me.crafting.CraftConfirmScreen", gui,
                new UiSnapshot.ObservedText("literal", "TTC: ~7s", List.of(), null)));
        assertNull(UiObservationStore.planTotalText("appeng.client.gui.me.crafting.CraftConfirmScreen", gui,
                new UiSnapshot.ObservedText("literal", "TTC: ~7s", List.of(), new Rect(100, 40, 35, 9))));
        assertNull(UiObservationStore.planTotalText("appeng.client.gui.me.crafting.CraftConfirmScreen", gui,
                new UiSnapshot.ObservedText("literal", "Other: ~7s", List.of(), bounds)));
        for (boolean enabled : List.of(false, true)) {
            assertTrue(StandardAe2Scenario.recurrentBadgeMatches(enabled ? List.of(new Rect(98, bounds.y() - 2, 39, 13))
                    : List.of(), bounds, enabled));
            assertFalse(StandardAe2Scenario.recurrentBadgeMatches(enabled ? List.of() : List.of(bounds), bounds, enabled));
        }
    }

    @Test
    void galleryPlansDistinguishUnknownRowsFromMissingObservations() {
        var known = UiObservationStore.observed(List.of(TtcText.ttc("~2s")), null);
        var unknown = UiObservationStore.observed(List.of(TtcText.ttcCollectingData()), null);
        var stone = new UiSnapshot.Row("minecraft:stone", 1, 0, null, unknown);
        var smooth = new UiSnapshot.Row("minecraft:smooth_stone", 1, 0, null, unknown);
        assertTrue(StandardAe2Scenario.galleryPlanReady(List.of(stone, smooth), 0));
        assertFalse(StandardAe2Scenario.galleryPlanReady(List.of(stone, smooth), 1));
        assertFalse(StandardAe2Scenario.galleryPlanReady(List.of(stone), 0));
        assertFalse(StandardAe2Scenario.galleryPlanReady(List.of(stone,
                new UiSnapshot.Row("minecraft:smooth_stone", 1, 0, null, List.of())), 0));
        var profiledStone = new UiSnapshot.Row("minecraft:stone", 1, 0, null, known);
        assertTrue(StandardAe2Scenario.galleryPlanReady(List.of(profiledStone, smooth), 1));
        assertFalse(StandardAe2Scenario.galleryPlanReady(List.of(profiledStone, smooth), 0));
        assertFalse(StandardAe2Scenario.galleryPlanReady(List.of(
                new UiSnapshot.Row("minecraft:stone", 0, 0, null, known), smooth), 1));
    }

    @Test
    void galleryAccuracyRequiresExactlyOneRealCompletedJobWithExpectedCoverage() {
        var full = new com.ctux.ae2craftingtime.core.TtcAccuracyStats(1, 1, 1, 0, 0, 1, 7, 20, 20, 2, 2);
        var partial = new com.ctux.ae2craftingtime.core.TtcAccuracyStats(1, 0, .5, 0, 0, 0, 10, 20, 20, 1, 2);
        assertTrue(StandardAe2Scenario.galleryAccuracyReady(full, false));
        assertTrue(StandardAe2Scenario.galleryAccuracyReady(partial, true));
        assertFalse(StandardAe2Scenario.galleryAccuracyReady(full, true));
        assertFalse(StandardAe2Scenario.galleryAccuracyReady(partial, false));
        assertFalse(StandardAe2Scenario.galleryAccuracyReady(new com.ctux.ae2craftingtime.core.TtcAccuracyStats(
                2, 1, .75, 0, 0, 1, 10, 20, 20, 1, 2), true));
        assertFalse(StandardAe2Scenario.galleryAccuracyReady(new com.ctux.ae2craftingtime.core.TtcAccuracyStats(
                1, 1, 1, 0, 0, 1, 7, 0, 0, 2, 2), false));
    }

    @TempDir
    Path temporary;

    @Test
    void craftingTreeRequiresBothWidgetLayoutsAndEveryTooltipLine() {
        assertTrue(CraftingTreeScenario.isScreen("com.neuvillette.ae2ct.gui.CraftingTreeScreen"));
        assertTrue(CraftingTreeScenario.isScreen("com.vcwdfca.ae2ct.gui.CraftingTreeScreen"));
        assertFalse(CraftingTreeScenario.isScreen("appeng.client.gui.me.crafting.CraftConfirmScreen"));
        var keys = List.of("text.ae2craftingtime.ttc", "text.ae2craftingtime.details_hint",
                "text.ae2craftingtime.reset_hint");
        for (int mask = 0; mask < 8; mask++) {
            var lines = new java.util.ArrayList<UiSnapshot.ObservedText>();
            for (int bit = 0; bit < 3; bit++) {
                if ((mask & (1 << bit)) != 0) {
                    lines.add(new UiSnapshot.ObservedText(keys.get(bit), "line", List.of(), null));
                }
            }
            var frame = new UiSnapshot("screen", "menu", new Rect(0, 0, 100, 100), 100, 100, 1, 1, 0,
                    List.of(), List.of(), List.of(), List.of(), List.of(), lines);
            assertEquals(mask == 7, CraftingTreeScenario.tooltipReady(frame));
            if (mask == 7) {
                for (var line : List.copyOf(lines)) {
                    lines.add(line);
                    assertFalse(CraftingTreeScenario.tooltipReady(new UiSnapshot("screen", "menu",
                            new Rect(0, 0, 100, 100), 100, 100, 1, 1, 0,
                            List.of(), List.of(), List.of(), List.of(), List.of(), lines)));
                    lines.remove(lines.size() - 1);
                }
            }
        }
        assertThrows(IllegalStateException.class, () -> new AdvancedAeFixture().place(null, null));
    }

    @Test
    void tooltipObservationKeepsAppendedEstimatesAndMissingDataDistinct() {
        var bounds = new Rect(1, 2, 3, 4);
        for (var value : List.of(TtcText.ttc("~1s"), TtcText.ttcCollectingData())) {
            var line = Component.empty().append(TtcText.tooltipTtc(value));
            var observations = UiObservationStore.observed(List.of(line), bounds);
            assertEquals(List.of("literal", "text.ae2craftingtime.stats.ttc", "literal",
                    "text.ae2craftingtime.ttc"), observations.stream().map(UiSnapshot.ObservedText::key).toList());
            var expected = UiObservationStore.observed(List.of(value), bounds).get(0);
            assertEquals(expected, observations.get(3));
            assertEquals(bounds, observations.get(3).bounds());
        }
        assertEquals(List.of(), UiObservationStore.observed(List.of(), null));
        assertEquals(List.of("text.ae2craftingtime.collecting_data"), UiObservationStore.observed(
                List.of(TtcText.ttcCollectingData()), null).get(0).arguments());
    }

    @Test
    void capturesRequireRenderedContentInsteadOfOnlyPopulatedMenus() {
        var literal = new UiSnapshot.ObservedText("literal", "Furnace", List.of(), null);
        var ttc = UiObservationStore.observed(List.of(TtcText.ttc("~1s")), null);
        var missing = UiObservationStore.observed(List.of(TtcText.ttcCollectingData()), null);
        assertFalse(CraftPlanScenario.wirelessTooltipReady(List.of(), false));
        assertTrue(CraftPlanScenario.wirelessTooltipReady(List.of(literal), false));
        assertFalse(CraftPlanScenario.wirelessTooltipReady(List.of(literal), true));
        assertFalse(CraftPlanScenario.wirelessTooltipReady(missing, true));
        assertTrue(CraftPlanScenario.wirelessTooltipReady(ttc, true));
        assertFalse(CraftPlanScenario.renderedPlan(null));
        var badge = new Rect(10, 10, 20, 10);
        var total = new UiSnapshot.ObservedText("text.ae2craftingtime.total_ttc", "TTC: ~1s", List.of(), badge);
        for (boolean drawBadge : List.of(false, true)) {
            for (var text : List.of(literal, total)) {
                var frame = new UiSnapshot("screen", "menu", badge, 100, 100, 1, 1, 0, List.of(),
                        List.of(text), drawBadge ? List.of(badge) : List.of(), List.of(), List.of(), List.of());
                assertEquals(text == total, CraftPlanScenario.renderedPlan(frame));
                assertEquals(text == total ? "target=false total=true waiting=target"
                                : "target=false total=false waiting=target",
                        CraftPlanScenario.planReadiness(frame, "diamond"));
            }
        }
        assertEquals("target=false total=false waiting=frame", CraftPlanScenario.planReadiness(null, "diamond"));
        var row = new UiSnapshot.Row("diamond", 1, 0, badge, List.of());
        var ready = new UiSnapshot("screen", "menu", badge, 100, 100, 1, 2, 0, List.of(row),
                List.of(total), List.of(), List.of(), List.of(), List.of());
        assertEquals("target=true total=true waiting=stable-rows", CraftPlanScenario.planReadiness(ready, "diamond"));
        assertTrue(LayoutValidator.validate(ready).isEmpty());
        var invalid = new UiSnapshot("screen", "menu", badge, 100, 100, 1, 3, 0, List.of(row),
                List.of(new UiSnapshot.ObservedText(total.key(), total.rendered(), List.of(), new Rect(90, 90, 20, 10))),
                List.of(), List.of(), List.of(), List.of());
        assertFalse(LayoutValidator.validate(invalid).isEmpty());
    }

    @Test
    void treeReadinessRequiresDurationDrawAtTheTargetNode() {
        var screen = "com.neuvillette.ae2ct.gui.CraftingTreeScreen";
        var node = new UiSnapshot.Row("diamond", 1, 0, new Rect(10, 10, 16, 16), List.of());
        for (var label : List.of("~1s", "⌚ ~1:02", "~1:02:03", "⌚ ~11s?", "50ms", "⌚ 5ms?",
                "🥇 Instant", "Instant", "Item ~1", "TTC: ~1s")) {
            var literal = new UiSnapshot.ObservedText("literal", label, List.of(), new Rect(8, 31, 20, 5));
            var text = CraftingTreeScenario.nodeTtcText(screen, literal);
            boolean duration = !label.startsWith("Item") && !label.startsWith("TTC:");
            assertEquals(duration, text.key().startsWith("text.ae2craftingtime.")); // Recovery must see unboxed TTC.
            assertEquals(literal, CraftingTreeScenario.nodeTtcText("other-screen", literal));
            for (var bounds : List.of(literal.bounds(), new Rect(50, 31, 20, 5), new Rect(8, 60, 20, 5))) {
                var drawn = new UiSnapshot.ObservedText(text.key(), label, List.of(), bounds);
                var frame = new UiSnapshot(screen, "menu", new Rect(0, 0, 100, 100), 100, 100, 1, 4, 0,
                        List.of(node), List.of(drawn), List.of(), List.of(), List.of(node.cell()), List.of());
                assertEquals(duration && bounds.equals(literal.bounds()), CraftingTreeScenario.nodeTtcDrawn(frame, node));
                assertTrue(LayoutValidator.validate(frame).isEmpty());
            }
        }
        var missing = new UiSnapshot(screen, "menu", new Rect(0, 0, 100, 100), 100, 100, 1, 5, 0,
                List.of(node), List.of(), List.of(new Rect(8, 29, 20, 9)), List.of(), List.of(node.cell()), List.of());
        assertFalse(CraftingTreeScenario.nodeTtcDrawn(missing, node));
        var scaledNode = new UiSnapshot.Row("diamond", 1, 0, new Rect(10, 10, 32, 32), List.of());
        var scaledText = CraftingTreeScenario.nodeTtcText(screen,
                new UiSnapshot.ObservedText("literal", "~1s", List.of(), new Rect(6, 52, 40, 10)));
        var scaledFrame = new UiSnapshot(screen, "menu", missing.gui(), 100, 100, 1, 6, 0,
                List.of(scaledNode), List.of(scaledText), List.of(), List.of(), List.of(scaledNode.cell()), List.of());
        assertTrue(CraftingTreeScenario.nodeTtcDrawn(scaledFrame, scaledNode));
        assertTrue(LayoutValidator.validate(scaledFrame).isEmpty());
    }

    @Test
    void addonFixturesAreRegisteredInOnePlace() {
        assertTrue(AddonCpuFixture.supports("crafting-tree-screen"));
        assertNull(AddonCpuFixture.create("crafting-tree-screen"));
        assertEquals(List.of("screen", "node-ttc", "tooltip", "layout", "details", "full-chat-rates", "reset"),
                DriverResult.requiredChecks("crafting-tree-screen"));
        assertTrue(AddonCpuFixture.supports("craft-plan"));
        assertTrue(AddonCpuFixture.supports("advancedae-cpu"));
          assertTrue(AddonCpuFixture.supports("data-energistics-cpu"));
          assertThrows(IllegalStateException.class, () -> AddonCpuFixture.create("data-energistics-cpu"));
        assertTrue(AddonCpuFixture.supports("extendedae-cpu"));
        assertTrue(AddonCpuFixture.supports("extendedae-plus-cpu"));
        assertTrue(AddonCpuFixture.supports("bmaddon-cpu"));
        assertTrue(AddonCpuFixture.supports("crazyae2addons-cpu"));
        assertTrue(AddonCpuFixture.supports("megacells-cpu"));
        assertTrue(AddonCpuFixture.supports("ae2wcwt-terminal"));
        assertTrue(AddonCpuFixture.supports("ae2wtlib-terminal"));
        assertTrue(AddonCpuFixture.supports("ae2importexportcard-terminal"));
        assertTrue(AddonCpuFixture.supports("aeinfinitybooster-terminal"));
        assertTrue(AddonCpuFixture.supports("ae2networkanalyser-screen"));
        assertTrue(AddonCpuFixture.supports("merequester-screen"));
        assertTrue(AddonCpuFixture.supports("neoeco-cpu"));
        assertTrue(AddonCpuFixture.supports("omnicells-cpu"));
        assertTrue(AddonCpuFixture.supports("projectcell-cpu"));
        assertTrue(AddonCpuFixture.supports("appliede-cpu"));
        assertTrue(AddonCpuFixture.supports("appflux-cpu"));
        assertTrue(AddonCpuFixture.supports("appmek-cpu"));
        assertTrue(AddonCpuFixture.supports("appbot-cpu"));
        assertTrue(AddonCpuFixture.supports("appbot-fork-cpu"));
        assertTrue(AddonCpuFixture.supports("advancedperipherals-cpu"));
        assertTrue(AddonCpuFixture.supports("ae2things-cpu"));
        assertTrue(AddonCpuFixture.supports("expandedae-cpu"));
        assertTrue(AddonCpuFixture.supports("modern-ae2-additions-cpu"));
        assertTrue(AddonCpuFixture.supports("omnisequence-cpu"));
        assertTrue(AddonCpuFixture.supports("lightningtech-cpu"));
        assertTrue(AddonCpuFixture.supports("data-energistics-trinity-cpu"));
        assertEquals(DriverResult.TRINITY_CPU_CHECKS,
                DriverResult.requiredChecks("data-energistics-trinity-cpu"));
        assertFalse(AddonCpuFixture.supports("missing-cpu"));
        assertNull(AddonCpuFixture.create("craft-plan"));
        assertNull(AddonCpuFixture.create("ae2wcwt-terminal"));
        assertNull(AddonCpuFixture.create("ae2wtlib-terminal"));
        assertNull(AddonCpuFixture.create("ae2importexportcard-terminal"));
        assertNull(AddonCpuFixture.create("aeinfinitybooster-terminal"));
        assertNull(AddonCpuFixture.create("ae2networkanalyser-screen"));
        assertNull(AddonCpuFixture.create("merequester-screen"));
        assertEquals("ae2wcwt", WirelessTerminalFixture.create("ae2wcwt-terminal").screenshotPrefix());
        assertEquals("ae2wtlib", WirelessTerminalFixture.create("ae2wtlib-terminal").screenshotPrefix());
        assertEquals("ae2importexportcard",
                WirelessTerminalFixture.create("ae2importexportcard-terminal").screenshotPrefix());
        assertEquals("aeinfinitybooster",
                WirelessTerminalFixture.create("aeinfinitybooster-terminal").screenshotPrefix());
        assertNull(WirelessTerminalFixture.create("missing-terminal"));
        assertThrows(IllegalArgumentException.class, () -> AddonCpuFixture.create("missing-cpu"));
    }

    @Test
    void rectanglesUseStrictOverlapAndInclusiveContainment() {
        var outer = new Rect(10, 10, 20, 20);
        assertTrue(new Rect(10, 10, 20, 20).inside(outer));
        assertFalse(new Rect(9, 10, 20, 20).inside(outer));
        assertFalse(new Rect(0, 0, 10, 10).overlaps(outer));
        assertTrue(new Rect(9, 9, 2, 2).overlaps(outer));
        assertThrows(IllegalArgumentException.class, () -> new Rect(0, 0, -1, 1));
    }

    @Test
    void stableFramesResetOnChange() {
        var frames = new StableFrames<String>(3);
        assertFalse(frames.observe("a"));
        assertFalse(frames.observe("a"));
        assertTrue(frames.observe("a"));
        assertFalse(frames.observe("b"));
        frames.reset();
        assertFalse(frames.observe("b"));
        assertThrows(IllegalArgumentException.class, () -> new StableFrames<>(0));
    }

    @Test
    void alternatingStatusScaleReadinessKeysCannotBothSettle() {
        var frames = new StableFrames<Object>(8);
        var outer = List.of("STATUS_SCALES", 0, "same-rendered-row", List.of());
        var inner = List.of(1, "same-rendered-row");
        for (int tick = 0; tick < 40; tick++) {
            if (frames.observe(outer)) {
                assertFalse(frames.observe(inner));
            }
        }
        frames.reset();
        for (int tick = 0; tick < 7; tick++) assertFalse(frames.observe(outer));
        assertTrue(frames.observe(outer));
    }

    @Test
    void scenarioClockStartsOnFirstObservation() {
        assertEquals(7, CraftPlanScenario.startTime(0, 7));
        assertEquals(3, CraftPlanScenario.startTime(3, 7));
    }

    @Test
    void scenarioClockWaitsOnlyForUnobservedUsableWorld() {
        assertTrue(CraftPlanScenario.waitForUsableWorld(ScenarioState.STARTING, 0, false));
        assertFalse(CraftPlanScenario.waitForUsableWorld(ScenarioState.STARTING, 0, true));
        assertFalse(CraftPlanScenario.waitForUsableWorld(ScenarioState.STARTING, 7, false));
        assertFalse(CraftPlanScenario.waitForUsableWorld(ScenarioState.WORLD_READY, 0, false));
    }

    @Test
    void scenarioReadinessRequiresUsableClientAndMatchingConnection() {
        assertTrue(CraftPlanScenario.readyToStart(false, false, true, true, true, false, true, false), "local world");
        assertFalse(CraftPlanScenario.readyToStart(true, false, true, true, true, false, true, false), "overlay");
        assertFalse(CraftPlanScenario.readyToStart(false, true, true, true, true, false, true, false), "screen");
        assertFalse(CraftPlanScenario.readyToStart(false, false, false, true, true, false, true, false), "missing level");
        assertFalse(CraftPlanScenario.readyToStart(false, false, true, false, true, false, true, false), "missing player");
        assertFalse(CraftPlanScenario.readyToStart(false, false, true, true, false, false, true, false), "missing game mode");
        assertFalse(CraftPlanScenario.readyToStart(false, false, true, true, true, false, false, false), "missing local server");
        assertFalse(CraftPlanScenario.readyToStart(false, false, true, true, true, false, true, true), "unexpected remote server");
        assertTrue(CraftPlanScenario.readyToStart(false, false, true, true, true, true, false, true), "dedicated world");
        assertFalse(CraftPlanScenario.readyToStart(false, false, true, true, true, true, true, true), "unexpected local server");
        assertFalse(CraftPlanScenario.readyToStart(false, false, true, true, true, true, false, false), "missing remote server");
    }

    @Test
    void blockTerminalReadinessWaitsForServerAndClientBeforeOneInteraction() {
        var readiness = new CraftPlanScenario.BlockTerminalReadiness();
        var server = new CompletableFuture<Void>();
        var preparations = new AtomicInteger();
        var interactions = new AtomicInteger();

        readiness.tick(() -> {
            preparations.incrementAndGet();
            return server;
        }, () -> false, interactions::incrementAndGet);
        readiness.tick(() -> {
            preparations.incrementAndGet();
            return server;
        }, () -> true, interactions::incrementAndGet);
        assertEquals(1, preparations.get());
        assertEquals(0, interactions.get());

        server.complete(null);
        readiness.tick(() -> server, () -> false, interactions::incrementAndGet);
        assertEquals(0, interactions.get());
        readiness.tick(() -> server, () -> true, interactions::incrementAndGet);
        readiness.tick(() -> server, () -> true, interactions::incrementAndGet);
        assertEquals(1, interactions.get());
    }

    @Test
    void blockTerminalReadinessPropagatesPreparationFailuresWithoutInteraction() {
        var unreadPart = (java.util.function.BooleanSupplier) () -> {
            throw new AssertionError("missing host must not read a part");
        };
        var validations = List.<Runnable>of(
                () -> CraftPlanScenario.BlockTerminalReadiness.requirePresent(null, "missing server"),
                () -> CraftPlanScenario.BlockTerminalReadiness.requirePresent(null, "missing player"),
                () -> CraftPlanScenario.BlockTerminalReadiness.requireTerminal(false, unreadPart, "missing host"),
                () -> CraftPlanScenario.BlockTerminalReadiness.requireTerminal(true, () -> false, "missing part"));
        for (var validation : validations) {
            var readiness = new CraftPlanScenario.BlockTerminalReadiness();
            var interactions = new AtomicInteger();
            readiness.tick(() -> CompletableFuture.runAsync(validation, Runnable::run),
                    () -> true, interactions::incrementAndGet);
            var failure = assertThrows(CompletionException.class,
                    () -> readiness.tick(() -> { throw new AssertionError("resubmitted"); },
                            () -> true, interactions::incrementAndGet));
            assertInstanceOf(IllegalStateException.class, failure.getCause());
            assertTrue(failure.getCause().getMessage().startsWith("missing "));
            assertEquals(0, interactions.get());
        }
        var player = new Object();
        assertSame(player, CraftPlanScenario.BlockTerminalReadiness.requirePresent(player, "missing player"));
        assertDoesNotThrow(() -> CraftPlanScenario.BlockTerminalReadiness.requireTerminal(
                true, () -> true, "missing part"));
    }

    @Test
    void blockTerminalClientReadinessChecksEveryBoundaryInOrder() {
        var present = new Object();
        var unread = (java.util.function.BooleanSupplier) () -> {
            throw new AssertionError("unready boundary must short circuit");
        };
        assertFalse(CraftPlanScenario.BlockTerminalReadiness.clientReady(null, present, unread, unread, unread));
        assertFalse(CraftPlanScenario.BlockTerminalReadiness.clientReady(present, null, unread, unread, unread));
        assertFalse(CraftPlanScenario.BlockTerminalReadiness.clientReady(
                present, present, () -> false, unread, unread));
        assertFalse(CraftPlanScenario.BlockTerminalReadiness.clientReady(
                present, present, () -> true, () -> false, unread));
        assertFalse(CraftPlanScenario.BlockTerminalReadiness.clientReady(
                present, present, () -> true, () -> true, () -> false));
        assertTrue(CraftPlanScenario.BlockTerminalReadiness.clientReady(
                present, present, () -> true, () -> true, () -> true));
    }

    @Test
    void blockTerminalReadinessUsesNativeReachBoundaryAndTerminalFace() {
        var eye = new net.minecraft.world.phys.Vec3(0, 0, 0);
        var moves = new AtomicInteger();
        assertTrue(CraftPlanScenario.BlockTerminalReadiness.withinReach(eye,
                new net.minecraft.world.phys.Vec3(0, 0, 5), 5));
        assertFalse(CraftPlanScenario.BlockTerminalReadiness.withinReach(eye,
                new net.minecraft.world.phys.Vec3(0, 0, 5.01), 5));
        assertFalse(CraftPlanScenario.BlockTerminalReadiness.moveIfOutsideReach(eye,
                new net.minecraft.world.phys.Vec3(0, 0, 5), 5, moves::incrementAndGet));
        assertTrue(CraftPlanScenario.BlockTerminalReadiness.moveIfOutsideReach(eye,
                new net.minecraft.world.phys.Vec3(0, 0, 5.01), 5, moves::incrementAndGet));
        assertEquals(1, moves.get());

        var terminal = new net.minecraft.core.BlockPos(10, 20, 30);
        assertEquals(new net.minecraft.world.phys.Vec3(10.5, 19, 32.5),
                CraftPlanScenario.BlockTerminalReadiness.standingPosition(terminal, net.minecraft.core.Direction.SOUTH));
        assertEquals(new net.minecraft.world.phys.Vec3(10.5, 19, 28.5),
                CraftPlanScenario.BlockTerminalReadiness.standingPosition(terminal, net.minecraft.core.Direction.NORTH));
        assertEquals(new net.minecraft.world.phys.Vec3(12.5, 19, 30.5),
                CraftPlanScenario.BlockTerminalReadiness.standingPosition(terminal, net.minecraft.core.Direction.EAST));
        assertEquals(new net.minecraft.world.phys.Vec3(8.5, 19, 30.5),
                CraftPlanScenario.BlockTerminalReadiness.standingPosition(terminal, net.minecraft.core.Direction.WEST));
        assertEquals(new net.minecraft.world.phys.Vec3(10.5, 21, 30.5),
                CraftPlanScenario.BlockTerminalReadiness.standingPosition(terminal, net.minecraft.core.Direction.UP));
        assertEquals(new net.minecraft.world.phys.Vec3(10.5, 17, 30.5),
                CraftPlanScenario.BlockTerminalReadiness.standingPosition(terminal, net.minecraft.core.Direction.DOWN));
    }

    @Test
    void everyScenarioTransitionIsExplicit() {
        var path = List.of(ScenarioState.STARTING, ScenarioState.WORLD_READY, ScenarioState.TERMINAL_OPEN,
                ScenarioState.PLAN_OPEN, ScenarioState.PLAN_STABLE, ScenarioState.BASE_CHECKED,
                ScenarioState.SORTS_CHECKED, ScenarioState.TOOLTIP_CHECKED, ScenarioState.RESULT_WRITTEN,
                ScenarioState.QUIT_REQUESTED);
        for (int i = 0; i < path.size() - 1; i++) {
            assertTrue(ScenarioFlow.allows(path.get(i), path.get(i + 1)));
        }
        var addonPath = List.of(ScenarioState.PLAN_STABLE, ScenarioState.ADDON_CPU_SELECTED,
                ScenarioState.ADDON_CRAFT_SUBMITTED, ScenarioState.ADDON_SAMPLE_RECORDED,
                ScenarioState.ADDON_PLAN_OPEN, ScenarioState.RESULT_WRITTEN);
        for (int i = 0; i < addonPath.size() - 1; i++) {
            assertTrue(ScenarioFlow.allows(addonPath.get(i), addonPath.get(i + 1)));
        }
        assertTrue(ScenarioFlow.allows(ScenarioState.PLAN_STABLE, ScenarioState.FAILED));
        assertTrue(ScenarioFlow.allows(ScenarioState.PLAN_STABLE, ScenarioState.RESULT_WRITTEN));
        assertFalse(ScenarioFlow.allows(ScenarioState.PLAN_STABLE, ScenarioState.TOOLTIP_CHECKED));
        assertFalse(ScenarioFlow.allows(ScenarioState.FAILED, ScenarioState.FAILED));
        assertFalse(ScenarioFlow.allows(ScenarioState.QUIT_REQUESTED, ScenarioState.FAILED));
    }

    @Test
    void sortObservationRequiresACompleteReverse() {
        assertTrue(SortObservation.valid(List.of("b", "a", "unknown"), List.of("a", "b", "unknown"),
                List.of("b", "a", "unknown"), List.of("a", "b"), List.of("b", "a")));
        assertFalse(SortObservation.valid(List.of(), List.of("a"), List.of("a"), List.of("a"), List.of("a")));
        assertFalse(SortObservation.valid(List.of("a"), List.of("a"), List.of("a", "b"), List.of("a"), List.of("a")));
        assertFalse(SortObservation.valid(List.of("a"), List.of("a", "b"), List.of("b", "a"),
                List.of("a", "b"), List.of("b", "a")));
        assertFalse(SortObservation.valid(List.of("a", "b"), List.of("a", "b"), List.of("a", "b"),
                List.of("a", "b"), List.of("a", "b")));
        assertFalse(SortObservation.valid(List.of("a"), List.of("a"), List.of("a"), List.of(), List.of()));
        assertFalse(SortObservation.valid(List.of("a"), List.of("a"), List.of("a"), List.of("a", "b"),
                List.of("a")));
        assertFalse(SortObservation.valid(List.of("a"), List.of("a"), List.of("a"), List.of("a"),
                List.of("a", "b")));
        assertFalse(SortObservation.valid(List.of("a"), List.of("a"), List.of("b"), List.of("a"),
                List.of("b")));
        assertFalse(SortObservation.valid(List.of("b"), List.of("a"), List.of("a"), List.of("a"),
                List.of("a")));
        assertFalse(SortObservation.valid(List.of("a", "b"), List.of("a", "b"), List.of("b", "a"),
                List.of("a", "b"), List.of("a", "b")));
        assertFalse(SortObservation.valid(List.of("a", "b"), List.of("b", "a"), List.of("a", "b"),
                List.of("a"), List.of("a")));
        assertFalse(SortObservation.valid(List.of("a", "b"), List.of("a", "b"), List.of("b", "a"),
                List.of("a"), List.of("a")));
        assertFalse(SortObservation.valid(List.of("a", "b", "c"), List.of("a", "b", "c"),
                List.of("c", "b", "a"), List.of("a"), List.of("a")));
        assertFalse(SortObservation.valid(List.of("a", "b", "c"), List.of("a", "b", "c"),
                List.of("a", "c", "b"), List.of("a"), List.of("a")));
    }

    @Test
    void sortObservationSeparatesMissingRowsFromTtcOrder() {
        var missing = new UiSnapshot.Row("missing", 0, 8, new Rect(0, 0, 1, 1), List.of());
        var timed = new UiSnapshot.Row("timed", 1, 0, new Rect(0, 0, 1, 1), List.of());
        var unknown = new UiSnapshot.Row("unknown", 2, 0, new Rect(0, 0, 1, 1), List.of());

        var missingFirst = List.of(missing, timed, unknown);
        assertEquals(List.of("timed", "unknown"), SortObservation.sortableIds(missingFirst));
        assertTrue(SortObservation.missingFirst(missingFirst));
        assertFalse(SortObservation.missingFirst(List.of(timed, missing)));
        assertTrue(SortObservation.missingFirst(List.of(timed, unknown)));
        assertTrue(SortObservation.missingFirst(List.of()));
        assertTrue(SortObservation.valid(List.of("timed"), List.of("timed"), List.of("timed"),
                List.of("timed"), List.of("timed")));
    }

    @Test
    void layoutRejectsOutsideAndOwnedIntersections() {
        var text = new UiSnapshot.ObservedText("text.ae2craftingtime.ttc", "TTC", List.of(),
                new Rect(12, 12, 5, 5));
        var valid = snapshot(text, List.of(), List.of(), new Rect(10, 10, 30, 30));
        assertTrue(LayoutValidator.validate(valid).isEmpty());
        assertEquals(List.of("text text.ae2craftingtime.ttc overlaps item cell"),
                LayoutValidator.validate(snapshot(text, List.of(), List.of(new Rect(11, 11, 3, 3)),
                        new Rect(10, 10, 30, 30))));
        assertEquals(List.of("text text.ae2craftingtime.ttc overlaps widget"),
                LayoutValidator.validate(snapshot(text,
                        List.of(new UiSnapshot.Widget("button", "", new Rect(11, 11, 3, 3), List.of())), List.of(),
                        new Rect(10, 10, 30, 30))));
        assertEquals(List.of("text text.ae2craftingtime.ttc outside GUI"),
                LayoutValidator.validate(snapshot(text, List.of(), List.of(), new Rect(20, 20, 30, 30))));
    }

    @Test
    void standardStatusAllowsUnboxedLabelsOnlyWhenBackgroundIsOff() {
        var text = new UiSnapshot.ObservedText("text.ae2craftingtime.ttc_delayed", "Delayed", List.of(),
                new Rect(12, 12, 8, 5));
        var unboxed = snapshot(text, List.of(), List.of(), new Rect(10, 10, 30, 30));
        var boxed = new UiSnapshot("screen", "menu", unboxed.gui(), 100, 100, 1, 1, 0,
                List.of(), List.of(text), List.of(new Rect(11, 11, 10, 8)), List.of(), List.of(), List.of());
        assertTrue(StandardAe2Scenario.statusBadgesValid(unboxed, false));
        assertFalse(StandardAe2Scenario.statusBadgesValid(unboxed, true));
        assertTrue(StandardAe2Scenario.statusBadgesValid(boxed, true));
        assertFalse(StandardAe2Scenario.statusBadgesValid(boxed, false));
    }

    @Test
    void requesterBadgesRejectTheOriginalItemOverlap() {
        var gui = new Rect(100, 40, 195, 250);
        var item = new Rect(127, 60, 16, 16);
        var statusOffset = RequesterTtcLayout.statusOffset("request_status_0");
        var widgets = List.of(
                new UiSnapshot.Widget("amount", "", new Rect(146, 59, 52, 12), List.of()),
                new UiSnapshot.Widget("status", "", new Rect(147 + statusOffset, 74, 118 - statusOffset, 2),
                        List.of()));
        var oldBadge = new Rect(126, 70, 28, 9);
        var rowBadge = new Rect(gui.x() + RequesterTtcLayout.BADGE_X,
                gui.y() + RequesterTtcLayout.rowTop(19, 19, 0), 28, 7);
        var headerBadge = new Rect(246, 46, 28, 9);
        for (var badge : List.of(oldBadge, rowBadge)) {
            var frame = new UiSnapshot(MeRequesterFixture.SCREEN, "menu", gui, 400, 400, 1, 1, 0,
                    List.of(), List.of(), List.of(badge, headerBadge), widgets, List.of(item), List.of());
            assertEquals(badge == rowBadge, LayoutValidator.validateBadges(frame).isEmpty());
        }
    }

    @Test
    void markerRejectsSourceAndMalformedCopies() throws Exception {
        var marker = temporary.resolve(".ae2-crafting-time-test-fixture.json");
        Files.writeString(marker, """
                {"schema":1,"scenario":"craft-plan","sourceFixtureId":"ae2-crafting-time","disposableWorldId":"copy",
                 "terminal":{"x":1,"y":2,"z":3,"face":"SOUTH"},"outputId":"minecraft:furnace"}
                """);
        assertEquals("copy", FixtureMarker.read(temporary).disposableWorldId());
        Files.writeString(marker, "{\"schema\":1}");
        assertThrows(IllegalArgumentException.class, () -> FixtureMarker.read(temporary));
        Files.writeString(marker, """
                {"schema":1,"scenario":"craft-plan","sourceFixtureId":"wrong","disposableWorldId":"copy",
                 "terminal":{"x":1,"y":2,"z":3,"face":"SOUTH"},"outputId":"minecraft:furnace"}
                """);
        assertThrows(IllegalArgumentException.class, () -> FixtureMarker.read(temporary));
    }

    @Test
    void resultIsAtomicAndRequiresExactChecks() throws Exception {
        var checks = checks(true);
        var result = new DriverResult(1, true, "driver.jar", "1.20.1-forge", "compatible", "craft-plan",
                "PASS", "en_us", java.util.Map.of(), null, checks, List.of("a.png", "b.png"), null);
        AtomicResultWriter.write(temporary, result);
        assertTrue(Files.exists(temporary.resolve("result.json")));
        assertFalse(Files.exists(temporary.resolve("result.json.tmp")));
        checks.put("extra", true);
        assertThrows(IllegalArgumentException.class, () -> new DriverResult(1, true, "driver.jar",
                "1.20.1-forge", "compatible", "craft-plan", "PASS", "en_us", java.util.Map.of(), null, checks, List.of(), null));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("neoeco-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("lightningtech-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("advancedae-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("extendedae-plus-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("bmaddon-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("crazyae2addons-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("megacells-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("omnicells-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("projectcell-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("appliede-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("appflux-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("appmek-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("appbot-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("appbot-fork-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("advancedperipherals-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("ae2things-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("expandedae-cpu"));
        assertEquals(List.of("cpu-selected", "job-accepted", "dispatch-amount", "returned-amount", "job-finished", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("modern-ae2-additions-cpu"));
        assertEquals(List.of("screen", "ttc-tooltip", "plan-ttc"),
                DriverResult.requiredChecks("ae2wcwt-terminal"));
        assertEquals(List.of("screen", "ttc-tooltip", "plan-ttc"),
                DriverResult.requiredChecks("ae2wtlib-terminal"));
        assertEquals(List.of("screen", "ttc-tooltip", "plan-ttc"),
                DriverResult.requiredChecks("ae2importexportcard-terminal"));
        assertEquals(List.of("screen", "plan-ttc"),
                DriverResult.requiredChecks("aeinfinitybooster-terminal"));
        assertEquals(List.of("screen", "layout"),
                DriverResult.requiredChecks("ae2networkanalyser-screen"));
        assertEquals(List.of("screen", "ttc-row", "total-ttc", "layout"),
                DriverResult.requiredChecks("merequester-screen"));
        var file = temporary.resolve("not-a-directory");
        Files.writeString(file, "x");
        assertThrows(Exception.class, () -> AtomicResultWriter.write(file, result));
    }

    @Test
    void endpointRequiresLoopbackSecretSizeAndOneController() {
        var token = "a".repeat(64);
        var policy = new EndpointPolicy(token);
        assertFalse(policy.authenticate("10.0.0.1", "Bearer " + token, 1, true));
        assertFalse(policy.authenticate("127.0.0.1", null, 1, true));
        assertFalse(policy.authenticate("127.0.0.1", "Bearer bad", 1, true));
        assertFalse(policy.authenticate("127.0.0.1", "Bearer " + token, EndpointPolicy.MAX_REQUEST_BYTES + 1, true));
        assertTrue(policy.authenticate("127.0.0.1", "Bearer " + token, 1, true));
        assertFalse(policy.authenticate("127.0.0.1", "Bearer " + token, 1, true));
        assertTrue(policy.authenticate("127.0.0.1", "Bearer " + token, -1, false));
        assertEquals(6, EndpointPolicy.TOOLS.size());
        assertThrows(IllegalArgumentException.class, () -> new EndpointPolicy("short"));
        assertThrows(IllegalArgumentException.class,
                () -> EndpointPolicy.bounded("x".repeat(EndpointPolicy.MAX_RESPONSE_BYTES + 1)));
    }

    @Test
    void schedulerBoundsCapacityAndTimeout() throws Exception {
        var direct = new DriverScheduler(1, Duration.ofSeconds(1));
        assertEquals("ok", direct.call(Runnable::run, () -> "ok"));
        Executor parked = command -> { };
        var bounded = new DriverScheduler(1, Duration.ofMillis(1));
        assertThrows(TimeoutException.class, () -> bounded.call(parked, () -> "never"));
        assertThrows(IllegalStateException.class, () -> bounded.call(parked, () -> "full"));
    }

    @Test
    void reportTextRedactsSecretsAndPaths() {
        var text = ReportText.safe("C:\\Users\\name\\file bearer abc /home/name/file");
        assertFalse(text.contains("Users"));
        assertFalse(text.contains("abc"));
        assertFalse(text.contains("/home"));
        assertEquals("", ReportText.safe(null));
    }

    @Test
    void reportTextIncludesNestedFailure() {
        var error = new java.util.concurrent.CompletionException(new IllegalStateException("fixture failed"));

        assertEquals("CompletionException: java.lang.IllegalStateException: fixture failed <- IllegalStateException: fixture failed",
                ReportText.failure(error));
    }

    @Test
    void wirelessTooltipRecognizesOnlyRegisteredScreens() {
        assertTrue(UiObservationStore.isWirelessScreen(
                "com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen"));
        assertTrue(UiObservationStore.isWirelessScreen("de.mari_023.ae2wtlib.wct.WCTScreen"));
        assertFalse(UiObservationStore.isWirelessScreen("appeng.client.gui.me.items.CraftingTermScreen"));
    }

    @Test
    void statusRowsUseRenderedTextWhenTheTableHasNoDescriptionHook() {
        var cell = new Rect(10, 20, 67, 22);
        var inside = new UiSnapshot.ObservedText("text.ae2craftingtime.ttc", "~1:00", List.of("~1:00"),
                new Rect(20, 30, 20, 6));
        var outside = new UiSnapshot.ObservedText("text.ae2craftingtime.ttc", "~2:00", List.of("~2:00"),
                new Rect(80, 30, 20, 6));
        assertEquals(List.of(inside), UiObservationStore.rowDescription(
                java.util.Map.of(), List.of(inside, outside), "minecraft:stone", cell));

        var recorded = new UiSnapshot.ObservedText("literal", "recorded", List.of(), null);
        assertEquals(List.of(recorded), UiObservationStore.rowDescription(
                java.util.Map.of("minecraft:stone", List.of(recorded)), List.of(inside), "minecraft:stone", cell));
    }

    @Test
    void cpuListItemModeWaitsForTwoRowsButOnlyOneKnownTtc() {
        var ttc = new UiSnapshot.ObservedText("text.ae2craftingtime.ttc", "~1:00", List.of("~1:00"), null);
        var known = new UiSnapshot.Row("minecraft:stone", 8, 0, new Rect(0, 0, 1, 1), List.of(ttc));
        var waiting = new UiSnapshot.Row("minecraft:smooth_stone", 8, 0, new Rect(1, 0, 1, 1), List.of());
        assertTrue(CpuListTtcScenario.itemRowsReady(List.of(known, waiting)));
        assertFalse(CpuListTtcScenario.itemRowsReady(List.of(known)));
        assertFalse(CpuListTtcScenario.itemRowsReady(List.of(waiting, waiting)));
    }

    @Test
    void cpuListDelayedReplyUsesTheHeldRequestsActualAge() {
        var request = new CpuTtcPacketControl.ObservedRequest(4, 1_000, List.of(1));
        var capture = new CpuTtcPacketControl.RequestCapture(List.of(request), List.of(1));
        assertFalse(CpuListTtcScenario.heldRequestExpired(capture, -1, 4_000));
        assertFalse(CpuListTtcScenario.heldRequestExpired(capture, 5, 4_000));
        assertFalse(CpuListTtcScenario.heldRequestExpired(capture, 4, 3_999));
        assertTrue(CpuListTtcScenario.heldRequestExpired(capture, 4, 4_000));
    }

    private static LinkedHashMap<String, Boolean> checks(boolean value) {
        var checks = new LinkedHashMap<String, Boolean>();
        DriverResult.requiredChecks("craft-plan").forEach(key -> checks.put(key, value));
        return checks;
    }

    private static UiSnapshot snapshot(UiSnapshot.ObservedText text, List<UiSnapshot.Widget> widgets,
            List<Rect> cells, Rect gui) {
        return new UiSnapshot("screen", "menu", gui, 100, 100, 1, 1, 0, List.of(), List.of(text), List.of(),
                widgets, cells, List.of());
    }

    private static UiSnapshot cpuSnapshot(Rect textBounds, Rect badge) {
        return cpuSnapshot(textBounds, badge, "~1s");
    }

    private static UiSnapshot cpuSnapshot(Rect textBounds, Rect badge, String rendered) {
        return cpuSnapshotWithText(textBounds == null ? null
                : new UiSnapshot.ObservedText("text.ae2craftingtime.ttc", rendered, List.of(rendered), textBounds), badge);
    }

    private static UiSnapshot cpuSnapshotWithText(UiSnapshot.ObservedText text, Rect badge) {
        var card = new UiSnapshot.CpuCard(4, "CPU", "minecraft:smooth_stone", 8, 9, false,
                new Rect(10, 10, 100, 60), new Rect(12, 12, 30, 12), new Rect(12, 45, 30, 10),
                new Rect(60, 45, 30, 10), text, badge);
        var name = new UiSnapshot.ObservedText("native-name", "CPU", List.of(), new Rect(14, 14, 20, 8));
        return new UiSnapshot("screen", "menu", new Rect(0, 0, 120, 80), 200, 100, 2, 1, 0,
                List.of(), List.of(name), List.of(), List.of(), List.of(), List.of(), List.of(card));
    }

    private static UiSnapshot selectedCpuSnapshot(Rect textBounds, Rect badge, String rendered) {
        var base = cpuSnapshot(textBounds, badge, rendered);
        var original = base.cpuCards().get(0);
        var card = new UiSnapshot.CpuCard(original.serial(), original.name(), original.jobId(), original.amount(),
                original.elapsedNanos(), true, original.bounds(), original.nameArea(), original.infoArea(),
                original.progressArea(), original.ttc(), original.badge());
        var texts = new ArrayList<>(base.text());
        texts.add(new UiSnapshot.ObservedText("native-title", "TTC: " + rendered,
                List.of(), new Rect(5, 2, 80, 8)));
        return new UiSnapshot(base.screen(), base.menu(), base.gui(), base.screenWidth(), base.screenHeight(),
                base.guiScale(), base.frame(), base.scroll(), base.rows(), texts, base.badges(), base.widgets(),
                base.itemCells(), base.tooltip(), List.of(card));
    }

    private static CpuListTtcControl.CpuState cpuState(int serial, String jobId, long amount,
            boolean busy, long progress) {
        return new CpuListTtcControl.CpuState("0,0," + serial, serial, jobId, amount, busy, busy,
                busy ? new com.ctux.ae2craftingtime.core.TtcEstimate(1000, true) : null, 1, progress);
    }

    private static CpuListTtcControl.ServerState serverState(CpuListTtcControl.CpuState... cpus) {
        return new CpuListTtcControl.ServerState("network", 1, 1, true, true, List.of(cpus));
    }
}
