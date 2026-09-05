package com.ctux.ae2craftingtime.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/** Process-local observations. Selection and presence never constitute hook evidence. */
public final class IntegrationDiagnostics {
    public enum State { PENDING, CONFIRMED, SKIPPED, DISABLED }
    public enum Outcome { INITIALIZED, SKIPPED, PENDING, PARTIAL, FAILED }
    public record Event(String level, String message, Throwable cause) {}
    public record Entry(String id, Set<String> targets, boolean clientOnly, String mode,
            List<String> capabilities, List<String> aliases) {}

    private static final Set<String> ALL = Set.of("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge", "26.1.2-neoforge");
    private static final Set<String> FBN = Set.of("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge");
    private static final Set<String> FN = Set.of("1.20.1-forge", "1.21.1-neoforge");
    private static final Set<String> FNX = Set.of("1.20.1-forge", "1.21.1-neoforge", "26.1.2-neoforge");
    private static final Set<String> F = Set.of("1.20.1-forge");
    public static final List<String> CPU = List.of("cpu-submit", "cpu-dispatch", "cpu-output", "cpu-finish", "cpu-capacity");
    public static final List<String> UI = List.of("plan-row", "plan-tooltip", "plan-total", "plan-sort", "plan-details", "plan-reset",
            "status-row", "status-tooltip", "status-total", "status-sort", "status-details", "status-reset");
    public static final List<Entry> CATALOG = List.of(
            entry("ae2ct", FBN, true, "adapter", List.of("layout", "node", "tooltip", "details", "reset")),
            entry("merequester", FBN, true, "adapter", List.of("request-read", "row", "total")),
            entry("advanced_ae", FNX, false, "adapter", plus(CPU, List.of("selected-cpu"))),
            entry("neoecoae", FN, false, "adapter", plus(CPU, List.of("cpu-dispatch-fastpath"))),
            entry("ae2lt", FN, false, "adapter", CPU),
            entry("wcwt", FN, true, "adapter", List.of("tooltip")),
            entry("ae2wtlib", ALL, true, "adapter", List.of("tooltip")),
            new Entry("ae2importexportcard", FNX, true, "adapter", List.of("tooltip"),
                    List.of("ae2importexportcard", "ae2insertexportcard")),
            entry("appmek", FN, false, "shared-hooks", plus(CPU, List.of("key-normalization"))),
            entry("appflux", FNX, false, "shared-hooks", plus(CPU, List.of("key-normalization"))),
            entry("appbot", FBN, false, "shared-hooks", plus(CPU, List.of("key-normalization", "mana-normalization"))),
            entry("molecularmanipulator", FN, false, "shared-hooks", CPU),
            new Entry("expatternprovider", ALL, false, "shared-hooks", plus(CPU, UI), List.of("expatternprovider", "extendedae")),
            entry("extendedae_plus", FN, false, "shared-hooks", plus(CPU, UI)),
            entry("bmaddon", FNX, false, "shared-hooks", plus(CPU, UI)),
            entry("crazyae2addons", F, false, "shared-hooks", plus(CPU, UI)),
            entry("megacells", FBN, false, "shared-hooks", plus(CPU, UI)),
            entry("ae2omnicells", FNX, false, "shared-hooks", plus(CPU, UI)),
            entry("projectcell", FN, false, "shared-hooks", plus(CPU, UI)),
            entry("appliede", FN, false, "shared-hooks", plus(CPU, UI)),
            entry("mae2", F, false, "shared-hooks", plus(CPU, UI)),
            entry("advancedperipherals", FN, false, "shared-hooks", CPU),
            entry("ae2things", FBN, false, "shared-hooks", plus(CPU, UI)),
            entry("expandedae", FN, false, "shared-hooks", plus(CPU, UI)),
            entry("ae2netanalyser", ALL, false, "compatibility-only", List.of()),
            entry("aeinfinitybooster", FNX, false, "compatibility-only", List.of()));

    private static final class Capability {
        private State state;
        private final EnumSet<State> emitted = EnumSet.noneOf(State.class);
        private Capability(State state) { this.state = state; }
    }
    private static final class Integration {
        private final Entry entry;
        private final String mod;
        private final String version;
        private final String variant;
        private final Map<String, Capability> capabilities = new LinkedHashMap<>();
        private boolean fatal;
        private boolean eligible;
        private boolean groupDisabled;
        private Boolean configured;
        private Integration(Entry entry, String mod, String version, String variant) {
            this.entry = entry;
            this.mod = mod;
            this.version = clean(version);
            this.variant = clean(variant);
        }
    }

