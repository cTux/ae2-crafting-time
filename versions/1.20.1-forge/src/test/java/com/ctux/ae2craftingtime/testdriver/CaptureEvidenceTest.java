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
        assertEquals(new Rect(0, 0, 80, 60), snapshot.gui());
        assertSame(snapshot, CaptureEvidence.snapshot(snapshot, "world", 80, 60, 2, 10));
        assertEquals("new", CaptureEvidence.snapshot(snapshot, "new", 80, 60, 2, 10).screen());
        var image = directory.resolve("checkpoint.png");
        Files.write(image, new byte[]{1,2,3});
        CaptureEvidence.write(image, options, snapshot, 160, 120, 10, "renderer", System.nanoTime());
        var json = JsonParser.parseString(Files.readString(directory.resolve("checkpoint.json"))).getAsJsonObject();
        assertEquals(9, json.get("frame").getAsLong());
        var capture = json.getAsJsonObject("capture");
        assertEquals(10, capture.get("frame").getAsLong());
        assertEquals(world + "/checkpoint.png", capture.get("id").getAsString());
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

    private static UiSnapshot snapshot(Rect rect,String text,int color) {
        var observed = new UiSnapshot.ObservedText("elapsed",text,List.of(text),rect,color,false);
        return new UiSnapshot("screen","menu",rect,80,60,2,1,0,
                List.of(new UiSnapshot.Row("stone",1,0,rect,List.of(observed))),List.of(observed),List.of(rect),List.of(),List.of(),List.of(observed));
    }
}
