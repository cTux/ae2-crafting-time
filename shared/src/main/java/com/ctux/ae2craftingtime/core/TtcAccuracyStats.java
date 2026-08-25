package com.ctux.ae2craftingtime.core;

public record TtcAccuracyStats(
        int sampleCount,
        int fullyCoveredSampleCount,
        double averageCoverage,
        double meanSignedErrorSeconds,
        double meanAbsolutePercentageError,
        double meanActualToPredictedRatio,
        long lastPredictedSeconds,
        double lastActualWallSeconds,
        double lastActualTickSeconds,
        int lastKnownRows,
        int lastTotalRows) {
}
