package com.ctux.ae2craftingtime.testdriver;

import java.util.Objects;

/** Waits for a new native Status object while the same Trinity worker remains selected. */
final class TrinityActiveStatusFreshness {
    private Baseline baseline;

    boolean observe(UiSnapshot.StatusObservation status, boolean selected, int serial,
            String jobUuid, String outputId, long frame) {
        if (!selected || status == null || !"native".equals(status.provenance())
                || jobUuid == null || jobUuid.isBlank() || outputId == null || outputId.isBlank()
                || !Objects.equals(status.selectedCpuSerial(), serial)
                || !jobUuid.equals(status.selectedJobUuid())
                || !outputId.equals(status.selectedOutputId())
                || status.cpuElapsedNanos() == null) return false;
        if (baseline == null) {
            baseline = new Baseline(status.identity(), frame, serial, jobUuid, outputId);
            return false;
        }
        return baseline.serial == serial && baseline.jobUuid.equals(jobUuid)
                && baseline.outputId.equals(outputId) && status.identity() != baseline.identity
                && frame > baseline.frame;
    }

    Integer baselineIdentity() { return baseline == null ? null : baseline.identity; }

    void reset() { baseline = null; }

    private record Baseline(int identity, long frame, int serial, String jobUuid, String outputId) {}
}
