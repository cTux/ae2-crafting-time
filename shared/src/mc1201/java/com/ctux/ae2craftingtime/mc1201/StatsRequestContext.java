package com.ctux.ae2craftingtime.mc1201;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.me.crafting.CraftingCPUMenu;
import net.minecraft.server.level.ServerPlayer;

public record StatsRequestContext(IGrid grid, Object craftingCpu) {
    public static StatsRequestContext current(ServerPlayer player) {
        if (player.containerMenu instanceof CraftingCPUMenu menu) {
            return new StatsRequestContext(craftingCpuGrid(menu), craftingCpu(menu));
        }
        if (player.containerMenu instanceof AEBaseMenu menu && menu.getTarget() instanceof IActionHost host) {
            var node = host.getActionableNode();
            return new StatsRequestContext(node == null ? null : node.getGrid(), null);
        }
        return new StatsRequestContext(null, null);
    }

    private static IGrid craftingCpuGrid(CraftingCPUMenu menu) {
        try {
            var method = CraftingCPUMenu.class.getDeclaredMethod("getGrid");
            method.setAccessible(true);
            return (IGrid) method.invoke(menu);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Object craftingCpu(CraftingCPUMenu menu) {
        try {
            var field = CraftingCPUMenu.class.getDeclaredField("cpu");
            field.setAccessible(true);
            return field.get(menu);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
