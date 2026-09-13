package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecurrentPlanControlTest {
    @TempDir Path directory;

    @Test void acknowledgementsAreRoleBoundAndIdempotentAcrossWaitingFrames() throws Exception {
        var keys = java.util.List.of("ae2craftingtime.test.control", "ae2craftingtime.test.campaign", "ae2craftingtime.test.role");
        var previous = keys.stream().map(System::getProperty).toList();
        try {
            System.setProperty(keys.get(0), directory.toString());
            System.setProperty(keys.get(1), UUID.randomUUID().toString());
            System.setProperty(keys.get(2), "alpha");
            var player = UUID.randomUUID();
            assertFalse(RecurrentPlanControl.request("initial", player));
            var command = RecurrentPlanControl.command("alpha");
            assertEquals(player.toString(), command.player());
            assertEquals("alpha", command.role());
            assertEquals(0, RecurrentPlanControl.command("beta").sequence());
            RecurrentPlanControl.publish("alpha", command.sequence(), "initial", "initial",
                    net.minecraft.core.BlockPos.ZERO, UUID.randomUUID(), true, "beta");
            assertFalse(RecurrentPlanControl.request("initial", player));
            RecurrentPlanControl.publish("alpha", command.sequence(), "initial", "swap",
                    net.minecraft.core.BlockPos.ZERO, player, false, "alpha");
            assertTrue(RecurrentPlanControl.request("initial", player));
            assertTrue(RecurrentPlanControl.request("initial", player));
            assertEquals(command, RecurrentPlanControl.command("alpha"));
            assertFalse(RecurrentPlanControl.request("swapped", player));
            assertEquals(command.sequence() + 1, RecurrentPlanControl.command("alpha").sequence());
            var oldEpoch = RecurrentPlanControl.command("alpha").epoch();
            System.setProperty(keys.get(1), UUID.randomUUID().toString());
            assertFalse(RecurrentPlanControl.request("swapped", player));
            assertNotEquals(oldEpoch, RecurrentPlanControl.command("alpha").epoch());
            Files.createDirectories(RecurrentPlanControl.directory("beta"));
            Files.writeString(RecurrentPlanControl.directory("beta").resolve("state.properties"), "x".repeat(65537));
            assertThrows(IllegalStateException.class, () -> RecurrentPlanControl.state("beta"));
        } finally {
            for (int i = 0; i < keys.size(); i++) {
                if (previous.get(i) == null) System.clearProperty(keys.get(i));
                else System.setProperty(keys.get(i), previous.get(i));
            }
        }
    }

    @Test void retriesTransientWindowsReplaceFailures() throws Exception {
        var target = directory.resolve("alpha/state.properties");
        var values = new Properties();
        values.setProperty("phase", "swap");
        var attempts = new AtomicInteger();
        RecurrentPlanControl.write(target, values, (source, destination) -> {
            if (attempts.incrementAndGet() == 1) throw new AccessDeniedException(destination.toString());
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        });
        assertEquals(2, attempts.get());
        var written = new Properties();
        try (var input = Files.newInputStream(target)) { written.load(input); }
        assertEquals("swap", written.getProperty("phase"));
    }
}
