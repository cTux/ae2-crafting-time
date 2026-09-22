package com.ctux.ae2craftingtime.testdriver;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

final class DriverScreenshots {
    static CompletableFuture<Void> capture(Minecraft minecraft, Path path) {
        try (var image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            image.writeToFile(path);
            return CompletableFuture.completedFuture(null);
        } catch (Exception error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    private DriverScreenshots() { }
}