    private final Map<String, Integration> integrations = new LinkedHashMap<>();
    private final Consumer<Event> log;
    private boolean summarized;

    public IntegrationDiagnostics(String target, boolean client, Function<String, String> versions,
            Map<String, IntegrationSelection.Decision> selections, Consumer<Event> log) {
        this.log = log;
        var core = entry("ae2craftingtime", ALL, false, "core", plus(plus(CPU, UI),
                List.of("config-registration", "network-registration", "client-network-registration", "key-registration",
                        "key-normalization", "mana-normalization")));
        var entries = new ArrayList<>(List.of(core));
        entries.addAll(CATALOG);
        for (var entry : entries) {
            var mod = entry.id();
            String version = null;
            for (var alias : entry.aliases()) {
                version = versions.apply(alias);
                if (version != null) { mod = alias; break; }
            }
            var selection = selections.get(entry.id());
            var integration = new Integration(entry, mod, version, selection == null ? "none" : selection.variant());
            integrations.put(entry.id(), integration);
            var reason = !entry.targets().contains(target) ? "target_not_supported"
                    : entry.clientOnly() && !client ? "dedicated_server"
                    : version == null ? "mod_absent"
                    : entry.mode().equals("compatibility-only") ? "no_owned_adapter"
                    : selection != null && selection.variant().isEmpty() ? "preflight_" + selection.reason()
                    : "awaiting_hook";
            integration.eligible = reason.equals("awaiting_hook");
            for (var capability : entry.capabilities()) {
                var applicable = reason.equals("awaiting_hook") && (client || !isClient(capability))
                        && (!capability.equals("client-network-registration") || target.equals("1.20.1-fabric"));
                integration.capabilities.put(capability, new Capability(applicable ? State.PENDING : State.SKIPPED));
            }
            log.accept(new Event("INFO", fields(integration) + " phase=startup state=" + lower(outcome(entry.id()))
                    + " reason=" + clean(reason) + " pending=" + states(integration, State.PENDING), null));
        }
    }

    public synchronized void observe(String id, String capability) {
        var current = integrations.get(id).capabilities.get(capability).state;
        if (current != State.PENDING) return;
        transition(id, capability, State.CONFIRMED, "hook_observed", false, null);
        if (id.equals("ae2craftingtime")) {
            for (var integration : integrations.values()) {
                if (integration.entry.mode().equals("shared-hooks") && integration.capabilities.containsKey(capability)) {
                    transition(integration.entry.id(), capability, State.CONFIRMED,
                            "shared_hooks_observed;addon_job_not_verified", false, null);
                }
            }
        }
    }

    public synchronized void configured(String id, String capability, boolean enabled) {
        var value = integrations.get(id).capabilities.get(capability);
        var previous = value.state;
        if (previous == State.DISABLED) return;
        if (!enabled) transition(id, capability, State.SKIPPED, "config_disabled", false, null);
        else if (previous == State.SKIPPED && value.emitted.contains(State.SKIPPED)) {
            transition(id, capability, State.PENDING, "config_enabled;awaiting_hook", false, null);
        }
        if (id.equals("ae2craftingtime") && value.state != previous) {
            for (var integration : integrations.values()) {
                if (integration.entry.mode().equals("shared-hooks") && integration.capabilities.containsKey(capability)) {
                    configured(integration.entry.id(), capability, enabled);
                }
            }
        }
    }

    public synchronized void fail(String id, String capability, String reason, boolean fatal, Throwable cause) {
        transition(id, capability, State.DISABLED, reason, fatal, cause);
    }

    private void transition(String id, String capability, State next, String reason, boolean fatal, Throwable cause) {
        var integration = integrations.get(id);
        var value = integration.capabilities.get(capability);
        if (value.state == State.DISABLED || value.state == next) return;
        if (value.state == State.SKIPPED && next == State.CONFIRMED) return;
        value.state = next;
        integration.fatal |= fatal;
        if (!value.emitted.add(next)) return;
        log.accept(new Event(fatal ? "ERROR" : next == State.DISABLED ? "WARN" : "INFO",
                fields(integration) + " phase=observation capability=" + capability + " state=" + lower(next)
                        + " outcome=" + lower(outcome(id)) + " reason=" + clean(reason)
                        + " action=" + (fatal ? "propagate" : next == State.DISABLED ? "disabled" : "none")
                        + " pending=" + states(integration, State.PENDING), cause));
    }

