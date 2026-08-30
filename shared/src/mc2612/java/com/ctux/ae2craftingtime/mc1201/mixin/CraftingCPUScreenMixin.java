package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.me.crafting.CraftingStatusScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.me.crafting.CraftingCPUMenu;
import appeng.menu.me.crafting.CraftingStatus;
import appeng.menu.me.crafting.CraftingStatusEntry;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.core.TtcSort;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.StatsClickHandler;
import com.ctux.ae2craftingtime.mc1201.StatsChatMessages;
import com.ctux.ae2craftingtime.mc1201.TtcBadge;
import com.ctux.ae2craftingtime.mc1201.TtcDetailsClick;
import com.ctux.ae2craftingtime.mc1201.TtcDetailsKeyMapping;
import com.ctux.ae2craftingtime.mc1201.TtcSortButton;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;

@Mixin(CraftingCPUScreen.class)
public abstract class CraftingCPUScreenMixin<T extends CraftingCPUMenu> extends AEBaseScreen<T>
        implements StatsClickHandler {
    @Unique
    private static final int AE2CRAFTINGTIME_TABLE_X = 9;
    @Unique
    private static final int AE2CRAFTINGTIME_TABLE_Y = 19;
    @Unique
    private static final int AE2CRAFTINGTIME_CELL_WIDTH = 67;
    @Unique
    private static final int AE2CRAFTINGTIME_CELL_HEIGHT = 22;
    @Unique
    private static final int AE2CRAFTINGTIME_CELL_BORDER = 1;
    @Unique
    private static final int AE2CRAFTINGTIME_COLS = 3;
    @Unique
    private static final int AE2CRAFTINGTIME_ROWS = 6;
    @Unique
    private static final int AE2CRAFTINGTIME_SCREEN_WIDTH = 238;
    @Unique
    private static final int AE2CRAFTINGTIME_TITLE_PADDING = 8;
    @Unique
    private static final int AE2CRAFTINGTIME_TITLE_TOP = 7;
    @Unique
    private static final int AE2CRAFTINGTIME_TITLE_TTC_COLOR = 0xE0E0E0;

    @Unique
    private int ae2craftingtime$ttcSortMode = 2;
    @Unique
    private Component ae2craftingtime$titleTtc;
    @Unique
    private int ae2craftingtime$titleTtcX;

    @Shadow(remap = false)
    private CraftingStatus status;

    @Shadow(remap = false)
    private Scrollbar scrollbar;

    protected CraftingCPUScreenMixin(T menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (TtcDetailsClick.tryHandle(event)) {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$addStatusTtcSortButton(T menu, Inventory playerInventory, Component title,
            ScreenStyle style, CallbackInfo ci) {
        if ((Object) this instanceof CraftingStatusScreen) {
            addToLeftToolbar(new TtcSortButton(this::ae2craftingtime$cycleTtcSortMode,
                    () -> ae2craftingtime$ttcSortMode));
        }
    }

    @Group(name = "sortStatusByTtc", min = 1, max = 1)
    @ModifyArg(method = "postUpdate", at = @At(value = "INVOKE", target = "Lappeng/menu/me/crafting/CraftingStatus;<init>(ZJJJLjava/util/List;)V"), index = 4, remap = false, require = 0)
    private List<CraftingStatusEntry> ae2craftingtime$sortStatusByTtcLegacy(List<CraftingStatusEntry> entries) {
        return ae2craftingtime$sortStatusByTtc(entries);
    }

    @Group(name = "sortStatusByTtc", min = 1, max = 1)
    @ModifyArg(method = "postUpdate", at = @At(value = "INVOKE", target = "Lappeng/menu/me/crafting/CraftingStatus;<init>(ZJJJLjava/util/List;Z)V"), index = 4, remap = false, require = 0)
    private List<CraftingStatusEntry> ae2craftingtime$sortStatusByTtcWithSuspended(List<CraftingStatusEntry> entries) {
        return ae2craftingtime$sortStatusByTtc(entries);
    }

    @Unique
    private List<CraftingStatusEntry> ae2craftingtime$sortStatusByTtc(List<CraftingStatusEntry> entries) {
        if (!((Object) this instanceof CraftingStatusScreen) || ae2craftingtime$ttcSortMode == 0) {
            return entries;
        }

        return TtcSort.copySorted(entries, CraftingCPUScreenMixin::ae2craftingtime$seconds,
                Comparator.naturalOrder(), ae2craftingtime$ttcSortMode == 2);
    }

    @SuppressWarnings("mapping")
    @ModifyArg(
            method = "updateBeforeRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/client/gui/me/crafting/CraftingCPUScreen;setTextContent(Ljava/lang/String;Lnet/minecraft/network/chat/Component;)V",
                    remap = true),
            index = 1,
            remap = false)
    private Component ae2craftingtime$appendStatusTotalTtc(Component title) {
        ae2craftingtime$titleTtc = null;
        if (status == null) {
            return title;
        }

        var eta = TimeEstimate.formatTotal(List.of(TimeEstimate.progressSeconds(
                status.getElapsedTime(), status.getStartItemCount(), status.getRemainingItemCount())));
        if (eta.isEmpty()) {
            return title;
        }

        var separator = Component.literal("  ");
        var total = TtcText.ttc(eta.get())
                .withStyle(style -> style.withColor(TextColor.fromRgb(AE2CRAFTINGTIME_TITLE_TTC_COLOR)));
        var font = getMinecraft().font;
        var availableWidth = AE2CRAFTINGTIME_SCREEN_WIDTH - AE2CRAFTINGTIME_TITLE_PADDING * 2;
        if (font.width(title) + font.width(separator) + font.width(total) > availableWidth) {
            return title;
        }

        ae2craftingtime$titleTtc = total;
        ae2craftingtime$titleTtcX = AE2CRAFTINGTIME_TITLE_PADDING + font.width(title) + font.width(separator);
        return title;
    }

    @Inject(method = "drawFG", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$drawTitleTtcBadge(GuiGraphicsExtractor guiGraphics, int offsetX, int offsetY, int mouseX,
            int mouseY, CallbackInfo ci) {
        if (ae2craftingtime$titleTtc == null) {
            return;
        }

        var font = getMinecraft().font;
        var textWidth = font.width(ae2craftingtime$titleTtc);
        TtcBadge.fillRoundedRect(guiGraphics, ae2craftingtime$titleTtcX - 2, AE2CRAFTINGTIME_TITLE_TOP - 2,
                ae2craftingtime$titleTtcX + textWidth + 2, AE2CRAFTINGTIME_TITLE_TOP + font.lineHeight + 2,
                TtcBadge.BACKGROUND);
        guiGraphics.text(font, ae2craftingtime$titleTtc, ae2craftingtime$titleTtcX,
                AE2CRAFTINGTIME_TITLE_TOP, 0xFF000000 | AE2CRAFTINGTIME_TITLE_TTC_COLOR, true);
    }

    @Unique
    private void ae2craftingtime$cycleTtcSortMode() {
        ae2craftingtime$ttcSortMode = (ae2craftingtime$ttcSortMode + 1) % 3;
        if (status != null) {
            postUpdate(status);
        }
    }

    @Shadow(remap = false)
    public abstract void postUpdate(CraftingStatus status);

    @Override
    public boolean ae2craftingtime$handleClickedStats(double mouseX, double mouseY, int button, boolean reset) {
        if (status == null) {
            return false;
        }

        var entries = status.getEntries();
        var entry = ae2craftingtime$clickedEntry(mouseX, mouseY, entries);
        if (entry == null) {
            return false;
        }

        var amount = entry.getActiveAmount() + entry.getPendingAmount();
        if (amount <= 0) {
            return false;
        }

        var key = ProfilerBridge.key(entry.getWhat());
        if (reset) {
            StatsChatMessages.reset(key, entry.getWhat().getDisplayName().getString());
            return true;
        }
        StatsChatMessages.show(key, entry.getWhat().getDisplayName().getString(),
                AeKeyAmounts.normalize(entry.getWhat(), amount));
        return true;
    }

    @Unique
    private CraftingStatusEntry ae2craftingtime$clickedEntry(double mouseX, double mouseY,
            List<CraftingStatusEntry> entries) {
        var index = ae2craftingtime$clickedTableIndex(mouseX, mouseY);
        if (index >= 0 && index < entries.size()) {
            return entries.get(index);
        }

        var hovered = getStackUnderMouse(mouseX, mouseY);
        var hoveredKey = hovered == null ? null : ProfilerBridge.key(hovered.stack().what());
        if (hoveredKey == null) {
            return null;
        }
        for (var entry : entries) {
            if (hoveredKey.equals(ProfilerBridge.key(entry.getWhat()))) {
                return entry;
            }
        }
        return null;
    }

    @Unique
    private int ae2craftingtime$clickedTableIndex(double mouseX, double mouseY) {
        var x = (int) mouseX - getGuiLeft() - AE2CRAFTINGTIME_TABLE_X;
        var y = (int) mouseY - getGuiTop() - AE2CRAFTINGTIME_TABLE_Y;
        var pitchX = AE2CRAFTINGTIME_CELL_WIDTH + AE2CRAFTINGTIME_CELL_BORDER;
        var pitchY = AE2CRAFTINGTIME_CELL_HEIGHT + AE2CRAFTINGTIME_CELL_BORDER;
        if (x < 0 || y < 0 || x % pitchX >= AE2CRAFTINGTIME_CELL_WIDTH || y % pitchY >= AE2CRAFTINGTIME_CELL_HEIGHT) {
            return -1;
        }

        var col = x / pitchX;
        var row = y / pitchY;
        if (col >= AE2CRAFTINGTIME_COLS || row >= AE2CRAFTINGTIME_ROWS) {
            return -1;
        }
        return (row + scrollbar.getCurrentScroll()) * AE2CRAFTINGTIME_COLS + col;
    }

    @Unique
    private static OptionalLong ae2craftingtime$seconds(CraftingStatusEntry entry) {
        var amount = entry.getActiveAmount() + entry.getPendingAmount();
        if (amount <= 0) {
            return OptionalLong.empty();
        }

        var key = ProfilerBridge.key(entry.getWhat());
        ClientStatsRequests.request(key);
        var stats = ClientStats.CACHE.get(key);
        if (stats.isEmpty()) {
            return OptionalLong.empty();
        }

        return TimeEstimate.seconds(AeKeyAmounts.normalize(entry.getWhat(), amount), stats.get());
    }
}
