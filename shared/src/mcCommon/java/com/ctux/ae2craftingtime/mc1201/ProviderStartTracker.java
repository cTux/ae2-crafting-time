package com.ctux.ae2craftingtime.mc1201;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.me.service.CraftingService;
import appeng.me.InWorldGridNode;
import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.core.ProfileKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;

/**
 * Server-side only. Remembers which patterns each crafting CPU dispatched per
 * output so a later DELAYED warning can resolve the providers that ran the
 * craft. Positions resolve at notify time through the live grid, never from
 * stale dispatch data.
 */
public final class ProviderStartTracker {
    private static final Map<Object, Map<ProfileKey, Set<IPatternDetails>>> PATTERNS = new IdentityHashMap<>();

    public static void noteDispatch(Object scope, IPatternDetails pattern, Map<ProfileKey, Long> outputs) {
        if (scope == null || pattern == null || outputs == null || outputs.isEmpty()) {
            return;
        }
        var scoped = PATTERNS.computeIfAbsent(scope, ignored -> new HashMap<>());
        for (var entry : outputs.entrySet()) {
            if (entry.getKey() != null && entry.getValue() > 0) {
                scoped.computeIfAbsent(entry.getKey(), ignored -> new HashSet<>()).add(pattern);
            }
        }
    }

    public static void clear(Object scope) {
        if (scope != null) {
            PATTERNS.remove(scope);
        }
    }

    public static void clearAll() {
        PATTERNS.clear();
    }

    /**
     * Resolves distinct world positions of providers currently offering the
     * output's dispatched patterns, capped for packets. Empty when nothing is
     * locatable.
     */
    public static List<BlockPos> positions(IGrid grid, Object scope, ProfileKey key) {
        if (grid == null || scope == null || key == null) {
            return List.of();
        }
        var scoped = PATTERNS.get(scope);
        if (scoped == null) {
            return List.of();
        }
        var patterns = scoped.getOrDefault(key, Set.of());
        if (patterns.isEmpty()) {
            return List.of();
        }
        CraftingService crafting;
        try {
            crafting = (CraftingService) grid.getCraftingService();
        } catch (Exception ignored) {
            return List.of();
        }
        var positions = new ArrayList<BlockPos>();
        for (var pattern : patterns) {
            if (pattern == null) {
                continue;
            }
            Iterable<ICraftingProvider> providers;
            try {
                providers = crafting.getProviders(pattern);
            } catch (Exception ignored) {
                continue;
            }
            for (var provider : providers) {
                if (provider == null) {
                    continue;
                }
                locate(grid, provider).ifPresent(pos -> {
                    if (!positions.contains(pos)) {
                        positions.add(pos);
                    }
                });
                if (positions.size() >= PacketLimits.MAX_HIGHLIGHT_POSITIONS) {
                    return List.copyOf(positions);
                }
            }
        }
        return List.copyOf(positions);
    }

    private static java.util.Optional<BlockPos> locate(IGrid grid, ICraftingProvider provider) {
        Iterable<IGridNode> nodes;
        try {
            nodes = grid.getNodes();
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
        for (var node : nodes) {
            try {
                if (node instanceof InWorldGridNode inWorld
                        && node.getService(ICraftingProvider.class) == provider) {
                    return java.util.Optional.of(inWorld.getLocation());
                }
            } catch (Exception ignored) {
                // One unreadable node must not hide the rest.
            }
        }
        return java.util.Optional.empty();
    }

    private ProviderStartTracker() {
    }
}
