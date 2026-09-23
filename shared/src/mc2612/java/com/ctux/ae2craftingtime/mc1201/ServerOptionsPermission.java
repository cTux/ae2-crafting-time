package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

final class ServerOptionsPermission {
    static boolean canEdit(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
    }

    private ServerOptionsPermission() { }
}
