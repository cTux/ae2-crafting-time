package com.ctux.ae2craftingtime.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class CpuTtcDisplayOrder {
    public static final class State {
        private long revision = Long.MIN_VALUE;
        private int mode = -1;
        private boolean channel;
        private List<Integer> rawSerials = List.of();
        private List<Integer> orderedSerials = List.of();

        public <T> List<T> display(List<T> raw, ToIntFunction<T> serial, Predicate<T> busy,
                Function<T, OptionalLong> seconds, long revision, int mode, boolean channel) {
            if (raw == null || serial == null || busy == null || seconds == null || mode < 0 || mode > 2) {
                throw new IllegalArgumentException("invalid CPU display input");
            }
            var currentSerials = raw.stream().mapToInt(serial).boxed().toList();
            if (this.revision != revision || this.mode != mode || this.channel != channel
                    || !rawSerials.equals(currentSerials)) {
                var sorted = mode == 0 || !channel ? List.copyOf(raw)
                        : TtcSort.copyPrioritizedSorted(raw, busy, seconds, (left, right) -> 0, true, mode == 2);
                orderedSerials = sorted.stream().mapToInt(serial).boxed().toList();
                rawSerials = List.copyOf(currentSerials);
                this.revision = revision;
                this.mode = mode;
                this.channel = channel;
            }

            var latest = new LinkedHashMap<Integer, T>();
            raw.forEach(value -> latest.put(serial.applyAsInt(value), value));
            var result = new ArrayList<T>(raw.size());
            orderedSerials.forEach(value -> {
                var row = latest.remove(value);
                if (row != null) result.add(row);
            });
            result.addAll(latest.values());
            return List.copyOf(result);
        }

        public void clear() {
            revision = Long.MIN_VALUE;
            mode = -1;
            channel = false;
            rawSerials = List.of();
            orderedSerials = List.of();
        }
    }

    public static boolean hitCurrent(CpuTtcCache.CpuView drawn, CpuTtcCache.CpuView current) {
        return drawn != null && current != null && drawn.serial() == current.serial()
                && current.sameJob(drawn) && current.elapsedNanos() >= drawn.elapsedNanos();
    }

    public static int inputScroll(int drawnScroll) {
        return drawnScroll < 0 ? -1 : drawnScroll;
    }

    private CpuTtcDisplayOrder() {
    }
}
