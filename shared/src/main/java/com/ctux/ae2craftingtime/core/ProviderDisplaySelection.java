package com.ctux.ae2craftingtime.core;

import java.util.LinkedHashMap;
import java.util.List;

public final class ProviderDisplaySelection {
    public record Candidate<T, P>(T value, String dimensionId, List<P> positions) {
    }

    public record Selected<T, P>(T value, String dimensionId, P position) {
    }

    private record PositionKey<P>(String dimensionId, P position) {
    }

    public static <T, P> List<Selected<T, P>> firstByPosition(List<Candidate<T, P>> candidates) {
        var selected = new LinkedHashMap<PositionKey<P>, Selected<T, P>>();
        for (var candidate : candidates) {
            for (var position : candidate.positions()) {
                selected.putIfAbsent(new PositionKey<>(candidate.dimensionId(), position),
                        new Selected<>(candidate.value(), candidate.dimensionId(), position));
            }
        }
        return List.copyOf(selected.values());
    }

    private ProviderDisplaySelection() {
    }
}
