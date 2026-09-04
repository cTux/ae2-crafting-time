package com.ctux.ae2craftingtime.mc1201;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.me.service.CraftingService;
import com.ctux.ae2craftingtime.core.CraftProfiler;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcAccuracyStats;
import com.ctux.ae2craftingtime.core.TtcAccuracyTracker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

public final class ProfilerBridge {
    private static final CraftProfiler PROFILER = new CraftProfiler(Ae2CraftingTimeConfig.MAX_SAMPLES.get(),
            Ae2CraftingTimeConfig.OUTLIER_MULTIPLIER.get());
    private static final TtcAccuracyTracker ACCURACY = new TtcAccuracyTracker(Ae2CraftingTimeConfig.MAX_SAMPLES.get());
    private static final Map<ProfileKey, String> DISPLAY_NAMES = new ConcurrentHashMap<>();
    private static Ae2CraftingTimeSavedData savedData;

    public static void observeProviders(String networkId, Object scope, IPatternDetails pattern,
            boolean hasProvider) {
        isEnabled();
        var outputs = new HashMap<ProfileKey, Long>();
        for (var output : pattern.getOutputs()) {
            outputs.merge(key(networkId, output.what()), output.amount(), Long::sum);
        }
        if (isEnabled()) {
            ProviderStartTracker.noteDispatch(scope, pattern, outputs);
        }
        PROFILER.observeProviders(scope, pattern, outputs, hasProvider);
    }

    public static void observeDispatchPower(String networkId, Object scope, IPatternDetails pattern,
            double required, double extracted, long tick) {
        isEnabled();
        var outputs = new HashMap<ProfileKey, Long>();
        for (var output : pattern.getOutputs()) {
            outputs.merge(key(networkId, output.what()), output.amount(), Long::sum);
        }
        PROFILER.observeDispatchPower(scope, pattern, outputs, required, extracted, tick);
    }

    public static java.util.Map<ProfileKey, com.ctux.ae2craftingtime.core.CraftingBlockReason> blockReasons(
            Object scope, IGrid grid, long tick) {
        return grid == null ? java.util.Map.of()
                : PROFILER.blockReasons(scope, tick, missingProviders(scope, grid));
    }

    public static Set<ProfileKey> missingProviders(Object scope, IGrid grid) {
        isEnabled();
        return grid == null ? Set.of() : PROFILER.missingProviderOutputs(scope,
                pattern -> ((CraftingService) grid.getCraftingService())
                        .getProviders((IPatternDetails) pattern).iterator().hasNext());
    }

    public static void start(String networkId, GenericStack output, long tick) {
        if (output == null || !isEnabled()) {
            return;
        }
        start(networkId, output.what(), output.amount(), tick);
    }

    public static void start(String networkId, AEKey what, long amount, long tick) {
        start(networkId, ProfilerBridge.class, what, amount, tick);
    }

    public static void start(String networkId, Object scope, AEKey what, long amount, long tick) {
        if (what == null || amount <= 0 || !isEnabled()) {
            return;
        }
        var profileKey = key(networkId, what);
        try {
            DISPLAY_NAMES.put(profileKey, what.getDisplayName().getString());
        } catch (Exception ignored) {
            // Display name is best-effort; output id remains the fallback.
        }
        PROFILER.start(profileKey, scope, normalizeAmount(what, amount), unit(what), tick);
    }

    public static void complete(String networkId, AEKey what, long amount, long tick) {
        complete(networkId, ProfilerBridge.class, what, amount, tick);
    }

    public static void complete(String networkId, Object scope, AEKey what, long amount, long tick) {
        if (what == null || !isEnabled()) {
            return;
        }
        if (PROFILER.complete(key(networkId, what), scope, normalizeAmount(what, amount), tick) && savedData != null) {
            savedData.replaceFrom(PROFILER.snapshotSamples());
        }
    }

    public static void startJob(String networkId, Object scope, ICraftingPlan plan, long tick, long nanoTime) {
        startJob(networkId, scope, plan, tick, nanoTime, null);
    }

    public static void startJob(String networkId, Object scope, ICraftingPlan plan, long tick, long nanoTime,
            UUID owner) {
        if (plan == null || plan.finalOutput() == null || !isEnabled()) {
            return;
        }

        var craftedAmounts = new KeyCounter();
        craftedAmounts.addAll(plan.emittedItems());
        for (var entry : plan.patternTimes().entrySet()) {
            for (var output : entry.getKey().getOutputs()) {
                craftedAmounts.add(output.what(), output.amount() * entry.getValue());
            }
        }

        long predictedSeconds = 0;
        int knownRows = 0;
        int totalRows = 0;
        var waitingKeys = new HashSet<ProfileKey>();
        for (var crafted : craftedAmounts) {
            if (crafted.getLongValue() <= 0) {
                continue;
            }
            totalRows++;
            var key = key(networkId, crafted.getKey());
            waitingKeys.add(key);
            var stats = PROFILER.stats(key);
            var estimate = stats.isEmpty() ? java.util.OptionalLong.empty()
                    : TimeEstimate.seconds(normalizeAmount(crafted.getKey(), crafted.getLongValue()), stats.get());
            if (estimate.isPresent()) {
                knownRows++;
                predictedSeconds += estimate.getAsLong();
            }
        }

        PROFILER.startWaiting(scope, waitingKeys, tick);
        PROFILER.setJobOwner(scope, owner);
        ProviderStartTracker.clear(scope);
        for (var crafted : craftedAmounts) {
            if (crafted.getLongValue() <= 0) {
                continue;
            }
            ProviderLocateRecords.noteStart(key(networkId, crafted.getKey()), owner, null,
                    displayNameOf(crafted.getKey()));
        }
        persistProviderState();
        ACCURACY.start(key(networkId, plan.finalOutput().what()), scope, predictedSeconds, knownRows, totalRows, tick,
                nanoTime);
    }

