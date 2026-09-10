package com.ctux.ae2craftingtime.testdriver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

record CpuListContinuation(int schema, String phase, String world, String epoch, String serverState,
        int terminalX, int terminalY, int terminalZ, List<String> checks, List<String> screenshots,
        long serverSequence, long clientSequence) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static CpuListContinuation read(Path path, String world, String epoch) throws IOException {
        var value = GSON.fromJson(Files.readString(path), CpuListContinuation.class);
        if (value == null || value.schema != 1 || !"relaunch-ready".equals(value.phase)
                || !world.equals(value.world) || !epoch.equals(value.epoch)
                || value.serverState == null || value.checks == null || value.screenshots == null
                || value.serverSequence < 0 || value.clientSequence <= value.serverSequence) {
            throw new IllegalArgumentException("invalid CPU-list relaunch continuation");
        }
        return value;
    }

    static void write(Path path, CpuListContinuation value) throws IOException {
        if (value == null || value.schema != 1 || !"relaunch-ready".equals(value.phase)
                || value.world == null || !value.world.matches("ae2ct-[a-f0-9]{32}")
                || value.epoch == null || !value.epoch.matches("[A-Za-z0-9._-]{1,128}")
                || value.serverState == null || value.checks == null || value.screenshots == null
                || value.serverSequence < 0 || value.clientSequence <= value.serverSequence) {
            throw new IllegalArgumentException("invalid CPU-list relaunch continuation");
        }
        Files.createDirectories(path.getParent());
        var temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(value));
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
