package com.ctux.ae2craftingtime.core;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public final class CpuTtcCache {
    public static final int MAX_CPUS = 32;
    public static final int MAX_PRIORITIES = 7;
    public static final int BACKGROUND_SLOTS = MAX_CPUS - MAX_PRIORITIES;
    public static final long REQUEST_INTERVAL_MILLIS = 1_000;
    public static final long REQUEST_TIMEOUT_MILLIS = 3_000;
    public static final long EXPIRY_MILLIS = 3_000;

    private long session = -1;
    private int container = -1;
    private long sequence = -1;
    private long nextGeneration;
    private long lastRequestAt = Long.MIN_VALUE;
    private long revision;
    private boolean fullList;
    private final Map<Integer, Observed> views = new LinkedHashMap<>();
    private final Map<Integer, Value> totals = new LinkedHashMap<>();
    private final ArrayDeque<Integer> queue = new ArrayDeque<>();
    private Set<Integer> priorities = Set.of();
    private Outstanding outstanding;

    public void open(long session, int container) {
        if (session < 0 || container < 0) {
            throw new IllegalArgumentException("session and container must be nonnegative");
        }
        clear();
        this.session = session;
        this.container = container;
    }

    public void observe(List<CpuView> currentViews, boolean fullList, long nowMillis) {
        if (currentViews == null) {
            throw new IllegalArgumentException("CPU views are required");
        }
        if (session < 0) {
            return;
        }

        var next = new LinkedHashMap<Integer, Observed>();
        for (var view : currentViews) {
            if (view.serial() <= 0) {
                throw new IllegalArgumentException("CPU serials must be positive");
            }
            if (next.containsKey(view.serial())) {
                continue;
            }
            var previous = views.get(view.serial());
            var generation = previous != null && previous.view().sameJob(view)
                    && view.elapsedNanos() >= previous.view().elapsedNanos()
                    ? previous.generation() : ++nextGeneration;
            next.put(view.serial(), new Observed(view, generation));
        }

        var changed = next.size() != views.size() || next.entrySet().stream().anyMatch(entry -> {
            var previous = views.get(entry.getKey());
            return previous == null || previous.generation() != entry.getValue().generation();
        });
        views.clear();
        views.putAll(next);
        totals.entrySet().removeIf(entry -> {
            var observed = views.get(entry.getKey());
            return observed == null || !observed.view().busy() || observed.generation() != entry.getValue().generation();
        });
        updateQueue();
        changed |= setFullList(fullList, nowMillis);
        changed |= clampAndExpire(nowMillis);
        if (changed) {
            revision++;
        }
    }

    public void setCollectionMode(boolean fullList, long nowMillis) {
        if (session >= 0 && (setFullList(fullList, nowMillis) | clampAndExpire(nowMillis))) {
            revision++;
        }
    }

    public Optional<Request> refresh(List<Integer> prioritySerials, long nowMillis) {
        if (prioritySerials == null) {
            throw new IllegalArgumentException("CPU priorities are required");
        }
        if (session < 0) {
            return Optional.empty();
        }

        var nextPriorities = new LinkedHashSet<Integer>();
        for (var serial : prioritySerials) {
            if (serial != null && views.containsKey(serial)) {
                nextPriorities.add(serial);
                if (nextPriorities.size() == MAX_PRIORITIES) {
                    break;
                }
            }
        }
        priorities = Set.copyOf(nextPriorities);
        if (!fullList && pruneToPriorities()) {
            revision++;
        }
        if (outstanding != null && nowMillis - outstanding.sentAt() >= REQUEST_TIMEOUT_MILLIS) {
            outstanding = null;
        }
        if (outstanding != null || lastRequestAt != Long.MIN_VALUE
                && nowMillis - lastRequestAt < REQUEST_INTERVAL_MILLIS) {
            return Optional.empty();
        }

        var requested = new LinkedHashSet<>(nextPriorities);
        if (fullList) {
            for (int visited = 0, size = queue.size(); visited < size && requested.size() < MAX_CPUS; visited++) {
                var serial = queue.removeFirst();
                queue.addLast(serial);
                requested.add(serial);
            }
        }
        if (requested.isEmpty()) {
            return Optional.empty();
        }

        var generations = new LinkedHashMap<Integer, Long>();
        requested.forEach(serial -> generations.put(serial, views.get(serial).generation()));
        lastRequestAt = nowMillis;
        var request = new Request(container, session, ++sequence, List.copyOf(requested));
        outstanding = new Outstanding(request.sequence(), nowMillis, Map.copyOf(generations));
        return Optional.of(request);
    }

    public boolean apply(long session, long sequence, List<Entry> entries, long nowMillis) {
        if (entries == null || this.session != session || outstanding == null
                || outstanding.sequence() != sequence
                || nowMillis - outstanding.sentAt() >= REQUEST_TIMEOUT_MILLIS) {
            return false;
        }
        var values = new LinkedHashMap<Integer, OptionalLong>();
        for (var entry : entries) {
            if (!outstanding.generations().containsKey(entry.serial())
                    || values.putIfAbsent(entry.serial(), entry.seconds()) != null) {
                return false;
            }
        }
        if (!values.keySet().equals(outstanding.generations().keySet())) {
            return false;
        }

        var expiry = expiryMillis();
        for (var entry : values.entrySet()) {
            var observed = views.get(entry.getKey());
            var generation = outstanding.generations().get(entry.getKey());
            if (observed == null || observed.generation() != generation || !observed.view().busy()
                    || !fullList && !priorities.contains(entry.getKey())
                    || entry.getValue().isEmpty() || entry.getValue().getAsLong() <= 0) {
                totals.remove(entry.getKey());
            } else {
                totals.put(entry.getKey(), new Value(entry.getValue().getAsLong(), generation, nowMillis,
                        deadline(nowMillis, expiry)));
            }
        }
        outstanding = null;
        revision++;
        return true;
    }

    public Snapshot snapshot(long nowMillis) {
        if (clampAndExpire(nowMillis)) {
            revision++;
        }
        var values = new LinkedHashMap<Integer, Long>();
        totals.forEach((serial, value) -> values.put(serial, value.seconds()));
        return new Snapshot(revision, values);
    }

    public OptionalLong seconds(int serial, long nowMillis) {
        var value = snapshot(nowMillis).seconds().get(serial);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    public void channelUnavailable() {
        if (!totals.isEmpty() || outstanding != null) {
            totals.clear();
            outstanding = null;
            revision++;
        }
    }

    private boolean setFullList(boolean value, long nowMillis) {
        var changed = fullList != value;
        fullList = value;
        if (changed) {
            outstanding = null;
        }
        if (!fullList) {
            changed |= pruneToPriorities();
        }
        return changed | clampDeadlines(nowMillis);
    }

    private boolean pruneToPriorities() {
        return totals.entrySet().removeIf(entry -> !priorities.contains(entry.getKey()));
    }

    private void updateQueue() {
        queue.removeIf(serial -> {
            var observed = views.get(serial);
            return observed == null || !observed.view().busy();
        });
        var queued = new java.util.HashSet<>(queue);
        for (var entry : views.entrySet()) {
            if (entry.getValue().view().busy() && queued.add(entry.getKey())) {
                queue.addLast(entry.getKey());
            }
        }
    }

    private boolean clampAndExpire(long nowMillis) {
        var changed = clampDeadlines(nowMillis);
        return totals.entrySet().removeIf(entry -> nowMillis >= entry.getValue().deadline()) | changed;
    }

    private boolean clampDeadlines(long nowMillis) {
        var expiry = expiryMillis();
        var changed = false;
        for (var entry : totals.entrySet()) {
            var value = entry.getValue();
            var deadline = deadline(value.receivedAt(), expiry);
            if (deadline < value.deadline()) {
                entry.setValue(new Value(value.seconds(), value.generation(), value.receivedAt(), deadline));
                changed = true;
            }
        }
        return changed;
    }

    private long expiryMillis() {
        if (!fullList) {
            return EXPIRY_MILLIS;
        }
        var busy = views.values().stream().filter(view -> view.view().busy()).count();
        var rounds = Math.max(1, (busy + BACKGROUND_SLOTS - 1) / BACKGROUND_SLOTS);
        return Math.max(EXPIRY_MILLIS, (rounds + 2) * REQUEST_INTERVAL_MILLIS);
    }

    private static long deadline(long receivedAt, long expiry) {
        return receivedAt > Long.MAX_VALUE - expiry ? Long.MAX_VALUE : receivedAt + expiry;
    }

    public void clear() {
        session = -1;
        container = -1;
        sequence = -1;
        nextGeneration = 0;
        lastRequestAt = Long.MIN_VALUE;
        revision = 0;
        fullList = false;
        views.clear();
        totals.clear();
        queue.clear();
        priorities = Set.of();
        outstanding = null;
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

        public boolean sameJob(CpuView other) {
            return java.util.Objects.equals(jobId, other.jobId) && jobAmount == other.jobAmount;
        }
    }

    public record Request(int containerId, long session, long sequence, List<Integer> serials) {
        public Request {
            if (serials == null) {
                throw new IllegalArgumentException("CPU serials are required");
            }
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

    public record Snapshot(long revision, Map<Integer, Long> seconds) {
        public Snapshot {
            seconds = Map.copyOf(seconds);
        }
    }

    private record Observed(CpuView view, long generation) {
    }

    private record Value(long seconds, long generation, long receivedAt, long deadline) {
    }

    private record Outstanding(long sequence, long sentAt, Map<Integer, Long> generations) {
    }
}
