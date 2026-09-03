package com.ctux.ae2craftingtime.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/** One immutable, early-startup choice per dependency. No game or loader classes. */
public final class IntegrationSelection {
    public record Candidate(String dependency, String variant, Set<String> targets,
            boolean clientOnly, Set<String> mixins, List<IntegrationContract.Member> contract) {
        public Candidate {
            targets = Set.copyOf(targets);
            mixins = Set.copyOf(mixins);
            contract = List.copyOf(contract);
        }
    }

    public record Decision(String dependency, String installedVersion, String variant,
            String reason, Set<String> mixins, List<String> rejected) {
        public Decision {
            mixins = Set.copyOf(mixins);
            rejected = List.copyOf(rejected);
        }
    }

    private final List<Candidate> candidates;
    private final String target;
    private final boolean client;
    private final Function<String, String> versions;
    private final Function<Candidate, String> probe;
    private final Consumer<Decision> report;
    private final Map<String, String> owners = new HashMap<>();
    private final Map<String, Decision> decisions = new HashMap<>();

    public IntegrationSelection(List<Candidate> candidates, String target, boolean client,
            Function<String, String> versions, Function<Candidate, String> probe, Consumer<Decision> report) {
        this.candidates = List.copyOf(candidates);
        this.target = target;
        this.client = client;
        this.versions = versions;
        this.probe = probe;
        this.report = report;
        var ids = new HashSet<String>();
        for (var candidate : candidates) {
            if (!ids.add(candidate.dependency() + "/" + candidate.variant())) {
                throw new IllegalArgumentException("Duplicate adapter: " + candidate.variant());
            }
            for (var mixin : candidate.mixins()) {
                var previous = owners.putIfAbsent(mixin, candidate.dependency());
                if (previous != null && !previous.equals(candidate.dependency())) {
                    throw new IllegalArgumentException("Mixin owned by two dependencies: " + mixin);
                }
            }
        }
    }

    public synchronized boolean shouldApply(String mixin) {
        var dependency = owners.get(mixin);
        if (dependency == null) {
            return true; // Required AE2 hooks are outside the optional selector.
        }
        if (!decisions.containsKey(dependency)) {
            var decision = choose(dependency);
            decisions.put(dependency, decision);
            report.accept(decision);
        }
        return decisions.get(dependency).mixins().contains(mixin);
    }

    /** Only plain immutable values escape startup; no class nodes or game objects. */
    public synchronized Map<String, Decision> snapshot() {
        return Map.copyOf(decisions);
    }

    public String newestVariant(String dependency) {
        return applicable(dependency).stream().filter(c -> client || !c.clientOnly())
                .map(Candidate::variant).findFirst().orElse("");
    }

    private List<Candidate> applicable(String dependency) {
        return candidates.stream().filter(c -> c.dependency().equals(dependency))
                .filter(c -> c.targets().contains(target)).toList();
    }

    private Decision choose(String dependency) {
        // Exceptions from metadata/probes propagate; never turn bootstrap failure into absence.
        var version = versions.apply(dependency);
        var applicable = applicable(dependency);
        if (applicable.isEmpty()) {
            return skipped(dependency, version, "unsupported_target", List.of());
        }
        applicable = applicable.stream().filter(c -> client || !c.clientOnly()).toList();
        if (applicable.isEmpty()) {
            return skipped(dependency, version, "wrong_side", List.of());
        }
        if (version == null) {
            return skipped(dependency, null, "absent", List.of());
        }
        var rejected = new ArrayList<String>();
        for (var candidate : applicable) {
            var reason = probe.apply(candidate);
            if (reason == null) {
                return new Decision(dependency, version, candidate.variant(), "selected",
                        candidate.mixins(), rejected);
            }
            rejected.add(candidate.variant() + ":" + reason);
        }
        return skipped(dependency, version, "no_compatible_variant", rejected);
    }

    private static Decision skipped(String dependency, String version, String reason, List<String> rejected) {
        return new Decision(dependency, version, "", reason, Set.of(), rejected);
    }
}
