package com.ctux.ae2craftingtime.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public final class CpuTtcCache {
    public static final int MAX_CPUS = 32;
    public static final long REQUEST_INTERVAL_MILLIS = 1_000;
    public static final long EXPIRY_MILLIS = 3_000;

    private long session = -1;
    private int container = -1;
    private long sequence = -1;
    private long lastRequestAt = Long.MIN_VALUE;
    private long receivedAt = Long.MIN_VALUE;
    private Map<Integer, CpuView> views = Map.of();
    private Set<Integer> requested = Set.of();
    private Map<Integer, OptionalLong> totals = Map.of();
    private boolean outstandingValid;

    public void open(long session, int container) {
        if (session < 0 || container < 0) {
            throw new IllegalArgumentException("session and container must be nonnegative");
        }
        clear();
        this.session = session;
        this.container = container;
    }

    public Optional<Request> refresh(List<CpuView> currentViews, long nowMillis) {
        if (session < 0) {
            return Optional.empty();
        }
        if (currentViews == null) {
            throw new IllegalArgumentException("CPU views are required");
        }
        var unique = new LinkedHashMap<Integer, CpuView>();
        for (var view : currentViews) {
            if (view.serial() <= 0) {
                throw new IllegalArgumentException("CPU serials must be positive");
            }
            unique.putIfAbsent(view.serial(), view);
            if (unique.size() == MAX_CPUS) {
                break;
            }
        }
        var nextViews = Map.copyOf(unique);
        var changed = !sameViews(nextViews);
        if (changed) {
            var retained = new LinkedHashMap<Integer, OptionalLong>();
            nextViews.forEach((serial, view) -> {
                var previous = views.get(serial);
                if (view.busy() && previous != null && previous.sameJob(view) && view.elapsedNanos() >= previous.elapsedNanos()
                        && totals.containsKey(serial)) {
                    retained.put(serial, totals.get(serial));
                }
            });
            totals = Map.copyOf(retained);
            outstandingValid = false;
        }
        views = nextViews;
        if (lastRequestAt != Long.MIN_VALUE && nowMillis - lastRequestAt < REQUEST_INTERVAL_MILLIS) {
            return Optional.empty();
        }
        requested = Set.copyOf(unique.keySet());
        lastRequestAt = nowMillis;
        outstandingValid = true;
        return Optional.of(new Request(container, session, ++sequence, List.copyOf(unique.keySet())));
    }

    public boolean apply(long session, long sequence, List<Entry> entries, long nowMillis) {
        if (entries == null) return false;
        if (this.session != session || this.sequence != sequence || !outstandingValid) {
            return false;
        }
        var values = new LinkedHashMap<Integer, OptionalLong>();
        for (var entry : entries) {
            if (!requested.contains(entry.serial()) || values.putIfAbsent(entry.serial(), entry.seconds()) != null) {
                return false;
            }
        }
        if (!values.keySet().equals(requested)) {
            return false;
        }
        values.replaceAll((serial, seconds) -> views.get(serial).busy() && seconds.isPresent() && seconds.getAsLong() > 0
                ? seconds : OptionalLong.empty());
        totals = Map.copyOf(values);
        receivedAt = nowMillis;
        outstandingValid = false;
        return true;
    }

    public OptionalLong seconds(int serial, long nowMillis) {
        if (nowMillis - receivedAt >= EXPIRY_MILLIS) {
            return OptionalLong.empty();
        }
        return totals.getOrDefault(serial, OptionalLong.empty());
    }

    private boolean sameViews(Map<Integer, CpuView> current) {
        if (!current.keySet().equals(views.keySet())) {
            return false;
        }
        return current.entrySet().stream().allMatch(entry -> {
            var previous = views.get(entry.getKey());
            var next = entry.getValue();
            return previous.sameJob(next) && next.elapsedNanos() >= previous.elapsedNanos();
        });
    }

    public void clear() {
        session = -1;
        container = -1;
        sequence = -1;
        lastRequestAt = Long.MIN_VALUE;
        receivedAt = Long.MIN_VALUE;
        views = Map.of();
        requested = Set.of();
        totals = Map.of();
        outstandingValid = false;
    }

    public record CpuView(int serial, String jobId, long jobAmount, long elapsedNanos) {
        public CpuView {
            if (jobAmount < 0 || elapsedNanos < 0) {
                throw new IllegalArgumentException("invalid CPU view");
            }
        }

        public boolean busy() {
            return jobId != null;
        }

        private boolean sameJob(CpuView other) {
            return java.util.Objects.equals(jobId, other.jobId) && jobAmount == other.jobAmount;
        }
    }

    public record Request(int containerId, long session, long sequence, List<Integer> serials) {
        public Request {
            if (serials == null) throw new IllegalArgumentException("CPU serials are required");
            serials = List.copyOf(serials);
        }
    }

    public record Entry(int serial, OptionalLong seconds) {
        public Entry {
            if (serial <= 0 || seconds == null || seconds.isPresent() && seconds.getAsLong() < 0) {
                throw new IllegalArgumentException("invalid CPU TTC entry");
            }
        }
    }
}
