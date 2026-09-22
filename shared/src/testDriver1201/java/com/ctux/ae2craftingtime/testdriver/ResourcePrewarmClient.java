package com.ctux.ae2craftingtime.testdriver;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;

final class ResourcePrewarmClient {
    private final Minecraft minecraft;
    private final DriverOptions options;
    private final ResourcePrewarmControl control;
    private net.minecraft.client.gui.screens.Screen parent;
    private Object connection;
    private int attempts;
    private int frames;
    private int generation;
    private boolean connecting;
    private boolean receiptWritten;
    private boolean armed;

    ResourcePrewarmClient(Minecraft minecraft, DriverOptions options) {
        this.minecraft = minecraft;
        this.options = options;
        control = ResourcePrewarmControl.configured(CpuListTtcControl.directory());
    }

    boolean tick() throws IOException {
        if (armed) return true;
        if (receiptWritten && control.clientAccepted(generation)) {
            if (!worldReady() || connection != minecraft.getConnection()) throw new IllegalStateException("prewarm client changed before acceptance");
            armed = true; return true;
        }
        control.waiting(System.currentTimeMillis());
        if (minecraft.screen instanceof DisconnectedScreen && minecraft.level == null
                && minecraft.getConnection() == null) {
            if (control.exists("arm.json") || receiptWritten) throw new IllegalStateException("prewarm connection changed after readiness");
            if (attempts >= 3) throw new IllegalStateException("resource prewarm exhausted three native connections");
            DriverPlatform.clearLevel(minecraft);
            parent = null; connecting = false; connection = null; frames = 0;
            return false;
        }
        if (parent == null && minecraft.screen instanceof TitleScreen && minecraft.getOverlay() == null) {
            parent = DriverPlatform.prepareInitialDedicatedConnect(minecraft);
            return false;
        }
        if (parent != null && !connecting && minecraft.screen == parent) {
            attempts = ResourcePrewarmControl.nextConnectionAttempt(attempts,
                    minecraft.level == null && minecraft.getConnection() == null,
                    receiptWritten || control.exists("arm.json"),
                    java.nio.file.Files.exists(control.root.resolve("resource/state.properties"))
                            || java.nio.file.Files.exists(control.root.resolve("resource/command.properties")));
            generation = attempts;
            var process = ProcessHandle.current();
            control.publishAttempt(generation, process.pid(), process.info().startInstant().orElseThrow());
            DriverPlatform.connectInitialDedicatedServer(parent, DriverPlatform.server(options.dedicatedAddress()));
            connecting = true;
            return false;
        }
        if (!worldReady() || frames < 40) return false;
        if (!control.exists("server-ready.json")) return false;
        var server = control.read("server-ready.json");
        // The server must echo the attempt published before this native connect.
        if (!control.currentServerReady(server, generation)) return false;
        if (!receiptWritten && frames >= 40) {
            var process = ProcessHandle.current();
            control.write("client-ready.json", control.ready(process.pid(), process.info().startInstant().orElseThrow(), generation, 0, 40));
            receiptWritten = true;
        }
        if (receiptWritten && control.clientAccepted(generation)) { armed = true; return true; }
        return false;
    }

    void afterRender() {
        if (armed) return;
        if (!worldReady()) { frames = ResourcePrewarmControl.advanceReadiness(frames, 40, false, true); return; }
        var current = minecraft.getConnection();
        boolean same = connection == current;
        if (connection != current) {
            if (receiptWritten) throw new IllegalStateException("prewarm client connection changed after readiness");
            connection = current; frames = 0;
        }
        frames = ResourcePrewarmControl.advanceReadiness(frames, 40, true, same);
    }

    private boolean worldReady() {
        return minecraft.level != null && minecraft.player != null && minecraft.gameMode != null
                && minecraft.screen == null && minecraft.getOverlay() == null && minecraft.getConnection() != null
                && minecraft.getConnection().getConnection().isConnected()
                && minecraft.player.getUUID().equals(ResourcePrewarmControl.PLAYER);
    }

    String checkpoint() { return "state=STARTING phase=PREWARM prewarm-attempt=" + attempts; }
}
