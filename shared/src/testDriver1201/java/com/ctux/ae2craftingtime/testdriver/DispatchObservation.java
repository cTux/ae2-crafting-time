package com.ctux.ae2craftingtime.testdriver;

/** Observes real profiler calls from one accepted fixture job; never creates samples. */
public final class DispatchObservation {
    private static String network;
    private static String output;
    private static Object scope;
    private static long expected;
    private static long dispatched;
    private static long returned;
    private static long fastPathCrafts;
    private static int starts;
    private static int finishes;
    private static boolean success;
    private static int ticks;

    public record Snapshot(long expected, long dispatched, long returned, int starts, int finishes, boolean success,
            long fastPathCrafts, String scopeType, int ticks) {
        public boolean completedExactlyOnce() {
            return expected > 0 && dispatched == expected && returned == expected && starts == 1
                    && finishes == 1 && success;
        }
    }

    public static synchronized void watch(String networkId, String outputId) {
        network = networkId;
        output = outputId;
        scope = null;
        expected = dispatched = returned = 0;
        fastPathCrafts = 0;
        starts = finishes = 0;
        ticks = 0;
        success = false;
    }

    public static synchronized void accepted(String networkId, String outputId, Object jobScope, long amount) {
        if (!networkId.equals(network) || !outputId.equals(output)) return;
        scope = jobScope;
        expected = amount;
        starts++;
    }

    public static synchronized void amount(Object jobScope, String outputId, long amount, boolean dispatch) {
        if (scope == null || scope != jobScope || !outputId.equals(output)) return;
        if (dispatch) dispatched += amount;
        else returned += amount;
    }

    public static synchronized void finished(Object jobScope, boolean successful) {
        if (scope == null || scope != jobScope) return;
        finishes++;
        success = successful;
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(expected, dispatched, returned, starts, finishes, success, fastPathCrafts,
                scope == null ? "" : scope.getClass().getName(), ticks);
    }

    public static synchronized void fastPath(Object jobScope, long crafts) {
        if (scope != null && scope == jobScope) fastPathCrafts += crafts;
    }

    public static synchronized void tick(Object jobScope) {
        if (scope != null && scope == jobScope) ticks++;
    }

    private DispatchObservation() {}
}
