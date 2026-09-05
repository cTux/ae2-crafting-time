package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.IntegrationDiagnostics;
import com.ctux.ae2craftingtime.integration.IntegrationMixinPlugin;
import java.util.function.Function;
import org.slf4j.LoggerFactory;

/** One report for this process, independent of world and network state. */
public final class IntegrationLog {
    private static volatile IntegrationDiagnostics diagnostics;

    public static synchronized void start(String target, boolean client, String loader,
            Function<String, String> versions) {
        if (diagnostics != null) return;
        var logger = LoggerFactory.getLogger("ae2craftingtime");
        logger.info("phase=startup_context target={} environment={} ae2craftingtime={} minecraft={} loader={} loader_version={} ae2={} unknown_reason=metadata_unavailable",
                target, client ? "client" : "dedicated_server", IntegrationDiagnostics.clean(versions.apply("ae2craftingtime")),
                IntegrationDiagnostics.clean(versions.apply("minecraft")), loader,
                IntegrationDiagnostics.clean(versions.apply(loader)), IntegrationDiagnostics.clean(versions.apply("ae2")));
        diagnostics = new IntegrationDiagnostics(target, client, versions, IntegrationMixinPlugin.snapshot(), event -> {
            switch (event.level()) {
                case "ERROR" -> logger.error(event.message(), event.cause());
                case "WARN" -> logger.warn(event.message(), event.cause());
                default -> logger.info(event.message());
            }
        });
    }

    public static void observe(String id, String capability) {
        var report = diagnostics;
        if (report != null) report.observe(id, capability);
    }

    public static void required(String capability, Runnable registration) {
        diagnostics.required(capability, registration);
    }

    public static void fail(String id, String capability, String reason, boolean fatal, Throwable failure) {
        var report = diagnostics;
        if (report != null) report.fail(id, capability, reason, fatal, failure);
    }

    public static boolean disabled(String id, String capability) {
        var report = diagnostics;
        return report != null && report.disabled(id, capability);
    }

    public static boolean available(String id) {
        var report = diagnostics;
        return report != null && report.available(id);
    }

    public static void disable(String id, com.ctux.ae2craftingtime.core.IntegrationRead.Failure failure) {
        var report = diagnostics;
        if (report != null) report.disable(id, failure.getMessage(), failure.getCause());
    }

    public static void cpu(String id, String capability) {
        var report = diagnostics;
        if (report != null) {
            var enabled = Ae2CraftingTimeConfig.ENABLED.get();
            report.configured(id, capability, enabled);
            if (enabled) report.observe(id, capability);
        }
    }

    public static void positive(String id, String capability, long amount) {
        var report = diagnostics;
        if (report != null) report.positive(id, capability, amount, Ae2CraftingTimeConfig.ENABLED.get());
    }

    public static void growth(String capability, int before, int after) {
        var report = diagnostics;
        if (report != null) report.growth(capability, before, after);
    }

    public static void wireless(String screen, boolean card) {
        var report = diagnostics;
        if (report != null) report.wireless(screen, card);
    }

    public static void normalized(String outputId) {
        var report = diagnostics;
        if (report != null) report.normalized(outputId);
    }

    public static boolean treeEnabled() {
        var report = diagnostics;
        if (report == null) return false;
        var enabled = Ae2CraftingTimeConfig.SHOW_IN_TREE.get();
        report.configureGroup("ae2ct", enabled);
        return report.available("ae2ct") && enabled;
    }

    public static void summary() { diagnostics.summary(); }

    public static void configuration() {
        diagnostics.configureProfiling(Ae2CraftingTimeConfig.ENABLED.get());
        diagnostics.configureGroup("ae2ct", Ae2CraftingTimeConfig.SHOW_IN_TREE.get());
    }
    private IntegrationLog() {}
}
