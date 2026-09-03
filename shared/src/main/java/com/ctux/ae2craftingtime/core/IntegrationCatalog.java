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
            new IntegrationSelection.Candidate("ae2ct", "tree-layout", PRE_26, true,
                    Set.of("CraftingTreeNewWidgetMixin"), treeLayout()),
            new IntegrationSelection.Candidate("ae2ct", "tree-helper", PRE_26, true,
                    Set.of("CraftingTreeWidgetMixin"), treeHelper()),
            eco("batched-long", "1.20.1-forge", "NeoEcoLongBatchDispatchMixin", BATCH + "JZ)V"),
            eco("pending-accounting", "1.20.1-forge", "NeoEcoPendingDispatchMixin",
                    "(L" + ECO + "$PendingPatternAccounting;)V"),
            eco("batched-int", "1.21.1-neoforge", "NeoEcoIntBatchDispatchMixin", BATCH + "I)V"),
            singleton("advanced_ae", "advanced-cpu",
                    Set.of("1.20.1-forge", "1.21.1-neoforge", "26.1.2-neoforge"), false,
                    "AdvancedCraftingCpuLogicMixin", "net/pedroksl/advanced_ae/common/logic/AdvCraftingCPULogic"),
            singleton("ae2lt", "time-wheel", Set.of("1.20.1-forge", "1.21.1-neoforge"), false,
                    "Ae2LtTimeWheelCraftingCpuLogicMixin", "com/moakiee/ae2lt/crafting/timewheel/Ae2LtTimeWheelCraftingCpuLogic"),
            singleton("merequester", "requester-screen", PRE_26, true, "MERequesterScreenMixin",
                    "com/almostreliable/merequester/client/abstraction/AbstractRequesterScreen"));

    private static IntegrationSelection.Candidate singleton(String dependency, String variant, Set<String> targets,
            boolean client, String mixin, String owner) {
        return new IntegrationSelection.Candidate(dependency, variant, targets, client, Set.of(mixin),
                List.of(new IntegrationContract.Member(owner, "", "")));
    }

    private static IntegrationSelection.Candidate eco(String variant, String target, String mixin, String descriptor) {
        return new IntegrationSelection.Candidate("neoecoae", variant, Set.of(target), false,
                Set.of("ECOCraftingCpuLogicMixin", mixin), List.of(method(ECO, "recordPushedPattern", descriptor)));
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
