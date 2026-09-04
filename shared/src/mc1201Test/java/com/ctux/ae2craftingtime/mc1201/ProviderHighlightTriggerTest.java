package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.CraftProfiler;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.core.StuckEpisodeTracker;
import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightCodec;
import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightCodec.Highlight;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Auto pings remember the red plate only, manual locates refresh the rainbow
 * edge on top of the plate, and every end-of-life path drops both.
 */
class ProviderHighlightTriggerTest {
    private static final String DIMENSION = "minecraft:overworld";
    private static final String IRON = "minecraft:iron_ingot";

    @AfterEach
    void clearState() {
        ProviderHighlightClient.clearPlates();
        ClientStats.CACHE.clear();
    }

    @Test
    void autoPlateOnlyShowsPlateWithoutEdge() {
        ProviderHighlightClient.showPlate(DIMENSION, List.of(new BlockPos(1, 2, 3)), IRON);

        assertNull(ProviderHighlightClient.live());
        assertEquals(1, ProviderHighlightClient.plates().size());
        assertEquals(IRON, ProviderHighlightClient.plates().get(0).outputId());
    }

    @Test
    void plateOnlyFlagRoundTripsAndLegacyPacketsDefaultToFull() {
        var plateOnly = new Highlight(DIMENSION, List.of(new BlockPos(1, 2, 3)), IRON, 15, true);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        ProviderHighlightCodec.write(buffer, plateOnly);
        assertEquals(plateOnly, ProviderHighlightCodec.read(buffer));
        assertEquals(0, buffer.readableBytes());

        var legacy = new FriendlyByteBuf(Unpooled.buffer());
        legacy.writeUtf(DIMENSION);
        legacy.writeVarInt(1);
        legacy.writeBlockPos(new BlockPos(1, 2, 3));
        legacy.writeUtf(IRON);
        legacy.writeVarInt(15);
        assertFalse(ProviderHighlightCodec.read(legacy).plateOnly());
    }

    @Test
    void manualShowSetsEdgeAndPlate() {
        ProviderHighlightClient.show(DIMENSION, List.of(new BlockPos(1, 2, 3)), 15, IRON);

        assertNotNull(ProviderHighlightClient.live());
        assertEquals(1, ProviderHighlightClient.plates().size());
    }

    @Test
    void reShowPreservesPlateAndRefreshesEdge() {
        ProviderHighlightClient.showPlate(DIMENSION, List.of(new BlockPos(1, 2, 3)), IRON);
        assertNull(ProviderHighlightClient.live());

        ProviderHighlightClient.show(DIMENSION, List.of(new BlockPos(1, 2, 3)), 15, IRON);

        assertNotNull(ProviderHighlightClient.live());
        assertEquals(List.of(new BlockPos(1, 2, 3)), ProviderHighlightClient.live().positions());
        assertEquals(1, ProviderHighlightClient.plates().size());
        assertEquals(List.of(new BlockPos(1, 2, 3)), ProviderHighlightClient.plates().get(0).positions());
    }

    @Test
    void trimPositionsRemovesBoth() {
        ProviderHighlightClient.show(DIMENSION, List.of(new BlockPos(1, 2, 3)), 15, IRON);

        ProviderHighlightClient.trimPositions(DIMENSION, pos -> false);

        assertNull(ProviderHighlightClient.live());
        assertTrue(ProviderHighlightClient.plates().isEmpty());
    }

    @Test
    void clearForAndEmptyHighlightRemoveBoth() {
        ProviderHighlightClient.show(DIMENSION, List.of(new BlockPos(1, 2, 3)), 15, IRON);
        ProviderHighlightClient.clearFor(IRON);
        assertNull(ProviderHighlightClient.live());
        assertTrue(ProviderHighlightClient.plates().isEmpty());

        ProviderHighlightClient.show(DIMENSION, List.of(new BlockPos(1, 2, 3)), 15, IRON);
        ProviderHighlightClient.show(DIMENSION, List.of(), 0, IRON);
        assertNull(ProviderHighlightClient.live());
        assertTrue(ProviderHighlightClient.plates().isEmpty());
    }

    @Test
    void delayedToCraftingClearRemovesBoth() {
        var profiler = new CraftProfiler(10);
        var key = new ProfileKey(IRON);
        var cpu = new Object();
        seedTypical(profiler, key);
        profiler.start(key, cpu, 10, ProfileUnit.ITEM, 100);
        profiler.setJobOwner(cpu, UUID.randomUUID());

        assertEquals(1, profiler.pollNewlyDelayed(cpu, 800).size());
        assertTrue(profiler.pollResolvedDelayed(cpu).isEmpty());
        ProviderHighlightClient.showPlate(DIMENSION, List.of(new BlockPos(1, 2, 3)), IRON);
        assertNull(ProviderHighlightClient.live());
        assertEquals(1, ProviderHighlightClient.plates().size());

        // Partial progress resolves the stall while the craft still runs.
        profiler.complete(key, cpu, 1, 860);
        assertTrue(profiler.pollNewlyDelayed(cpu, 900).isEmpty());
        assertEquals(List.of(key), profiler.pollResolvedDelayed(cpu));
        assertTrue(profiler.pollResolvedDelayed(cpu).isEmpty());

        // The explicit server clear drops the plate and the edge alike.
        ProviderHighlightClient.clearFor(key.outputId());
        assertNull(ProviderHighlightClient.live());
        assertTrue(ProviderHighlightClient.plates().isEmpty());
    }

    @Test
    void resolvedDelayedIsNullSafe() {
        var profiler = new CraftProfiler(10);
        assertTrue(profiler.pollResolvedDelayed(null).isEmpty());
        assertTrue(profiler.pollResolvedDelayed(new Object()).isEmpty());
    }

    @Test
    void stuckEpisodeResolvedDrainsOnce() {
        var tracker = new StuckEpisodeTracker();
        var scope = new Object();
        var key = new ProfileKey(IRON);

        assertEquals(List.of(key), tracker.pollNewlyStuck(scope, Set.of(key)));
        assertTrue(tracker.pollNewlyStuck(scope, Set.of()).isEmpty());
        assertEquals(List.of(key), tracker.pollResolved(scope));
        assertTrue(tracker.pollResolved(scope).isEmpty());
        assertTrue(tracker.pollResolved(null).isEmpty());
    }

    private static void seedTypical(CraftProfiler profiler, ProfileKey key) {
        var seed = new Object();
        profiler.start(key, seed, 1, ProfileUnit.ITEM, 0);
        profiler.complete(key, seed, 1, 200);
    }
}
