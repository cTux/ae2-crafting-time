package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ClientConfig;
import com.ctux.ae2craftingtime.core.ServerConfig;
import com.ctux.ae2craftingtime.core.ServerOptionsWire;
import com.ctux.ae2craftingtime.mc1201.net.ServerOptionsUpdateC2S;
import java.io.IOException;
import java.util.Arrays;

/** One unsaved working copy shared by the Client and Server tabs. */
final class OptionsSession {
    enum SaveResult { SAVED, WAITING, STALE, REJECTED }

    private final ClientConfig client = ClientOptionsRuntime.current().copy();
    private ServerOptionsWire.Snapshot source;
    private ServerConfig server;
    private ServerOptionsWire.Snapshot waitingFrom;
    private ServerConfig submitted;
    private int waitTicks;

    OptionsSession() {
        refreshIfMissing();
    }

    ClientConfig client() { return client; }
    ServerOptionsWire.Snapshot source() { return source; }
    ServerConfig server() { return server; }
    boolean isSaving() { return waitingFrom != null; }

    void refreshIfMissing() {
        if (source == null && ClientServerOptions.snapshot() != null) reload();
    }

    void reload() {
        source = ClientServerOptions.snapshot();
        server = source == null ? null : source.config().copy();
    }

    void resetAll() {
        client.reset();
        if (source != null && source.editable()) server.reset();
    }

    SaveResult save() throws IOException {
        if (waitingFrom != null) return SaveResult.WAITING;
        boolean serverChanged = source != null && source.editable() && !sameConfig(server, source.config());
        if (serverChanged && (ClientServerOptions.snapshot() == null
                || ClientServerOptions.snapshot().revision() != source.revision())) return SaveResult.STALE;
        ClientOptionsRuntime.apply(client);
        if (!serverChanged) return SaveResult.SAVED;
        waitingFrom = ClientServerOptions.snapshot();
        submitted = server.copy();
        waitTicks = 0;
        StatsNetwork.sendToServer(new ServerOptionsUpdateC2S(ServerOptionsWire.encode(
                new ServerOptionsWire.Snapshot(source.revision(), false, submitted))));
        return SaveResult.WAITING;
    }

    SaveResult poll() {
        if (waitingFrom == null) return null;
        var latest = ClientServerOptions.snapshot();
        if (latest != waitingFrom) {
            waitingFrom = null;
            return latest != null && latest.revision() > source.revision()
                    && sameConfig(latest.config(), submitted) ? SaveResult.SAVED : SaveResult.REJECTED;
        }
        if (++waitTicks < 200) return SaveResult.WAITING;
        waitingFrom = null;
        return SaveResult.REJECTED;
    }

    private static boolean sameConfig(ServerConfig left, ServerConfig right) {
        return Arrays.equals(ServerOptionsWire.encode(new ServerOptionsWire.Snapshot(0, false, left)),
                ServerOptionsWire.encode(new ServerOptionsWire.Snapshot(0, false, right)));
    }
}
