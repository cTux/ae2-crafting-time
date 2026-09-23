package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;

final class ServerOptionsPermission {
    static boolean canEdit(MinecraftServer server, ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_ADMIN)
                || server.isSingleplayerOwner(player.nameAndId());
    }

    private ServerOptionsPermission() { }
}
