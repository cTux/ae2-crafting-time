package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftConfirmMenuAccessor;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.ObjectArguments;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import de.srendi.advancedperipherals.common.blocks.blockentities.MeBridgeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

final class AdvancedPeripheralsFixture extends AddonCpuFixture<AdvancedPeripheralsFixture.Placement> {
    private final NativeCpuFixture nativeCpu = new NativeCpuFixture();

    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        var cpu = nativeCpu.place(player, marker);
        var level = player.serverLevel();
        var host = (IInWorldGridNodeHost) level.getBlockEntity(cpu.terminal());
        var grid = Arrays.stream(Direction.values()).map(host::getGridNode).filter(Objects::nonNull)
                .map(node -> node.getGrid()).filter(Objects::nonNull).findFirst().orElseThrow();
        var drive = grid.getMachines(DriveBlockEntity.class).stream()
                .filter(candidate -> candidate.getMainNode().isActive()).findFirst().orElseThrow();
        var inventory = drive.getInternalInventory();
        var slot = -1;
        for (var index = 0; index < inventory.size(); index++) {
            if (inventory.getStackInSlot(index).isEmpty()) {
                slot = index;
                break;
            }
        }
        if (slot < 0) {
            throw new IllegalStateException("ME Bridge fixture needs an empty drive slot");
        }
        inventory.setItemDirect(slot, AEItems.ITEM_CELL_1K.stack());
        drive.onChangeInventory(inventory, slot);
        var bridgeBlock = Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(
                Objects.requireNonNull(ResourceLocation.tryBuild("advancedperipherals", "me_bridge"))));
        var computerBlock = Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(
                Objects.requireNonNull(ResourceLocation.tryBuild("computercraft", "computer_advanced"))));
        for (var anchor : BlockPos.betweenClosed(cpu.terminal().offset(-12, -4, -12), cpu.terminal().offset(12, 4, 12))) {
            if (!(level.getBlockEntity(anchor) instanceof IInWorldGridNodeHost candidate)) {
                continue;
            }
            var node = candidate.getGridNode(Direction.UP);
            var bridgePos = anchor.above().immutable();
            var computerPos = bridgePos.above();
            if (node != null && node.getGrid() == grid && level.isEmptyBlock(bridgePos) && level.isEmptyBlock(computerPos)) {
                level.setBlockAndUpdate(bridgePos, bridgeBlock.defaultBlockState());
                level.setBlockAndUpdate(computerPos, computerBlock.defaultBlockState());
                var bridge = (MeBridgeEntity) level.getBlockEntity(bridgePos);
                var computer = (AbstractComputerBlockEntity) level.getBlockEntity(computerPos);
                computer.createServerComputer().turnOn();
                return new Placement(cpu, grid, bridge, computer);
            }
        }
        throw new IllegalStateException("ME Bridge fixture has no space beside the grid");
    }

    @Override
    protected boolean finish(ServerPlayer player, Placement placement) {
        if (!nativeCpu.finish(player, placement.cpu())) {
            return false;
        }
        var node = placement.bridge().getActionableNode();
        if (node == null) {
            return false;
        }
        var host = (IInWorldGridNodeHost) player.serverLevel().getBlockEntity(placement.cpu().terminal());
        var terminalNode = Arrays.stream(Direction.values()).map(host::getGridNode).filter(Objects::nonNull)
                .findFirst().orElseThrow();
        if (node.getGrid() != terminalNode.getGrid()) {
            GridHelper.createConnection(terminalNode, node);
        }
        var peripheral = placement.bridge().getPeripheralOptional().orElseThrow();
        placement.computer().updateInputsImmediately();
        if (!peripheral.isConnected() || !placement.bridge().getConnectedComputers().iterator().hasNext()) {
            return false;
        }
        var storage = placement.grid().getStorageService().getInventory();
        var source = IActionSource.ofPlayer(player);
        var cobblestone = AEItemKey.of(Items.COBBLESTONE);
        var needed = 64 - storage.extract(cobblestone, 64, Actionable.SIMULATE, source);
        if (needed > 0 && storage.insert(cobblestone, needed, Actionable.MODULATE, source) != needed) {
            throw new IllegalStateException("ME Bridge fixture rejected crafting ingredients");
        }
        return true;
    }

    @Override
    protected void startCraft(ServerPlayer player, Placement placement, CraftConfirmMenu menu) {
        var output = ((CraftConfirmMenuAccessor) menu).ae2craftingtime_test_driver$result().finalOutput();
        var storage = (appeng.blockentity.crafting.CraftingBlockEntity) player.serverLevel()
                .getBlockEntity(placement.cpu().storage());
        storage.setName("AE2CT ME Bridge smoke CPU");
        var computer = placement.bridge().getConnectedComputers().iterator().next();
        try {
            var result = placement.bridge().getPeripheralOptional().orElseThrow().craftItem(computer,
                    new ObjectArguments(Map.of("name", output.what().getId().toString(), "count", output.amount()),
                            storage.getCluster().getName().getString()));
            if (!Boolean.TRUE.equals(result.getResult()[0])) {
                throw new IllegalStateException("ME Bridge rejected craft: " + Arrays.toString(result.getResult()));
            }
            menu.getHost().returnToMainMenu(player, menu);
        } catch (LuaException exception) {
            throw new IllegalStateException("ME Bridge craftItem failed", exception);
        }
    }

    @Override
    protected ICraftingCPU cpu(ServerPlayer player, Placement placement, IGrid grid) {
        return nativeCpu.cpu(player, placement.cpu(), grid);
    }

    record Placement(NativeCpuFixture.Placement cpu, IGrid grid, MeBridgeEntity bridge,
            AbstractComputerBlockEntity computer) {
    }
}
