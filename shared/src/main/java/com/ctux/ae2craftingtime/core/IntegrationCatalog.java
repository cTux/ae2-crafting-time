package com.ctux.ae2craftingtime.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Explicit API order, never dependency-version string order. */
public final class IntegrationCatalog {
    private IntegrationCatalog() {}

    private static final Set<String> PRE_26 = Set.of("1.20.1-forge", "1.20.1-fabric", "1.21.1-neoforge");
    private static final String ECO = "cn/dancingsnow/neoecoae/api/me/ECOCraftingCPULogic";
    private static final String BATCH = "(Lcn/dancingsnow/neoecoae/api/me/ExecutingCraftingJob;"
            + "Lcn/dancingsnow/neoecoae/impl/crafting/fastpath/ECOExtractedPatternExecution;";

    public static final List<IntegrationSelection.Candidate> CANDIDATES = List.of(
            new IntegrationSelection.Candidate("appliedenhancements", "aelis-plan-114", Set.of("1.20.1-forge"), true,
                    Set.of("AelisPlanScreenMixin", "AelisPlanLayoutMixin"), aelis()),
            new IntegrationSelection.Candidate("ae2ct", "tree-layout", PRE_26, true,
                    Set.of("CraftingTreeNewWidgetMixin"), treeLayout()),
            new IntegrationSelection.Candidate("ae2ct", "tree-helper", PRE_26, true,
                    Set.of("CraftingTreeWidgetMixin"), treeHelper()),
            eco("batched-long", "1.20.1-forge", "NeoEcoLongBatchDispatchMixin", BATCH + "JZ)V"),
            eco("pending-accounting", "1.20.1-forge", "NeoEcoPendingDispatchMixin",
                    "(L" + ECO + "$PendingPatternAccounting;)V"),
            eco("batched-int", "1.21.1-neoforge", "NeoEcoIntBatchDispatchMixin", BATCH + "I)V"),
            advanced("advanced-cpu-1201", "1.20.1-forge", "AdvancedCpuEstimate1201Mixin"),
            advanced("advanced-cpu-1211", "1.21.1-neoforge", "AdvancedCpuEstimate1211Mixin"),
            advanced("advanced-cpu-26", "26.1.2-neoforge", "AdvancedCpuEstimate26Mixin"),
            new IntegrationSelection.Candidate("ae2lt", "time-wheel", Set.of("1.20.1-forge", "1.21.1-neoforge"),
                    false, Set.of("Ae2LtTimeWheelCraftingCpuLogicMixin", "Ae2LtEstimateMixin"),
                    List.of(new IntegrationContract.Member("com/moakiee/ae2lt/crafting/timewheel/"
                            + "Ae2LtTimeWheelCraftingCpuLogic", "", ""))),
            singleton("merequester", "requester-screen", PRE_26, true, "MERequesterScreenMixin",
                    "com/almostreliable/merequester/client/abstraction/AbstractRequesterScreen"));

    private static IntegrationSelection.Candidate singleton(String dependency, String variant, Set<String> targets,
            boolean client, String mixin, String owner) {
        return new IntegrationSelection.Candidate(dependency, variant, targets, client, Set.of(mixin),
                List.of(new IntegrationContract.Member(owner, "", "")));
    }

