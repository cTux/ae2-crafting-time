package com.ctux.ae2craftingtime.integration;

import com.ctux.ae2craftingtime.core.IntegrationContract;
import java.util.List;
import java.util.regex.Pattern;

public final class NeoEcoStatusMixinPlugin extends IntegrationMixinPlugin {
    private static final String GUI = "cn/dancingsnow/neoecoae/gui/computation/";
    private static final String PANEL = GUI + "ComputationCpuStatusPanel";
    private static final String ENTRY = GUI + "ComputationCpuItemEntry";
    private static final String PROGRESS = "cn/dancingsnow/neoecoae/crafting/display/processing/ECOProcessingProgress";
    private static final String HOLDER = "Lnet/minecraft/core/HolderLookup$Provider;";
    private static final String TAG = "Lnet/minecraft/nbt/CompoundTag;";

    private static final class Ui {
        private static final boolean SUPPORTED = IntegrationContract.check(List.of(
                method("cn/dancingsnow/neoecoae/api/me/ECOCraftingCPU", "getGrid", "()Lappeng/api/networking/IGrid;"),
                method(PANEL + "$ServerState", "snapshot", "()" + TAG),
                method(PANEL + "$ServerState", "page",
                        "(Lappeng/api/networking/crafting/ICraftingCPU;Ljava/util/List;IILjava/util/Map;II)Lnet/minecraft/nbt/ListTag;"),
                method(ENTRY, "key", "()Lappeng/api/stacks/AEKey;"),
                method(ENTRY, "active", "()Ljava/math/BigInteger;"),
                method(ENTRY, "pending", "()Ljava/math/BigInteger;"),
                method(ENTRY, "read", "(" + TAG + HOLDER + ")L" + ENTRY + ";"),
                method(ENTRY, "withProcessing", "(L" + PROGRESS + ";)L" + ENTRY + ";"),
                method(PANEL, "itemDescription", "(L" + ENTRY + ";Z)Ljava/util/List;"),
                method(GUI + "ComputationCards", "item", "(Ljava/util/List;L" + ENTRY + ";Z)"
                        + "Lcn/dancingsnow/neoecoae/platform/ui/runtime/gui/ui/utils/ModularUITooltipComponent;")),
                IntegrationMixinPlugin::bytecode) == null;
    }

    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        return super.shouldApplyMixin(target, mixin) && Ui.SUPPORTED;
    }

    private static IntegrationContract.Member method(String owner, String name, String descriptor) {
        return new IntegrationContract.Member(owner, "method:" + name, Pattern.quote(descriptor));
    }
}
