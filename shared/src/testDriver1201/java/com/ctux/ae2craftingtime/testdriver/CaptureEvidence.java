package com.ctux.ae2craftingtime.testdriver;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

public final class CaptureEvidence {
    private CaptureEvidence() {}

    public static UiSnapshot snapshot(UiSnapshot observed, String screen, int width, int height,
            double scale, long frame) {
        if (observed != null && observed.screen().equals(screen)) return observed;
        return new UiSnapshot(screen, "", new Rect(0, 0, width, height), width, height, scale,
                frame, 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public static void write(Path image, DriverOptions options, UiSnapshot snapshot,
            int width, int height, long frame, String renderer, long started) throws IOException {
        var data = new Gson().toJsonTree(snapshot).getAsJsonObject();
        var capture = new com.google.gson.JsonObject();
        capture.addProperty("schema", 2);
        capture.addProperty("id", options.world() + "/" + options.scenario() + "/" + image.getFileName());
        capture.addProperty("world", options.world());
        capture.addProperty("scenario", options.scenario());
        capture.addProperty("profile", options.profile());
        capture.addProperty("frame", frame);
        capture.addProperty("width", width);
        capture.addProperty("height", height);
        capture.addProperty("renderer", renderer);
        capture.addProperty("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        capture.addProperty("sha256", sha256(Files.readAllBytes(image)));
        capture.addProperty("capturedAt", Instant.now().toString());
        capture.addProperty("durationNanos", System.nanoTime() - started);
        data.add("capture", capture);
        var sidecar = image.resolveSibling(image.getFileName().toString().replace(".png", ".json"));
        var temporary = sidecar.resolveSibling(sidecar.getFileName() + ".tmp");
        Files.writeString(temporary, new Gson().toJson(data));
        Files.move(temporary, sidecar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static List<?> readiness(UiSnapshot snapshot) {
        // Dynamic elapsed values do not define layout readiness; their assertions still run.
        return List.of(snapshot.screen(), snapshot.menu(), snapshot.gui(), snapshot.guiScale(),
                snapshot.rows().stream().map(row -> List.of(row.outputId(), row.cell())).toList(),
                snapshot.text().stream().map(text -> List.of(text.key(), String.valueOf(text.bounds()),
                        String.valueOf(text.color()), text.bold())).toList(),
                snapshot.badges(), snapshot.widgets(),
                snapshot.tooltip().stream().map(UiSnapshot.ObservedText::key).toList());
    }
}