    public static UUID jobOwner(appeng.api.networking.security.IActionSource source) {
        if (source == null) {
            return null;
        }
        try {
            var player = source.player();
            if (player.isEmpty() || player.get() == null) {
                return null;
            }
            return player.get().getUUID();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Optional<UUID> jobOwner(Object scope) {
        if (scope == null || !isEnabled()) {
            return Optional.empty();
        }
        return PROFILER.jobOwner(scope);
    }

    public static List<CraftProfiler.DelayedEvent> pollNewlyDelayed(Object scope, long tick) {
        if (scope == null || !isEnabled()) {
            return List.of();
        }
        return PROFILER.pollNewlyDelayed(scope, tick);
    }

    public static String displayName(ProfileKey key) {
        if (key == null) {
            return "?";
        }
        var name = DISPLAY_NAMES.get(key);
        return name != null && !name.isBlank() ? name : key.outputId();
    }

    public static String dimensionId(IGrid grid) {
        if (grid == null || grid.getPivot() == null) {
            return "";
        }
        try {
            return grid.getPivot().getLevel().dimension().location().toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * Provider positions for a delayed output: freshly resolved through the
     * live grid first, persisted fallback second, empty when not locatable.
     */
    public static List<BlockPos> locatePositions(Object scope, IGrid grid, ProfileKey key) {
        if (scope == null || key == null) {
            return List.of();
        }
        var live = ProviderStartTracker.positions(grid, scope, key);
        if (!live.isEmpty()) {
            return live;
        }
        return ProviderLocateRecords.startFor(key)
                .map(ProviderLocateRecords.ProviderStartInfo::positions)
                .orElse(List.of());
    }

    public static void replaceProviderStart(ProfileKey key, UUID owner,
            List<BlockPos> positions, String outputName) {
        ProviderLocateRecords.replaceStart(key, owner, positions, outputName);
    }

    public static void persistProviderState() {
        if (savedData != null) {
            savedData.replaceProviderStarts(ProviderLocateRecords.snapshotStarts());
        }
    }

    public static void finishJob(Object scope, boolean success, long tick, long nanoTime) {
        ACCURACY.finish(scope, success && isEnabled(), tick, nanoTime);
        PROFILER.clearPending(scope);
        ProviderStartTracker.clear(scope);
        BlockReasonNotifier.clear(scope);
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

    public static Optional<TtcAccuracyStats> accuracy(ProfileKey key) {
        if (key == null || !isEnabled()) {
            return Optional.empty();
        }
        return ACCURACY.stats(key);
    }

    public static OptionalLong waitingTicks(ProfileKey key, Object scope, long tick) {
        return key == null || !isEnabled() ? OptionalLong.empty() : PROFILER.waitingTicks(key, scope, tick);
    }

    public static void updateCapacity(Object scope, int usedParallelSlots, int totalParallelSlots, long tick) {
        if (isEnabled()) {
            PROFILER.updateCapacity(scope, usedParallelSlots, totalParallelSlots, tick);
        }
    }

    public static Optional<StatsEntry> entry(ProfileKey lookupKey, ProfileKey displayKey) {
        return entry(lookupKey, displayKey, null, 0);
    }

    public static Optional<StatsEntry> entry(ProfileKey lookupKey, ProfileKey displayKey, Object scope, long tick) {
        return stats(lookupKey).or(() -> PROFILER.inProgressStats(lookupKey, tick))
                .map(stats -> new StatsEntry(displayKey, stats, accuracy(lookupKey),
                        scope == null ? Optional.empty() : PROFILER.stall(lookupKey, scope, tick)));
    }

    public static boolean clearStats(ProfileKey key) {
        if (key == null || !isEnabled()) {
            return false;
        }
        var cleared = PROFILER.clearSamples(key);
        ACCURACY.clear(key);
        if (cleared && savedData != null) {
            savedData.replaceFrom(PROFILER.snapshotSamples());
        }
        return cleared;
    }

    private static boolean isEnabled() {
        var enabled = Ae2CraftingTimeConfig.ENABLED.get();
        PROFILER.setEnabled(enabled);
        return enabled;
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
        var dimensionId = grid.getPivot().getLevel().dimension().location().toString();
        var controllerAnchors = new java.util.ArrayList<net.minecraft.core.BlockPos>();
        for (var controller : grid.getMachines(ControllerBlockEntity.class)) {
            controllerAnchors.add(controller.getBlockPos());
        }
        return GridNetworkIds.fromControllers(dimensionId, controllerAnchors);
    }

    public static void load(Ae2CraftingTimeSavedData data) {
        savedData = data;
        ACCURACY.clear();
        PROFILER.loadSamples(data.samples());
        ProviderStartTracker.clearAll();
        ProviderLocateRecords.clearAll();
        BlockReasonNotifier.clearAll();
        ProviderLocateRecords.restoreStarts(data.providerStarts());
        var migrated = PROFILER.snapshotSamples();
        if (!migrated.equals(data.samples())) {
            savedData.replaceFrom(migrated);
        }
    }

    private static String displayNameOf(AEKey key) {
        try {
            return key.getDisplayName().getString();
        } catch (Exception ignored) {
            return key.getId().toString();
        }
    }

    private static ProfileUnit unit(AEKey key) {
        return AeKeyAmounts.unit(key);
    }

    private static long normalizeAmount(AEKey key, long amount) {
        return AeKeyAmounts.normalize(key, amount);
    }

    private ProfilerBridge() {
    }
}
