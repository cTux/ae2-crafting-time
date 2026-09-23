package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientConfigFileTest {
    @TempDir Path directory;

    @Test
    void missingAndLegacyFilesUseSafeDefaults() throws IOException {
        var path = directory.resolve("client.toml");
        var legacy = directory.resolve("common.toml");
        assertTrue(ClientConfigFile.load(path, legacy).features().enabled(OptionFeature.CRAFTING_TREE));
        Files.write(legacy, List.of("enabled = false", "showInTree = false"));
        var migrated = ClientConfigFile.load(path, legacy);
        assertFalse(migrated.features().enabled(OptionFeature.CRAFTING_TREE));
        assertTrue(migrated.features().enabled(OptionFeature.PLAN_ROWS));
    }

    @Test
    void validValuesRoundTripAndClientFileWinsOverLegacy() throws IOException {
        var path = directory.resolve("nested/client.toml");
        var legacy = directory.resolve("common.toml");
        Files.write(legacy, List.of("showInTree = false"));
        var config = new ClientConfig();
        config.features().setEnabled(OptionFeature.RECEIVE_CRAFT_WARNINGS, false);
        config.setPlanSort(0);
        config.setStatusSort(1);
        config.setBadgeOpacity(0);
        config.setColor(ClientConfig.Color.FAST, 0x123456);
        ClientConfigFile.save(path, config);
        var loaded = ClientConfigFile.load(path, legacy);
        assertFalse(loaded.features().enabled(OptionFeature.RECEIVE_CRAFT_WARNINGS));
        assertTrue(loaded.features().enabled(OptionFeature.CRAFTING_TREE));
        assertEquals(0, loaded.planSort());
        assertEquals(1, loaded.statusSort());
        assertEquals(0, loaded.badgeOpacity());
        assertEquals(0x123456, loaded.color(ClientConfig.Color.FAST));
    }

    @Test
    void malformedFieldsFallBackIndividually() throws IOException {
        var path = directory.resolve("client.toml");
        Files.write(path, List.of(
                "# comment", "unknown = ignored", "not a setting", "planRows = false",
                "receiveCraftWarnings = maybe", "showInTree = TRUE", "statusSort = 9",
                "planSort = nope", "badgeOpacity = 999", "fastColor = \"broken\"",
                "middleColor = \"#ABCDEF\"", "slowColor = \"#000000\""));
        var config = ClientConfigFile.load(path, directory.resolve("missing.toml"));
        assertFalse(config.features().enabled(OptionFeature.PLAN_ROWS));
        assertTrue(config.features().enabled(OptionFeature.RECEIVE_CRAFT_WARNINGS));
        assertTrue(config.features().enabled(OptionFeature.CRAFTING_TREE));
        assertEquals(2, config.planSort());
        assertEquals(2, config.statusSort());
        assertEquals(176, config.badgeOpacity());
        assertEquals(ClientConfig.Color.FAST.defaultRgb(), config.color(ClientConfig.Color.FAST));
        assertEquals(0xABCDEF, config.color(ClientConfig.Color.MIDDLE));
        assertEquals(0, config.color(ClientConfig.Color.SLOW));
    }
}
