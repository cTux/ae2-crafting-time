package com.ctux.ae2craftingtime.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/** World-scoped server values. Invalid fields keep their own defaults. */
public final class ServerConfigFile {
    private static final System.Logger LOGGER = System.getLogger("ae2craftingtime");
    public static ServerConfig load(Path path, Path legacyPath) throws IOException {
        var config = new ServerConfig();
        if (Files.isRegularFile(path)) read(path, config, false);
        else if (Files.isRegularFile(legacyPath)) read(legacyPath, config, true);
        if (Files.notExists(path)) {
            try { save(path, config); }
            catch (IOException error) {
                LOGGER.log(System.Logger.Level.WARNING, "Could not create server options at " + path, error);
            }
        }
        return config;
    }

    public static void save(Path path, ServerConfig config) throws IOException {
        var lines = new ArrayList<String>();
        lines.add("# AE2 Crafting Time server options");
        for (var feature : OptionFeature.values()) {
            if (feature.owner() == OptionFeature.Owner.SERVER)
                lines.add(feature.key() + " = " + config.features().enabled(feature));
        }
        lines.add("maxSamples = " + config.maxSamples());
        lines.add("outlierMultiplier = " + config.outlierMultiplier());
        lines.add("minimumNoProgressSeconds = " + config.minimumNoProgressSeconds());
        lines.add("typicalDurationMultiplier = " + config.typicalDurationMultiplier());
        ClientConfigFile.saveLines(path, lines);
    }

    private static void read(Path path, ServerConfig config, boolean legacyOnly) throws IOException {
        for (var line : Files.readAllLines(path)) {
            var parts = line.split("=", 2);
            if (parts.length != 2) continue;
            var key = parts[0].trim();
            if (legacyOnly && !legacyKey(key)) continue;
            try { set(config, key, parts[1].trim()); }
            catch (IllegalArgumentException error) {
                LOGGER.log(System.Logger.Level.WARNING, "Invalid server option {0} in {1}; using default",
                        key, path);
            }
        }
    }

    private static boolean legacyKey(String key) {
        return key.equals("enabled") || key.equals("notifyOnDelayed") || key.equals("showChatMessages")
                || key.equals("maxSamples") || key.equals("outlierMultiplier");
    }

    private static void set(ServerConfig config, String key, String value) {
        for (var feature : OptionFeature.values()) {
            if (feature.owner() == OptionFeature.Owner.SERVER && feature.key().equals(key)) {
                if (value.equalsIgnoreCase("true")) config.features().setEnabled(feature, true);
                else if (value.equalsIgnoreCase("false")) config.features().setEnabled(feature, false);
                else throw new IllegalArgumentException("Expected boolean");
                return;
            }
        }
        switch (key) {
            case "maxSamples" -> config.setMaxSamples(Integer.parseInt(value));
            case "outlierMultiplier" -> config.setOutlierMultiplier(Double.parseDouble(value));
            case "minimumNoProgressSeconds" -> config.setMinimumNoProgressSeconds(Integer.parseInt(value));
            case "typicalDurationMultiplier" -> config.setTypicalDurationMultiplier(Double.parseDouble(value));
            default -> { }
        }
    }

    private ServerConfigFile() { }
}
