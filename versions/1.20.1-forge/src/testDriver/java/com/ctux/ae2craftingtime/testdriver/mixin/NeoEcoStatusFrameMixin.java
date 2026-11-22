package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusEntry;
import com.ctux.ae2craftingtime.testdriver.NeoEcoStatusFixture;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.gui.computation.ComputationCpuStatusPanel", remap = false)
public abstract class NeoEcoStatusFrameMixin {
    @Shadow private long estimatedRemainingMillis;
    @Shadow private int selectedSerial;

    @Inject(method = "drawBackgroundAdditional", at = @At("HEAD"), require = 0)
    private void beginFrame(CallbackInfo callback) {
        NeoEcoStatusFixture.beginFrame();
    }

    @Inject(method = "drawBackgroundAdditional", at = @At("TAIL"), require = 0)
    private void observeRenderedEstimate(CallbackInfo callback) {
        NeoEcoStatusFixture.rendered(estimatedRemainingMillis, selectedSerial);
    }

    @Inject(method = "drawItem", at = @At("HEAD"), require = 0)
    private static void observeItem(@Coerce Object context, Font font, @Coerce Object entry,
            float x, float y, boolean colored, CallbackInfo callback) {
        if (NeoEcoStatusFixture.observeRows() && entry instanceof NeoEcoStatusEntry row) {
            NeoEcoStatusFixture.drawKey(row.key(), Math.round(x) + 54, Math.round(y) + 10);
        }
    }

    @Inject(method = "drawItemDescription", at = @At("HEAD"), require = 0)
    private static void observeDescription(@Coerce Object context, Font font, List<Component> lines,
            float x, float y, CallbackInfo callback) {
        NeoEcoStatusFixture.drawLines(lines);
    }
}
