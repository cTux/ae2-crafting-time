package com.ctux.ae2craftingtime.testdriver;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record FixtureMarker(int schema, String scenario, String sourceFixtureId, String disposableWorldId,
                            Position terminal, String outputId) {
    public static FixtureMarker read(Path worldDirectory) throws IOException {
        var marker = new Gson().fromJson(Files.readString(
                worldDirectory.resolve(".ae2-crafting-time-test-fixture.json")), FixtureMarker.class);
        if (marker == null || marker.schema != 1 || !"craft-plan".equals(marker.scenario)
                || !"ae2-crafting-time".equals(marker.sourceFixtureId)
                || marker.disposableWorldId == null || marker.disposableWorldId.isBlank()
                || marker.disposableWorldId.equals(marker.sourceFixtureId)
                || marker.terminal == null || !java.util.Set.of("DOWN", "UP", "NORTH", "SOUTH", "WEST", "EAST")
                        .contains(marker.terminal.face)
                || marker.outputId == null || marker.outputId.isBlank()) {
            throw new IllegalArgumentException("invalid test-fixture marker");
        }
        return marker;
    }

    public record Position(int x, int y, int z, String face) {
    }
}
