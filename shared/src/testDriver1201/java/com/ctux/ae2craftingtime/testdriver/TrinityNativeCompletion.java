package com.ctux.ae2craftingtime.testdriver;

import java.math.BigInteger;
import java.util.UUID;

/** Independent native completion gate for the one-output Trinity furnace fixture. */
record TrinityNativeCompletion(UUID submittedJob, UUID activeJob, boolean sameHost, boolean sameGrid,
        boolean terminal, boolean linkDone, boolean linkCanceled, long requested,
        long inputDelta, long outputDelta, BigInteger peakWaiting, BigInteger waitingDecrease) {
    boolean complete() {
        return submittedJob != null && submittedJob.equals(activeJob) && sameHost && sameGrid
                && terminal && linkDone && !linkCanceled && requested > 0
                && inputDelta == requested && outputDelta == requested
                && peakWaiting.equals(BigInteger.valueOf(requested))
                && waitingDecrease.equals(BigInteger.valueOf(requested));
    }
}
