package com.ctux.ae2craftingtime.testdriver.mixin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Connection.class)
public class ConnectionReadOrderingMixin {
    @Shadow private Channel channel;

    @Redirect(method = "setProtocol", at = @At(value = "INVOKE",
            target = "Lio/netty/channel/ChannelConfig;setAutoRead(Z)Lio/netty/channel/ChannelConfig;",
            remap = false))
    private ChannelConfig ae2craftingtime_test_driver$queueAutoRead(ChannelConfig config, boolean enabled) {
        // Match Forge: the connector's queued clearReadPending must run before enabling reads.
        channel.eventLoop().execute(() -> config.setAutoRead(enabled));
        return config;
    }
}
