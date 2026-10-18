package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CraftingJobEstimateCodecTest {
    private static final UUID JOB = UUID.fromString("a49302d3-6630-493a-932e-41cf90a594cf");
    private static final ProfileKey ROOT = new ProfileKey("network", "minecraft:stone");
    private static final ProfileKey CHILD = new ProfileKey("network", "minecraft:glass");

    @Test
    void roundTripRetainsGraphProgressAndPartialReliability() {
        var graph = new CraftingJobEstimate(ROOT, Map.of(ROOT, 8L, CHILD, 4L),
                new CraftingJobEstimate.DependencyGraph(Map.of(ROOT, Set.of(CHILD)), false));
        graph.complete(CHILD, 2);
        var bytes = CraftingJobEstimateCodec.encode(JOB, graph).orElseThrow();
        var decoded = CraftingJobEstimateCodec.decode(bytes).orElseThrow();
        assertEquals(JOB, decoded.jobId());
        assertEquals(graph.snapshot().orElseThrow(), decoded.graph());
        assertFalse(decoded.graph().complete());
        assertEquals(2L, decoded.graph().remainingAmounts().get(CHILD));
        assertEquals(graph.remainingEstimate((key, amount) -> java.util.Optional.of(new TtcEstimate(amount, true))),
                CraftingJobEstimate.restore(decoded.graph()).remainingEstimate(
                        (key, amount) -> java.util.Optional.of(new TtcEstimate(amount, true))));
        assertEquals(decoded.graph(), CraftingJobEstimateCodec.decode(
                CraftingJobEstimateCodec.encode(decoded).orElseThrow()).orElseThrow().graph());
    }

    @Test
    void preservesZeroAndCyclesWithoutInventingTime() {
        var graph = new CraftingJobEstimate(ROOT, Map.of(ROOT, 0L, CHILD, 0L),
                Map.of(ROOT, Set.of(CHILD), CHILD, Set.of(ROOT)));
        var restored = CraftingJobEstimate.restore(CraftingJobEstimateCodec.decode(
                CraftingJobEstimateCodec.encode(JOB, graph).orElseThrow()).orElseThrow().graph());
        assertTrue(restored.remainingEstimate((key, amount) -> java.util.Optional.of(new TtcEstimate(1, true)))
                .isEmpty());
        assertEquals(graph.snapshot().orElseThrow(), restored.snapshot().orElseThrow());
    }

    @Test
    void rejectsUnsupportedMalformedAndTruncatedPayloads() {
        var bytes = CraftingJobEstimateCodec.encode(JOB,
                new CraftingJobEstimate(ROOT, Map.of(ROOT, 1L, CHILD, 2L), Map.of(ROOT, Set.of(CHILD))))
                .orElseThrow();
        assertTrue(CraftingJobEstimateCodec.decode(null).isEmpty());
        assertTrue(CraftingJobEstimateCodec.decode(new byte[CraftingJobEstimateCodec.MAX_BYTES + 1]).isEmpty());
        for (int length = 0; length < bytes.length; length++)
            assertTrue(CraftingJobEstimateCodec.decode(Arrays.copyOf(bytes, length)).isEmpty(), "length " + length);
        var trailing = Arrays.copyOf(bytes, bytes.length + 1);
        assertTrue(CraftingJobEstimateCodec.decode(trailing).isEmpty());
        var version = bytes.clone();
        version[0] = 2;
        assertTrue(CraftingJobEstimateCodec.decode(version).isEmpty());
        var zeroJob = bytes.clone();
        Arrays.fill(zeroJob, 1, 17, (byte) 0);
        assertTrue(CraftingJobEstimateCodec.decode(zeroJob).isEmpty());
        var networkLength = bytes.clone();
        ByteBuffer.wrap(networkLength).putInt(17, 4097);
        assertTrue(CraftingJobEstimateCodec.decode(networkLength).isEmpty());
        ByteBuffer.wrap(networkLength).putInt(17, -1);
        assertTrue(CraftingJobEstimateCodec.decode(networkLength).isEmpty());
        ByteBuffer.wrap(networkLength).putInt(17, 4096);
        assertTrue(CraftingJobEstimateCodec.decode(networkLength).isEmpty());
        var completeness = bytes.clone();
        completeness[21 + "network".length()] = 2;
        assertTrue(CraftingJobEstimateCodec.decode(completeness).isEmpty());
        var count = bytes.clone();
        ByteBuffer.wrap(count).putInt(22 + "network".length(), 4097);
        assertTrue(CraftingJobEstimateCodec.decode(count).isEmpty());
        ByteBuffer.wrap(count).putInt(22 + "network".length(), 0);
        assertTrue(CraftingJobEstimateCodec.decode(count).isEmpty());
        ByteBuffer.wrap(count).putInt(22 + "network".length(), -1);
        assertTrue(CraftingJobEstimateCodec.decode(count).isEmpty());
        ByteBuffer.wrap(count).putInt(22 + "network".length(), 4096);
        assertTrue(CraftingJobEstimateCodec.decode(count).isEmpty());
        var rootIndex = bytes.clone();
        ByteBuffer.wrap(rootIndex).putInt(26 + "network".length(), 2);
        assertTrue(CraftingJobEstimateCodec.decode(rootIndex).isEmpty());
        ByteBuffer.wrap(rootIndex).putInt(26 + "network".length(), -1);
        assertTrue(CraftingJobEstimateCodec.decode(rootIndex).isEmpty());
        var invalidUtf8 = bytes.clone();
        invalidUtf8[21] = (byte) 0xff;
        assertTrue(CraftingJobEstimateCodec.decode(invalidUtf8).isEmpty());
    }

    @Test
    void rejectsInvalidNodesEdgesAndOverLimitSources() {
        var valid = CraftingJobEstimateCodec.encode(JOB,
                new CraftingJobEstimate(ROOT, Map.of(ROOT, 1L), Map.of(ROOT, Set.of(ROOT)))).orElseThrow();
        var nodeOffset = 30 + "network".length();
        var invalidOutput = valid.clone();
        invalidOutput[nodeOffset + 4] = (byte) '!';
        assertTrue(CraftingJobEstimateCodec.decode(invalidOutput).isEmpty());
        var negative = valid.clone();
        var firstOutputLength = ByteBuffer.wrap(valid).getInt(nodeOffset);
        var badOutputLength = valid.clone();
        ByteBuffer.wrap(badOutputLength).putInt(nodeOffset, 129);
        assertTrue(CraftingJobEstimateCodec.decode(badOutputLength).isEmpty());
        ByteBuffer.wrap(badOutputLength).putInt(nodeOffset, -1);
        assertTrue(CraftingJobEstimateCodec.decode(badOutputLength).isEmpty());
        var badOutputUtf8 = valid.clone();
        badOutputUtf8[nodeOffset + 4] = (byte) 0xff;
        assertTrue(CraftingJobEstimateCodec.decode(badOutputUtf8).isEmpty());
        ByteBuffer.wrap(negative).putLong(nodeOffset + 4 + firstOutputLength, -1);
        assertTrue(CraftingJobEstimateCodec.decode(negative).isEmpty());
        var edgePosition = nodeOffset + 4 + firstOutputLength + 8 + 4;
        var badIndex = valid.clone();
        ByteBuffer.wrap(badIndex).putInt(edgePosition, 99);
        assertTrue(CraftingJobEstimateCodec.decode(badIndex).isEmpty());
        ByteBuffer.wrap(badIndex).putInt(edgePosition, -1);
        assertTrue(CraftingJobEstimateCodec.decode(badIndex).isEmpty());
        var badEdgeCount = valid.clone();
        ByteBuffer.wrap(badEdgeCount).putInt(edgePosition - 4, 16_385);
        assertTrue(CraftingJobEstimateCodec.decode(badEdgeCount).isEmpty());
        ByteBuffer.wrap(badEdgeCount).putInt(edgePosition - 4, -1);
        assertTrue(CraftingJobEstimateCodec.decode(badEdgeCount).isEmpty());
        ByteBuffer.wrap(badEdgeCount).putInt(edgePosition - 4, 2);
        assertTrue(CraftingJobEstimateCodec.decode(badEdgeCount).isEmpty());
        var repeatedEdge = Arrays.copyOf(valid, valid.length + 4);
        ByteBuffer.wrap(repeatedEdge).putInt(edgePosition - 4, 2).putInt(edgePosition + 4, 0);
        assertTrue(CraftingJobEstimateCodec.decode(repeatedEdge).isEmpty());
        var twoNodes = CraftingJobEstimateCodec.encode(JOB,
                new CraftingJobEstimate(ROOT, Map.of(ROOT, 1L, CHILD, 1L), Map.of())).orElseThrow();
        var duplicate = twoNodes.clone();
        var firstLength = ByteBuffer.wrap(duplicate).getInt(nodeOffset);
        var secondNode = nodeOffset + 4 + firstLength + 8 + 4;
        assertEquals(firstLength, ByteBuffer.wrap(duplicate).getInt(secondNode));
        System.arraycopy(duplicate, nodeOffset + 4, duplicate, secondNode + 4, firstLength);
        assertTrue(CraftingJobEstimateCodec.decode(duplicate).isEmpty());
        var many = new HashMap<ProfileKey, Long>();
        for (int i = 0; i <= CraftingJobEstimateCodec.MAX_NODES; i++)
            many.put(new ProfileKey("network", "test:item" + i), 1L);
        var oversized = new CraftingJobEstimate(ROOT, many, Map.of());
        assertTrue(oversized.snapshot().isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode(JOB, oversized).isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode(new UUID(0, 0),
                new CraftingJobEstimate(ROOT, Map.of(ROOT, 1L), Map.of())).isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode(JOB,
                new CraftingJobEstimate(new ProfileKey("other", "minecraft:stone"),
                        Map.of(ROOT, 1L), Map.of())).isEmpty());
    }

    @Test
    void encoderBoundsNetworkStringsBeforeUtf8AllocationAndDoesNotAlterLiveGraph() {
        var maximum = new ProfileKey("a".repeat(4096), ROOT.outputId());
        var boundary = new CraftingJobEstimate(maximum, Map.of(maximum, 1L), Map.of());
        assertTrue(CraftingJobEstimateCodec.decode(CraftingJobEstimateCodec.encode(JOB, boundary)
                .orElseThrow()).isPresent());
        for (var network : new String[] {"a".repeat(4097), "é".repeat(2049)}) {
            var key = new ProfileKey(network, ROOT.outputId());
            var graph = new CraftingJobEstimate(key, Map.of(key, 4L), Map.of());
            assertTrue(CraftingJobEstimateCodec.encode(JOB, graph).isEmpty());
            assertEquals(4L, graph.snapshot().orElseThrow().remainingAmounts().get(key));
        }
        var longOutput = new ProfileKey("network", "test:" + "a".repeat(124));
        assertTrue(CraftingJobEstimateCodec.encode(JOB,
                new CraftingJobEstimate(longOutput, Map.of(longOutput, 1L), Map.of())).isEmpty());
    }

    @Test
    void graphSnapshotRefusesOversizedUnionEdgesAndInvalidAmountsBeforeCopying() {
        var amounts = new HashMap<ProfileKey, Long>();
        amounts.put(ROOT, 1L);
        for (int i = 0; i < CraftingJobEstimateCodec.MAX_NODES - 1; i++)
            amounts.put(new ProfileKey("network", "test:item" + i), 1L);
        var extra = new ProfileKey("network", "test:extra");
        assertTrue(new CraftingJobEstimate(ROOT, amounts, Map.of(ROOT, Set.of(extra))).snapshot().isEmpty());
        var manyEdges = new java.util.HashSet<ProfileKey>();
        for (int i = 0; i <= CraftingJobEstimateCodec.MAX_EDGES; i++)
            manyEdges.add(new ProfileKey("network", "test:edge" + i));
        assertTrue(new CraftingJobEstimate(ROOT, Map.of(ROOT, 1L), Map.of(ROOT, manyEdges))
                .snapshot().isEmpty());
        var negative = new HashMap<ProfileKey, Long>();
        negative.put(ROOT, -1L);
        assertTrue(new CraftingJobEstimate(ROOT, negative, Map.of()).snapshot().isEmpty());
        negative.put(ROOT, null);
        assertTrue(new CraftingJobEstimate(ROOT, negative, Map.of()).snapshot().isEmpty());
        assertTrue(new CraftingJobEstimate(null, Map.of(), Map.of()).snapshot().isEmpty());
    }

    @Test
    void directSnapshotEncodingChecksNullsAndAllCollectionBounds() {
        var graph = new CraftingJobEstimate.Snapshot(ROOT, Map.of(), Map.of(), true);
        assertTrue(CraftingJobEstimateCodec.encode(new CraftingJobEstimateCodec.Saved(null, graph)).isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode(new CraftingJobEstimateCodec.Saved(JOB, null)).isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode(new CraftingJobEstimateCodec.Saved(new UUID(0, 0), graph)).isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode(new CraftingJobEstimateCodec.Saved(JOB,
                new CraftingJobEstimate.Snapshot(null, Map.of(), Map.of(), true))).isEmpty());
        var amounts = new HashMap<ProfileKey, Long>();
        amounts.put(ROOT, null);
        assertTrue(CraftingJobEstimateCodec.encode(new CraftingJobEstimateCodec.Saved(JOB,
                new CraftingJobEstimate.Snapshot(ROOT, amounts, Map.of(), true))).isEmpty());
        amounts.clear();
        amounts.put(null, 1L);
        assertTrue(new CraftingJobEstimate(ROOT, amounts, Map.of()).snapshot().isEmpty());
        amounts.clear();
        var edges = new HashMap<ProfileKey, Set<ProfileKey>>();
        for (int i = 0; i <= CraftingJobEstimateCodec.MAX_NODES; i++) {
            var key = new ProfileKey("network", "test:bound" + i);
            amounts.put(key, 1L);
            edges.put(key, Set.of());
        }
        assertTrue(CraftingJobEstimateCodec.encode(new CraftingJobEstimateCodec.Saved(JOB,
                new CraftingJobEstimate.Snapshot(ROOT, amounts, Map.of(), true))).isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode(new CraftingJobEstimateCodec.Saved(JOB,
                new CraftingJobEstimate.Snapshot(ROOT, Map.of(), edges, true))).isEmpty());
        assertTrue(new CraftingJobEstimate(ROOT, Map.of(), edges).snapshot().isEmpty());
        amounts.remove(new ProfileKey("network", "test:bound4096"));
        assertTrue(new CraftingJobEstimate(ROOT, amounts, Map.of()).snapshot().isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode(new CraftingJobEstimateCodec.Saved(JOB,
                new CraftingJobEstimate.Snapshot(ROOT, amounts, Map.of(), true))).isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode(new CraftingJobEstimateCodec.Saved(JOB,
                new CraftingJobEstimate.Snapshot(ROOT, amounts, Map.of(ROOT, Set.of(CHILD)), true))).isEmpty());
        var tooManyEdges = new java.util.HashSet<ProfileKey>();
        for (int i = 0; i <= CraftingJobEstimateCodec.MAX_EDGES; i++)
            tooManyEdges.add(new ProfileKey("network", "test:edge" + i));
        assertTrue(CraftingJobEstimateCodec.encode(new CraftingJobEstimateCodec.Saved(JOB,
                new CraftingJobEstimate.Snapshot(ROOT, Map.of(), Map.of(ROOT, tooManyEdges), true))).isEmpty());
    }

    @Test
    void zeroUuidHalvesAndEmptyNetworkAreValidButImpossibleCountsAreNot() {
        var root = new ProfileKey("", "minecraft:stone");
        for (var job : new UUID[] {new UUID(0, 1), new UUID(1, 0)}) {
            var bytes = CraftingJobEstimateCodec.encode(job,
                    new CraftingJobEstimate(root, Map.of(root, 0L), Map.of())).orElseThrow();
            assertEquals(job, CraftingJobEstimateCodec.decode(bytes).orElseThrow().jobId());
            ByteBuffer.wrap(bytes).putInt(22, 4096);
            assertTrue(CraftingJobEstimateCodec.decode(bytes).isEmpty());
        }
    }

    @Test
    void sourceValidationDoesNotChangeLiveGraph() {
        var graph = new CraftingJobEstimate(ROOT, Map.of(ROOT, 3L), Map.of());
        assertTrue(CraftingJobEstimateCodec.encode(null, graph).isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode(JOB, null).isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode((CraftingJobEstimateCodec.Saved) null).isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode(new CraftingJobEstimateCodec.Saved(JOB,
                new CraftingJobEstimate.Snapshot(ROOT, Map.of(ROOT, -1L), Map.of(), true))).isEmpty());
        assertTrue(CraftingJobEstimateCodec.encode(new CraftingJobEstimateCodec.Saved(JOB,
                new CraftingJobEstimate.Snapshot(ROOT, Map.of(ROOT, 1L),
                        Map.of(ROOT, Set.of(new ProfileKey("other", "minecraft:glass"))), true))).isEmpty());
        assertEquals(3L, graph.snapshot().orElseThrow().remainingAmounts().get(ROOT));
    }
}
