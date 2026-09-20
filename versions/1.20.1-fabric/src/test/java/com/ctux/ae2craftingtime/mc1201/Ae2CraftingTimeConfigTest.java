package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Ae2CraftingTimeConfigTest {
    @TempDir
    Path directory;

    private Path config;

    @BeforeEach
    void resetBeforeTest() throws IOException {
        config = directory.resolve("ae2craftingtime-common.toml");
        load("outlierMultiplier=4.0");
    }

    @AfterEach
    void resetAfterTest() throws IOException {
        load("outlierMultiplier=4.0");
    }

    @Test
    void invalidInitialValueKeepsTheDefault() throws IOException {
        load("outlierMultiplier=NaN");

        assertEquals(4.0, Ae2CraftingTimeConfig.OUTLIER_MULTIPLIER.get());
    }

    @Test
    void invalidDuplicateKeepsThePreviousValidValue() throws IOException {
        load("outlierMultiplier=2.5\noutlierMultiplier=Infinity");

        assertEquals(2.5, Ae2CraftingTimeConfig.OUTLIER_MULTIPLIER.get());
    }

    @Test
    void finiteValuesAreKeptOrClamped() throws IOException {
        load("outlierMultiplier=0.5");
        assertEquals(1.0, Ae2CraftingTimeConfig.OUTLIER_MULTIPLIER.get());

        load("outlierMultiplier=1000.5");
        assertEquals(1000.0, Ae2CraftingTimeConfig.OUTLIER_MULTIPLIER.get());

        load("outlierMultiplier=3.5");
        assertEquals(3.5, Ae2CraftingTimeConfig.OUTLIER_MULTIPLIER.get());
    }

    private void load(String text) throws IOException {
        Files.writeString(config, text);
        Ae2CraftingTimeConfig.load(config);
    }
}
