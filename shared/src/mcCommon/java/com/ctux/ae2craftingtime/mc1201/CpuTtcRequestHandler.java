package com.ctux.ae2craftingtime.mc1201;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.core.CpuTtcCache;
import com.ctux.ae2craftingtime.core.CpuTtcRateLimit;
import com.ctux.ae2craftingtime.core.CpuTtcResolver;
import com.ctux.ae2craftingtime.mc1201.mixin.CraftingStatusMenuAccessor;
import com.ctux.ae2craftingtime.mc1201.net.CpuTtcPacketCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Predicate;

public final class CpuTtcRequestHandler {
    private static final CpuTtcRateLimit RATE_LIMIT = new CpuTtcRateLimit();

    public static Response collect(ServerPlayer player, CpuTtcPacketCodec.Request request) {
        if (!RATE_LIMIT.allow(player.getUUID(), request.serials().size(),
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()))
                || !(player.containerMenu instanceof CraftingStatusMenu menu)
                || menu.containerId != request.containerId() || !menu.stillValid(player)) {
            return null;
        }
        var context = StatsRequestContext.current(player);
        if (context.grid() == null) {
            return null;
        }
        var accessor = (CraftingStatusMenuAccessor) menu;
        var listed = accessor.ae2craftingtime$getLastCpuSet();
        var serialMap = accessor.ae2craftingtime$getCpuSerialMap();
        var entries = resolveListed(request.serials(), serialMap, listed,
                context.grid().getCraftingService().getCpus(), ICraftingCPU::isBusy,
                ProfilerBridge::remainingJobSeconds);
        return new Response(request.session(), request.sequence(), entries);
    }

    static <T> List<CpuTtcCache.Entry> resolveListed(List<Integer> requested, Map<T, Integer> serials,
            Collection<T> listed, Collection<T> gridCpus, Predicate<T> busy,
            Function<T, OptionalLong> estimate) {
        var liveListed = gridCpus.stream().filter(listed::contains).toList();
        return CpuTtcResolver.resolve(requested, serials, liveListed, busy, estimate);
    }

    public static void clear(UUID playerId) {
        RATE_LIMIT.clear(playerId);
    }

    public static void clear() {
        RATE_LIMIT.clear();
    }

    public record Response(long session, long sequence, java.util.List<CpuTtcCache.Entry> entries) {
        public Response {
            entries = java.util.List.copyOf(entries);
        }
    }

    private CpuTtcRequestHandler() {
    }
}
