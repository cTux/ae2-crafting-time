package com.ctux.ae2craftingtime.core;

public final class ProfileAmounts {
    public static ProfileUnit unit(String outputId, int amountPerUnit) {
        if ("botania:mana".equals(outputId)) {
            return ProfileUnit.MANA;
        }
        return amountPerUnit > 1 ? ProfileUnit.MILLIBUCKET : ProfileUnit.ITEM;
    }

    public static long normalize(String outputId, int amountPerUnit, long amount) {
        return unit(outputId, amountPerUnit) == ProfileUnit.MILLIBUCKET
                ? amount * 1000L / amountPerUnit : amount;
    }

    public static PersistedOutputSamples migrate(PersistedOutputSamples output) {
        if (!"botania:mana".equals(output.key().outputId()) || output.unit() != ProfileUnit.MILLIBUCKET) {
            return output;
        }
        var samples = output.samples().stream()
                .filter(sample -> sample.amount() > 0 && sample.amount() <= Long.MAX_VALUE / 1000)
                .map(sample -> new PersistedCraftSample(sample.amount() * 1000, sample.durationTicks())).toList();
        return new PersistedOutputSamples(output.key(), ProfileUnit.MANA, samples);
    }

    private ProfileAmounts() {
    }
}
