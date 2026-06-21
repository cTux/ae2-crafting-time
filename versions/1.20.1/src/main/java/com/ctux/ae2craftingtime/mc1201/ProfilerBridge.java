package com.ctux.ae2craftingtime.mc1201;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import com.ctux.ae2craftingtime.core.CraftProfiler;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;

import java.util.Optional;

public final class ProfilerBridge {
    private static final CraftProfiler PROFILER = new CraftProfiler(10);
    private static Ae2CraftingTimeSavedData savedData;

    public static void start(GenericStack output, long tick) {
        if (output == null || !isEnabled()) {
            return;
        }
        PROFILER.start(key(output.what()), normalizeAmount(output.what(), output.amount()), unit(output.what()), tick);
    }

    public static void complete(AEKey what, long amount, long tick) {
        if (what == null || !isEnabled()) {
            return;
        }
        if (PROFILER.complete(key(what), normalizeAmount(what, amount), tick) && savedData != null) {
            savedData.replaceFrom(PROFILER.snapshotSamples());
        }
    }

    public static Optional<ProfileStats> stats(AEKey what) {
        if (what == null || !isEnabled()) {
            return Optional.empty();
        }
        return PROFILER.stats(key(what));
    }

    public static Optional<ProfileStats> stats(ProfileKey key) {
        if (key == null || !isEnabled()) {
            return Optional.empty();
        }
        return PROFILER.stats(key);
    }

    private static boolean isEnabled() {
        return Ae2CraftingTimeConfig.ENABLED.get();
    }

    public static ProfileKey key(AEKey key) {
        return new ProfileKey(key.getId().toString());
    }

    public static void load(Ae2CraftingTimeSavedData data) {
        savedData = data;
        PROFILER.loadSamples(data.samples());
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
