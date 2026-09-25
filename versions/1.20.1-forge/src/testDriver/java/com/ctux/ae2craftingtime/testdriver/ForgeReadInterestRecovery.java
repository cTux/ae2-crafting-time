package com.ctux.ae2craftingtime.testdriver;

import io.netty.channel.Channel;
import io.netty.channel.nio.AbstractNioChannel;
import java.lang.reflect.Field;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.network.Connection;

/** Recovers Forge test-client login channels left with auto-read enabled but no read interest. */
final class ForgeReadInterestRecovery {
    private static final long DELAY_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final Field CONNECTION = connectionField();
    private static final Field SELECTION_KEY = field(AbstractNioChannel.class, "selectionKey");

    private final IdentityHashMap<Channel, Long> firstSeen = new IdentityHashMap<>();
    private final Set<Channel> attempted = Collections.newSetFromMap(new IdentityHashMap<>());

    void tick() {
        if (!(Minecraft.getInstance().screen instanceof ConnectScreen screen)) return;
        try {
            var connection = (Connection) CONNECTION.get(screen);
            if (connection == null || connection.channel() == null) return;
            var channel = connection.channel();
            long now = System.nanoTime();
            long first = firstSeen.computeIfAbsent(channel, ignored -> now);
            if (now - first < DELAY_NANOS || attempted.contains(channel) || !missingReadInterest(channel)) return;
            attempted.add(channel);
            channel.eventLoop().execute(() -> {
                if (!missingReadInterest(channel)) return;
                int before = selectionKey(channel).interestOps();
                channel.read();
                System.out.println("Forge test-client read interest rearmed: channel=" + channel.id().asLongText()
                        + " before=" + before + " after=" + selectionKey(channel).interestOps());
            });
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Cannot inspect Forge test-client connection", error);
        }
    }

    private static boolean missingReadInterest(Channel channel) {
        if (!channel.isActive() || !channel.isRegistered() || !channel.config().isAutoRead()) return false;
        var key = selectionKey(channel);
        try {
            return key != null && key.isValid() && (key.interestOps() & SelectionKey.OP_READ) == 0;
        } catch (CancelledKeyException ignored) {
            return false;
        }
    }

    private static SelectionKey selectionKey(Channel channel) {
        try {
            return (SelectionKey) SELECTION_KEY.get(channel);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Cannot inspect Forge test-client read interest", error);
        }
    }

    private static Field connectionField() {
        for (var candidate : ConnectScreen.class.getDeclaredFields()) {
            if (candidate.getType() == Connection.class) {
                candidate.setAccessible(true);
                return candidate;
            }
        }
        throw new IllegalStateException("Forge test-client connection field is unavailable");
    }

    private static Field field(Class<?> owner, String name) {
        try {
            var value = owner.getDeclaredField(name);
            value.setAccessible(true);
            return value;
        } catch (NoSuchFieldException error) {
            throw new IllegalStateException("Forge test-client read-interest field is unavailable", error);
        }
    }
}
