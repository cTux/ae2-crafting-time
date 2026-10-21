package com.ctux.ae2craftingtime.core;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Checks the raw long ledger and the shared output-id profiler totals before publication. */
public final class TrinityQuantityBounds {
    private static final BigInteger MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger MILLI = BigInteger.valueOf(1000);

    public static <K> boolean fits(Map<K, BigInteger> amounts, Function<K, String> outputId,
            ToIntFunction<K> amountPerUnit) {
        return project(amounts, outputId, amountPerUnit).isPresent();
    }

    /** Native quantities stay raw per AE key; estimates use one normalized row per output id. */
    public static <K> Optional<Projection> project(Map<K, BigInteger> amounts, Function<K, String> outputId,
            ToIntFunction<K> amountPerUnit) {
        var normalizedByOutput = new HashMap<String, BigInteger>();
        for (var entry : amounts.entrySet()) {
            var raw = entry.getValue();
            if (entry.getKey() == null || raw == null || raw.signum() < 0 || raw.compareTo(MAX) > 0)
                return Optional.empty();
            var id = outputId.apply(entry.getKey());
            int perUnit = amountPerUnit.applyAsInt(entry.getKey());
            if (id == null || id.isBlank() || perUnit <= 0) return Optional.empty();
            var normalized = ProfileAmounts.unit(id, perUnit) == ProfileUnit.MILLIBUCKET
                    ? raw.multiply(MILLI).divide(BigInteger.valueOf(perUnit)) : raw;
            if (raw.signum() > 0 && normalized.signum() == 0) return Optional.empty();
            normalizedByOutput.merge(id, normalized, BigInteger::add);
            if (normalizedByOutput.get(id).compareTo(MAX) > 0) return Optional.empty();
        }
        var normalizedLong = new HashMap<String, Long>();
        normalizedByOutput.forEach((id, value) -> normalizedLong.put(id, value.longValueExact()));
        return Optional.of(new Projection(Map.copyOf(normalizedLong)));
    }

    public record Projection(Map<String, Long> normalizedByOutput) { }

    private TrinityQuantityBounds() { }
}
