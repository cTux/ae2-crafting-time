package com.ctux.ae2craftingtime.testdriver;

import appeng.api.features.GridLinkables;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import com.lhy.wcwt.WcwtMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.Objects;

final class WcwtTerminalFixture {
    static void setup(ServerPlayer player, FixtureMarker marker) {
        if (!ModList.get().isLoaded("wcwt")) {
            throw new IllegalStateException("AE2 WCWT is unavailable");
        }
        var level = player.serverLevel();
        var terminal = new BlockPos(marker.terminal().x(), marker.terminal().y(), marker.terminal().z());
        if (!(level.getBlockEntity(terminal) instanceof IInWorldGridNodeHost terminalHost)) {
            throw new IllegalStateException("fixture terminal is unavailable");
        }
        var terminalNode = Arrays.stream(Direction.values()).map(terminalHost::getGridNode)
                .filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("fixture terminal node is unavailable"));
        var accessPointBlock = ForgeRegistries.BLOCKS.getValue(
                Objects.requireNonNull(ResourceLocation.tryBuild("ae2", "wireless_access_point")));
        if (accessPointBlock == null) {
            throw new IllegalStateException("AE2 wireless access point is unavailable");
        }
        var accessPointPos = BlockPos.betweenClosedStream(terminal.offset(-6, -2, -6), terminal.offset(6, 2, 6))
                .filter(pos -> level.getBlockState(pos).isAir()).findFirst()
                .map(BlockPos::immutable)
                .orElseThrow(() -> new IllegalStateException("no space for the WCWT access point"));
        level.setBlockAndUpdate(accessPointPos, accessPointBlock.defaultBlockState());
        if (!(level.getBlockEntity(accessPointPos) instanceof WirelessAccessPointBlockEntity accessPoint)) {
            throw new IllegalStateException("AE2 wireless access point was not placed");
        }
        if (!accessPoint.getMainNode().isReady()) {
            accessPoint.onReady();
        }
        var accessPointNode = accessPoint.getMainNode().getNode();
        if (accessPointNode == null) {
            throw new IllegalStateException("AE2 wireless access point node is unavailable");
        }
        if (terminalNode.getGrid() != accessPointNode.getGrid()) {
            GridHelper.createConnection(terminalNode, accessPointNode);
        }

        var stack = WcwtMod.chargedTerminalStack();
        var linkable = GridLinkables.get(stack.getItem());
        if (linkable == null || !linkable.canLink(stack)) {
            throw new IllegalStateException("AE2 WCWT terminal cannot be linked");
        }
        linkable.link(stack, GlobalPos.of(level.dimension(), accessPointPos));
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, stack);
        player.inventoryMenu.broadcastChanges();
    }

    private WcwtTerminalFixture() {
    }
}
