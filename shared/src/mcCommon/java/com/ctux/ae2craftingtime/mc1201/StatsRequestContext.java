package com.ctux.ae2craftingtime.mc1201;

import appeng.api.networking.IGrid;
import com.ctux.ae2craftingtime.core.IntegrationRead;
import appeng.api.networking.security.IActionHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.me.crafting.CraftingCPUMenu;
import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.mc1201.mixin.CraftingCPUMenuAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record StatsRequestContext(IGrid grid, Object craftingCpu) {
    public static java.util.UUID currentJobId(Object cpu) {
        return com.ctux.ae2craftingtime.core.RowStatsJob.readJobId(cpu);
    }

    public static long cpuContext(AbstractContainerMenu menu) {
        var serial = menu instanceof CraftingStatusMenu status ? status.getSelectedCpuSerial() : -1;
        return ((long) menu.containerId << 32) | Integer.toUnsignedLong(serial);
    }

    public static StatsRequestContext current(ServerPlayer player) {
        if (player.containerMenu instanceof CraftingCPUMenu menu) {
            var accessor = (CraftingCPUMenuAccessor) menu;
            var cpu = accessor.ae2craftingtime$getCpu();
            return new StatsRequestContext(accessor.ae2craftingtime$getGrid(),
                    cpu != null ? cpu : optionalSelectedCpu(menu));
        }
        if (player.containerMenu instanceof AEBaseMenu menu && menu.getTarget() instanceof IActionHost host) {
            var node = host.getActionableNode();
            return new StatsRequestContext(node == null ? null : node.getGrid(), null);
        }
        return new StatsRequestContext(null, null);
    }

    private static Object optionalAdvancedCpu(CraftingCPUMenu menu) {
        if (!ServerOptionsRuntime.enabled(com.ctux.ae2craftingtime.core.OptionFeature.ADVANCED_AE)
                || !IntegrationLog.available("advanced_ae") || IntegrationLog.disabled("advanced_ae", "selected-cpu")) return null;
        try {
            var cpu = IntegrationRead.field(menu, CraftingCPUMenu.class, "advancedAE$advCpu", Object.class);
            IntegrationLog.observe("advanced_ae", "selected-cpu");
            return cpu;
        } catch (IntegrationRead.Failure failure) {
            IntegrationLog.fail("advanced_ae", "selected-cpu", failure.getMessage()
                    + ";retained=network_aggregates,cpu_profiling", false, failure.getCause());
            return null;
        }
    }

    private static Object optionalSelectedCpu(CraftingCPUMenu menu) {
        var advanced = optionalAdvancedCpu(menu);
        return advanced != null ? advanced : optionalTrinityCpu(menu);
    }

    private static Object optionalTrinityCpu(CraftingCPUMenu menu) {
        if (!ServerOptionsRuntime.enabled(com.ctux.ae2craftingtime.core.OptionFeature.DATA_ENERGISTICS_TRINITY)
                || !IntegrationLog.available("data_energistics")
                || IntegrationLog.disabled("data_energistics", "selected-cpu")) return null;
        try {
            var cpu = IntegrationRead.field(menu, CraftingCPUMenu.class, "dataEnergistics$cpu", Object.class);
            if (cpu != null && cpu.getClass().getName().equals(
                    "com.fish_dan_.data_energistics.common.crafting.trinity.execution.cpu.TrinityDataCoreVirtualCpu")) {
                IntegrationLog.observe("data_energistics", "selected-cpu");
                return cpu;
            }
            return null;
        } catch (IntegrationRead.Failure failure) {
            IntegrationLog.fail("data_energistics", "selected-cpu", failure.getMessage()
                    + ";retained=network_aggregates,cpu_profiling", false, failure.getCause());
            return null;
        }
    }
}
