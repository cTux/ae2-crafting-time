package com.ctux.ae2craftingtime.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class CraftProfiler {
    private final int maxSamples;
    private final double outlierMultiplier;
    private static final Object DEFAULT_SCOPE = new Object();
    private static final long MIN_DELAY_TICKS = 200L;
    private final Map<Object, Map<ProfileKey, ArrayDeque<PendingCraft>>> pending = new IdentityHashMap<>();
    private final Map<Object, Map<ProfileKey, Long>> lastProgressTicks = new IdentityHashMap<>();
    private final Map<Object, CapacityState> capacities = new IdentityHashMap<>();
    private final Map<Object, WaitingState> waiting = new IdentityHashMap<>();
    private final Map<Object, UUID> jobOwners = new IdentityHashMap<>();
    private final Map<Object, Set<ProfileKey>> delayedNotified = new IdentityHashMap<>();
    private final Map<ProfileKey, BusyWindow> busyWindows = new HashMap<>();
    private final Map<ProfileKey, ArrayDeque<CraftSample>> samples = new HashMap<>();
    private final Map<ProfileKey, PersistedOutputStatus> rememberedStatuses = new HashMap<>();
    private final MissingProviderTracker<Object> missingProviders = new MissingProviderTracker<>();
    private final DispatchPowerTracker dispatchPower = new DispatchPowerTracker();
    private boolean enabled = true;

    public CraftProfiler(int maxSamples) {
        this(maxSamples, 4.0);
    }

    public CraftProfiler(int maxSamples, double outlierMultiplier) {
        if (maxSamples <= 0) {
            throw new IllegalArgumentException("maxSamples must be positive");
        }
        if (outlierMultiplier < 1.0) {
            throw new IllegalArgumentException("outlierMultiplier must be at least 1");
        }
        this.maxSamples = maxSamples;
        this.outlierMultiplier = outlierMultiplier;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        missingProviders.setEnabled(enabled);
        dispatchPower.setEnabled(enabled);
        if (!enabled) {
            pending.clear();
            lastProgressTicks.clear();
            capacities.clear();
            waiting.clear();
            busyWindows.clear();
            jobOwners.clear();
            delayedNotified.clear();
            rememberedStatuses.clear();
        }
    }

    public void observeProviders(Object scope, Object pattern, Map<ProfileKey, Long> outputs, boolean hasProvider) {
        missingProviders.observe(scope, pattern, outputs, hasProvider);
    }

    public Set<ProfileKey> missingProviderOutputs(Object scope, Predicate<Object> hasProvider) {
        return missingProviders.missingOutputs(scope, hasProvider);
    }

    public void observeDispatchPower(Object scope, Object pattern, Map<ProfileKey, Long> outputs,
            double required, double extracted, long tick) {
        dispatchPower.observe(scope, pattern, outputs, required, extracted, tick);
    }

    public Map<ProfileKey, CraftingBlockReason> blockReasons(Object scope, long tick, Set<ProfileKey> missing) {
        return dispatchPower.reasons(scope, tick, missing);
    }

    public void startWaiting(Object scope, Iterable<ProfileKey> keys, long tick) {
        if (!enabled || scope == null || keys == null) {
            return;
        }

        missingProviders.clear(scope);
        dispatchPower.clear(scope);
        // A new job starts a fresh delayed-notification episode.
        delayedNotified.remove(scope);
        var waitingKeys = new HashSet<ProfileKey>();
        for (var key : keys) {
            if (key != null) {
                waitingKeys.add(key);
            }
        }
        if (waitingKeys.isEmpty()) {
            waiting.remove(scope);
        } else {
            waiting.put(scope, new WaitingState(tick, waitingKeys));
        }
    }

    public OptionalLong waitingTicks(ProfileKey key, Object scope, long tick) {
        var state = waiting.get(scope);
        return key != null && state != null && state.keys.contains(key)
                ? OptionalLong.of(Math.max(0, tick - state.acceptedAtTick))
                : OptionalLong.empty();
    }

    public void start(ProfileKey key, long amount, ProfileUnit unit, long tick) {
        start(key, DEFAULT_SCOPE, amount, unit, tick);
    }

    public void start(ProfileKey key, Object scope, long amount, ProfileUnit unit, long tick) {
        if (!enabled || amount <= 0) {
            return;
        }
        // Fresh live observations supersede any remembered status for the key.
        rememberedStatuses.remove(key);
        var waitingState = waiting.get(scope);
        if (waitingState != null && waitingState.keys.remove(key) && waitingState.keys.isEmpty()) {
            waiting.remove(scope);
        }
        pending.computeIfAbsent(scope, ignored -> new HashMap<>())
                .computeIfAbsent(key, ignored -> new ArrayDeque<>())
                .addLast(new PendingCraft(amount, unit, tick));
        lastProgressTicks.computeIfAbsent(scope, ignored -> new HashMap<>()).putIfAbsent(key, tick);
        busyWindows.computeIfAbsent(key, ignored -> new BusyWindow(unit, tick));
    }

    public boolean complete(ProfileKey key, long amount, long tick) {
        return complete(key, DEFAULT_SCOPE, amount, tick);
    }

    public boolean complete(ProfileKey key, Object scope, long amount, long tick) {
        if (!enabled || amount <= 0) {
            return false;
        }

        var scopedPending = pending.get(scope);
        var queue = scopedPending == null ? null : scopedPending.get(key);
        if (queue == null) {
            return false;
        }

        var window = busyWindows.get(key);
        var remaining = amount;
        while (remaining > 0 && !queue.isEmpty()) {
            var craft = queue.peekFirst();
            var consumed = Math.min(remaining, craft.remainingAmount);
            craft.remainingAmount -= consumed;
            remaining -= consumed;
            window.completedAmount += consumed;

            if (craft.remainingAmount == 0) {
                queue.removeFirst();
            }
        }

        if (queue.isEmpty()) {
            scopedPending.remove(key);
            removeLastProgress(scope, key);
            if (scopedPending.isEmpty()) {
                pending.remove(scope);
            }
        } else {
            lastProgressTicks.get(scope).put(key, tick);
        }

        if (hasPending(key)) {
            return false;
        }

        busyWindows.remove(key);
        rememberedStatuses.remove(key);
        addSample(key, new CraftSample(window.completedAmount, window.unit,
                Math.max(1, tick - window.startedTick)));
        return true;
    }

    public void clearPending(Object scope) {
        missingProviders.clear(scope);
        dispatchPower.clear(scope);
        var removed = pending.remove(scope);
        lastProgressTicks.remove(scope);
        capacities.remove(scope);
        waiting.remove(scope);
        jobOwners.remove(scope);
        delayedNotified.remove(scope);
        if (removed == null) {
            return;
        }
        for (var key : removed.keySet()) {
            if (!hasPending(key)) {
                rememberedStatuses.remove(key);
            }
            rebuildBusyWindow(key);
        }
    }

    public void updateCapacity(Object scope, int usedParallelSlots, int totalParallelSlots, long tick) {
        if (!enabled || scope == null || totalParallelSlots <= 0) {
            return;
        }
        capacities.put(scope, new CapacityState(
                Math.max(0, Math.min(usedParallelSlots, totalParallelSlots)), totalParallelSlots, tick));
    }

    public Optional<StallDiagnostic> stall(ProfileKey key, Object scope, long tick) {
        var scopedPending = pending.get(scope);
        var queue = scopedPending == null ? null : scopedPending.get(key);
        var stats = stats(key);
        if (queue == null || stats.isEmpty()) {
            return Optional.empty();
        }

        var lastProgress = lastProgressTicks.get(scope).get(key);
        var idleTicks = Math.max(0, tick - lastProgress);
        var typicalTicks = stats.get().averageDurationTicks();
        var delayedAfter = Math.max(MIN_DELAY_TICKS, (long) Math.ceil(typicalTicks * 2.0));
        if (idleTicks < delayedAfter) {
            return Optional.empty();
        }

        var capacity = capacities.get(scope);
        var capacityIsFresh = capacity != null && tick - capacity.tick <= 20;
        return Optional.of(new StallDiagnostic(idleTicks, typicalTicks, queue.size(),
                capacityIsFresh ? capacity.usedParallelSlots : 0,
                capacityIsFresh ? capacity.totalParallelSlots : 0));
    }

    public void setJobOwner(Object scope, UUID owner) {
        if (scope == null || !enabled) {
            return;
        }
        if (owner == null) {
            jobOwners.remove(scope);
        } else {
            jobOwners.put(scope, owner);
        }
        // A new job starts a fresh delayed-notification episode.
        delayedNotified.remove(scope);
    }

    public Optional<UUID> jobOwner(Object scope) {
        if (scope == null || !enabled) {
            return Optional.empty();
        }
        return Optional.ofNullable(jobOwners.get(scope));
    }

    public record DelayedEvent(ProfileKey key, StallDiagnostic diagnostic) {
    }

    /**
     * Returns outputs that newly transitioned to DELAYED since the last poll.
     * Each delayed output is reported once per delayed episode; progress that
     * clears the stall makes a later transition eligible again.
     */
    public List<DelayedEvent> pollNewlyDelayed(Object scope, long tick) {
        if (!enabled || scope == null) {
            return List.of();
        }
        var scopedPending = pending.get(scope);
        if (scopedPending == null) {
            delayedNotified.remove(scope);
            return List.of();
        }
        var notified = delayedNotified.computeIfAbsent(scope, ignored -> new HashSet<>());
        var currentlyDelayed = new HashMap<ProfileKey, StallDiagnostic>();
        for (var key : scopedPending.keySet()) {
            stall(key, scope, tick).ifPresent(diagnostic -> currentlyDelayed.put(key, diagnostic));
        }
        // Progress that clears the stall ends the episode and re-arms notification.
        notified.retainAll(currentlyDelayed.keySet());
        var newly = new ArrayList<DelayedEvent>();
        for (var entry : currentlyDelayed.entrySet()) {
            if (notified.add(entry.getKey())) {
                newly.add(new DelayedEvent(entry.getKey(), entry.getValue()));
                rememberedStatuses.put(entry.getKey(),
                        new PersistedOutputStatus(entry.getKey(), StatusKind.DELAYED,
                                entry.getValue().idleTicks(), entry.getValue().typicalDurationTicks(), tick));
            }
        }
        if (notified.isEmpty()) {
            delayedNotified.remove(scope);
        }
        return List.copyOf(newly);
    }

    public void rememberStatus(PersistedOutputStatus status) {
        if (status == null) {
            return;
        }
        rememberedStatuses.put(status.key(), status);
    }

    /**
     * Snapshot remembered statuses plus currently waiting keys for the world
     * save. Live data always wins on display; this is only the fallback shown
     * after a reload until fresh observations arrive.
     */
    public List<PersistedOutputStatus> snapshotStatuses() {
        var snapshot = new HashMap<>(rememberedStatuses);
        for (var state : waiting.values()) {
            for (var key : state.keys) {
                snapshot.putIfAbsent(key,
                        new PersistedOutputStatus(key, StatusKind.WAITING, 0, 0, state.acceptedAtTick));
            }
        }
        return List.copyOf(snapshot.values());
    }

    public void restoreStatuses(List<PersistedOutputStatus> statuses) {
        rememberedStatuses.clear();
        if (statuses == null) {
            return;
        }
        for (var status : statuses) {
            if (status != null) {
                rememberedStatuses.put(status.key(), status);
            }
        }
    }

    /**
     * Remembered DELAYED diagnostic for display when no live pending exists
     * for the key. Live observations always win: any pending craft means the
     * live stall check (or its absence) is authoritative.
     */
    public Optional<StallDiagnostic> rememberedStall(ProfileKey key) {
        if (key == null) {
            return Optional.empty();
        }
        var remembered = rememberedStatuses.get(key);
        if (remembered == null || remembered.kind() != StatusKind.DELAYED || hasPending(key)) {
            return Optional.empty();
        }
        return Optional.of(new StallDiagnostic(remembered.idleTicks(), remembered.typicalDurationTicks(), 0, 0, 0));
    }

    public OptionalLong rememberedWaitingTicks(ProfileKey key, long tick) {
        if (key == null) {
            return OptionalLong.empty();
        }
        var remembered = rememberedStatuses.get(key);
        if (remembered == null || remembered.kind() != StatusKind.WAITING || hasPending(key)) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Math.max(0, tick - remembered.acceptedAtTick()));
    }

    public Map<ProfileKey, CraftingBlockReason> rememberedReasons() {
        var reasons = new HashMap<ProfileKey, CraftingBlockReason>();
        for (var status : rememberedStatuses.values()) {
            if (status.kind() == StatusKind.NO_POWER) {
                reasons.put(status.key(), CraftingBlockReason.NO_POWER);
            } else if (status.kind() == StatusKind.NO_PROVIDER) {
                reasons.put(status.key(), CraftingBlockReason.NO_PROVIDER);
            }
        }
        return reasons;
    }

    public boolean hasPending(ProfileKey key) {
        return key != null && pending.values().stream().anyMatch(scoped -> scoped.containsKey(key));
    }

    public Optional<ProfileStats> stats(ProfileKey key) {
        var queue = samples.get(key);
        if (queue == null) {
            return Optional.empty();
        }

        long durationTotal = 0;
        long lastDuration = 0;
        var filtered = filteredSamples(queue, outlierMultiplier);
        var sampleDurationTicks = new ArrayList<Long>(queue.size());
        var sampleAmounts = new ArrayList<Long>(queue.size());
        long weightedDurationTotal = 0;
        long weightedAmountTotal = 0;
        long weight = 1;
        ProfileUnit unit = null;

        for (var sample : queue) {
            durationTotal += sample.durationTicks;
            lastDuration = sample.durationTicks;
            sampleDurationTicks.add(sample.durationTicks);
            sampleAmounts.add(sample.amount);
            unit = sample.unit;
        }
        for (var sample : filtered) {
            weightedDurationTotal += sample.durationTicks * weight;
            weightedAmountTotal += sample.amount * weight;
            weight++;
        }

        var averageDuration = (double) durationTotal / queue.size();
        var amountPerTick = (double) weightedAmountTotal / weightedDurationTotal;
        return Optional.of(new ProfileStats(
                queue.size(),
                averageDuration,
                amountPerTick,
                amountPerTick * 20.0,
                lastDuration,
                unit,
                queue.size() >= 3 && filtered.size() == queue.size(),
                filtered.size(),
                outlierMultiplier,
                sampleDurationTicks,
                sampleAmounts));
    }

    public Optional<ProfileStats> inProgressStats(ProfileKey key, long tick) {
        var window = busyWindows.get(key);
        if (window == null || window.completedAmount <= 0) {
            return Optional.empty();
        }

        var duration = Math.max(1, tick - window.startedTick);
        var amountPerTick = (double) window.completedAmount / duration;
        return Optional.of(new ProfileStats(
                1,
                duration,
                amountPerTick,
                amountPerTick * 20.0,
                duration,
                window.unit,
                false,
                1,
                outlierMultiplier,
                List.of(duration),
                List.of(window.completedAmount)));
    }

    public boolean clearSamples(ProfileKey key) {
        var cleared = samples.remove(key) != null;
        busyWindows.remove(key);
        rememberedStatuses.remove(key);
        pending.values().forEach(scoped -> scoped.remove(key));
        pending.values().removeIf(Map::isEmpty);
        lastProgressTicks.values().forEach(scoped -> scoped.remove(key));
        lastProgressTicks.values().removeIf(Map::isEmpty);
        delayedNotified.values().forEach(notified -> notified.remove(key));
        delayedNotified.values().removeIf(Set::isEmpty);
        return cleared;
    }

    private static List<CraftSample> filteredSamples(ArrayDeque<CraftSample> queue, double outlierMultiplier) {
        if (queue.size() < 5) {
            return List.copyOf(queue);
        }

        var samples = List.copyOf(queue);
        var durationsPerUnit = new ArrayList<Double>(samples.size());
        for (var sample : samples) {
            durationsPerUnit.add((double) sample.durationTicks / sample.amount);
        }
        durationsPerUnit.sort(Double::compare);
        var median = durationsPerUnit.get(durationsPerUnit.size() / 2);
        var filtered = new ArrayList<CraftSample>();
        for (var sample : samples) {
            var durationPerUnit = (double) sample.durationTicks / sample.amount;
            if (durationPerUnit >= median / outlierMultiplier && durationPerUnit <= median * outlierMultiplier) {
                filtered.add(sample);
            }
        }
        return filtered;
    }

    private void addSample(ProfileKey key, CraftSample sample) {
        var queue = samples.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        queue.addLast(sample);
        while (queue.size() > maxSamples) {
            queue.removeFirst();
        }
    }

    public List<PersistedOutputSamples> snapshotSamples() {
        var snapshot = new ArrayList<PersistedOutputSamples>();
        for (var entry : samples.entrySet()) {
            var persisted = new ArrayList<PersistedCraftSample>();
            ProfileUnit unit = null;
            for (var sample : entry.getValue()) {
                unit = sample.unit;
                persisted.add(new PersistedCraftSample(sample.amount, sample.durationTicks));
            }
            snapshot.add(new PersistedOutputSamples(entry.getKey(), unit, persisted));
        }
        return snapshot;
    }

    public void loadSamples(List<PersistedOutputSamples> persisted) {
        samples.clear();
        rememberedStatuses.clear();
        missingProviders.clear();
        dispatchPower.clear();
        pending.clear();
        lastProgressTicks.clear();
        capacities.clear();
        waiting.clear();
        busyWindows.clear();
        jobOwners.clear();
        delayedNotified.clear();
        for (var output : persisted) {
            if (output == null || output.key() == null || output.unit() == null) {
                continue;
            }
            output = ProfileAmounts.migrate(output);
            for (var sample : output.samples()) {
                if (sample.amount() > 0 && sample.durationTicks() > 0) {
                    addSample(output.key(), new CraftSample(sample.amount(), output.unit(), sample.durationTicks()));
                }
            }
        }
    }

    private static final class PendingCraft {
        private final ProfileUnit unit;
        private final long startedTick;
        private long remainingAmount;

        private PendingCraft(long amount, ProfileUnit unit, long startedTick) {
            this.remainingAmount = amount;
            this.unit = unit;
            this.startedTick = startedTick;
        }
    }

    private void removeLastProgress(Object scope, ProfileKey key) {
        var scoped = lastProgressTicks.get(scope);
        scoped.remove(key);
        if (scoped.isEmpty()) {
            lastProgressTicks.remove(scope);
        }
    }

    private void rebuildBusyWindow(ProfileKey key) {
        BusyWindow rebuilt = null;
        for (var scoped : pending.values()) {
            var queue = scoped.get(key);
            if (queue == null) {
                continue;
            }
            for (var craft : queue) {
                if (rebuilt == null || craft.startedTick < rebuilt.startedTick) {
                    rebuilt = new BusyWindow(craft.unit, craft.startedTick);
                }
            }
        }
        if (rebuilt == null) {
            busyWindows.remove(key);
        } else {
            busyWindows.put(key, rebuilt);
        }
    }

    private static final class BusyWindow {
        private final ProfileUnit unit;
        private final long startedTick;
        private long completedAmount;

        private BusyWindow(ProfileUnit unit, long startedTick) {
            this.unit = unit;
            this.startedTick = startedTick;
        }
    }

    private record CraftSample(long amount, ProfileUnit unit, long durationTicks) {
    }

    private record CapacityState(int usedParallelSlots, int totalParallelSlots, long tick) {
    }

    private record WaitingState(long acceptedAtTick, Set<ProfileKey> keys) {
    }
}
