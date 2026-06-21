package com.ctux.ae2cpd.mc1201;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import com.ctux.ae2cpd.core.CraftProfiler;
import com.ctux.ae2cpd.core.ProfileKey;
import com.ctux.ae2cpd.core.ProfileStats;
import com.ctux.ae2cpd.core.ProfileUnit;

import java.util.Optional;

public final class ProfilerBridge {
    private static CraftProfiler profiler = new CraftProfiler(20);
    private static int sampleLimit = 20;

    public static void start(GenericStack output, long tick) {
        if (output == null || !isEnabled()) {
            return;
        }
        current().start(key(output.what()), normalizeAmount(output.what(), output.amount()), unit(output.what()), tick);
    }

    public static void complete(AEKey what, long amount, long tick) {
        if (what == null || !isEnabled()) {
            return;
        }
        current().complete(key(what), normalizeAmount(what, amount), tick);
    }

    public static Optional<ProfileStats> stats(AEKey what) {
        if (what == null || !isEnabled()) {
            return Optional.empty();
        }
        return current().stats(key(what));
    }

    private static boolean isEnabled() {
        return Ae2CpdConfig.ENABLED.get();
    }

    private static CraftProfiler current() {
        var configuredLimit = Ae2CpdConfig.SAMPLES.get();
        if (configuredLimit != sampleLimit) {
            sampleLimit = configuredLimit;
            profiler = new CraftProfiler(sampleLimit);
        }
        profiler.setEnabled(true);
        return profiler;
    }

    private static ProfileKey key(AEKey key) {
        return new ProfileKey(key.getId().toString());
    }

    private static ProfileUnit unit(AEKey key) {
        return key.getType() == AEKeyType.fluids() ? ProfileUnit.MILLIBUCKET : ProfileUnit.ITEM;
    }

    private static long normalizeAmount(AEKey key, long amount) {
        if (key.getType() != AEKeyType.fluids()) {
            return amount;
        }
        return amount * 1000L / key.getAmountPerUnit();
    }

    private ProfilerBridge() {
    }
}
