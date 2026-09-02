package com.ctux.ae2craftingtime.testdriver;

import com.ctux.ae2craftingtime.mc1201.TtcText;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDriverCoreTest {
    @TempDir
    Path temporary;

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
    void addonFixturesAreRegisteredInOnePlace() {
        assertTrue(AddonCpuFixture.supports("craft-plan"));
        assertTrue(AddonCpuFixture.supports("advancedae-cpu"));
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
    void scenarioClockStartsOnFirstObservation() {
        assertEquals(7, CraftPlanScenario.startTime(0, 7));
        assertEquals(3, CraftPlanScenario.startTime(3, 7));
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
                "PASS", checks, List.of("a.png", "b.png"), null);
        AtomicResultWriter.write(temporary, result);
        assertTrue(Files.exists(temporary.resolve("result.json")));
        assertFalse(Files.exists(temporary.resolve("result.json.tmp")));
        checks.put("extra", true);
        assertThrows(IllegalArgumentException.class, () -> new DriverResult(1, true, "driver.jar",
                "1.20.1-forge", "compatible", "craft-plan", "PASS", checks, List.of(), null));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("neoeco-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("lightningtech-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("advancedae-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("extendedae-plus-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("bmaddon-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("crazyae2addons-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("megacells-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("omnicells-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("projectcell-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("appliede-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("appflux-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("appmek-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("appbot-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("appbot-fork-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("advancedperipherals-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("ae2things-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
                DriverResult.requiredChecks("expandedae-cpu"));
        assertEquals(List.of("cpu-selected", "profile-sample", "ttc-after-sample"),
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
}