    private static List<IntegrationContract.Member> aelis() {
        var screen = "com/appliedenhancements/client/plan/AelisCraftConfirmScreen";
        var layout = "com/appliedenhancements/client/plan/AelisPlanLayout";
        var row = "com/appliedenhancements/client/plan/AelisPlanRows$Row";
        var members = new ArrayList<IntegrationContract.Member>();
        members.add(method(screen, "drawFG", "(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"));
        members.add(method(screen, "drawBG", "(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V"));
        members.add(method(screen, "m_88315_", "(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"));
        members.add(method(screen, "rowUnderMouse", "(DD)L" + row + ";"));
        for (var name : List.of("rows", "visible"))
            members.add(new IntegrationContract.Member(screen, "field:" + name, "Ljava/util/List;"));
        members.add(new IntegrationContract.Member(screen, "field:scroll", "I"));
        members.add(new IntegrationContract.Member(screen, "field:view", "L" + screen + "\\$View;"));
        members.add(new IntegrationContract.Member(screen, "field:layout", "L" + layout + ";"));
        members.add(method(layout, "fit", "(II)L" + layout + ";"));
        members.add(method(layout, "width", "()I"));
        members.add(method(layout, "visibleRows", "()I"));
        members.add(method(layout, "columns", "(Z)L" + layout + "$Columns;"));
        members.add(method(layout + "$Columns", "<init>", "(IZ)V"));
        members.add(method(row, "key", "()Ljava/lang/Object;"));
        for (var name : List.of("craft", "missing", "cyclic"))
            members.add(method(row, name, "()Ljava/math/BigInteger;"));
        return members;
    }

    private static IntegrationSelection.Candidate advanced(String variant, String target, String estimateMixin) {
        return new IntegrationSelection.Candidate("advanced_ae", variant, Set.of(target), false,
                Set.of("AdvancedCraftingCpuLogicMixin", estimateMixin),
                List.of(new IntegrationContract.Member("net/pedroksl/advanced_ae/common/logic/AdvCraftingCPULogic",
                        "", "")));
    }

    private static IntegrationSelection.Candidate eco(String variant, String target, String mixin, String descriptor) {
        return new IntegrationSelection.Candidate("neoecoae", variant, Set.of(target), false,
                Set.of("ECOCraftingCpuLogicMixin", "NeoEcoEstimateMixin", mixin),
                List.of(method(ECO, "recordPushedPattern", descriptor),
                        method(ECO, "getCraftingJobId", "()Ljava/util/UUID;")));
    }

    private static List<IntegrationContract.Member> widget(String owner, String node, String clickReturn) {
        var members = new ArrayList<IntegrationContract.Member>();
        for (var field : List.of("outputX", "outputY", "spacingX", "spacingY")) {
            members.add(new IntegrationContract.Member(owner, "field:" + field, "I"));
        }
        // Minecraft GUI names differ between production mappings; addon-owned types do not.
        members.add(new IntegrationContract.Member(owner, "method:draw", "\\(L[^;]+;IIII\\)V"));
        members.add(new IntegrationContract.Member(owner, "method:drawNode",
                "\\(L[^;]+;" + Pattern.quote("L" + node + ";)V")));
        members.add(method(owner, "mouseClicked", "(DDI)" + clickReturn));
        return members;
    }

    private static List<IntegrationContract.Member> treeHelper() {
        var owner = "com/neuvillette/ae2ct/gui/CraftingTreeWidget";
        var node = "com/neuvillette/ae2ct/api/CraftingTreeHelper$Node";
        var members = widget(owner, node, "Z");
        members.add(method(owner, "getMousePoint", "(DD)Ljava/awt/Point;"));
        members.add(new IntegrationContract.Member(node, "", ""));
        return members;
    }

    private static List<IntegrationContract.Member> treeLayout() {
        var owner = "com/vcwdfca/ae2ct/gui/CraftingTreeWidget";
        var entry = "com/vcwdfca/ae2ct/tree/LegacyTreeLayout$Entry";
        var members = widget(owner, entry, "V");
        members.add(new IntegrationContract.Member(owner, "method:updateTooltip", "\\(L[^;]+;II\\)V"));
        members.add(method(owner, "getMouseEntry", "(DD)L" + entry + ";"));
        members.add(method(entry, "node", "()Lcom/vcwdfca/ae2ct/tree/LegacyTreeNode;"));
        members.add(method(entry, "point", "()Ljava/awt/Point;"));
        return members;
    }

    private static IntegrationContract.Member method(String owner, String name, String descriptor) {
        return new IntegrationContract.Member(owner, "method:" + name, Pattern.quote(descriptor));
    }
}
