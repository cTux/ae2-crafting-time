package com.ctux.ae2craftingtime.core;

import java.util.EnumMap;
import java.util.Optional;
import java.util.UUID;

/** One saved graph may attach once, after the server and its CPU are ready. */
public final class CraftingJobRecovery {
    private final Lifecycle lifecycle;
    private CraftingJobEstimateCodec.Saved pending;
    private Object server;
    private int generation;
    private int integrationGeneration;
    private OptionFeature integration;

    public CraftingJobRecovery(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public void read(byte[] bytes, OptionFeature scopeIntegration) {
        clear();
        var saved = CraftingJobEstimateCodec.decode(bytes);
        if (saved.isEmpty()) return;
        pending = saved.get();
        server = lifecycle.server;
        generation = lifecycle.generation;
        integration = scopeIntegration;
        integrationGeneration = lifecycle.integrationGeneration(scopeIntegration);
    }

    public Optional<CraftingJobEstimate> attach(Object activeServer, UUID jobId, String outputId,
            String networkId, boolean enabled) {
        if (pending == null) return Optional.empty();
        if (!valid(activeServer, enabled)) {
            clear();
            return Optional.empty();
        }
        if (!lifecycle.ready || jobId == null || outputId == null || networkId == null)
            return Optional.empty();
        if (!pending.jobId().equals(jobId) || !pending.graph().root().outputId().equals(outputId)
                || !pending.graph().root().networkId().equals(networkId)) {
            clear();
            return Optional.empty();
        }
        var graph = CraftingJobEstimate.restore(pending.graph());
        clear();
        return Optional.of(graph);
    }

    public Optional<byte[]> savePending(Object activeServer, UUID jobId, String outputId,
            String networkId, boolean enabled) {
        if (pending == null) return Optional.empty();
        if (!valid(activeServer, enabled) || jobId == null || !pending.jobId().equals(jobId)
                || outputId != null && !pending.graph().root().outputId().equals(outputId)
                || networkId != null && !pending.graph().root().networkId().equals(networkId)) {
            clear();
            return Optional.empty();
        }
        return CraftingJobEstimateCodec.encode(pending);
    }

    public boolean preservePendingTag(Object activeServer, boolean enabled) {
        if (pending == null) return false;
        if (valid(activeServer, enabled)) return true;
        clear();
        return false;
    }

    public void missedOutput() { clear(); }

    public void clear() {
        pending = null;
        server = null;
        integration = null;
    }

    private boolean valid(Object activeServer, boolean enabled) {
        return enabled && server != null && server == activeServer && lifecycle.server == activeServer
                && generation == lifecycle.generation
                && integrationGeneration == lifecycle.integrationGeneration(integration);
    }

    public static final class Lifecycle {
        private Object server;
        private boolean ready;
        private int generation;
        private final EnumMap<OptionFeature, Integer> integrationGenerations = new EnumMap<>(OptionFeature.class);

        public Object server() { return server; }

        public void begin(Object server) {
            this.server = server;
            ready = false;
            generation++;
        }

        public void ready(Object server) {
            if (server != null && this.server == server) {
                if (ready) generation++;
                ready = true;
            }
        }

        public void reset() {
            ready = false;
            generation++;
        }

        public void disable(OptionFeature integration) {
            if (integration == null || integration == OptionFeature.PROFILING) generation++;
            else integrationGenerations.merge(integration, 1, Integer::sum);
        }

        public void stop(Object server) {
            if (this.server == server) {
                this.server = null;
                ready = false;
                generation++;
            }
        }

        private int integrationGeneration(OptionFeature integration) {
            return integration == null ? 0 : integrationGenerations.getOrDefault(integration, 0);
        }
    }
}
