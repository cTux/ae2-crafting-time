package com.ctux.ae2craftingtime.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class ProviderDispatchTracker {
    public enum AttemptResult {
        SUCCESS,
        UNKNOWN,
        NO_TARGET,
        INPUT_BLOCKED,
        LOCKED,
        NO_CHANNEL
    }

    private record Failure(Set<ProfileKey> outputs, CraftingBlockReason reason, long tick) { }

    private final Map<Object, Map<Object, Failure>> failures = new IdentityHashMap<>();
    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clear();
        }
    }

    public void observe(Object cpu, Object pattern, Map<ProfileKey, Long> outputs,
            CraftingBlockReason reason, long tick) {
        if (!enabled || cpu == null) {
            return;
        }
        var patterns = failures.computeIfAbsent(cpu, ignored -> new HashMap<>());
        var keys = new HashSet<ProfileKey>();
        if (isDispatchReason(reason)) {
            outputs.forEach((key, amount) -> {
                if (amount > 0) {
                    keys.add(key);
                }
            });
        }
        if (keys.isEmpty()) {
            patterns.remove(pattern);
        } else {
            patterns.put(pattern, new Failure(Set.copyOf(keys), reason, tick));
        }
        removeEmpty(cpu, patterns);
    }

    public Map<ProfileKey, CraftingBlockReason> reasons(Object cpu, long tick) {
        var patterns = failures.get(cpu);
        if (patterns == null) {
            return Map.of();
        }
        patterns.values().removeIf(failure -> tick < failure.tick || tick - failure.tick >= 20);
        var reasons = new HashMap<ProfileKey, CraftingBlockReason>();
        for (var failure : patterns.values()) {
            for (var output : failure.outputs) {
                reasons.merge(output, failure.reason, ProviderDispatchTracker::higherPriority);
            }
        }
        removeEmpty(cpu, patterns);
        return reasons;
    }

    public void clear(Object cpu) {
        failures.remove(cpu);
    }

    public void clear() {
        failures.clear();
    }

    private static boolean isDispatchReason(CraftingBlockReason reason) {
        return reason == CraftingBlockReason.NO_CHANNEL
                || reason == CraftingBlockReason.NO_TARGET
                || reason == CraftingBlockReason.INPUT_BLOCKED
                || reason == CraftingBlockReason.LOCKED;
    }

    private static CraftingBlockReason higherPriority(CraftingBlockReason left, CraftingBlockReason right) {
        return priority(left) >= priority(right) ? left : right;
    }

    private static int priority(CraftingBlockReason reason) {
        if (reason == CraftingBlockReason.NO_CHANNEL) {
            return 4;
        }
        if (reason == CraftingBlockReason.LOCKED) {
            return 3;
        }
        return reason == CraftingBlockReason.INPUT_BLOCKED ? 2 : 1;
    }

    public static AttemptResult activityResult(boolean active, boolean nodePresent, boolean powered,
            boolean booted, boolean meetsChannelRequirements) {
        return !active && nodePresent && powered && booted && !meetsChannelRequirements
                ? AttemptResult.NO_CHANNEL
                : AttemptResult.UNKNOWN;
    }

    private void removeEmpty(Object cpu, Map<Object, Failure> patterns) {
        if (patterns.isEmpty()) {
            failures.remove(cpu);
        }
    }

    public static final class Evaluation {
        private boolean sawCandidate;
        private boolean exhausted;
        private boolean unknown;
        private boolean succeeded;
        private CraftingBlockReason reason;

        public void candidate() {
            sawCandidate = true;
        }

        public void busy(boolean busy) {
            if (busy) {
                unknown = true;
            }
        }

        public void attempt(AttemptResult result) {
            var observed = switch (result) {
                case SUCCESS, UNKNOWN -> null;
                case NO_CHANNEL -> CraftingBlockReason.NO_CHANNEL;
                case NO_TARGET -> CraftingBlockReason.NO_TARGET;
                case INPUT_BLOCKED -> CraftingBlockReason.INPUT_BLOCKED;
                case LOCKED -> CraftingBlockReason.LOCKED;
            };
            if (result == AttemptResult.SUCCESS) {
                succeeded = true;
                return;
            }
            if (result == AttemptResult.UNKNOWN) {
                unknown = true;
                return;
            }
            if (reason == null) {
                reason = observed;
            } else if (reason != observed) {
                unknown = true;
            }
        }

        public void exhausted() {
            exhausted = true;
        }

        public CraftingBlockReason result() {
            return exhausted && sawCandidate && !unknown && !succeeded ? reason : null;
        }

        public boolean succeeded() {
            return succeeded;
        }
    }
}
