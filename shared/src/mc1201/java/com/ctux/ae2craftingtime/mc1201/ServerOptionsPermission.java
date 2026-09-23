package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.server.level.ServerPlayer;

final class ServerOptionsPermission {
    static boolean canEdit(ServerPlayer player) { return player.hasPermissions(4); }

    private ServerOptionsPermission() { }
}
