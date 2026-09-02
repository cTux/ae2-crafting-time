package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.testdriver.VmTextureProbe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.nio.IntBuffer;

@Mixin(value = RenderSystem.class, remap = false)
public abstract class RenderSystemMixin {
    @Redirect(method = "maxSupportedTextureSize", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"))
    private static void ae2ct$probeRectangularAtlas(int target, int level, int internalFormat,
            int width, int height, int border, int format, int type, IntBuffer pixels) {
        int probeHeight = VmTextureProbe.height(Boolean.getBoolean("ae2craftingtime.test.vmTextureProbe"),
                System.getProperty("ae2craftingtime.test.scenario", ""), GL11.glGetString(GL11.GL_RENDERER), width, height);
        GlStateManager._texImage2D(target, level, internalFormat, width, probeHeight, border, format, type, pixels);
    }
}
