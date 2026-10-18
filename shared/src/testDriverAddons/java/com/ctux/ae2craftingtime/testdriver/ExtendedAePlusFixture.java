package com.ctux.ae2craftingtime.testdriver;

import net.minecraft.server.level.ServerPlayer;

final class ExtendedAePlusFixture extends ExtendedAeFixture {
    @Override
    protected Placement place(ServerPlayer player, FixtureMarker marker) {
        if (!DriverPlatform.isModLoaded("extendedae_plus")) {
            throw new IllegalStateException("ExtendedAE-Plus is unavailable");
        }
        return super.place(player, marker);
    }
}
