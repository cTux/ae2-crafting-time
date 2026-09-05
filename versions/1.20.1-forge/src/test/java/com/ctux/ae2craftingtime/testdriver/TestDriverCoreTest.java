package com.ctux.ae2craftingtime.testdriver;

import com.ctux.ae2craftingtime.core.RequesterTtcLayout;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDriverCoreTest {
    @Test
    void standardResultCannotOmitAnyRequiredPlanStatusOrOutputCheck() {
        assertFalse(AddonCpuFixture.supports("standard-ae2"));
        assertEquals(6, StandardAe2Scenario.CHECKS.size());
        for (var entry : StandardAe2Scenario.CHECKS.entrySet()) {
            var scenario = entry.getKey();
            assertTrue(AddonCpuFixture.supports(scenario));
            assertNull(AddonCpuFixture.create(scenario));
            assertEquals(entry.getValue(), DriverResult.requiredChecks(scenario));
            assertTrue(new StandardAe2Scenario(scenario).checkpoint().contains("PREPARE"));
            new StandardAe2Scenario(scenario).releaseKeys();
            for (String missing : entry.getValue()) {
                var checks = new LinkedHashMap<String, Boolean>();
                entry.getValue().stream().filter(key -> !key.equals(missing)).forEach(key -> checks.put(key, true));
                assertThrows(IllegalArgumentException.class, () -> new DriverResult(1, true, "driver.jar",
                        "1.20.1-forge", "compatible", scenario, "PASS", "en_us", java.util.Map.of(), null, checks, List.of(), null));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> new StandardAe2Scenario("standard-ae2"));
    }

    @Test
    void smokeUsesThePackagedCataloguesOrderForEveryTarget() {
        assertEquals("batched-long", SmokeAdapterCatalog.newest("1.20.1-forge").get("neoecoae"));
        assertEquals("batched-int", SmokeAdapterCatalog.newest("1.21.1-neoforge").get("neoecoae"));
        assertEquals("tree-layout", SmokeAdapterCatalog.newest("1.20.1-fabric").get("ae2ct"));
        assertEquals(java.util.Map.of("advanced_ae", "advanced-cpu"), SmokeAdapterCatalog.newest("26.1.2-neoforge"));
        assertTrue(SmokeAdapterCatalog.newest("unknown").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> SmokeAdapterCatalog.main(new String[0]));
        SmokeAdapterCatalog.main(new String[] { "unknown" });
    }

    @Test
    void noSpaceRequiresTheRenderedWarningAndBothAdviceLines() {
        assertTrue(AddonCpuFixture.supports(NoSpaceScenario.SCENARIO));
        assertNull(AddonCpuFixture.create(NoSpaceScenario.SCENARIO));
        assertEquals(NoSpaceScenario.CHECKS, DriverResult.requiredChecks(NoSpaceScenario.SCENARIO));
        var tooltip = List.of(NoSpaceScenario.KEY, NoSpaceScenario.KEY + ".explanation",
                NoSpaceScenario.KEY + ".suggestion").stream()
                .map(key -> new UiSnapshot.ObservedText(key, key, List.of(), null)).toList();
        assertTrue(NoSpaceScenario.tooltipReady(tooltip));
        for (int missing = 0; missing < tooltip.size(); missing++) {
            var incomplete = new java.util.ArrayList<>(tooltip);
            incomplete.remove(missing);
            assertFalse(NoSpaceScenario.tooltipReady(incomplete));
        }
        assertFalse(NoSpaceScenario.tooltipReady(List.of()));
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
        var stone = new UiSnapshot.Row("minecraft:stone", 1, null, ready);
        var smooth = new UiSnapshot.Row("minecraft:smooth_stone", 1, null, ready);
        assertFalse(StandardAe2Scenario.planEstimatesReady(List.of()));
        assertFalse(StandardAe2Scenario.planEstimatesReady(List.of(stone)));
        for (var unresolved : List.of(pending, List.<UiSnapshot.ObservedText>of())) {
            assertFalse(StandardAe2Scenario.planEstimatesReady(List.of(smooth,
                    new UiSnapshot.Row("minecraft:stone", 1, null, unresolved))));
            assertFalse(StandardAe2Scenario.planEstimatesReady(List.of(stone,
                    new UiSnapshot.Row("minecraft:smooth_stone", 1, null, unresolved))));
        }
        assertTrue(StandardAe2Scenario.planEstimatesReady(List.of(smooth, stone)));
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
                assertEquals(drawBadge && text == total, CraftPlanScenario.renderedPlan(frame));
            }
        }
    }

    @Test
    void addonFixturesAreRegisteredInOnePlace() {
        assertTrue(AddonCpuFixture.supports("crafting-tree-screen"));
        assertNull(AddonCpuFixture.create("crafting-tree-screen"));
        assertEquals(List.of("screen", "node-ttc", "tooltip", "layout", "details", "reset"),
                DriverResult.requiredChecks("crafting-tree-screen"));
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
