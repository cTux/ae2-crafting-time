package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.menu.me.common.GridInventoryEntry;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import com.ctux.ae2craftingtime.mc1201.ClientStatsRequests;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.TtcText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(MEStorageScreen.class)
public abstract class WirelessTerminalScreenMixin {
    private static final String TERMINAL_SCREEN = "de.mari_023.ae2wtlib.wct.WCTScreen";
    private static final Set<String> IMPORT_EXPORT_INTERFACES = Set.of(
            "com.ultramega.ae2insertexportcard.util.UpgradeInterface",
            "com.ultramega.ae2importexportcard.util.UpgradeInterface");

    @ModifyVariable(method = "renderGridInventoryEntryTooltip", at = @At("STORE"), ordinal = 0, remap = false)
    private List<Component> ae2craftingtime$appendTtc(List<Component> lines, GuiGraphicsExtractor graphics,
            GridInventoryEntry entry, int x, int y) {
        if ((!getClass().getName().equals(TERMINAL_SCREEN) && !ae2craftingtime$hasImportExportCard())
                || !entry.isCraftable()) {
            return lines;
        }

        var result = new ArrayList<>(lines);
        var key = ProfilerBridge.key(entry.getWhat());
        ClientStatsRequests.request(key);
        ClientStats.CACHE.get(key).ifPresentOrElse(
                stats -> TimeEstimate.format(AeKeyAmounts.normalize(entry.getWhat(), 1), stats)
                        .ifPresentOrElse(eta -> result.add(TtcText.ttc(eta)),
                                () -> result.add(TtcText.ttcCollectingData())),
                () -> result.add(TtcText.ttcCollectingData()));
        return result;
    }

    private boolean ae2craftingtime$hasImportExportCard() {
        for (Class<?> type = ((MEStorageScreen<?>) (Object) this).getMenu().getClass(); type != null;
                type = type.getSuperclass()) {
            for (var contract : type.getInterfaces()) {
                if (IMPORT_EXPORT_INTERFACES.contains(contract.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
