package com.ctux.ae2craftingtime.testdriver;

public final class CpuListInputControl {
    private static Integer wheelSerial;
    private static Integer wheelResult;
    private static Integer staleSerial;
    private static Boolean staleSuppressed;
    private static Boolean noFirstDraw;

    public static void armWheel(int serial) { wheelSerial = serial; wheelResult = null; }
    public static Integer wheelSerial() { return wheelSerial; }
    public static void wheelResult(Integer serial) { wheelResult = serial; wheelSerial = null; }
    public static Integer wheelResult() { return wheelResult; }
    public static void armStale(int serial) { staleSerial = serial; staleSuppressed = null; }
    public static Integer staleSerial() { return staleSerial; }
    public static void staleResult(Integer serial) { staleSuppressed = serial == null; staleSerial = null; }
    public static Boolean staleSuppressed() { return staleSuppressed; }
    public static void noFirstDraw(Integer serial) {
        if (noFirstDraw == null) noFirstDraw = serial == null;
    }
    public static Boolean noFirstDraw() { return noFirstDraw; }

    public static void reset() {
        wheelSerial = null;
        wheelResult = null;
        staleSerial = null;
        staleSuppressed = null;
        noFirstDraw = null;
    }

    private CpuListInputControl() { }
}
