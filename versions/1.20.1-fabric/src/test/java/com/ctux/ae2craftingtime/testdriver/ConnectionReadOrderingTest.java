package com.ctux.ae2craftingtime.testdriver;

import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionReadOrderingTest {
    @Test
    void protocolEnableRunsAfterTheConnectorsQueuedReadDisable() throws Exception {
        var armed = new AtomicBoolean();
        var channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter() {
            @Override
            public void read(ChannelHandlerContext context) {
                armed.set(true);
            }
        });
        try (var loader = new URLClassLoader(new java.net.URL[] {
                Path.of("build/classes/java/testDriver").toUri().toURL()
        }, getClass().getClassLoader())) {
            // Model NioSocketChannel's off-loop autoReadCleared: remove OP_READ later.
            channel.config().setAutoRead(false);
            channel.eventLoop().execute(() -> armed.set(false));
            channel.config().setAutoRead(true); // Vanilla channelActive/setProtocol.
            channel.runPendingTasks();
            channel.config().setAutoRead(true); // Login protocol repeats true, so no read().
            assertTrue(channel.config().isAutoRead());
            assertFalse(armed.get(), "vanilla ordering leaves an open channel without read interest");

            var mixin = loader.loadClass(
                    "com.ctux.ae2craftingtime.testdriver.mixin.ConnectionReadOrderingMixin");
            var instance = mixin.getConstructor().newInstance();
            var field = mixin.getDeclaredField("channel");
            field.setAccessible(true);
            field.set(instance, channel);
            var enable = mixin.getDeclaredMethod("ae2craftingtime_test_driver$queueAutoRead",
                    ChannelConfig.class, boolean.class);
            enable.setAccessible(true);
            channel.config().setAutoRead(false);
            channel.eventLoop().execute(() -> armed.set(false));
            assertSame(channel.config(), enable.invoke(instance, channel.config(), true));
            assertFalse(channel.config().isAutoRead(), "enable must not overtake queued clearReadPending");
            channel.runPendingTasks();
            assertTrue(channel.config().isAutoRead());
            assertTrue(armed.get(), "the queued enable must restore read interest after the clear");
            assertTrue(Files.readString(Path.of(
                    "src/testDriver/resources/ae2craftingtime_test_driver-fabric.mixins.json"))
                    .contains("\"ConnectionReadOrderingMixin\""));
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
