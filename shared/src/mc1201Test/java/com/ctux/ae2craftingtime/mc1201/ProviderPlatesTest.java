package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void platesRememberLocatedOutputs() {
        ProviderHighlightClient.show("minecraft:overworld", List.of(new BlockPos(1, 2, 3)), 15,
                "minecraft:iron_ingot");
        ProviderHighlightClient.show("minecraft:overworld", List.of(new BlockPos(4, 5, 6)), 15, "");
        ProviderHighlightClient.show("minecraft:overworld", List.of(new BlockPos(7, 8, 9)), 15, null);

        var plates = ProviderHighlightClient.plates();
        assertEquals(1, plates.size());
        assertEquals("minecraft:iron_ingot", plates.get(0).outputId());
        assertEquals(List.of(new BlockPos(1, 2, 3)), plates.get(0).positions());
    }

    @Test
    void prunePlatesDropsOutputsWithoutStall() {
        var iron = new ProfileKey("minecraft:iron_ingot");
        var copper = new ProfileKey("minecraft:copper_plate");
        ProviderHighlightClient.show("minecraft:overworld", List.of(new BlockPos(1, 2, 3)), 15,
                "minecraft:iron_ingot");
        ProviderHighlightClient.show("minecraft:overworld", List.of(new BlockPos(4, 5, 6)), 15,
                "minecraft:copper_plate");

        ClientStats.CACHE.replace(List.of(iron, copper),
                List.of(new StatsEntry(iron, stats(), Optional.empty(),
                        Optional.of(new StallDiagnostic(300, 20.0, 1, 0, 0)))));
        ProviderHighlightClient.prunePlates(List.of("minecraft:iron_ingot", "minecraft:copper_plate"));

        var plates = ProviderHighlightClient.plates();
        assertEquals(1, plates.size());
        assertEquals("minecraft:iron_ingot", plates.get(0).outputId());

        ProviderHighlightClient.prunePlates(null);
        ProviderHighlightClient.prunePlates(List.of());
        assertEquals(1, ProviderHighlightClient.plates().size());
    }

    private static ProfileStats stats() {
        return new ProfileStats(1, 20.0, 0.05, 1.0, 20, ProfileUnit.ITEM, false, 1, 4.0, List.of(20L),
                List.of(1L));
    }
}
