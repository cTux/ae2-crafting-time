package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.core.StallDiagnostic;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.mc1201.net.ProviderLocateC2S;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProviderPlatesTest {
    @AfterEach
    void clearPlates() {
        ProviderHighlightClient.clearPlates();
        ClientStats.CACHE.clear();
    }

    @Test
    void locatePacketRoundTrips() {
        var packet = new ProviderLocateC2S("minecraft:iron_ingot");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        ProviderLocateC2S.encode(packet, buffer);
        assertEquals(packet, ProviderLocateC2S.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void locatePacketRejectsInvalidIds() {
        assertThrows(IllegalArgumentException.class, () -> new ProviderLocateC2S("not an id"));
        assertThrows(IllegalArgumentException.class, () -> new ProviderLocateC2S("x".repeat(129)));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("x".repeat(200));
        assertThrows(Exception.class, () -> ProviderLocateC2S.decode(buffer));
    }

    @Test
    void platesRememberAutoOutputsOnly() {
        ProviderHighlightClient.showPlate("minecraft:overworld", List.of(new BlockPos(1, 2, 3)),
                "minecraft:iron_ingot");
        ProviderHighlightClient.showPlate("minecraft:overworld", List.of(new BlockPos(4, 5, 6)), "");
        ProviderHighlightClient.showPlate("minecraft:overworld", List.of(new BlockPos(7, 8, 9)), null);
        ProviderHighlightClient.show("minecraft:overworld", List.of(new BlockPos(9, 9, 9)), 15,
                "minecraft:manual_only");

        var plates = ProviderHighlightClient.plates();
        assertEquals(1, plates.size());
        assertEquals("minecraft:iron_ingot", plates.get(0).outputId());
        assertEquals(List.of(new BlockPos(1, 2, 3)), plates.get(0).positions());
    }

    @Test
    void prunePlatesKeepsServerAuthoritativePlates() {
        var iron = new ProfileKey("minecraft:iron_ingot");
        var copper = new ProfileKey("minecraft:copper_plate");
        ProviderHighlightClient.showPlate("minecraft:overworld", List.of(new BlockPos(1, 2, 3)),
                "minecraft:iron_ingot");
        ProviderHighlightClient.showPlate("minecraft:overworld", List.of(new BlockPos(4, 5, 6)),
                "minecraft:copper_plate");

        // A healthy snapshot from another CPU must not remove still-delayed plates.
        ClientStats.CACHE.replace(List.of(iron, copper),
                List.of(new StatsEntry(iron, stats(), Optional.empty(),
                        Optional.of(new StallDiagnostic(300, 20.0, 1, 0, 0)))));
        ProviderHighlightClient.prunePlates(List.of("minecraft:iron_ingot", "minecraft:copper_plate"));

        var plates = ProviderHighlightClient.plates();
        assertEquals(2, plates.size());

        ProviderHighlightClient.prunePlates(null);
        ProviderHighlightClient.prunePlates(List.of());
        assertEquals(2, ProviderHighlightClient.plates().size());
    }

    @Test
    void plateGateShowsUnknownOutputsWithoutOpenScreen() {
        assertTrue(ProviderHighlightClient.shouldShowPlates("minecraft:iron_ingot"));
    }

    @Test
    void plateGateShowsStalledOutputs() {
        var iron = new ProfileKey("minecraft:iron_ingot");
        ClientStats.CACHE.replace(List.of(iron),
                List.of(new StatsEntry(iron, stats(), Optional.empty(),
                        Optional.of(new StallDiagnostic(300, 20.0, 1, 0, 0)))));
        assertTrue(ProviderHighlightClient.shouldShowPlates("minecraft:iron_ingot"));
    }

    @Test
    void plateGateShowsDespiteHealthySnapshot() {
        var iron = new ProfileKey("minecraft:iron_ingot");
        ClientStats.CACHE.replace(List.of(iron),
                List.of(new StatsEntry(iron, stats(), Optional.empty(), Optional.empty())));
        assertTrue(ProviderHighlightClient.shouldShowPlates("minecraft:iron_ingot"));
    }

    @Test
    void plateGateRejectsBlankOutputIds() {
        assertFalse(ProviderHighlightClient.shouldShowPlates(null));
        assertFalse(ProviderHighlightClient.shouldShowPlates(""));
        assertFalse(ProviderHighlightClient.shouldShowPlates("   "));
    }

    @Test
    void expiredEdgeClearsButPlatePersists() {
        ProviderHighlightClient.showPlate("minecraft:overworld", List.of(new BlockPos(1, 2, 3)),
                "minecraft:iron_ingot");
        ProviderHighlightClient.show("minecraft:overworld", List.of(new BlockPos(1, 2, 3)), 15,
                "minecraft:iron_ingot");
        var shownAt = System.currentTimeMillis();
        assertNotNull(ProviderHighlightClient.liveAt(shownAt));
        assertNull(ProviderHighlightClient.liveAt(shownAt + 16_000L));
        assertEquals(1, ProviderHighlightClient.plates().size());
        assertTrue(ProviderHighlightClient.shouldShowPlates("minecraft:iron_ingot"));
    }

    @Test
    void clearForRemovesPlateOnlyKeepsEdge() {
        ProviderHighlightClient.showPlate("minecraft:overworld", List.of(new BlockPos(1, 2, 3)),
                "minecraft:iron_ingot");
        ProviderHighlightClient.showPlate("minecraft:overworld", List.of(new BlockPos(4, 5, 6)),
                "minecraft:copper_plate");
        ProviderHighlightClient.show("minecraft:overworld", List.of(new BlockPos(1, 2, 3)), 15,
                "minecraft:iron_ingot");
        ProviderHighlightClient.clearFor("minecraft:iron_ingot");
        assertEquals(1, ProviderHighlightClient.plates().size());
        assertNotNull(ProviderHighlightClient.live());
        ProviderHighlightClient.clearFor("minecraft:copper_plate");
        assertTrue(ProviderHighlightClient.plates().isEmpty());
        assertNotNull(ProviderHighlightClient.live());
        ProviderHighlightClient.clearEdgeFor("minecraft:iron_ingot");
        assertNull(ProviderHighlightClient.live());
        ProviderHighlightClient.clearFor(null);
        ProviderHighlightClient.clearFor("   ");
        ProviderHighlightClient.clearEdgeFor(null);
    }

    @Test
    void serverFinishClearsPlateKeepsRainbow() {
        ProviderHighlightClient.showPlate("minecraft:overworld", List.of(new BlockPos(1, 2, 3)),
                "minecraft:iron_ingot");
        ProviderHighlightClient.show("minecraft:overworld", List.of(new BlockPos(1, 2, 3)), 15,
                "minecraft:iron_ingot");
        ProviderHighlightClient.clearFor("minecraft:iron_ingot");
        assertTrue(ProviderHighlightClient.plates().isEmpty());
        assertNotNull(ProviderHighlightClient.live());
    }

    @Test
    void trimPositionsDropsBrokenBlocks() {
        var kept = new BlockPos(1, 2, 3);
        var broken = new BlockPos(4, 5, 6);
        ProviderHighlightClient.showPlate("minecraft:overworld", List.of(kept, broken),
                "minecraft:iron_ingot");
        ProviderHighlightClient.show("minecraft:overworld", List.of(kept, broken), 15,
                "minecraft:iron_ingot");
        ProviderHighlightClient.trimPositions("minecraft:overworld", pos -> !pos.equals(broken));
        assertEquals(List.of(kept), ProviderHighlightClient.live().positions());
        assertEquals(List.of(kept), ProviderHighlightClient.plates().get(0).positions());
        ProviderHighlightClient.trimPositions("minecraft:the_nether", pos -> false);
        assertNotNull(ProviderHighlightClient.live());
        assertEquals(1, ProviderHighlightClient.plates().size());
        ProviderHighlightClient.trimPositions("minecraft:overworld", pos -> false);
        assertNull(ProviderHighlightClient.live());
        assertTrue(ProviderHighlightClient.plates().isEmpty());
        ProviderHighlightClient.trimPositions(null, pos -> true);
        ProviderHighlightClient.trimPositions("minecraft:overworld", null);
    }

    private static ProfileStats stats() {
        return new ProfileStats(1, 20.0, 0.05, 1.0, 20, ProfileUnit.ITEM, false, 1, 4.0, List.of(20L),
                List.of(1L));
    }
}
