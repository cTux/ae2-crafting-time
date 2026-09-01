package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEItemKey;
import gripe._90.appliede.AppliedE;
import gripe._90.appliede.part.EMCModulePart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import moze_intel.projecte.api.proxy.ITransmutationProxy;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

final class AppliedEFixture extends AddonCpuFixture<AppliedEFixture.Placement> {
    private int finishAttempts;

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        if (player == null) {
            throw new IllegalStateException("fixture player is unavailable");
        }
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        var blockEntity = player.serverLevel().getBlockEntity(terminal);
        if (!(blockEntity instanceof IInWorldGridNodeHost terminalHost) || !(blockEntity instanceof IPartHost partHost)) {
            throw new IllegalStateException("AppliedE fixture terminal is unavailable");
        }
        var grid = Arrays.stream(Direction.values()).map(terminalHost::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("AppliedE fixture grid is unavailable"));
        var moduleItem = AppliedE.EMC_MODULE.get();
        var side = Arrays.stream(Direction.values()).filter(candidate -> partHost.getPart(candidate) == null
                        && partHost.canAddPart(new ItemStack(moduleItem), candidate))
                .findFirst().orElseThrow(() -> new IllegalStateException("AppliedE fixture has no free cable side"));
        var provider = ITransmutationProxy.INSTANCE.getKnowledgeProviderFor(player.getUUID());
        var cobblestone = AEItemKey.of(Blocks.COBBLESTONE);
        provider.addKnowledge(cobblestone.toStack());
        provider.setEmc(provider.getEmc().add(BigInteger.valueOf(1_000_000)));
        provider.sync(player);
        var inventory = grid.getStorageService().getInventory();
        var source = IActionSource.ofPlayer(player);
        inventory.extract(cobblestone, Long.MAX_VALUE, Actionable.MODULATE, source);
        if (inventory.insert(cobblestone, 1, Actionable.MODULATE, source) != 1) {
            throw new IllegalStateException("AppliedE fixture could not expose its crafting target");
        }
        grid.getEnergyService().injectPower(1_000_000, Actionable.MODULATE);
        var module = partHost.addPart((IPartItem<?>) moduleItem, side, player);
        if (module == null) {
            throw new IllegalStateException("AppliedE fixture could not place the transmutation module");
        }
        return new Placement(grid, (EMCModulePart) module, cobblestone);
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var moduleNode = placement.module().getGridNode();
        placement.grid().getEnergyService().injectPower(1_000_000, Actionable.MODULATE);
        if (!moduleNode.isActive()) {
            failIfSetupStalled(placement, moduleNode);
            return false;
        }
        placement.grid().getCraftingService().refreshNodeCraftingProvider(moduleNode);
        if (!placement.grid().getCraftingService().isCraftable(placement.cobblestone())) {
            failIfSetupStalled(placement, moduleNode);
            return false;
        }
        return true;
    }

    private void failIfSetupStalled(Placement placement, IGridNode moduleNode) {
        if (++finishAttempts >= 40) {
            throw new IllegalStateException("AppliedE fixture did not become craftable: active="
                    + moduleNode.isActive() + ", powered=" + moduleNode.isPowered()
                    + ", channel=" + moduleNode.meetsChannelRequirements()
                    + " (used=" + moduleNode.getUsedChannels() + ", max=" + moduleNode.getMaxChannels() + ")"
                    + ", owner=" + moduleNode.getOwningPlayerProfileId()
                    + ", patterns=" + placement.module().getAvailablePatterns().size());
        }
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        return grid.getCraftingService().getCpus().stream().filter(cpu -> !cpu.isBusy()).findFirst().orElse(null);
    }

    @Override
    protected String outputId(Placement placement, FixtureMarker marker) {
        return placement.cobblestone().getId().toString();
    }

    record Placement(IGrid grid, EMCModulePart module, AEItemKey cobblestone) {
    }
}
