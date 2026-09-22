package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.core.ProfileKey;
import appeng.api.stacks.AEItemKey;
import com.ctux.ae2craftingtime.mc1201.ProviderLocateRecords.StoredStart;
import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightCodec;
import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightCodec.Highlight;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Items;
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
        // Item lookup needs registries; constructing click/hover components does not.
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
            var highlight = new Highlight("minecraft:overworld", positions, "minecraft:iron_ingot", 15);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            ProviderHighlightCodec.write(buffer, highlight);
            assertEquals(highlight, ProviderHighlightCodec.read(buffer));
            assertEquals(0, buffer.readableBytes());
        }
    }

    @Test
    void highlightRoundTripsBlankOutputIdAsEmpty() {
        var highlight = new Highlight("minecraft:overworld", List.of(new BlockPos(1, 2, 3)), null, 15);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        ProviderHighlightCodec.write(buffer, highlight);
        var read = ProviderHighlightCodec.read(buffer);
        assertEquals("", read.outputId());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void highlightRejectsOversizeOutputId() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        assertThrows(IllegalArgumentException.class,
                () -> ProviderHighlightCodec.write(buffer,
                        new Highlight("minecraft:overworld", List.of(), "x".repeat(129), 15)));
    }

    @Test
    void highlightTruncatesOversizePositionsOnWriteAndRejectsThemOnRead() {
        var many = new ArrayList<BlockPos>();
        for (var i = 0; i < PacketLimits.MAX_HIGHLIGHT_POSITIONS + 4; i++) {
            many.add(new BlockPos(i, 64, i));
        }
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        ProviderHighlightCodec.write(buffer,
                new Highlight("minecraft:overworld", many, "minecraft:iron_ingot", 15));
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
    void highlightingMessageStructureAndCoords() {
        var message = DelayedChatText.highlightingMessage(Component.literal("Pattern Provider"),
                List.of(new BlockPos(1, 2, 3), new BlockPos(-4, 5, -6)), "minecraft:overworld");
        var contents = (TranslatableContents) message.getContents();
        assertEquals("text.ae2craftingtime.chat.highlighting", contents.getKey());
        assertEquals(3, contents.getArgs().length);
        assertEquals("Pattern Provider", ((Component) contents.getArgs()[0]).getString());
        assertEquals("(1, 2, 3), (-4, 5, -6)", ((Component) contents.getArgs()[1]).getString());
        assertEquals("minecraft:overworld", contents.getArgs()[2]);
    }

    @Test
    void highlightingMessageCoordsTeleportOnClick() {
        var message = DelayedChatText.highlightingMessage(Component.literal("Pattern Provider"),
                List.of(new BlockPos(1, 2, 3), new BlockPos(-4, 5, -6)), "minecraft:overworld");
        var coords = (Component) ((TranslatableContents) message.getContents()).getArgs()[1];
        var clickable = new ArrayList<Component>();
        for (var sibling : coords.getSiblings()) {
            if (sibling.getStyle().getClickEvent() != null) {
                clickable.add(sibling);
            }
        }
        assertEquals(2, clickable.size());
        assertEquals("(1, 2, 3)", clickable.get(0).getString());
        assertTrue(clickable.get(0).getStyle().isUnderlined());
        assertTrue(clickable.get(0).getStyle().getClickEvent().toString().contains("/tp @s 1 2 3"));
        assertNotNull(clickable.get(0).getStyle().getHoverEvent());
        assertEquals("(-4, 5, -6)", clickable.get(1).getString());
        assertTrue(clickable.get(1).getStyle().getClickEvent().toString().contains("/tp @s -4 5 -6"));
    }

    @Test
    void highlightingMessageToleratesMissingParts() {
        var message = DelayedChatText.highlightingMessage(null, null, null);
        var contents = (TranslatableContents) message.getContents();
        assertEquals("text.ae2craftingtime.chat.highlighting", contents.getKey());
        assertEquals("", ((Component) contents.getArgs()[0]).getString());
        assertEquals("", ((Component) contents.getArgs()[1]).getString());
        assertEquals("", contents.getArgs()[2]);
    }

    @Test
    void delayedMessageNameIsClickable() {
        var message = DelayedChatText.delayedMessage("Basic Control Circuit", UUID.randomUUID(), 640, 320.0);
        var name = (Component) ((TranslatableContents) message.getContents()).getArgs()[0];
        assertEquals("Basic Control Circuit", name.getString());
        assertTrue(name.getStyle().isUnderlined());
        assertNotNull(name.getStyle().getClickEvent());
        assertNotNull(name.getStyle().getHoverEvent());
    }

    @Test
    void blockedMessageStructureAndRedWord() {
        var message = DelayedChatText.blockedMessage("Basic Control Circuit", null,
                "text.ae2craftingtime.chat.no_power.word",
                Component.translatable("text.ae2craftingtime.no_power.explanation"));
        var contents = (TranslatableContents) message.getContents();
        assertEquals("text.ae2craftingtime.chat.blocked", contents.getKey());
        assertEquals(3, contents.getArgs().length);

        var name = (Component) contents.getArgs()[0];
        assertEquals("Basic Control Circuit", name.getString());
        assertFalse(name.getStyle().isUnderlined());
        assertNull(name.getStyle().getClickEvent());

        var word = (Component) contents.getArgs()[1];
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), word.getStyle().getColor());
        assertEquals("text.ae2craftingtime.chat.no_power.word",
                ((TranslatableContents) word.getContents()).getKey());
    }

    @Test
    void blockedMessageNameIsClickable() {
        var message = DelayedChatText.blockedMessage("Basic Control Circuit", UUID.randomUUID(),
                "text.ae2craftingtime.chat.no_space.word",
                Component.translatable("text.ae2craftingtime.no_space.explanation"));
        var name = (Component) ((TranslatableContents) message.getContents()).getArgs()[0];
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

    @Test
    void highlightRainbowStaysInVisibleRangeAndCycles() {
        var seen = new ArrayList<String>();
        for (var time : new long[] {0L, 500L, 1000L, 1500L, 2000L, 2500L}) {
            var rainbow = ProviderHighlightClient.rainbowRgb(time);
            assertEquals(3, rainbow.length);
            for (var channel : rainbow) {
                assertTrue(channel >= 0.0f && channel <= 1.0f);
            }
            seen.add(rainbow[0] + "," + rainbow[1] + "," + rainbow[2]);
        }
        assertTrue(seen.stream().distinct().count() > 1);
        var live = ProviderHighlightClient.rainbowRgb();
        assertEquals(3, live.length);
        for (var channel : live) {
            assertTrue(channel >= 0.0f && channel <= 1.0f);
        }
    }

    @Test
    void visibleFacesPointTowardCamera() {
        var pos = new BlockPos(0, 0, 0);
        assertEquals(List.of(Direction.UP), ProviderFaceIcons.visibleFaces(pos, 0.5, 10.0, 0.5));
        assertEquals(List.of(Direction.DOWN), ProviderFaceIcons.visibleFaces(pos, 0.5, -10.0, 0.5));
        assertEquals(List.of(Direction.NORTH), ProviderFaceIcons.visibleFaces(pos, 0.5, 0.5, -10.0));
        assertEquals(List.of(Direction.SOUTH), ProviderFaceIcons.visibleFaces(pos, 0.5, 0.5, 10.0));
        assertEquals(List.of(Direction.WEST), ProviderFaceIcons.visibleFaces(pos, -10.0, 0.5, 0.5));
        assertEquals(List.of(Direction.EAST), ProviderFaceIcons.visibleFaces(pos, 10.0, 0.5, 0.5));
        var corner = ProviderFaceIcons.visibleFaces(pos, 10.0, 10.0, 10.0);
        assertEquals(3, corner.size());
        assertTrue(corner.contains(Direction.UP));
        assertTrue(corner.contains(Direction.SOUTH));
        assertTrue(corner.contains(Direction.EAST));
    }

    @Test
    void resolveItemRequiresTypedItem() {
        assertTrue(ProviderHighlightShapes.resolveItem(null).isEmpty());
        assumeTrue(bootstrapped, "item registry needs MC registries");
        assertTrue(ProviderHighlightShapes.resolveItem(appeng.api.stacks.AEFluidKey.of(
                net.minecraft.world.level.material.Fluids.WATER)).isEmpty());
    }

    @Test
    void typedHighlightAndPersistedStartRoundTripWithoutGuessingFromOutputId() {
        assumeTrue(bootstrapped);
        var displayKey = AEItemKey.of(Items.STONE);
        var highlight = new Highlight("net", "minecraft:overworld", List.of(new BlockPos(1, 2, 3)),
                "custom:opaque_output", 15, true, displayKey);
        var buffer = ProviderDisplayKeyTestContext.buffer();
        ProviderHighlightCodec.write(buffer, highlight);
        assertEquals(highlight, ProviderHighlightCodec.read(buffer));

        var start = new StoredStart(new ProfileKey("net", "custom:opaque_output"), UUID.randomUUID(),
                "minecraft:overworld", List.of(new BlockPos(1, 2, 3)), "Opaque", displayKey);
        var tag = PersistedProviderTag.writeStarts(List.of(start), ProviderDisplayKeyTestContext.persistence());
        assertEquals(List.of(start), PersistedProviderTag.readStarts(tag, ProviderDisplayKeyTestContext.persistence()));
        ((CompoundTag) tag.get(0)).remove("displayKey");
        assertNull(PersistedProviderTag.readStarts(tag, ProviderDisplayKeyTestContext.persistence()).get(0).displayKey());
        ((CompoundTag) tag.get(0)).put("displayKey", new CompoundTag());
        assertNull(PersistedProviderTag.readStarts(tag, ProviderDisplayKeyTestContext.persistence()).get(0).displayKey());
    }

    @Test
    void highlightRejectsOversizeKeyAndKeepsPlateForUnknownType() {
        var oversized = new FriendlyByteBuf(Unpooled.buffer());
        ProviderHighlightCodec.write(oversized, new Highlight("net", "minecraft:overworld", List.of(),
                "minecraft:stone", 15, true, null));
        oversized.writeZero(16 * 1024);
        assertThrows(IllegalArgumentException.class, () -> ProviderHighlightCodec.read(oversized));

        var unknown = new FriendlyByteBuf(Unpooled.buffer());
        ProviderHighlightCodec.write(unknown, new Highlight("net", "minecraft:overworld", List.of(),
                "minecraft:stone", 15, true, null));
        unknown.writerIndex(unknown.writerIndex() - 1);
        unknown.writeBoolean(true);
        unknown.writeVarInt(Integer.MAX_VALUE);
        var decoded = ProviderHighlightCodec.read(unknown);
        assertEquals("minecraft:stone", decoded.outputId());
        assertNull(decoded.displayKey());

        var truncated = new FriendlyByteBuf(Unpooled.buffer());
        ProviderHighlightCodec.write(truncated, new Highlight("net", "minecraft:overworld", List.of(),
                "minecraft:stone", 15, true, null));
        truncated.setBoolean(truncated.writerIndex() - 1, true);
        assertThrows(Exception.class, () -> ProviderHighlightCodec.read(truncated));

        var trailing = new FriendlyByteBuf(Unpooled.buffer());
        ProviderHighlightCodec.write(trailing, new Highlight("net", "minecraft:overworld", List.of(),
                "minecraft:stone", 15, true, null));
        trailing.writeByte(0);
        assertThrows(IllegalArgumentException.class, () -> ProviderHighlightCodec.read(trailing));
    }

    @Test
    void typedFluidSurvivesTransportTrimmingWinnerPromotionAndSnapshot() {
        assumeTrue(bootstrapped);
        var fluid = appeng.api.stacks.AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER);
        var item = AEItemKey.of(Items.STONE);
        var kept = new BlockPos(1, 2, 3);
        var removed = new BlockPos(4, 5, 6);
        var packet = new Highlight("net", "minecraft:overworld", List.of(kept, removed),
                "minecraft:water", 15, true, fluid);
        var buffer = ProviderDisplayKeyTestContext.buffer();
        try {
            ProviderHighlightCodec.write(buffer, packet);
            assertEquals(packet, ProviderHighlightCodec.read(buffer));
        } finally {
            buffer.release();
        }
        ProviderHighlightClient.clearPlates();
        ProviderLocateRecords.clearAll();
        try {
            ProviderHighlightClient.showPlate("net", "minecraft:overworld", List.of(kept, removed),
                    "minecraft:stone", item);
            ProviderHighlightClient.showPlate("net", "minecraft:overworld", List.of(kept, removed),
                    "minecraft:water", fluid);
            assertEquals(item, ProviderHighlightClient.renderPlates().get(0).displayKey());
            ProviderHighlightClient.trimPositions("minecraft:overworld", kept::equals);
            ProviderHighlightClient.clearFor("net", "minecraft:stone");
            assertEquals(fluid, ProviderHighlightClient.renderPlates().get(0).displayKey());
            var key = new ProfileKey("net", "minecraft:water");
            ProviderLocateRecords.replaceStart(key, UUID.randomUUID(), "minecraft:overworld", List.of(kept), "Water", fluid);
            var snapshot = ProviderLocateRecords.snapshotStarts();
            ProviderLocateRecords.clearAll();
            ProviderLocateRecords.restoreStarts(snapshot);
            assertEquals(fluid, ProviderLocateRecords.startFor(key).orElseThrow().displayKey());
        } finally {
            ProviderHighlightClient.clearPlates();
            ProviderLocateRecords.clearAll();
        }
    }

    @Test
    void liveOutputReplacesSavedFallbackAndExplicitReplacementCanClearIt() {
        assumeTrue(bootstrapped);
        var scope = new Object();
        var fluid = appeng.api.stacks.AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER);
        var item = AEItemKey.of(Items.WATER_BUCKET);
        var key = new ProfileKey("net", fluid.getId().toString());
        ProviderLocateRecords.replaceStart(key, UUID.randomUUID(), "minecraft:overworld",
                List.of(new BlockPos(1, 2, 3)), "Water", item);
        try {
            assertEquals(item, ProfilerBridge.displayKey(scope, key));
            var outputs = new appeng.api.stacks.GenericStack[] { new appeng.api.stacks.GenericStack(fluid, 1) };
            var pattern = (appeng.api.crafting.IPatternDetails) java.lang.reflect.Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[] { appeng.api.crafting.IPatternDetails.class },
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getOutputs" -> outputs;
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            ProviderStartTracker.noteDispatch(scope, pattern, java.util.Map.of(key, 1L));
            assertEquals(fluid, ProfilerBridge.displayKey(scope, key));
            ProviderLocateRecords.replaceStart(key, UUID.randomUUID(), "minecraft:overworld",
                    List.of(new BlockPos(1, 2, 3)), "Water", null);
            assertNull(ProviderLocateRecords.startFor(key).orElseThrow().displayKey());
        } finally {
            ProviderStartTracker.clear(scope);
            ProviderLocateRecords.clearAll();
        }
    }

    @Test
    void resolveItemKeepsItemAppearance() {
        assumeTrue(bootstrapped, "item registry needs MC registries");
        assertTrue(ProviderHighlightShapes.resolveItem(appeng.api.stacks.AEItemKey.of(
                net.minecraft.world.item.Items.STONE)).is(net.minecraft.world.item.Items.STONE));
    }

    private static List<BlockPos> positions(int count) {
        var positions = new ArrayList<BlockPos>();
        for (var i = 0; i < count; i++) {
            positions.add(new BlockPos(i, 64, -i));
        }
        return positions;
    }
}
