package com.ctux.ae2craftingtime.testdriver;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SuitePlanTest {
    @TempDir Path temporary;
    private static final String FIRST = "ae2ct-" + "1".repeat(32);
    private static final String SECOND = "ae2ct-" + "2".repeat(32);
    private final SuitePlan.Case first = new SuitePlan.Case("craft-plan", FIRST);
    private final SuitePlan.Case second = new SuitePlan.Case("merequester-screen", SECOND);

    private DriverOptions options() {
        return new DriverOptions("suite", "compatible", FIRST, temporary, false);
    }

    @Test void parsesAPlanAndDerivesIsolatedOutputs() throws Exception {
        var plan = new SuitePlan(1, List.of(first, second));
        Files.writeString(temporary.resolve("suite-plan.json"), new Gson().toJson(plan));
        var cases = SuitePlan.read(options());
        assertEquals(List.of("craft-plan", "merequester-screen"), cases.stream().map(DriverOptions::scenario).toList());
        assertEquals(temporary.resolve("craft-plan"), cases.get(0).output());
        assertEquals(SECOND, cases.get(1).world());
        assertFalse(cases.get(0).interactive());
        Files.writeString(temporary.resolve("suite-plan.json"), "null");
        assertThrows(IllegalArgumentException.class, () -> SuitePlan.read(options()));
        Files.writeString(temporary.resolve("suite-plan.json"), "{");
        assertThrows(Exception.class, () -> SuitePlan.read(options()));
    }

    @Test void vmTextureProbeChangesOnlyTheOptedInSvgaSquare() {
        assertEquals(8192, VmTextureProbe.height(true, "suite", "SVGA3D; LLVM", 16384, 16384));
        assertEquals(16384, VmTextureProbe.height(false, "suite", "SVGA3D", 16384, 16384));
        assertEquals(16384, VmTextureProbe.height(true, "", "SVGA3D", 16384, 16384));
        assertEquals(16384, VmTextureProbe.height(true, "suite", null, 16384, 16384));
        assertEquals(16384, VmTextureProbe.height(true, "suite", "NVIDIA", 16384, 16384));
        assertEquals(16384, VmTextureProbe.height(true, "suite", "SVGA3D", 32768, 16384));
        assertEquals(8192, VmTextureProbe.height(true, "suite", "SVGA3D", 16384, 8192));
    }

    @Test void rejectsInvalidPlansBeforeOpeningAnyWorld() {
        for (var plan : List.of(new SuitePlan(2, List.of(first)), new SuitePlan(1, null),
                new SuitePlan(1, List.of()), new SuitePlan(1, Collections.nCopies(65, first)),
                new SuitePlan(1, Arrays.asList((SuitePlan.Case) null)),
                new SuitePlan(1, List.of(new SuitePlan.Case(null, FIRST))),
                new SuitePlan(1, List.of(new SuitePlan.Case("../escape", FIRST))),
                new SuitePlan(1, List.of(new SuitePlan.Case("craft-plan", null))),
                new SuitePlan(1, List.of(new SuitePlan.Case("craft-plan", "ae2-crafting-time"))),
                new SuitePlan(1, List.of(first, new SuitePlan.Case("craft-plan", SECOND))),
                new SuitePlan(1, List.of(first, new SuitePlan.Case("merequester-screen", FIRST))),
                new SuitePlan(1, List.of(second)))) {
            assertThrows(IllegalArgumentException.class, () -> plan.options(options()));
        }
        assertThrows(IllegalArgumentException.class, () -> new SuitePlan(1, List.of(first)).options(
                new DriverOptions("suite", "compatible", FIRST, temporary, true)));
    }

    @Test void expandedSuitesReachCaseValidationThroughSixtyFourEntries() {
        var names = List.of("standard-plan-controls", "standard-status-controls", "waiting-status",
                "running-status", "delayed-status", "craft-lifecycle", "craft-plan", "crafting-tree-screen",
                "advancedae-cpu", "extendedae-cpu", "extendedae-plus-cpu", "bmaddon-cpu", "crazyae2addons-cpu",
                "appbot-fork-cpu", "advancedperipherals-cpu", "ae2things-cpu", "megacells-cpu", "lightningtech-cpu",
                "omnicells-cpu", "projectcell-cpu", "appliede-cpu", "appflux-cpu", "appmek-cpu",
                "modern-ae2-additions-cpu", "omnisequence-cpu", "ae2wcwt-terminal", "ae2wtlib-terminal",
                "ae2importexportcard-terminal", "aeinfinitybooster-terminal", "merequester-screen",
                "ae2networkanalyser-screen", "no-space-status", "no-provider-status", "no-power-status");
        for (int count : List.of(1, 32, 34)) {
            var cases = java.util.stream.IntStream.range(0, count).mapToObj(index -> new SuitePlan.Case(
                    names.get(index), index == 0 ? FIRST : String.format("ae2ct-%032x", index))).toList();
            assertEquals(count, new SuitePlan(1, cases).options(options()).size());
        }
        // There are fewer than 64 registered leaves today. At 64, duplicate
        // validation must still run; at 65 the count gate must reject first.
        assertEquals("invalid or duplicate suite case", assertThrows(IllegalArgumentException.class,
                () -> new SuitePlan(1, Collections.nCopies(64, first)).options(options())).getMessage());
        assertEquals("invalid suite schema, case count, or interactive mode", assertThrows(IllegalArgumentException.class,
                () -> new SuitePlan(1, Collections.nCopies(65, first)).options(options())).getMessage());
    }

    @Test void requiresTheMatchingDisposableMarker() throws Exception {
        var saves = temporary.resolve("saves");
        var world = Files.createDirectories(saves.resolve(FIRST));
        var marker = new FixtureMarker(1, "craft-plan", "ae2-crafting-time", FIRST,
                new FixtureMarker.Position(0, 0, 0, "NORTH"), "minecraft:furnace");
        Files.writeString(world.resolve(".ae2-crafting-time-test-fixture.json"), new Gson().toJson(marker));
        SuitePlan.verifyWorld(saves, options());
        Files.writeString(world.resolve(".ae2-crafting-time-test-fixture.json"), new Gson().toJson(
                new FixtureMarker(1, "craft-plan", "ae2-crafting-time", SECOND, marker.terminal(), marker.outputId())));
        assertThrows(IllegalArgumentException.class, () -> SuitePlan.verifyWorld(saves, options()));
        assertThrows(Exception.class, () -> SuitePlan.verifyWorld(temporary.resolve("missing"), options()));
    }

    @Test void failureAfterAPassPreservesEarlierEvidenceAndLeavesLaterCasesUnrun() {
        var third = new SuitePlan.Case("delayed-status", "ae2ct-" + "3".repeat(32));
        var cases = new SuitePlan(1, List.of(first, second, third)).options(options());
        var progress = new SuiteProgress(cases);
        var now = Instant.parse("2026-09-05T00:00:00Z");
        progress.start(now);
        assertTrue(progress.finish(true, now.plusSeconds(1)));
        progress.start(now.plusSeconds(2));
        assertFalse(progress.finish(false, now.plusSeconds(3)));
        var result = progress.snapshot(42);
        assertEquals("FAIL", result.result());
        assertFalse(result.complete());
        assertEquals(List.of("PASS", "FAIL", "NOT_RUN"),
                result.cases().stream().map(SuiteProgress.CaseResult::result).toList());
        assertEquals(now.toString(), result.cases().get(0).startedAt());
        assertEquals(now.plusSeconds(1).toString(), result.cases().get(0).finishedAt());
    }
    @Test void reportsProgressWithoutTurningUnfinishedCasesIntoPasses() throws Exception {
        var cases = new SuitePlan(1, List.of(first, second)).options(options());
        var progress = new SuiteProgress(cases);
        var now = Instant.parse("2026-09-02T00:00:00Z");
        assertEquals("NOT_RUN", progress.snapshot(42).cases().get(0).result());
        progress.start(now);
        assertEquals("RUNNING", progress.snapshot(42).result());
        assertTrue(progress.finish(true, now.plusSeconds(1)));
        assertFalse(progress.snapshot(42).complete());
        progress.start(now.plusSeconds(2));
        assertFalse(progress.finish(true, now.plusSeconds(3)));
        var result = progress.snapshot(42);
        assertTrue(result.complete());
        assertEquals("PASS", result.result());
        assertEquals(42, result.processId());
        assertEquals(now.toString(), result.cases().get(0).startedAt());
        AtomicResultWriter.write(temporary, result);
        assertEquals("PASS", new Gson().fromJson(Files.readString(temporary.resolve("result.json")),
                SuiteProgress.Result.class).result());
        var failure = new SuiteProgress(cases);
        failure.start(now);
        assertFalse(failure.finish(false, now));
        assertEquals("FAIL", failure.snapshot(42).result());
        assertFalse(failure.snapshot(42).complete());
        assertEquals("NOT_RUN", failure.snapshot(42).cases().get(1).result());
        UiObservationStore.reset();
        assertNull(UiObservationStore.latest());
        assertTrue(UiObservationStore.wirelessTooltip().isEmpty());
    }
}
