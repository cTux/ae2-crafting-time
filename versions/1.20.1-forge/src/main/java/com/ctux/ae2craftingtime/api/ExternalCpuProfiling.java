package com.ctux.ae2craftingtime.api;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import java.util.OptionalDouble;
import net.minecraft.server.MinecraftServer;

/** Optional server API for CPUs that report their own crafting lifecycle. */
public final class ExternalCpuProfiling {
    private ExternalCpuProfiling() {
    }

    public static void startJob(ICraftingCPU cpu, IGrid grid, ICraftingPlan plan, IActionSource source, long tick) {
        if (server() == null || cpu == null || grid == null || !ProfilerBridge.trackingEnabled(cpu)) return;
        ProfilerBridge.startExternalJob(ProfilerBridge.networkId(grid), cpu, plan, tick,
                ProfilerBridge.jobOwner(source));
    }

    public static void expectOutput(ICraftingCPU cpu, IGrid grid, AEKey key, long amount, long tick) {
        if (server() == null || grid == null || !ProfilerBridge.tracksExternalJob(cpu)) return;
        ProfilerBridge.start(ProfilerBridge.networkId(grid), cpu, key, amount, tick);
    }

    public static void acceptOutput(ICraftingCPU cpu, IGrid grid, AEKey key, long amount, long tick) {
        var server = server();
        if (server == null || grid == null || !ProfilerBridge.tracksExternalJob(cpu)) return;
        ProfilerBridge.completeDispatched(ProfilerBridge.networkId(grid), cpu, key, amount, tick, server);
    }

    public static void finishJob(ICraftingCPU cpu, boolean success, long tick) {
        var server = server();
        if (server == null || cpu == null) return;
        ProfilerBridge.finishJob(cpu, success, tick, System.nanoTime(), server);
    }

    public static void setSuspended(ICraftingCPU cpu, boolean suspended, long tick) {
        var server = server();
        if (server == null || !ProfilerBridge.tracksExternalJob(cpu)) return;
        ProfilerBridge.setSuspended(cpu, suspended, tick, server);
    }

    public static OptionalDouble remainingMillis(ICraftingCPU cpu) {
        if (server() == null || !ProfilerBridge.tracksExternalJob(cpu) || !cpu.isBusy()) return OptionalDouble.empty();
        var estimate = ProfilerBridge.remainingJobEstimate(cpu);
        return estimate.isEmpty() ? OptionalDouble.empty() : OptionalDouble.of(estimate.get().durationMs());
    }

    private static MinecraftServer server() {
        var server = ProfilerBridge.recoveryServer();
        return server != null && server.isSameThread() ? server : null;
    }
}
