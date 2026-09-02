package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import lu.kolja.expandedae.definition.ExpBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;

final class ExpandedAeFixture extends AddonCpuFixture<ExpandedAeFixture.Placement> {
    private final NativeCpuFixture nativeCpu = new NativeCpuFixture();

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        var cpu = nativeCpu.place(player, marker);
        var level = player.serverLevel();
        for (var direction : Direction.values()) {
            var accelerator = cpu.storage().relative(direction);
            if (level.isEmptyBlock(accelerator)) {
                level.setBlockAndUpdate(accelerator, ExpBlocks.CPU_2.block().defaultBlockState());
                return new Placement(cpu, accelerator);
            }
        }
        throw new IllegalStateException("Expanded AE fixture has no room for its accelerator");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        var level = player.serverLevel();
        var storage = (CraftingBlockEntity) level.getBlockEntity(placement.cpu().storage());
        var accelerator = (CraftingBlockEntity) level.getBlockEntity(placement.accelerator());
        if (!storage.getMainNode().isReady()) {
            return false;
        }
        if (!accelerator.getMainNode().isReady()) {
            return false;
        }
        if (storage.getCluster() == null || storage.getCluster() != accelerator.getCluster()) {
            var first = placement.cpu().storage();
            var second = placement.accelerator();
            var min = new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()),
                    Math.min(first.getZ(), second.getZ()));
            var max = new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()),
                    Math.max(first.getZ(), second.getZ()));
            var calculator = new CraftingCPUCalculator(storage);
            calculator.updateBlockEntities(calculator.createCluster(level, min, max), level, min, max);
        }
        return nativeCpu.finish(player, placement.cpu());
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        var cpu = nativeCpu.cpu(player, placement.cpu(), grid);
        if (cpu != null && cpu.getCoProcessors() != 2) {
            throw new IllegalStateException("Expanded AE accelerator expected 2 co-processors, got "
                    + cpu.getCoProcessors());
        }
        return cpu;
    }

    record Placement(NativeCpuFixture.Placement cpu, BlockPos accelerator) {
    }
}
