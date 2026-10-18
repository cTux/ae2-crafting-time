package com.ctux.ae2craftingtime.core;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Bounded, optional data carried by the CPU's saved job. */
public final class CraftingJobEstimateCodec {
    public static final int MAX_BYTES = 1_048_576;
    public static final int MAX_NODES = 4_096;
    public static final int MAX_EDGES = 16_384;
    private static final int MAX_NETWORK_BYTES = 4_096;
    private static final int MAX_OUTPUT_BYTES = 128;

    public record Saved(UUID jobId, CraftingJobEstimate.Snapshot graph) { }

    public static Optional<byte[]> encode(UUID jobId, CraftingJobEstimate estimate) {
        if (jobId == null || jobId.getMostSignificantBits() == 0 && jobId.getLeastSignificantBits() == 0
                || estimate == null) return Optional.empty();
        return estimate.snapshot().flatMap(graph -> encode(new Saved(jobId, graph)));
    }

    public static Optional<byte[]> encode(Saved saved) {
        if (saved == null || saved.jobId() == null || saved.graph() == null) return Optional.empty();
        try {
            var graph = saved.graph();
            var root = graph.root();
            var network = root.networkId();
            if (network.length() > MAX_NETWORK_BYTES) return Optional.empty();
            var networkBytes = network.getBytes(StandardCharsets.UTF_8);
            if (networkBytes.length > MAX_NETWORK_BYTES || saved.jobId().getMostSignificantBits() == 0
                    && saved.jobId().getLeastSignificantBits() == 0) return Optional.empty();
            var nodes = new HashSet<ProfileKey>();
            nodes.add(root);
            if (graph.remainingAmounts().size() > MAX_NODES || graph.dependencies().size() > MAX_NODES)
                return Optional.empty();
            nodes.addAll(graph.remainingAmounts().keySet());
            var edges = 0;
            for (var entry : graph.dependencies().entrySet()) {
                nodes.add(entry.getKey());
                if (entry.getValue().size() > MAX_EDGES - edges) return Optional.empty();
                edges += entry.getValue().size();
                nodes.addAll(entry.getValue());
                if (nodes.size() > MAX_NODES) return Optional.empty();
            }
            if (nodes.size() > MAX_NODES) return Optional.empty();
            var ordered = List.copyOf(nodes);
            var indexes = new HashMap<ProfileKey, Integer>();
            var outputs = new ArrayList<byte[]>(ordered.size());
            long length = 1 + 16 + 4 + networkBytes.length + 1 + 4 + 4;
            for (int i = 0; i < ordered.size(); i++) {
                var key = ordered.get(i);
                if (!network.equals(key.networkId())) return Optional.empty();
                var output = PacketLimits.checkedOutputId(key.outputId()).getBytes(StandardCharsets.UTF_8);
                var amount = graph.remainingAmounts().getOrDefault(key, 0L);
                if (amount == null || amount < 0) return Optional.empty();
                outputs.add(output);
                indexes.put(key, i);
                length += 4L + output.length + 8 + 4 + 4L * graph.dependencies().getOrDefault(key, Set.of()).size();
            }
            var buffer = ByteBuffer.allocate((int) length);
            buffer.put((byte) 1).putLong(saved.jobId().getMostSignificantBits())
                    .putLong(saved.jobId().getLeastSignificantBits());
            buffer.putInt(networkBytes.length).put(networkBytes).put((byte) (graph.complete() ? 1 : 0));
            buffer.putInt(ordered.size()).putInt(indexes.get(root));
            for (int i = 0; i < ordered.size(); i++) {
                var key = ordered.get(i);
                var output = outputs.get(i);
                var dependencies = graph.dependencies().getOrDefault(key, Set.of());
                buffer.putInt(output.length).put(output).putLong(graph.remainingAmounts().getOrDefault(key, 0L));
                buffer.putInt(dependencies.size());
                for (var dependency : dependencies) buffer.putInt(indexes.get(dependency));
            }
            return Optional.of(buffer.array());
        } catch (IllegalArgumentException | NullPointerException error) {
            return Optional.empty();
        }
    }

    public static Optional<Saved> decode(byte[] bytes) {
        if (bytes == null || bytes.length > MAX_BYTES) return Optional.empty();
        try {
            var buffer = ByteBuffer.wrap(bytes);
            if (buffer.get() != 1) return Optional.empty();
            var jobId = new UUID(buffer.getLong(), buffer.getLong());
            if (jobId.getMostSignificantBits() == 0 && jobId.getLeastSignificantBits() == 0)
                return Optional.empty();
            var network = readString(buffer, MAX_NETWORK_BYTES);
            var completeness = buffer.get();
            if (completeness != 0 && completeness != 1) return Optional.empty();
            var count = buffer.getInt();
            if (count < 1 || count > MAX_NODES || count > buffer.remaining() / 16) return Optional.empty();
            var rootIndex = buffer.getInt();
            if (rootIndex < 0 || rootIndex >= count) return Optional.empty();
            var keys = new ArrayList<ProfileKey>(count);
            var amounts = new HashMap<ProfileKey, Long>();
            var indexes = new ArrayList<int[]>(count);
            var unique = new HashSet<ProfileKey>();
            var edgeCount = 0;
            for (int i = 0; i < count; i++) {
                var key = new ProfileKey(network, PacketLimits.checkedOutputId(readString(buffer, MAX_OUTPUT_BYTES)));
                if (!unique.add(key)) return Optional.empty();
                keys.add(key);
                var amount = buffer.getLong();
                if (amount < 0) return Optional.empty();
                amounts.put(key, amount);
                var dependencies = buffer.getInt();
                if (dependencies < 0 || dependencies > MAX_EDGES - edgeCount
                        || dependencies > buffer.remaining() / 4) return Optional.empty();
                edgeCount += dependencies;
                var positions = new int[dependencies];
                var seen = new HashSet<Integer>();
                for (int j = 0; j < dependencies; j++) {
                    positions[j] = buffer.getInt();
                    if (positions[j] < 0 || positions[j] >= count || !seen.add(positions[j]))
                        return Optional.empty();
                }
                indexes.add(positions);
            }
            if (buffer.hasRemaining()) return Optional.empty();
            var dependencies = new HashMap<ProfileKey, Set<ProfileKey>>();
            for (int i = 0; i < count; i++) {
                var values = new HashSet<ProfileKey>();
                for (var index : indexes.get(i)) values.add(keys.get(index));
                if (!values.isEmpty()) dependencies.put(keys.get(i), Set.copyOf(values));
            }
            return Optional.of(new Saved(jobId, new CraftingJobEstimate.Snapshot(keys.get(rootIndex),
                    Map.copyOf(amounts), Map.copyOf(dependencies), completeness == 1)));
        } catch (IllegalArgumentException | BufferUnderflowException error) {
            return Optional.empty();
        }
    }

    private static String readString(ByteBuffer buffer, int maximum) {
        var length = buffer.getInt();
        if (length < 0 || length > maximum || length > buffer.remaining())
            throw new IllegalArgumentException("invalid saved estimate string length");
        var bytes = new byte[length];
        buffer.get(bytes);
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException error) {
            throw new IllegalArgumentException("invalid saved estimate UTF-8", error);
        }
    }

    private CraftingJobEstimateCodec() { }
}
