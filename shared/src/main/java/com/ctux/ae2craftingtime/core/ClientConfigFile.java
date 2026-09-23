package com.ctux.ae2craftingtime.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Locale;

/** Small flat TOML reader/writer for client-owned options. */
public final class ClientConfigFile {
    public static ClientConfig load(Path path, Path legacyPath) throws IOException {
        var config = new ClientConfig();
        if (Files.isRegularFile(path)) {
            read(path, config, false);
        } else if (Files.isRegularFile(legacyPath)) {
            read(legacyPath, config, true);
        }
        return config;
    }

    public static void save(Path path, ClientConfig config) throws IOException {
        var lines = new ArrayList<String>();
        lines.add("# AE2 Crafting Time client options");
        for (var feature : OptionFeature.values()) {
            if (feature.owner() == OptionFeature.Owner.CLIENT) {
                lines.add(feature.key() + " = " + config.features().enabled(feature));
            }
        }
        lines.add("planSort = " + config.planSort());
        lines.add("statusSort = " + config.statusSort());
        lines.add("badgeOpacity = " + config.badgeOpacity());
        for (var color : ClientConfig.Color.values()) {
            lines.add(colorKey(color) + " = \"#" + String.format(Locale.ROOT, "%06X", config.color(color)) + "\"");
        }
        saveLines(path, lines);
    }

    static void saveLines(Path path, ArrayList<String> lines) throws IOException {
        Files.createDirectories(path.getParent());
        var temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, lines);
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void read(Path path, ClientConfig config, boolean legacyOnly) throws IOException {
        for (var line : Files.readAllLines(path)) {
            var parts = line.split("=", 2);
            if (parts.length != 2) continue;
            var key = parts[0].trim();
            var value = parts[1].trim();
            if (legacyOnly && !key.equals("showInTree")) continue;
            try {
                set(config, key, value);
            } catch (IllegalArgumentException ignored) {
                // A malformed field keeps only its own default.
            }
        }
    }

    private static void set(ClientConfig config, String key, String value) {
        for (var feature : OptionFeature.values()) {
            if (feature.owner() == OptionFeature.Owner.CLIENT && feature.key().equals(key)) {
                if (value.equalsIgnoreCase("true")) config.features().setEnabled(feature, true);
                else if (value.equalsIgnoreCase("false")) config.features().setEnabled(feature, false);
                return;
            }
        }
        switch (key) {
            case "planSort" -> config.setPlanSort(Integer.parseInt(value));
            case "statusSort" -> config.setStatusSort(Integer.parseInt(value));
            case "badgeOpacity" -> config.setBadgeOpacity(Integer.parseInt(value));
            default -> {
                for (var color : ClientConfig.Color.values()) {
                    if (colorKey(color).equals(key)) {
                        var rgb = value.replace("\"", "");
                        if (!rgb.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Invalid RGB");
                        config.setColor(color, Integer.parseInt(rgb.substring(1), 16));
                        return;
                    }
                }
            }
        }
    }

    private static String colorKey(ClientConfig.Color color) {
        return color.name().toLowerCase(Locale.ROOT) + "Color";
    }

    private ClientConfigFile() {
    }
}
