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
                new SuitePlan(1, List.of()), new SuitePlan(1, Collections.nCopies(33, first)),
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
