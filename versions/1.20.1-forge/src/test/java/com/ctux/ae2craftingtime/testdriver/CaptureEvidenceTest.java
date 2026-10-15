package com.ctux.ae2craftingtime.testdriver;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CaptureEvidenceTest {
    @TempDir Path directory;

    @Test void captureKeepsTheSnapshotAndBindsTheCompletedBytes() throws Exception {
        var world = "ae2ct-00000000000000000000000000000000";
        var options = new DriverOptions("craft-plan", "compatible", world, directory, false);
        var snapshot = CaptureEvidence.snapshot(null, "world", 80, 60, 2, 9);
        assertEquals("world", snapshot.screen());
        assertNull(snapshot.status());
        assertEquals(new Rect(0, 0, 80, 60), snapshot.gui());
        assertSame(snapshot, CaptureEvidence.snapshot(snapshot, "world", 80, 60, 2, 10));
        assertEquals("new", CaptureEvidence.snapshot(snapshot, "new", 80, 60, 2, 10).screen());
        var image = directory.resolve("checkpoint.png");
        Files.write(image, new byte[]{1,2,3});
        CaptureEvidence.write(image, options, snapshot, 160, 120, 10, "renderer", System.nanoTime());
        var json = JsonParser.parseString(Files.readString(directory.resolve("checkpoint.json"))).getAsJsonObject();
        assertEquals(9, json.get("frame").getAsLong());
        assertTrue(json.get("status").isJsonNull());
        assertTrue(json.get("tableCellBoundsSource").getAsString().startsWith("expected-layout-policy"));
        assertTrue(json.getAsJsonObject("nativeHoveredCell").entrySet().isEmpty());
        var capture = json.getAsJsonObject("capture");
        assertEquals(10, capture.get("frame").getAsLong());
        assertEquals(world + "/craft-plan/checkpoint.png", capture.get("id").getAsString());
        assertEquals(CaptureEvidence.sha256(new byte[]{1,2,3}), capture.get("sha256").getAsString());
        assertEquals(160, capture.get("width").getAsInt());
        assertEquals(120, capture.get("height").getAsInt());
        assertTrue(capture.get("durationNanos").getAsLong() >= 0);
        assertFalse(Files.exists(directory.resolve("checkpoint.json.tmp")));
        Files.write(image, new byte[]{4});
        CaptureEvidence.write(image, options, snapshot, 160, 120, 11, "renderer", System.nanoTime());
        assertTrue(Files.readString(directory.resolve("checkpoint.json")).contains(CaptureEvidence.sha256(new byte[]{4})));
        assertThrows(java.io.IOException.class, () -> CaptureEvidence.write(directory.resolve("missing.png"), options, snapshot, 1, 1, 1, "renderer", 0));
    }

    @Test void readinessTracksGeometryAndOrderWithoutWaitingForChangingElapsedDigits() {
        var rect = new Rect(1,2,10,10);
        var first = snapshot(rect,"1s",0);
        var later = snapshot(rect,"2s",0);
        assertEquals(CaptureEvidence.readiness(first), CaptureEvidence.readiness(later));
        assertNotEquals(CaptureEvidence.readiness(first), CaptureEvidence.readiness(snapshot(new Rect(2,2,10,10),"1s",0)));
        assertNotEquals(CaptureEvidence.readiness(first), CaptureEvidence.readiness(snapshot(rect,"1s",123)));
        var frames = new StableFrames<Object>(2);
        assertFalse(frames.observe(CaptureEvidence.readiness(first)));
        assertTrue(frames.observe(CaptureEvidence.readiness(later)));
        assertFalse(frames.observe(CaptureEvidence.readiness(snapshot(rect,"1s",123))));
    }

    @Test void statusInputsAndSelectedCardAreWrittenWithoutChangingLayoutReadiness() throws Exception {
        var plain = snapshot(new Rect(1, 2, 10, 10), "1s", 0);
        var status = new UiSnapshot.StatusObservation(17, "native", 10_000_000_000L,
                100, 50, 50, 0.5, 4, "00000000-0000-0000-0000-000000000001", "minecraft:stone", 11_000_000_000L);
        var measured = new UiSnapshot(plain.screen(), plain.menu(), plain.gui(), plain.screenWidth(),
                plain.screenHeight(), plain.guiScale(), plain.frame(), plain.scroll(), plain.rows(),
                plain.text(), plain.badges(), plain.widgets(), plain.itemCells(), plain.tooltip(),
                List.of(), List.of(), status);
        assertEquals(CaptureEvidence.readiness(plain), CaptureEvidence.readiness(measured));
        var image = directory.resolve("status.png");
        Files.write(image, new byte[]{1});
        CaptureEvidence.write(image, new DriverOptions("standard-status-controls", "compatible",
                "ae2ct-00000000000000000000000000000000", directory, false), measured,
                80, 60, 1, "renderer", System.nanoTime());
        var written = JsonParser.parseString(Files.readString(directory.resolve("status.json")))
                .getAsJsonObject().getAsJsonObject("status");
        assertEquals(10_000_000_000L, written.get("elapsedNanos").getAsLong());
        assertEquals(50, written.get("completedCount").getAsLong());
        assertEquals(0.5, written.get("progress").getAsDouble());
        assertEquals(4, written.get("selectedCpuSerial").getAsInt());
        assertEquals("00000000-0000-0000-0000-000000000001", written.get("selectedJobUuid").getAsString());
        assertEquals("minecraft:stone", written.get("selectedOutputId").getAsString());
        assertEquals(11_000_000_000L, written.get("cpuElapsedNanos").getAsLong());
    }

    private static UiSnapshot snapshot(Rect rect,String text,int color) {
        var observed = new UiSnapshot.ObservedText("elapsed",text,List.of(text),rect,color,false);
        return new UiSnapshot("screen","menu",rect,80,60,2,1,0,
                List.of(new UiSnapshot.Row("stone",1,0,rect,List.of(observed))),List.of(observed),List.of(rect),List.of(),List.of(),List.of(observed));
    }
}
