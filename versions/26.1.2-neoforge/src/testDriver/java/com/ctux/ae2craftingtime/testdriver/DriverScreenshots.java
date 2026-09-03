package com.ctux.ae2craftingtime.testdriver;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

final class DriverScreenshots {
    static CompletableFuture<Void> capture(Minecraft minecraft, Path path) {
        var result = new CompletableFuture<Void>();
        Screenshot.takeScreenshot(minecraft.getMainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(path);
                result.complete(null);
            } catch (Exception error) {
                result.completeExceptionally(error);
            }
        });
        return result;
    }
}
