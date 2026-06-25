package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigFilenameTest {
    @Test
    void commonConfigFilenameStartsWithModId() throws IOException {
        var source = Files.readString(Path.of("src/main/java/com/ctux/ae2craftingtime/mc1201/Ae2CraftingTime.java"));

        assertTrue(source.contains("\"ae2craftingtime-common.toml\""));
    }

    @Test
    void exposesAccuracyConfigKnobs() throws IOException {
        var source = Files.readString(Path.of("src/main/java/com/ctux/ae2craftingtime/mc1201/Ae2CraftingTimeConfig.java"));

        assertTrue(source.contains("MAX_SAMPLES"));
        assertTrue(source.contains("OUTLIER_MULTIPLIER"));
        assertTrue(source.contains("SHOW_CHAT_MESSAGES"));
    }

    @Test
    void loadsCommonConfigFile(@TempDir Path tempDir) throws IOException {
        var config = tempDir.resolve("ae2craftingtime-common.toml");
        Files.writeString(config, """
                enabled = false
                showInTree = false
                showChatMessages = false
                maxSamples = 25
                outlierMultiplier = 7.5
                """);

        Ae2CraftingTimeConfig.load(config);

        assertFalse(Ae2CraftingTimeConfig.ENABLED.get());
        assertFalse(Ae2CraftingTimeConfig.SHOW_IN_TREE.get());
        assertFalse(Ae2CraftingTimeConfig.SHOW_CHAT_MESSAGES.get());
        assertEquals(25, Ae2CraftingTimeConfig.MAX_SAMPLES.get());
        assertEquals(7.5, Ae2CraftingTimeConfig.OUTLIER_MULTIPLIER.get());
    }

    @Test
    void malformedBooleanKeepsExistingValue(@TempDir Path tempDir) throws IOException {
        var config = tempDir.resolve("ae2craftingtime-common.toml");
        Files.writeString(config, """
                enabled = true
                showInTree = true
                showChatMessages = true
                """);
        Ae2CraftingTimeConfig.load(config);

        Files.writeString(config, """
                enabled = treu
                showInTree = maybe
                showChatMessages = nope
                """);
        Ae2CraftingTimeConfig.load(config);

        assertTrue(Ae2CraftingTimeConfig.ENABLED.get());
        assertTrue(Ae2CraftingTimeConfig.SHOW_IN_TREE.get());
        assertTrue(Ae2CraftingTimeConfig.SHOW_CHAT_MESSAGES.get());
    }
}
