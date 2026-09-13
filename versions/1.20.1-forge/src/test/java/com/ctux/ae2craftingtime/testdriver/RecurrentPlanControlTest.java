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

    @Test void acknowledgementsAreRecipientBoundAndIdempotentAcrossWaitingFrames() throws Exception {
        var keys = java.util.List.of("ae2craftingtime.test.control", "ae2craftingtime.test.campaign", "ae2craftingtime.test.role");
        var previous = keys.stream().map(System::getProperty).toList();
        try {
            System.setProperty(keys.get(0), directory.toString());
            System.setProperty(keys.get(1), UUID.randomUUID().toString());
            System.setProperty(keys.get(2), "alpha");
            var player = UUID.randomUUID();
            assertFalse(RecurrentPlanControl.request("initial", player, 3, 1));
            var command = RecurrentPlanControl.command("alpha");
            assertEquals(player.toString(), command.player());
            assertEquals("alpha", command.role());
            assertThrows(IllegalArgumentException.class, () -> RecurrentPlanControl.command("beta"));
            RecurrentPlanControl.publish("alpha", command.sequence(), "initial", "initial",
                    net.minecraft.core.BlockPos.ZERO, UUID.randomUUID(), true, "alpha", 3, 1);
            assertFalse(RecurrentPlanControl.request("initial", player, 3, 1));
            RecurrentPlanControl.publish("alpha", command.sequence(), "initial", "swap",
                    net.minecraft.core.BlockPos.ZERO, player, false, "alpha", 3, 1);
            assertTrue(RecurrentPlanControl.request("initial", player, 3, 1));
            assertTrue(RecurrentPlanControl.request("initial", player, 3, 1));
            assertEquals(command, RecurrentPlanControl.command("alpha"));
            assertFalse(RecurrentPlanControl.request("swapped", player, 3, 2));
            assertEquals(command.sequence() + 1, RecurrentPlanControl.command("alpha").sequence());
            var oldEpoch = RecurrentPlanControl.command("alpha").epoch();
            System.setProperty(keys.get(1), UUID.randomUUID().toString());
            assertFalse(RecurrentPlanControl.request("swapped", player, 3, 2));
            assertNotEquals(oldEpoch, RecurrentPlanControl.command("alpha").epoch());
            Files.writeString(RecurrentPlanControl.directory("alpha").resolve("state.properties"), "x".repeat(65537));
            assertThrows(IllegalStateException.class, () -> RecurrentPlanControl.state("alpha"));
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

    @Test void commandBindsEpochRecipientMenuRevisionAndRejectsDuplicates() {
        var player = UUID.randomUUID();
        var command = new RecurrentPlanControl.Command("epoch", 2, "grid", "alpha", player.toString(), 3, 4);
        assertTrue(command.matches("epoch", player, 3, 4, 1));
        assertFalse(command.matches("old", player, 3, 4, 1));
        assertFalse(command.matches("epoch", UUID.randomUUID(), 3, 4, 1));
        assertFalse(command.matches("epoch", player, 2, 4, 1));
        assertFalse(command.matches("epoch", player, 3, 3, 1));
        assertFalse(command.matches("epoch", player, 3, 4, 2));
        assertFalse(new RecurrentPlanControl.Command("epoch", 2, "grid", "other", player.toString(), 3, 4)
                .matches("epoch", player, 3, 4, 1));
        assertFalse(new RecurrentPlanControl.Command("epoch", 2, "grid", "alpha", player.toString(), 3, 0)
                .matches("epoch", player, 3, 0, 1));
    }
}
