package com.ctux.ae2craftingtime.core;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** A working copy of one environment's switches; all features default on. */
public final class FeatureOptions {
    private final OptionFeature.Owner owner;
    private final EnumSet<OptionFeature> disabled = EnumSet.noneOf(OptionFeature.class);

    public FeatureOptions(OptionFeature.Owner owner) {
        this.owner = Objects.requireNonNull(owner);
    }

    public boolean enabled(OptionFeature feature) {
        requireOwned(feature);
        return !disabled.contains(feature);
    }

    public void setEnabled(OptionFeature feature, boolean enabled) {
        requireOwned(feature);
        if (enabled) disabled.remove(feature);
        else disabled.add(feature);
    }

    public void reset() {
        disabled.clear();
    }

    public Set<OptionFeature> disabled() {
        return Set.copyOf(disabled);
    }

    public FeatureOptions copy() {
        var copy = new FeatureOptions(owner);
        copy.disabled.addAll(disabled);
        return copy;
    }

    private void requireOwned(OptionFeature feature) {
        if (Objects.requireNonNull(feature).owner() != owner) {
            throw new IllegalArgumentException("Option belongs to " + feature.owner());
        }
    }
}
