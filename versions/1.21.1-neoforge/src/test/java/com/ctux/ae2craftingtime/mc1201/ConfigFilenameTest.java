package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ConfigFilenameTest {
    @Test
    void commonConfigFilenameStartsWithModId() throws IOException {
        var source = Files.readString(Path.of("src/main/java/com/ctux/ae2craftingtime/mc1201/Ae2CraftingTime.java"));

        assertTrue(source.contains("\"ae2craftingtime-common.toml\""));
    }

    @Test
    void exposesAccuracyConfigKnobs() throws IOException {
        var source = Files.readString(Path.of("src/main/java/com/ctux/ae2craftingtime/mc1201/Ae2CraftingTimeConfig.java"));

        assertTrue(source.contains("\"maxSamples\""));
        assertTrue(source.contains("\"outlierMultiplier\""));
        assertTrue(source.contains("\"showChatMessages\""));
    }

    @Test
    void suppliesRuntimeDependenciesAcceptedByNeoForge() throws IOException {
        var build = Files.readString(Path.of("build.gradle"));
        var metadata = Files.readString(Path.of("build/resources/main/META-INF/neoforge.mods.toml"));

        assertTrue(build.contains("runtimeOnly 'org.appliedenergistics:guideme:"));
        assertTrue(metadata.contains("versionRange=\"[1.0.1,)\""));
    }
}
