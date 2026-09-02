package com.ctux.ae2craftingtime.testdriver;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

final class ExtendedAePlusFixture extends ExtendedAeFixture {
    @Override
    protected List<BlockPos> place(ServerPlayer player, FixtureMarker marker) {
        if (!DriverPlatform.isModLoaded("extendedae_plus")) {
            throw new IllegalStateException("ExtendedAE-Plus is unavailable");
        }
        return super.place(player, marker);
    }
}
