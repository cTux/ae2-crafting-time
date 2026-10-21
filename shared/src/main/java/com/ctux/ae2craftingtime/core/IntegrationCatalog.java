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
    private static final String TRINITY = "com/fish_dan_/data_energistics/common/crafting/trinity/execution/cpu/";
    private static final String TRINITY_LOGIC = TRINITY + "TrinityDataCoreCpuLogic";
    private static final String TRINITY_JOB = "L" + TRINITY + "TrinityDataCoreExecutingCraftingJob;";
    private static final String TRINITY_INV = "com/fish_dan_/data_energistics/common/crafting/trinity/execution/state/inventory/TrinityExactKeyInventory";
    private static final String TRINITY_EXTRACT_LONG = "(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J";
    private static final String TRINITY_EXTRACT_EXACT = "(Lappeng/api/stacks/AEKey;Ljava/math/BigInteger;Lappeng/api/config/Actionable;)Ljava/math/BigInteger;";

    public static final List<IntegrationSelection.Candidate> CANDIDATES = List.of(
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
            new IntegrationSelection.Candidate("data_energistics", "trinity-3.3.5",
                    Set.of("1.21.1-neoforge"), false, Set.of("TrinityCpuLogicMixin"), trinity()),
            new IntegrationSelection.Candidate("data_energistics_trinity_ui", "trinity-material-3.3.5",
                    Set.of("1.21.1-neoforge"), true, Set.of("TrinityMaterialTableMixin"), List.of(method(
                    "com/fish_dan_/data_energistics/client/screen/crafting/confirm/TrinityCraftConfirmMaterialTable",
                    "drawEntry", "(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/Minecraft;"
                            + "Lnet/minecraft/client/gui/Font;Lappeng/menu/me/crafting/CraftingPlanSummaryEntry;II)V"))),
            singleton("merequester", "requester-screen", PRE_26, true, "MERequesterScreenMixin",
                    "com/almostreliable/merequester/client/abstraction/AbstractRequesterScreen"));

    private static IntegrationSelection.Candidate singleton(String dependency, String variant, Set<String> targets,
            boolean client, String mixin, String owner) {
        return new IntegrationSelection.Candidate(dependency, variant, targets, client, Set.of(mixin),
                List.of(new IntegrationContract.Member(owner, "", "")));
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

    private static IntegrationContract.Member staticMethod(String owner, String name, String descriptor) {
        return new IntegrationContract.Member(owner, "static:method:" + name, Pattern.quote(descriptor));
    }

    private static IntegrationContract.Member field(String owner, String name, String descriptor) {
        return new IntegrationContract.Member(owner, "field:" + name, Pattern.quote(descriptor));
    }

    private static IntegrationContract.Member call(String method, String descriptor, String calledOwner,
            String calledName, String calledDescriptor, int count) {
        return new IntegrationContract.Member(TRINITY_LOGIC,
                "call:" + method + descriptor + "->" + calledOwner + "#" + calledName + calledDescriptor,
                Integer.toString(count));
    }

    private static List<IntegrationContract.Member> trinity() {
        var contract = new ArrayList<IntegrationContract.Member>();
        var cpu = "L" + TRINITY + "TrinityDataCoreVirtualCpu;";
        var commit = "L" + TRINITY_LOGIC + "$PreparedPatternCommit;";
        var work = "Lcom/fish_dan_/data_energistics/common/crafting/trinity/execution/state/TrinityPlanExecution$Work;";
        var execution = "Lcom/fish_dan_/data_energistics/common/crafting/trinity/execution/state/TrinityPlanExecution;";
        var submission = "Lcom/fish_dan_/data_energistics/common/crafting/trinity/reusable/cpu/ReusableCpuSessionLedger$Submission;";
        var steps = "(Lappeng/api/networking/energy/IEnergyService;Lappeng/me/service/CraftingService;"
                + "Lcom/fish_dan_/data_energistics/common/crafting/trinity/dispatch/commit/CraftingDispatchWindow;"
                + "Lcom/fish_dan_/data_energistics/common/crafting/trinity/dispatch/governor/CraftingDispatchBudget;)";
        var waiting = "(" + TRINITY_JOB + "Lit/unimi/dsi/fastutil/objects/ObjectList;)V";
        contract.add(field(TRINITY_LOGIC, "cpu", cpu));
        contract.add(field(TRINITY_LOGIC, "job", TRINITY_JOB));
        contract.add(field(TRINITY + "TrinityDataCoreExecutingCraftingJob", "link", "Lappeng/crafting/CraftingLink;"));
        contract.add(field(TRINITY + "TrinityDataCoreExecutingCraftingJob", "finalOutput", "Lappeng/api/stacks/GenericStack;"));
        contract.add(field(TRINITY + "TrinityDataCoreExecutingCraftingJob", "waitingFor", "L" + TRINITY_INV + ";"));
        contract.add(method(TRINITY_LOGIC + "$PreparedPatternCommit", "expectedOutputs",
                "()Lit/unimi/dsi/fastutil/objects/ObjectList;"));
        contract.add(method(TRINITY_LOGIC + "$PreparedPatternCommit", "expectedContainerItems",
                "()Lit/unimi/dsi/fastutil/objects/ObjectList;"));
        var ledger = "com/fish_dan_/data_energistics/common/crafting/trinity/reusable/cpu/ReusableCpuSessionLedger";
        contract.add(method(ledger + "$Submission", "count", "()J"));
        contract.add(method(ledger + "$Submission", "outputs", "()L" + ledger + "$OutputContract;"));
        contract.add(method(ledger + "$OutputContract", "products",
                "()Lit/unimi/dsi/fastutil/objects/ObjectList;"));
        contract.add(method(ledger + "$OutputContract", "remainders",
                "()Lit/unimi/dsi/fastutil/objects/ObjectList;"));
        contract.add(method(TRINITY + "TrinityDataCoreVirtualCpu", "grid", "()Lappeng/api/networking/IGrid;"));
        contract.add(method(TRINITY + "TrinityDataCoreVirtualCpu", "number", "()I"));
        contract.add(field(TRINITY + "TrinityDataCoreVirtualCpu", "logic", "L" + TRINITY_LOGIC + ";"));
        contract.add(method("com/fish_dan_/data_energistics/common/crafting/trinity/planning/plan/TrinityCraftingPlan",
                "plannedOutputs", "()Ljava/util/Map;"));
        contract.add(method(TRINITY_LOGIC, "trySubmitJob", "(Lappeng/api/networking/IGrid;"
                + "Lappeng/api/networking/crafting/ICraftingPlan;Lappeng/api/networking/security/IActionSource;"
                + "Lappeng/api/networking/crafting/ICraftingRequester;)Lappeng/api/networking/crafting/ICraftingSubmitResult;"));
        contract.add(method(TRINITY_LOGIC, "commitPatternPush", "(" + TRINITY_JOB + "L" + TRINITY
                + "TrinityDataCoreExecutingCraftingJob$TaskProgress;" + commit + ")V"));
        contract.add(method(TRINITY_LOGIC, "commitTrinityPatternPush", "(" + TRINITY_JOB + execution
                + work + "J" + commit + ")V"));
        contract.add(method(TRINITY_LOGIC, "accountReusable", "(" + TRINITY_JOB + work + submission
                + "Lit/unimi/dsi/fastutil/objects/ObjectList;)V"));
        contract.add(method(TRINITY_LOGIC, "recoverReusableAccounting",
                "(Lcom/fish_dan_/data_energistics/common/crafting/trinity/reusable/cpu/ReusableCpuSessionLedger$Session;"
                + "Lcom/fish_dan_/data_energistics/common/crafting/trinity/reusable/cpu/ReusableCpuSessionLedger$SubmissionEntry;)V"));
        contract.add(method(TRINITY_LOGIC, "addWaiting", waiting));
        contract.add(method(TRINITY_LOGIC, "dispatchStep", steps
                + "Lcom/fish_dan_/data_energistics/common/crafting/trinity/dispatch/server/CraftingDispatchStepResult;"));
        contract.add(method(TRINITY_LOGIC, "tickCraftingLogic", steps + "V"));
        contract.add(method(TRINITY_LOGIC, "insert", "(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J"));
        contract.add(method(TRINITY_LOGIC, "insertExact", TRINITY_EXTRACT_EXACT));
        contract.add(method(TRINITY_LOGIC, "drainNoOutputVirtualCompletions", "(" + TRINITY_JOB + ")Z"));
        contract.add(method(TRINITY_LOGIC, "finishJob", "(Z)V"));
        contract.add(method(TRINITY_LOGIC, "abortFailedSubmission", "()V"));
        contract.add(method(TRINITY_LOGIC, "writeToTag", "(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;"));
        contract.add(method(TRINITY_LOGIC, "readFromTag", "(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V"));
        contract.add(staticMethod(TRINITY_LOGIC, "lambda$dispatchExactBatch$29", "(" + TRINITY_JOB
                + "Lappeng/api/stacks/AEKey;Ljava/math/BigInteger;)V"));
        contract.add(staticMethod(TRINITY_LOGIC, "lambda$dispatchExactBatch$30", "(" + TRINITY_JOB
                + "Lappeng/api/stacks/AEKey;Ljava/math/BigInteger;)V"));
        contract.add(staticMethod(TRINITY_LOGIC, "lambda$prepareReusableWithdrawal$52", "(" + TRINITY_JOB
                + "Lappeng/api/stacks/AEKey;Ljava/math/BigInteger;)V"));
        contract.add(call("commitPatternPush", "(" + TRINITY_JOB + "L" + TRINITY
                + "TrinityDataCoreExecutingCraftingJob$TaskProgress;" + commit + ")V",
                TRINITY_LOGIC, "addWaiting", waiting, 2));
        contract.add(call("commitTrinityPatternPush", "(" + TRINITY_JOB + execution + work + "J" + commit + ")V",
                TRINITY_LOGIC, "addWaiting", waiting, 2));
        contract.add(call("insert", TRINITY_EXTRACT_LONG, TRINITY_INV, "extract", TRINITY_EXTRACT_LONG, 3));
        contract.add(call("insertExact", TRINITY_EXTRACT_EXACT, TRINITY_INV, "extract", TRINITY_EXTRACT_EXACT, 1));
        contract.add(call("drainNoOutputVirtualCompletions", "(" + TRINITY_JOB + ")Z",
                TRINITY_INV, "extract", TRINITY_EXTRACT_EXACT, 2));
        for (var suffix : List.of("29", "30", "52")) {
            var methodName = suffix.equals("52") ? "lambda$prepareReusableWithdrawal$52" : "lambda$dispatchExactBatch$" + suffix;
            contract.add(call(methodName, "(" + TRINITY_JOB + "Lappeng/api/stacks/AEKey;Ljava/math/BigInteger;)V",
                    TRINITY_INV, suffix.equals("52") ? "extract" : "insert",
                    suffix.equals("52") ? TRINITY_EXTRACT_EXACT : TRINITY_EXTRACT_EXACT.substring(0, TRINITY_EXTRACT_EXACT.length() - "Ljava/math/BigInteger;".length()) + "V", 1));
        }
        return List.copyOf(contract);
    }
}
