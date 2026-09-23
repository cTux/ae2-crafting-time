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

class ServerConfigFileTest {
    @TempDir Path directory;

    @Test
    void missingLegacyAndNewFilesKeepOwnershipAndDefaults() throws IOException {
        var path = directory.resolve("world/serverconfig/server.toml");
        var legacy = directory.resolve("common.toml");
        assertTrue(ServerConfigFile.load(path, legacy).features().enabled(OptionFeature.PROFILING));
        Files.write(legacy, List.of("enabled = false", "notifyOnDelayed = false", "showChatMessages = false", "maxSamples = 20",
                "outlierMultiplier = 5.0", "showInTree = false", "unrelated = false"));
        var migrated = ServerConfigFile.load(path, legacy);
        assertFalse(migrated.features().enabled(OptionFeature.PROFILING));
        assertFalse(migrated.features().enabled(OptionFeature.NOTIFY_ON_DELAYED));
        assertFalse(migrated.features().enabled(OptionFeature.SHOW_CHAT_MESSAGES));
        assertEquals(20, migrated.maxSamples());
        assertEquals(5.0, migrated.outlierMultiplier());
        assertTrue(migrated.features().enabled(OptionFeature.SAVE_HISTORY));

        var changed = new ServerConfig();
        changed.features().setEnabled(OptionFeature.SAVE_HISTORY, false);
        changed.setMaxSamples(100);
        changed.setOutlierMultiplier(1000.0);
        changed.setMinimumNoProgressSeconds(3600);
        changed.setTypicalDurationMultiplier(1.0);
        ServerConfigFile.save(path, changed);
        var loaded = ServerConfigFile.load(path, legacy);
        assertTrue(loaded.features().enabled(OptionFeature.PROFILING));
        assertFalse(loaded.features().enabled(OptionFeature.SAVE_HISTORY));
        assertEquals(100, loaded.maxSamples());
        assertEquals(1000.0, loaded.outlierMultiplier());
        assertEquals(3600, loaded.minimumNoProgressSeconds());
        assertEquals(1.0, loaded.typicalDurationMultiplier());
    }

    @Test
    void invalidFieldsFallBackIndividually() throws IOException {
        var path = directory.resolve("server.toml");
        Files.write(path, List.of("# comment", "not a setting", "unknown = 1", "enabled = FALSE",
                "showChatMessages = maybe", "saveHistory = true", "maxSamples = bad",
                "outlierMultiplier = NaN", "minimumNoProgressSeconds = 0",
                "typicalDurationMultiplier = Infinity", "noPowerDetection = false"));
        var loaded = ServerConfigFile.load(path, directory.resolve("missing.toml"));
        assertFalse(loaded.features().enabled(OptionFeature.PROFILING));
        assertTrue(loaded.features().enabled(OptionFeature.SHOW_CHAT_MESSAGES));
        assertTrue(loaded.features().enabled(OptionFeature.SAVE_HISTORY));
        assertFalse(loaded.features().enabled(OptionFeature.NO_POWER_DETECTION));
        assertEquals(10, loaded.maxSamples());
        assertEquals(4.0, loaded.outlierMultiplier());
        assertEquals(10, loaded.minimumNoProgressSeconds());
        assertEquals(2.0, loaded.typicalDurationMultiplier());
    }
}
