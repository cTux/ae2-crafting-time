package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

final class ServerOptionsPermission {
    static boolean canEdit(MinecraftServer server, ServerPlayer player) {
        return player.hasPermissions(4) || server.isSingleplayerOwner(player.getGameProfile());
    }

    private ServerOptionsPermission() { }
}
