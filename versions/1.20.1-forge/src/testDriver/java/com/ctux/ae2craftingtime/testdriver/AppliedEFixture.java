package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEBlocks;
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
    private final CrazyAe2AddonsFixture nativeCpu = new CrazyAe2AddonsFixture();
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
        var terminalFace = Direction.valueOf(marker.terminal().face());
        var energyPosition = Arrays.stream(Direction.values()).filter(candidate -> candidate != terminalFace)
                .map(terminal::relative).filter(player.serverLevel()::isEmptyBlock).findFirst()
                .orElseThrow(() -> new IllegalStateException("AppliedE fixture has no space for grid power"));
        player.serverLevel().setBlockAndUpdate(energyPosition,
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        var moduleItem = AppliedE.EMC_MODULE.get();
        var side = Arrays.stream(Direction.values()).filter(candidate -> partHost.getPart(candidate) == null
                        && partHost.canAddPart(new ItemStack(moduleItem), candidate))
                .findFirst().orElseThrow(() -> new IllegalStateException("AppliedE fixture has no free cable side"));
        var provider = ITransmutationProxy.INSTANCE.getKnowledgeProviderFor(player.getUUID());
        var target = AEItemKey.of(Blocks.FURNACE);
        provider.addKnowledge(target.toStack());
        provider.setEmc(provider.getEmc().add(BigInteger.valueOf(1_000_000)));
        provider.sync(player);
        var module = partHost.addPart((IPartItem<?>) moduleItem, side, player);
        if (module == null) {
            throw new IllegalStateException("AppliedE fixture could not place the transmutation module");
        }
        return new Placement(grid, (EMCModulePart) module, target, nativeCpu.place(player, marker));
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        if (!nativeCpu.finish(player, placement.cpu())) {
            return false;
        }
        var moduleNode = placement.module().getGridNode();
        if (!moduleNode.isActive()) {
            failIfSetupStalled(placement, moduleNode);
            return false;
        }
        placement.grid().getCraftingService().refreshNodeCraftingProvider(moduleNode);
        if (!placement.grid().getCraftingService().isCraftable(placement.target())) {
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
        return nativeCpu.cpu(player, placement.cpu(), grid);
    }

    record Placement(IGrid grid, EMCModulePart module, AEItemKey target,
            CrazyAe2AddonsFixture.Placement cpu) {
    }
}
