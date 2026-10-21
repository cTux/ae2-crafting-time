package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

/** A physically full, blocking native provider must not commit work until released. */
final class DataEnergisticsTrinityProviderFixture extends DataEnergisticsTrinityFixture {
    private boolean obstructed;

    @Override boolean supportsProviderBlock() { return true; }

    @Override
    protected boolean finishVariant(ServerPlayer player, appeng.api.networking.IGrid grid, Placement placement) {
        if (obstructed) return true;
        var provider = (PatternProviderBlockEntity) player.serverLevel().getBlockEntity(placement.provider());
        provider.getLogic().getConfigManager().putSetting(Settings.BLOCKING_MODE, YesNo.YES);
        if (!provider.getLogic().isBlocking())
            throw new IllegalStateException("native provider refused blocking mode");
        var furnace = (FurnaceBlockEntity) player.serverLevel().getBlockEntity(placement.provider().below());
        furnace.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        furnace.setItem(1, ItemStack.EMPTY);
        furnace.setItem(2, ItemStack.EMPTY);
        furnace.setChanged();
        obstructed = true;
        return true;
    }

    @Override
    boolean releaseBlockedProvider(ServerPlayer player, String networkId, String outputId) {
        if (!obstructed || worker == null || !worker.isBusy() || !"minecraft:glass".equals(outputId)) return false;
        var provider = (PatternProviderBlockEntity) player.serverLevel().getBlockEntity(placement.provider());
        var key = new ProfileKey(networkId, outputId);
        if (!provider.getLogic().isBlocking()
                || ProfilerBridge.hasPending(key)
                || ProfilerBridge.stats(key).map(stats -> stats.sampleCount()).orElse(0) != 0)
            throw new IllegalStateException("physically blocked native provider committed or profiled work");
        if (submittedGrid.getStorageService().getInventory().getAvailableStacks().get(outputKey()) != beforeOutput)
            throw new IllegalStateException("blocked native provider produced output before release");
        var furnace = (FurnaceBlockEntity) player.serverLevel().getBlockEntity(placement.provider().below());
        furnace.setItem(0, ItemStack.EMPTY);
        furnace.setItem(2, ItemStack.EMPTY);
        furnace.setItem(1, new ItemStack(Items.COAL, 64));
        furnace.setChanged();
        return true;
    }
}
