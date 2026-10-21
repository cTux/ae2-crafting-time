package com.ctux.ae2craftingtime.core;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/** Job-local, aggregate accounting for native Trinity waiting mutations. */
public final class TrinityJobAccounting<K> {
    private UUID jobId;
    private boolean unsupported;
    private final Map<K, Long> pending = new HashMap<>();

    public void begin(UUID id) {
        if (id == null) throw new IllegalArgumentException("Trinity job ID is required");
        if (!id.equals(jobId)) {
            jobId = id;
            unsupported = false;
            pending.clear();
        }
    }

    public boolean matches(UUID id) { return id != null && id.equals(jobId) && !unsupported; }
    public boolean unsupported() { return unsupported; }

    /** Convert and merge the complete committed batch before publishing any start. */
    public Map<K, Long> start(UUID id, Map<K, BigInteger> amounts) {
        return start(id, amounts, ignored -> true);
    }

    /** The predicate sees the proposed full pending ledger before any profiler start. */
    public Map<K, Long> start(UUID id, Map<K, BigInteger> amounts, Predicate<Map<K, Long>> withinBounds) {
        if (!matches(id) || amounts == null) return Map.of();
        var next = new HashMap<>(pending);
        var accepted = new HashMap<K, Long>();
        try {
            for (var entry : amounts.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue().signum() < 0)
                    throw new ArithmeticException("invalid Trinity committed amount");
                long amount = entry.getValue().longValueExact();
                if (amount == 0) continue;
                next.merge(entry.getKey(), amount, Math::addExact);
                accepted.merge(entry.getKey(), amount, Math::addExact);
            }
        } catch (ArithmeticException failure) {
            unsupported = true;
            pending.clear();
            return Map.of();
        }
        if (!withinBounds.test(Map.copyOf(next))) {
            unsupported = true;
            pending.clear();
            return Map.of();
        }
        pending.clear();
        pending.putAll(next);
        return Map.copyOf(accepted);
    }

    /** Only a native accepted waiting decrement may complete a pending start. */
    public long complete(UUID id, K key, BigInteger accepted) {
        if (!matches(id) || key == null || accepted == null || accepted.signum() <= 0) return 0;
        final long amount;
        try { amount = accepted.longValueExact(); }
        catch (ArithmeticException failure) {
            unsupported = true;
            pending.clear();
            return 0;
        }
        long consumed = Math.min(amount, pending.getOrDefault(key, 0L));
        if (consumed > 0) {
            long remaining = pending.get(key) - consumed;
            if (remaining == 0) pending.remove(key);
            else pending.put(key, remaining);
        }
        return consumed;
    }

    /** Cancellation removes unfinished work without creating a completion. */
    public long withdraw(UUID id, K key, BigInteger withdrawn) {
        return complete(id, key, withdrawn);
    }

    public boolean reject(UUID id) {
        if (id == null || !id.equals(jobId) || unsupported) return false;
        unsupported = true;
        pending.clear();
        return true;
    }

    public void clear() {
        jobId = null;
        unsupported = false;
        pending.clear();
    }
}
