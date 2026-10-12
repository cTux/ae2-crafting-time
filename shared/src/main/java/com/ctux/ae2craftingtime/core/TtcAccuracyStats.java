package com.ctux.ae2craftingtime.core;

public record TtcAccuracyStats(
        int sampleCount,
        int fullyCoveredSampleCount,
        double averageCoverage,
        double meanSignedErrorSeconds,
        double meanAbsolutePercentageError,
        double meanActualToPredictedRatio,
        double lastPredictedSeconds,
        double lastActualWallSeconds,
        double lastActualTickSeconds,
        int lastKnownRows,
        int lastTotalRows) {
    public TtcAccuracyStats {
        if (!Double.isFinite(lastPredictedSeconds) || lastPredictedSeconds <= 0)
            throw new IllegalArgumentException("invalid accuracy prediction");
    }
}