    public synchronized void configureGroup(String id, boolean enabled) {
        var integration = integrations.get(id);
        if (!integration.eligible || Boolean.valueOf(enabled).equals(integration.configured)) return;
        integration.configured = enabled;
        for (var capability : integration.entry.capabilities()) configured(id, capability, enabled);
    }

    public synchronized void positive(String id, String capability, long amount, boolean enabled) {
        configured(id, capability, enabled);
        if (enabled && amount > 0) observe(id, capability);
    }

    public void required(String capability, Runnable registration) {
        try {
            registration.run();
        } catch (RuntimeException | Error failure) {
            fail("ae2craftingtime", capability, "required_registration_failed", true, failure);
            throw failure;
        }
        observe("ae2craftingtime", capability);
    }

    public synchronized void growth(String capability, int before, int after) {
        if (after > before) observe("ae2craftingtime", capability);
    }

    public synchronized void wireless(String screen, boolean importExportCard) {
        if (screen.equals("com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen")) observe("wcwt", "tooltip");
        if (screen.equals("de.mari_023.ae2wtlib.wct.WCTScreen")) observe("ae2wtlib", "tooltip");
        if (importExportCard) observe("ae2importexportcard", "tooltip");
    }

    public synchronized void normalized(String outputId) {
        observe("ae2craftingtime", "key-normalization");
        if (outputId.equals("botania:mana")) observe("ae2craftingtime", "mana-normalization");
    }

    public synchronized boolean available(String id) {
        var integration = integrations.get(id);
        return integration.eligible && !integration.groupDisabled;
    }

    public synchronized void disable(String id, String reason, Throwable cause) {
        var integration = integrations.get(id);
        if (!available(id)) return;
        integration.groupDisabled = true;
        integration.capabilities.values().forEach(value -> value.state = State.DISABLED);
        log.accept(new Event("WARN", fields(integration) + " phase=observation capability=" + integration.entry.capabilities()
                + " state=disabled outcome=failed action=disabled reason=" + clean(reason)
                + " retained=host_behavior,core_profiling,stored_history", cause));
    }

    public synchronized boolean disabled(String id, String capability) {
        return integrations.get(id).capabilities.get(capability).state == State.DISABLED;
    }

    public synchronized Outcome outcome(String id) {
        var integration = integrations.get(id);
        if (integration.fatal) return Outcome.FAILED;
        var states = EnumSet.noneOf(State.class);
        integration.capabilities.values().forEach(value -> states.add(value.state));
        if (states.contains(State.DISABLED)) {
            return states.contains(State.CONFIRMED) || states.contains(State.PENDING) ? Outcome.PARTIAL : Outcome.FAILED;
        }
        if (states.contains(State.PENDING)) return Outcome.PENDING;
        return states.contains(State.CONFIRMED) ? Outcome.INITIALIZED : Outcome.SKIPPED;
    }

    public synchronized void summary() {
        if (summarized) return;
        summarized = true;
        var counts = new int[Outcome.values().length];
        CATALOG.forEach(entry -> counts[outcome(entry.id()).ordinal()]++);
        log.accept(new Event("INFO", "phase=entrypoint_checks initialized=" + counts[0] + " skipped=" + counts[1]
                + " pending=" + counts[2] + " partial=" + counts[3] + " failed=" + counts[4], null));
    }

    public static String clean(String value) {
        if (value == null || value.isBlank()) return "unknown";
        var text = new StringBuilder();
        for (int i = 0; i < value.length() && text.length() < 256; i++) {
            var c = value.charAt(i);
            text.append(Character.isISOControl(c) || Character.isWhitespace(c) ? '_' : c);
        }
        return text.toString();
    }

    private static boolean isClient(String capability) {
        return UI.contains(capability) || capability.equals("key-registration") || capability.equals("client-network-registration");
    }
    private static String fields(Integration integration) {
        return "integration=" + integration.entry.id() + " mod=" + integration.mod + " version=" + integration.version
                + " mode=" + integration.entry.mode() + " variant=" + integration.variant;
    }
    private static List<String> states(Integration integration, State state) {
        return integration.capabilities.entrySet().stream().filter(e -> e.getValue().state == state).map(Map.Entry::getKey).toList();
    }
    private static String lower(Enum<?> value) { return value.name().toLowerCase(java.util.Locale.ROOT); }
    private static Entry entry(String id, Set<String> targets, boolean client, String mode, List<String> capabilities) {
        return new Entry(id, targets, client, mode, capabilities, List.of(id));
    }
    private static List<String> plus(List<String> first, List<String> second) {
        var result = new ArrayList<>(first);
        result.addAll(second);
        return List.copyOf(result);
    }
}
