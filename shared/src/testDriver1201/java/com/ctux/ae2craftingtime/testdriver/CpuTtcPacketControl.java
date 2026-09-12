package com.ctux.ae2craftingtime.testdriver;

import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import com.ctux.ae2craftingtime.mc1201.net.CpuTtcPacketCodec;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class CpuTtcPacketControl {
    private enum Mode { NORMAL, DROP, HOLD_FIRST, HOLD_LATEST }
    private static Mode mode = Mode.NORMAL;
    private static CpuTtcPacketCodec.Snapshot held;
    private static boolean replaying;
    private static Boolean channelAvailable;
    private static final List<ObservedRequest> REQUESTS = new ArrayList<>();

    public static synchronized void beginRequestCapture() {
        REQUESTS.clear();
    }

    public static synchronized void observeRequest(CpuTtcPacketCodec.Request request) {
        REQUESTS.add(new ObservedRequest(request.sequence(),
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()), request.serials()));
    }

    public static synchronized RequestCapture requestCapture() {
        var unique = new LinkedHashSet<Integer>();
        REQUESTS.forEach(request -> unique.addAll(request.serials()));
        return new RequestCapture(List.copyOf(REQUESTS), List.copyOf(unique));
    }

    public static void channelAvailable(Boolean available) { channelAvailable = available; }
    public static Boolean channelAvailable() { return channelAvailable; }

    public static void drop() {
        mode = Mode.DROP;
        held = null;
    }

    public static void hold() {
        mode = Mode.HOLD_FIRST;
        held = null;
    }

    public static void holdLatest() {
        mode = Mode.HOLD_LATEST;
        held = null;
    }

    public static boolean intercept(CpuTtcPacketCodec.Snapshot snapshot) {
        if (replaying || mode == Mode.NORMAL) return true;
        if (mode == Mode.HOLD_LATEST || mode == Mode.HOLD_FIRST && held == null) held = snapshot;
        return false;
    }

    public static boolean hasHeld() {
        return held != null;
    }

    static long heldSequence() {
        return held == null ? -1 : held.sequence();
    }

    public static void releaseHeld() {
        var snapshot = held;
        mode = Mode.NORMAL;
        held = null;
        if (snapshot == null) return;
        replaying = true;
        try { CpuTtcClient.receive(snapshot); }
        finally { replaying = false; }
    }

    public static void resume() {
        mode = Mode.NORMAL;
        held = null;
    }

    public record ObservedRequest(long sequence, long sentAtMillis, List<Integer> serials) {
        public ObservedRequest { serials = List.copyOf(serials); }
    }

    public record RequestCapture(List<ObservedRequest> batches, List<Integer> uniqueSerials) { }

    private CpuTtcPacketControl() { }
}
