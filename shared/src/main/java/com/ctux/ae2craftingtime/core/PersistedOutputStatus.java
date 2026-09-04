package com.ctux.ae2craftingtime.core;

import java.util.Objects;

public record PersistedOutputStatus(ProfileKey key, StatusKind kind, long idleTicks, double typicalDurationTicks,
        long acceptedAtTick) {
    public PersistedOutputStatus {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(kind, "kind");
    }
}
