package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ServerOptionsWire;
import org.slf4j.LoggerFactory;

/** Last valid effective values from the connected server. */
public final class ClientServerOptions {
    private static ServerOptionsWire.Snapshot snapshot;

    public static ServerOptionsWire.Snapshot snapshot() { return snapshot; }

    public static void receive(byte[] bytes) {
        try {
            var incoming = ServerOptionsWire.decode(bytes);
            if (snapshot == null || incoming.revision() >= snapshot.revision()) snapshot = incoming;
        } catch (IllegalArgumentException error) {
            LoggerFactory.getLogger("ae2craftingtime").warn("Ignored invalid server options snapshot", error);
        }
    }

    public static void clear() { snapshot = null; }

    private ClientServerOptions() { }
}
