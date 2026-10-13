package com.ctux.ae2craftingtime.core;

/** Enabled only after Mixin confirms every paired presentation call site. */
public final class PresentationHooks {
    private static volatile boolean tableRenderer;
    private static volatile boolean planInput;
    private static volatile boolean statusInput;
    private static volatile boolean cpuCard;

    public static void tableRenderer(boolean ready) { tableRenderer = ready; }
    public static void planInput(boolean ready) { planInput = ready; }
    public static void statusInput(boolean ready) { statusInput = ready; }
    public static void cpuCard(boolean ready) { cpuCard = ready; }

    public static boolean table(boolean status) {
        return tableRenderer && (status ? statusInput : planInput);
    }

    public static boolean cpu() { return cpuCard; }

    private PresentationHooks() { }
}
