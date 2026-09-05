package com.ctux.ae2craftingtime.testdriver;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

interface RequesterFixture {
    String SCENARIO = "merequester-screen";
    String RECOVERY = "merequester-read-recovery";
    static boolean supports(String scenario) { return SCENARIO.equals(scenario) || RECOVERY.equals(scenario); }
    String SCREEN = "com.almostreliable.merequester.client.RequesterScreen";

    static RequesterFixture create() {
        try {
            return (RequesterFixture) Class.forName("com.ctux.ae2craftingtime.testdriver.MeRequesterFixture")
                    .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError error) {
            throw new IllegalStateException("ME Requester fixture is unavailable for this target", error);
        }
    }

    boolean setup(ServerPlayer player, FixtureMarker marker);
    BlockPos position();
}
