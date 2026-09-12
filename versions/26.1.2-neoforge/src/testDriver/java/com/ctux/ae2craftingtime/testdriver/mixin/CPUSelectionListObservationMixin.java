package com.ctux.ae2craftingtime.testdriver.mixin;

import appeng.client.Point;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.me.crafting.CraftingStatusMenu;
import com.ctux.ae2craftingtime.testdriver.UiObservationStore;
import com.ctux.ae2craftingtime.testdriver.CpuListScrollControl;
import com.ctux.ae2craftingtime.testdriver.CpuListInputControl;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CPUSelectionList.class, remap = false)
public abstract class CPUSelectionListObservationMixin {
    @Shadow @Final private CraftingStatusMenu menu;
    @Shadow @Final private Scrollbar scrollbar;
    @Shadow @Final private Blitter buttonBg;
    @Shadow private Rect2i bounds;
    @Shadow private CraftingStatusMenu.CraftingCpuListEntry hitTestCpu(Point mousePos) { throw new AssertionError(); }
    @Unique private final java.util.Map<Integer, Point> ae2craftingtime_test_driver$points = new java.util.HashMap<>();
    @Unique private Rect2i ae2craftingtime_test_driver$screenBounds;
    @Unique private boolean ae2craftingtime_test_driver$drawn;

    @Inject(method = "updateBeforeRender", at = @At("HEAD"))
    private void ae2craftingtime_test_driver$inputBoundaries(CallbackInfo ci) {
        if (!ae2craftingtime_test_driver$drawn) {
            var first = hitTestCpu(new Point(bounds.getX() + 10, bounds.getY() + 20));
            CpuListInputControl.noFirstDraw(first == null ? null : first.serial());
        }
        var stale = CpuListInputControl.staleSerial();
        if (stale != null && menu.cpuList.cpus().stream().noneMatch(cpu -> cpu.serial() == stale)) {
            var point = ae2craftingtime_test_driver$points.get(stale);
            if (point == null) throw new IllegalStateException("stale CPU was not captured in the preceding draw");
            var hit = hitTestCpu(point);
            ((CPUSelectionList) (Object) this).onMouseUp(point, 0);
            CpuListInputControl.staleResult(hit == null ? null : hit.serial());
        }
        var wheel = CpuListInputControl.wheelSerial();
        var point = wheel == null ? null : ae2craftingtime_test_driver$points.get(wheel);
        if (point != null) {
            ((CPUSelectionList) (Object) this).onMouseWheel(point, -1);
            var hit = hitTestCpu(point);
            ((CPUSelectionList) (Object) this).onMouseUp(point, 0);
            CpuListInputControl.wheelResult(hit == null ? null : hit.serial());
        }
    }

    @Inject(method = "drawBackgroundLayer", at = @At("HEAD"))
    private void ae2craftingtime_test_driver$cards(GuiGraphicsExtractor graphics, Rect2i screenBounds, Point mouse,
            CallbackInfo ci) {
        CpuListScrollControl.bind(scrollbar);
        ae2craftingtime_test_driver$drawn = true;
        ae2craftingtime_test_driver$points.clear();
        ae2craftingtime_test_driver$screenBounds = screenBounds;
        UiObservationStore.beginCpuCards(menu.cpuList.cpus(), scrollbar.getCurrentScroll());
    }

    @WrapOperation(method = "drawBackgroundLayer", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/widgets/CPUSelectionList;getCpuName(Lappeng/menu/me/crafting/CraftingStatusMenu$CraftingCpuListEntry;)Lnet/minecraft/network/chat/Component;"),
            remap = false)
    private Component ae2craftingtime_test_driver$card(CPUSelectionList instance,
            CraftingStatusMenu.CraftingCpuListEntry cpu, Operation<Component> original,
            @Local(ordinal = 0) int x, @Local(ordinal = 1) int y) {
        var name = original.call(instance, cpu);
        ae2craftingtime_test_driver$points.put(cpu.serial(), new Point(
                x - ae2craftingtime_test_driver$screenBounds.getX() + 1,
                y - ae2craftingtime_test_driver$screenBounds.getY() + 1));
        UiObservationStore.cpuCard(cpu, menu.getSelectedCpuSerial(), x, y,
                buttonBg.getSrcWidth(), buttonBg.getSrcHeight());
        return name;
    }
}
