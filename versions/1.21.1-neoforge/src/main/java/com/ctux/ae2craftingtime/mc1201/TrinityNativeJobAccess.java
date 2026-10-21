package com.ctux.ae2craftingtime.mc1201;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.CraftingLink;
import com.ctux.ae2craftingtime.core.IntegrationRead;
import com.ctux.ae2craftingtime.core.CraftingJobEstimate;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.TrinityPlanGraph;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import net.minecraft.server.MinecraftServer;

/** Narrow reflective access to the checked, package-private Trinity 3.3.5 job API. */
public final class TrinityNativeJobAccess {
    private static final String PLAN_CLASS =
            "com.fish_dan_.data_energistics.common.crafting.trinity.planning.plan.TrinityCraftingPlan";
    private static final String CPU = "cpu";
    private static final String JOB = "job";
    private static final Map<Object, WeakReference<Observer>> OBSERVERS = new WeakHashMap<>();

    public interface Observer {
        void exactCommitted(Object job, AEKey key, BigInteger amount);
        void waitingWithdrawn(Object job, Object waiting, AEKey key, BigInteger amount);
    }

    public static synchronized void observeJob(Object job, Observer observer) {
        if (job != null) OBSERVERS.put(job, new WeakReference<>(observer));
    }

    public static synchronized void forgetJob(Object job) { if (job != null) OBSERVERS.remove(job); }

    public static void exactCommitted(Object job, AEKey key, BigInteger amount) {
        Observer observer = observer(job);
        if (observer != null) observer.exactCommitted(job, key, amount);
    }

    public static void waitingWithdrawn(Object job, Object waiting, AEKey key, BigInteger amount) {
        Observer observer = observer(job);
        if (observer != null) observer.waitingWithdrawn(job, waiting, key, amount);
    }

    private static synchronized Observer observer(Object job) {
        WeakReference<Observer> reference = OBSERVERS.get(job);
        return reference == null ? null : reference.get();
    }

    public static Object cpu(Object logic) { return field(logic, CPU); }
    public static Object job(Object logic) { return field(logic, JOB); }
    public static Object waiting(Object job) { return job == null ? null : field(job, "waitingFor"); }
    public static GenericStack output(Object job) {
        return job == null ? null : (GenericStack) field(job, "finalOutput");
    }
    public static UUID id(Object job) {
        return job == null ? null : ((CraftingLink) field(job, "link")).getCraftingID();
    }
    public static IGrid grid(Object cpu) { return cpu == null ? null : (IGrid) call(cpu, "grid"); }
    public static int number(Object cpu) { return cpu == null ? 0 : ((Number) call(cpu, "number")).intValue(); }
    public static MinecraftServer server(IGrid grid) {
        return grid == null || grid.getPivot() == null ? null : grid.getPivot().getLevel().getServer();
    }
    public static long count(Object submission) { return ((Number) call(submission, "count")).longValue(); }
    @SuppressWarnings("unchecked")
    public static List<GenericStack> products(Object submission) {
        return (List<GenericStack>) call(call(submission, "outputs"), "products");
    }
    @SuppressWarnings("unchecked")
    public static List<GenericStack> remainders(Object submission) {
        return (List<GenericStack>) call(call(submission, "outputs"), "remainders");
    }
    @SuppressWarnings("unchecked")
    public static List<GenericStack> expected(Object commit, String name) {
        return (List<GenericStack>) call(commit, name);
    }
    @SuppressWarnings("unchecked")
    public static Map<AEKey, BigInteger> exactPlannedOutputs(ICraftingPlan plan) {
        return isTrinityPlan(plan)
                ? (Map<AEKey, BigInteger>) call(plan, "plannedOutputs") : Map.of();
    }

    public static boolean isTrinityPlan(ICraftingPlan plan) {
        return plan != null && isTrinityPlanClassName(plan.getClass().getName());
    }

    static boolean isTrinityPlanClassName(String name) { return PLAN_CLASS.equals(name); }

    /** Native firing inputs/outputs retain dependencies absent from ICraftingPlan.patternTimes(). */
    @SuppressWarnings("unchecked")
    public static CraftingJobEstimate.DependencyGraph scheduledGraph(ICraftingPlan plan, String networkId,
            Set<String> plannedOutputIds) {
        var crafted = new HashSet<ProfileKey>();
        plannedOutputIds.forEach(id -> crafted.add(new ProfileKey(networkId, id)));
        var patterns = new ArrayList<CraftingJobEstimate.Pattern>();
        boolean incomplete = !((List<?>) call(plan, "cycleRepeatBlocks")).isEmpty();
        for (var stage : (List<?>) call(plan, "stages")) {
            incomplete |= (Boolean) call(stage, "cycleStage");
            incomplete |= !((it.unimi.dsi.fastutil.ints.IntSet) call(stage, "dependencies")).isEmpty();
            for (var firing : (List<?>) call(stage, "firings")) {
                var outputs = ((Map<AEKey, BigInteger>) call(firing, "outputs")).keySet().stream()
                        .map(key -> ProfilerBridge.key(networkId, key)).toList();
                var inputs = ((Map<AEKey, BigInteger>) call(firing, "inputs")).keySet().stream()
                        .map(key -> Set.of(ProfilerBridge.key(networkId, key))).toList();
                patterns.add(new CraftingJobEstimate.Pattern(1, outputs, inputs));
            }
        }
        return TrinityPlanGraph.project(crafted, patterns, incomplete);
    }

    private static Object field(Object owner, String name) {
        return IntegrationRead.field(owner, name, Object.class);
    }

    private static Object call(Object owner, String name) {
        return IntegrationRead.invoke(owner, name, Object.class);
    }

    private TrinityNativeJobAccess() { }
}
