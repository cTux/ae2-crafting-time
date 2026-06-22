package com.ctux.ae2craftingtime.mc1201;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.ctux.ae2craftingtime.core.CraftProfiler;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;

import java.util.Optional;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ProfilerBridge {
    private static final CraftProfiler PROFILER = new CraftProfiler(Ae2CraftingTimeConfig.MAX_SAMPLES.get(),
            Ae2CraftingTimeConfig.OUTLIER_MULTIPLIER.get());
    private static Ae2CraftingTimeSavedData savedData;

    public static void start(String networkId, GenericStack output, long tick) {
        if (output == null || !isEnabled()) {
            return;
        }
        PROFILER.start(key(networkId, output.what()), normalizeAmount(output.what(), output.amount()), unit(output.what()), tick);
    }

    public static void complete(String networkId, AEKey what, long amount, long tick) {
        if (what == null || !isEnabled()) {
            return;
        }
        if (PROFILER.complete(key(networkId, what), normalizeAmount(what, amount), tick) && savedData != null) {
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

    public static boolean clearStats(ProfileKey key) {
        if (key == null || !isEnabled()) {
            return false;
        }
        var cleared = PROFILER.clearSamples(key);
        if (cleared && savedData != null) {
            savedData.replaceFrom(PROFILER.snapshotSamples());
        }
        return cleared;
    }

    private static boolean isEnabled() {
        return Ae2CraftingTimeConfig.ENABLED.get();
    }

    public static ProfileKey key(AEKey key) {
        return new ProfileKey(key.getId().toString());
    }

    public static ProfileKey key(String networkId, AEKey key) {
        return new ProfileKey(networkId, key.getId().toString());
    }

    public static String networkId(IGrid grid) {
        if (grid == null || grid.getPivot() == null) {
            return "";
        }
        var pivot = grid.getPivot();
        if (pivot.getOwner() instanceof BlockEntity blockEntity) {
            return pivot.getLevel().dimension().location() + ":" + blockEntity.getBlockPos().asLong();
        }
        return pivot.getLevel().dimension().location() + ":" + pivot.getOwningPlayerId() + ":" + grid.size();
    }

    public static void load(Ae2CraftingTimeSavedData data) {
        savedData = data;
        PROFILER.loadSamples(data.samples());
    }

    private static ProfileUnit unit(AEKey key) {
        return key.getAmountPerUnit() > 1 ? ProfileUnit.MILLIBUCKET : ProfileUnit.ITEM;
    }

    private static long normalizeAmount(AEKey key, long amount) {
        return AeKeyAmounts.normalize(key, amount);
    }

    private ProfilerBridge() {
    }
}
