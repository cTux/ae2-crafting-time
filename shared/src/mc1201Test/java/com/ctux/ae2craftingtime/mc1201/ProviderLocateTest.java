package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.ProviderLocateRecords.StoredStart;
import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightCodec;
import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightCodec.Highlight;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ProviderLocateTest {
    private static boolean bootstrapped;

    @BeforeAll
    static void ensureBootstrapped() {
        // Building click/hover events touches item registries on some versions.
        try {
            net.minecraft.server.Bootstrap.bootStrap();
            bootstrapped = true;
        } catch (Throwable ignored) {
            bootstrapped = false;
        }
    }

    @Test
    void highlightRoundTrips() {
        List<List<BlockPos>> cases = List.of(List.of(),
                List.of(new BlockPos(1, 2, 3), new BlockPos(-4, 5, -6)));
        for (var positions : cases) {
            var highlight = new Highlight("minecraft:overworld", positions, 15);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            ProviderHighlightCodec.write(buffer, highlight);
            assertEquals(highlight, ProviderHighlightCodec.read(buffer));
            assertEquals(0, buffer.readableBytes());
        }
    }

    @Test
    void highlightTruncatesOversizePositionsOnWriteAndRejectsThemOnRead() {
        var many = new ArrayList<BlockPos>();
        for (var i = 0; i < PacketLimits.MAX_HIGHLIGHT_POSITIONS + 4; i++) {
            many.add(new BlockPos(i, 64, i));
        }
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        ProviderHighlightCodec.write(buffer,
                new Highlight("minecraft:overworld", many, 15));
        assertEquals(PacketLimits.MAX_HIGHLIGHT_POSITIONS,
                ProviderHighlightCodec.read(buffer).positions().size());

        for (var count : new int[] {-1, PacketLimits.MAX_HIGHLIGHT_POSITIONS + 1}) {
            var bad = new FriendlyByteBuf(Unpooled.buffer());
            bad.writeUtf("minecraft:overworld");
            bad.writeVarInt(count);
            assertThrows(IllegalArgumentException.class, () -> ProviderHighlightCodec.read(bad));
        }
    }

    @Test
    void startsRoundTripAndSkipInvalidEntries() {
        var key = new ProfileKey("net", "minecraft:iron_plate");
        var owner = UUID.randomUUID();
        var raw = List.of(new StoredStart(key, owner, List.of(new BlockPos(7, 64, 9)), "Iron Plate"),
                new StoredStart(new ProfileKey("net", "minecraft:copper_plate"), UUID.randomUUID(), List.of(),
                        "Copper Plate"));
        assertEquals(raw, PersistedProviderTag.readStarts(PersistedProviderTag.writeStarts(raw)));

        var tags = PersistedProviderTag.writeStarts(raw);
        var badKey = new CompoundTag();
        badKey.putString("networkId", "net");
        badKey.putString("key", "not an id");
        badKey.putString("owner", owner.toString());
        badKey.put("positions", new ListTag());
        badKey.putString("name", "Bad");
        tags.add(badKey);
        var badOwner = new CompoundTag();
        badOwner.putString("networkId", "net");
        badOwner.putString("key", "minecraft:stone");
        badOwner.putString("owner", "not-a-uuid");
        badOwner.put("positions", new ListTag());
        badOwner.putString("name", "Bad");
        tags.add(badOwner);
        var longName = new CompoundTag();
        longName.putString("networkId", "net");
        longName.putString("key", "minecraft:stone");
        longName.putString("owner", owner.toString());
        longName.put("positions", new ListTag());
        longName.putString("name", "x".repeat(600));
        tags.add(longName);
        tags.add(new CompoundTag());
        assertEquals(raw, PersistedProviderTag.readStarts(tags));
    }

    @Test
    void startsWriteSkipsNullsAndCapsPositions() {
        var tags = PersistedProviderTag.writeStarts(List.of(
                new StoredStart(new ProfileKey("net", "minecraft:iron_plate"), UUID.randomUUID(),
                        positions(PacketLimits.MAX_HIGHLIGHT_POSITIONS + 2), "Iron")));
        assertEquals(1, tags.size());
        var stored = PersistedProviderTag.readStarts(tags);
        assertEquals(1, stored.size());
        assertEquals(PacketLimits.MAX_HIGHLIGHT_POSITIONS, stored.get(0).positions().size());
        assertTrue(PersistedProviderTag.readStarts(new ListTag()).isEmpty());
    }

    @Test
    void delayedMessageStructureAndRedWord() {
        var message = DelayedChatText.delayedMessage("Basic Control Circuit", null, 640, 320.0);
        var contents = (TranslatableContents) message.getContents();
        assertEquals("text.ae2craftingtime.chat.delayed", contents.getKey());
        assertEquals(4, contents.getArgs().length);

        var name = (Component) contents.getArgs()[0];
        assertEquals("Basic Control Circuit", name.getString());
        assertFalse(name.getStyle().isUnderlined());
        assertNull(name.getStyle().getClickEvent());
        assertNull(name.getStyle().getHoverEvent());

        var word = (Component) contents.getArgs()[1];
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), word.getStyle().getColor());
        assertEquals("text.ae2craftingtime.chat.delayed.word",
                ((TranslatableContents) word.getContents()).getKey());
    }

    @Test
    void delayedMessageNameIsClickable() {
        assumeTrue(bootstrapped, "click events need MC registries");
        var message = DelayedChatText.delayedMessage("Basic Control Circuit", UUID.randomUUID(), 640, 320.0);
        var name = (Component) ((TranslatableContents) message.getContents()).getArgs()[0];
        assertEquals("Basic Control Circuit", name.getString());
        assertTrue(name.getStyle().isUnderlined());
        assertNotNull(name.getStyle().getClickEvent());
        assertNotNull(name.getStyle().getHoverEvent());
    }

    @Test
    void highlightPulseStaysInVisibleRange() {
        for (var i = 0; i < 5; i++) {
            var alpha = ProviderHighlightClient.pulseAlpha();
            assertTrue(alpha >= 0.35f && alpha <= 1.0f);
        }
    }

    private static List<BlockPos> positions(int count) {
        var positions = new ArrayList<BlockPos>();
        for (var i = 0; i < count; i++) {
            positions.add(new BlockPos(i, 64, -i));
        }
        return positions;
    }
}
