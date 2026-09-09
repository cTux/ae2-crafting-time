package com.ctux.ae2craftingtime.testdriver;

import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import com.ctux.ae2craftingtime.mc1201.net.CpuTtcPacketCodec;

public final class CpuTtcPacketControl {
    private enum Mode { NORMAL, DROP, HOLD }
    private static Mode mode = Mode.NORMAL;
    private static CpuTtcPacketCodec.Snapshot held;
    private static boolean replaying;

    public static void drop() {
        mode = Mode.DROP;
        held = null;
    }

    public static void hold() {
        mode = Mode.HOLD;
        held = null;
    }

    public static boolean intercept(CpuTtcPacketCodec.Snapshot snapshot) {
        if (replaying || mode == Mode.NORMAL) return true;
        if (mode == Mode.HOLD && held == null) held = snapshot;
        return false;
    }

    public static boolean hasHeld() {
        return held != null;
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

    private CpuTtcPacketControl() { }
}
