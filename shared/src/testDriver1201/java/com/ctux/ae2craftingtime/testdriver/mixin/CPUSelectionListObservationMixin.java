package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.client.Point;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.testdriver.UiObservationStore;
import com.ctux.ae2craftingtime.testdriver.CpuListScrollControl;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CPUSelectionList.class, remap = false)
public abstract class CPUSelectionListObservationMixin {
    @Shadow @Final private CraftingStatusMenu menu;
    @Shadow @Final private Scrollbar scrollbar;
    @Shadow @Final private Blitter buttonBg;
    @Shadow private Rect2i bounds;

    @Inject(method = "drawBackgroundLayer", at = @At("HEAD"))
    private void ae2craftingtime_test_driver$cards(GuiGraphics graphics, Rect2i screenBounds, Point mouse,
            CallbackInfo ci) {
        CpuListScrollControl.bind(scrollbar);
        UiObservationStore.cpuCards(menu.cpuList.cpus(), scrollbar.getCurrentScroll(), menu.getSelectedCpuSerial(),
                screenBounds.getX() + bounds.getX() + 9, screenBounds.getY() + bounds.getY() + 19,
                buttonBg.getSrcWidth(), buttonBg.getSrcHeight());
    }
}
