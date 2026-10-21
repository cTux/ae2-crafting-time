package com.ctux.ae2craftingtime.testdriver;

import java.util.UUID;

/** Independent terminal and inventory gate for instantaneous counted book cloning. */
record TrinityCountedCompletion(UUID submittedJob, UUID capturedJob, int workerNumber,
        boolean sameHost, boolean sameGrid, boolean terminal, boolean linkDone,
        boolean linkStandalone, boolean linkCanceled, long requested,
        long blanksConsumed, long copiesProduced, long originalBefore, long originalAfter) {
    boolean complete() {
        return submittedJob != null && submittedJob.equals(capturedJob) && workerNumber > 0
                && sameHost && sameGrid && terminal && (linkDone || linkStandalone) && !linkCanceled
                && requested == 3 && blanksConsumed == 3 && copiesProduced == 3
                && originalBefore == 1 && originalAfter == originalBefore;
    }
}
