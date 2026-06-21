package com.ctux.ae2craftingtime.core;

import java.util.List;

public record PersistedOutputSamples(ProfileKey key, ProfileUnit unit, List<PersistedCraftSample> samples) {
    public PersistedOutputSamples {
        samples = List.copyOf(samples);
    }
}
