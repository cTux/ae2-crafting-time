package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.hooks.ticking.TickHandler;
import appeng.me.service.CraftingService;
import com.ctux.ae2craftingtime.core.CraftingJobRecovery;
import com.ctux.ae2craftingtime.core.TrinityJobAccounting;
import com.ctux.ae2craftingtime.core.TrinityQuantityBounds;
import com.ctux.ae2craftingtime.mc1201.IntegrationLog;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.SavedEstimateTag;
import com.ctux.ae2craftingtime.mc1201.TrinityNativeJobAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The checked 3.3.5 native worker boundary; all mutations remain owned by Trinity. */
@Pseudo
@Mixin(targets = "com.fish_dan_.data_energistics.common.crafting.trinity.execution.cpu.TrinityDataCoreCpuLogic", remap = false)
public abstract class TrinityCpuLogicMixin implements TrinityNativeJobAccess.Observer {
    @Unique private static final String EXACT = "Lcom/fish_dan_/data_energistics/common/crafting/trinity/execution/state/inventory/TrinityExactKeyInventory;";
    @Unique private static final String LONG_EXTRACT = EXACT + "extract(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J";
    @Unique private static final String EXACT_EXTRACT = EXACT + "extract(Lappeng/api/stacks/AEKey;Ljava/math/BigInteger;Lappeng/api/config/Actionable;)Ljava/math/BigInteger;";
    @Unique private final TrinityJobAccounting<AEKey> ae2craftingtime$accounting = new TrinityJobAccounting<>();
    @Unique private final CraftingJobRecovery ae2craftingtime$recovery = ProfilerBridge.newRecovery();
    @Unique private Object ae2craftingtime$finishing;
    @Unique private boolean ae2craftingtime$warned;
    @Unique private boolean ae2craftingtime$restored;
    @Unique private boolean ae2craftingtime$recoveringReusable;
    @Unique private CraftingJobRecovery.Lifecycle.Stamp ae2craftingtime$generation;

    @Inject(method = "trySubmitJob", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$submitted(IGrid grid, ICraftingPlan plan, IActionSource source,
            ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        if (cir.getReturnValue() == null || !cir.getReturnValue().successful()) return;
        ae2craftingtime$observe(() -> {
            var cpu = TrinityNativeJobAccess.cpu(this);
            var job = TrinityNativeJobAccess.job(this);
            if (cpu == null || TrinityNativeJobAccess.number(cpu) <= 0 || job == null
                    || grid == null || grid.getPivot() == null || TrinityNativeJobAccess.grid(cpu) != grid) return;
            ae2craftingtime$recovery.clear();
            ae2craftingtime$accounting.begin(TrinityNativeJobAccess.id(job));
            ae2craftingtime$generation = ProfilerBridge.recoveryStamp(cpu);
            TrinityNativeJobAccess.observeJob(job, this);
            ae2craftingtime$warned = false;
            ae2craftingtime$restored = false;
            // A job accepted while disabled must stay excluded even if options change before its first tick.
            if (!ProfilerBridge.trackingEnabled(cpu)) {
                ae2craftingtime$accounting.reject(TrinityNativeJobAccess.id(job));
                ae2craftingtime$warned = true;
                return;
            }
            if (!ae2craftingtime$planFits(plan)) {
                ae2craftingtime$accounting.reject(TrinityNativeJobAccess.id(job));
                ae2craftingtime$checkUnsupported(cpu);
                return;
            }
            ProfilerBridge.startJob(ProfilerBridge.networkId(grid), cpu, plan, tick(), System.nanoTime(),
                    ProfilerBridge.jobOwner(source));
            IntegrationLog.cpu("data_energistics", "cpu-submit");
        });
    }

    @Inject(method = "commitPatternPush", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$committedOrdinary(@Coerce Object job, @Coerce Object progress,
            @Coerce Object commit, CallbackInfo ci) {
        ae2craftingtime$committed(job, commit);
    }

    @Inject(method = "commitTrinityPatternPush", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$committedCounted(@Coerce Object job, @Coerce Object execution,
            @Coerce Object work, long offer, @Coerce Object commit, CallbackInfo ci) {
        ae2craftingtime$committed(job, commit);
    }

    @Unique private void ae2craftingtime$committed(Object job, Object commit) {
        ae2craftingtime$observe(() -> ae2craftingtime$start(job,
                TrinityNativeJobAccess.expected(commit, "expectedOutputs"),
                TrinityNativeJobAccess.expected(commit, "expectedContainerItems"), 1));
    }

    @Inject(method = "lambda$dispatchExactBatch$29", at = @At("RETURN"), remap = false)
    private static void ae2craftingtime$exactOutput(@Coerce Object job, AEKey key, BigInteger amount, CallbackInfo ci) {
        TrinityNativeJobAccess.exactCommitted(job, key, amount);
    }

    @Inject(method = "lambda$dispatchExactBatch$30", at = @At("RETURN"), remap = false)
    private static void ae2craftingtime$exactContainer(@Coerce Object job, AEKey key, BigInteger amount, CallbackInfo ci) {
        TrinityNativeJobAccess.exactCommitted(job, key, amount);
    }

    @Override public void exactCommitted(Object job, AEKey key, BigInteger amount) {
        ae2craftingtime$observe(() -> ae2craftingtime$startAmounts(job, Map.of(key, amount)));
    }

    @Inject(method = "accountReusable", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$reusable(@Coerce Object job, @Coerce Object work,
            @Coerce Object submission, ObjectList<?> physical, CallbackInfo ci) {
        // The native ledger invokes this only after accepted, exactly-once ownership transfer.
        if (ae2craftingtime$recoveringReusable) return;
        ae2craftingtime$observe(() -> ae2craftingtime$start(job,
                TrinityNativeJobAccess.products(submission), TrinityNativeJobAccess.remainders(submission),
                TrinityNativeJobAccess.count(submission)));
    }

    @Inject(method = "recoverReusableAccounting", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$beforeReusableRecovery(@Coerce Object session, @Coerce Object entry,
            CallbackInfo ci) { ae2craftingtime$recoveringReusable = true; }

    @Inject(method = "recoverReusableAccounting", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$afterReusableRecovery(@Coerce Object session, @Coerce Object entry,
            CallbackInfo ci) { ae2craftingtime$recoveringReusable = false; }

    @WrapOperation(method = "insert", at = @At(value = "INVOKE", target = LONG_EXTRACT, ordinal = 1), remap = false)
    private long ae2craftingtime$exactAccepted(@Coerce Object waiting, AEKey key, long requested,
            Actionable mode, Operation<Long> original) {
        long accepted = original.call(waiting, key, requested, mode);
        ae2craftingtime$accepted(waiting, key, BigInteger.valueOf(accepted), mode);
        return accepted;
    }

    @WrapOperation(method = "insert", at = @At(value = "INVOKE", target = LONG_EXTRACT, ordinal = 2), remap = false)
    private long ae2craftingtime$dynamicAccepted(@Coerce Object waiting, AEKey planned, long requested,
            Actionable mode, Operation<Long> original) {
        long accepted = original.call(waiting, planned, requested, mode);
        ae2craftingtime$accepted(waiting, planned, BigInteger.valueOf(accepted), mode);
        return accepted;
    }

    @WrapOperation(method = "insertExact", at = @At(value = "INVOKE", target = EXACT_EXTRACT), remap = false)
    private BigInteger ae2craftingtime$exactAccepted(@Coerce Object waiting, AEKey key, BigInteger requested,
            Actionable mode, Operation<BigInteger> original) {
        BigInteger accepted = original.call(waiting, key, requested, mode);
        ae2craftingtime$accepted(waiting, key, accepted, mode);
        return accepted;
    }

    @WrapOperation(method = "drainNoOutputVirtualCompletions", at = @At(value = "INVOKE",
            target = EXACT_EXTRACT, ordinal = 1), remap = false)
    private BigInteger ae2craftingtime$logicalCompletion(@Coerce Object waiting, AEKey key,
            BigInteger requested, Actionable mode, Operation<BigInteger> original) {
        BigInteger accepted = original.call(waiting, key, requested, mode);
        ae2craftingtime$accepted(waiting, key, accepted, mode);
        return accepted;
    }

    @WrapOperation(method = "lambda$prepareReusableWithdrawal$52", at = @At(value = "INVOKE",
            target = EXACT_EXTRACT), remap = false)
    private static BigInteger ae2craftingtime$cancelledSuffix(@Coerce Object waiting, AEKey key,
            BigInteger requested, Actionable mode, Operation<BigInteger> original,
            @Coerce Object job, AEKey withdrawnKey, BigInteger withdrawnAmount) {
        BigInteger removed = original.call(waiting, key, requested, mode);
        if (mode == Actionable.MODULATE && removed != null && removed.signum() > 0) {
            // Static native lambda carries its job; ownership was registered at submit/read.
            TrinityNativeJobAccess.waitingWithdrawn(job, waiting, key, removed);
        }
        return removed;
    }

    @Override public void waitingWithdrawn(Object job, Object waiting, AEKey key, BigInteger removed) {
        ae2craftingtime$observe(() -> {
            if (job == null || job != TrinityNativeJobAccess.job(this)
                    || waiting != TrinityNativeJobAccess.waiting(job)) return;
            var cpu = TrinityNativeJobAccess.cpu(this);
            var grid = TrinityNativeJobAccess.grid(cpu);
            if (!ae2craftingtime$eligible(cpu, grid)) return;
            long amount = ae2craftingtime$accounting.withdraw(TrinityNativeJobAccess.id(job), key, removed);
            if (amount > 0) ProfilerBridge.withdraw(ProfilerBridge.networkId(grid), cpu, key, amount);
            ae2craftingtime$checkUnsupported(cpu);
        });
    }

    @Unique private void ae2craftingtime$accepted(Object waiting, AEKey key, BigInteger accepted, Actionable mode) {
        if (mode != Actionable.MODULATE || accepted == null || accepted.signum() <= 0) return;
        ae2craftingtime$observe(() -> {
            var job = TrinityNativeJobAccess.job(this);
            if (job == null || waiting != TrinityNativeJobAccess.waiting(job)) return;
            var cpu = TrinityNativeJobAccess.cpu(this);
            var grid = TrinityNativeJobAccess.grid(cpu);
            if (!ae2craftingtime$eligible(cpu, grid)) return;
            ProfilerBridge.restoreEstimate(cpu, ae2craftingtime$recovery, ProfilerBridge.recoveryServer(),
                    TrinityNativeJobAccess.id(job), ae2craftingtime$outputKey(job), grid, true);
            long amount = ae2craftingtime$accounting.complete(TrinityNativeJobAccess.id(job), key, accepted);
            if (amount > 0) {
                ProfilerBridge.complete(ProfilerBridge.networkId(grid), cpu, key, amount, tick(),
                        TrinityNativeJobAccess.server(grid));
                IntegrationLog.positive("data_energistics", "cpu-output", amount);
            } else if (ae2craftingtime$restored) {
                try {
                    ProfilerBridge.completeRestoredEstimate(ProfilerBridge.networkId(grid), cpu, key,
                            accepted.longValueExact());
                } catch (ArithmeticException overflow) {
                    ae2craftingtime$accounting.reject(TrinityNativeJobAccess.id(job));
                }
            }
            ae2craftingtime$checkUnsupported(cpu);
        });
    }

    @Inject(method = "finishJob", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$beforeFinish(boolean success, CallbackInfo ci) {
        ae2craftingtime$captureTerminalJob();
    }

    @Inject(method = "finishJob", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$afterFinish(boolean success, CallbackInfo ci) {
        ae2craftingtime$finishTerminalJob(success);
    }

    @Inject(method = "abortFailedSubmission", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$beforeFailedSubmission(CallbackInfo ci) {
        ae2craftingtime$captureTerminalJob();
    }

    @Inject(method = "abortFailedSubmission", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$afterFailedSubmission(CallbackInfo ci) {
        ae2craftingtime$finishTerminalJob(false);
    }

    @Unique private void ae2craftingtime$captureTerminalJob() {
        ae2craftingtime$observe(() -> ae2craftingtime$finishing = TrinityNativeJobAccess.job(this));
    }

    @Unique private void ae2craftingtime$finishTerminalJob(boolean success) {
        ae2craftingtime$observe(() -> {
            var old = ae2craftingtime$finishing;
            ae2craftingtime$finishing = null;
            if (old == null || TrinityNativeJobAccess.job(this) != null) return;
            var cpu = TrinityNativeJobAccess.cpu(this);
            boolean profiled = !ae2craftingtime$accounting.unsupported();
            ae2craftingtime$recovery.clear();
            ae2craftingtime$accounting.clear();
            TrinityNativeJobAccess.forgetJob(old);
            ae2craftingtime$restored = false;
            ae2craftingtime$generation = null;
            ProfilerBridge.finishJob(cpu, success && profiled, tick(), System.nanoTime(),
                    TrinityNativeJobAccess.server(TrinityNativeJobAccess.grid(cpu)));
            IntegrationLog.cpu("data_energistics", "cpu-finish");
        });
    }

    @Inject(method = "writeToTag", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$save(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> cir) {
        ae2craftingtime$observe(() -> {
            var job = TrinityNativeJobAccess.job(this);
            if (job == null) return;
            var cpu = TrinityNativeJobAccess.cpu(this);
            SavedEstimateTag.write(cir.getReturnValue(), ProfilerBridge.saveEstimate(cpu, ae2craftingtime$recovery,
                    ProfilerBridge.recoveryServer(), TrinityNativeJobAccess.id(job), ae2craftingtime$outputKey(job),
                    TrinityNativeJobAccess.grid(cpu)));
        });
    }

    @Inject(method = "readFromTag", at = @At("RETURN"), remap = false)
    private void ae2craftingtime$read(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        ae2craftingtime$observe(() -> {
            var cpu = TrinityNativeJobAccess.cpu(this);
            if (ae2craftingtime$generation == null)
                ae2craftingtime$generation = ProfilerBridge.recoveryStamp(cpu);
            ae2craftingtime$accounting.clear();
            ae2craftingtime$warned = false;
            ae2craftingtime$restored = true;
            ProfilerBridge.readEstimate(cpu, ae2craftingtime$recovery, SavedEstimateTag.read(data));
            var job = TrinityNativeJobAccess.job(this);
            if (job != null) {
                ae2craftingtime$accounting.begin(TrinityNativeJobAccess.id(job));
                TrinityNativeJobAccess.observeJob(job, this);
            }
        });
    }

    @Inject(method = "tickCraftingLogic", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$recoverBeforeWork(IEnergyService energy, CraftingService service,
            @Coerce Object window, @Coerce Object budget, CallbackInfo ci) {
        ae2craftingtime$recover();
    }

    @Inject(method = "dispatchStep", at = @At("HEAD"), remap = false)
    private void ae2craftingtime$recoverBeforeStep(IEnergyService energy, CraftingService service,
            @Coerce Object window, @Coerce Object budget, CallbackInfoReturnable<?> cir) {
        ae2craftingtime$recover();
    }

    @Unique private void ae2craftingtime$recover() {
        ae2craftingtime$observe(() -> {
            var job = TrinityNativeJobAccess.job(this);
            if (job == null) return;
            var cpu = TrinityNativeJobAccess.cpu(this);
            if (ae2craftingtime$stale(job, cpu)) return;
            if (!ProfilerBridge.trackingEnabled(cpu)) {
                if (ae2craftingtime$accounting.reject(TrinityNativeJobAccess.id(job))) {
                    ae2craftingtime$recovery.clear();
                    ae2craftingtime$warned = true;
                    ae2craftingtime$restored = false;
                    ProfilerBridge.discardUnsupportedJob(cpu);
                    ProfilerBridge.discardDisabledScope(cpu, tick(),
                            TrinityNativeJobAccess.server(TrinityNativeJobAccess.grid(cpu)));
                }
                return;
            }
            if (ae2craftingtime$accounting.unsupported()) return;
            var grid = TrinityNativeJobAccess.grid(cpu);
            if (!ae2craftingtime$eligible(cpu, grid)) return;
            ProfilerBridge.restoreEstimate(cpu, ae2craftingtime$recovery, ProfilerBridge.recoveryServer(),
                    TrinityNativeJobAccess.id(job), ae2craftingtime$outputKey(job), grid, false);
        });
    }

    @Unique private void ae2craftingtime$start(Object job, List<GenericStack> first,
            List<GenericStack> second, long multiplier) {
        var amounts = new HashMap<AEKey, BigInteger>();
        for (var list : List.of(first, second)) {
            for (var stack : list) amounts.merge(stack.what(), BigInteger.valueOf(stack.amount())
                    .multiply(BigInteger.valueOf(multiplier)), BigInteger::add);
        }
        ae2craftingtime$startAmounts(job, amounts);
    }

    @Unique private boolean ae2craftingtime$planFits(ICraftingPlan plan) {
        try {
            var totals = new HashMap<AEKey, BigInteger>();
            for (var entry : plan.emittedItems())
                totals.merge(entry.getKey(), BigInteger.valueOf(entry.getLongValue()), BigInteger::add);
            for (var entry : plan.patternTimes().entrySet()) {
                if (entry.getValue() < 0) return false;
                for (var output : entry.getKey().getOutputs())
                    totals.merge(output.what(), BigInteger.valueOf(output.amount())
                            .multiply(BigInteger.valueOf(entry.getValue())), BigInteger::add);
            }
            return ae2craftingtime$fits(totals)
                    && ae2craftingtime$fits(TrinityNativeJobAccess.exactPlannedOutputs(plan));
        } catch (ArithmeticException error) {
            return false;
        }
    }

    @Unique private void ae2craftingtime$startAmounts(Object job, Map<AEKey, BigInteger> amounts) {
        if (job == null || job != TrinityNativeJobAccess.job(this)) return;
        if (ae2craftingtime$restored) return;
        var cpu = TrinityNativeJobAccess.cpu(this);
        var grid = TrinityNativeJobAccess.grid(cpu);
        if (!ae2craftingtime$eligible(cpu, grid)) return;
        if (!ae2craftingtime$fits(amounts)) {
            ae2craftingtime$accounting.reject(TrinityNativeJobAccess.id(job));
            ae2craftingtime$checkUnsupported(cpu);
            return;
        }
        String network = ProfilerBridge.networkId(grid);
        for (var entry : ae2craftingtime$accounting.start(TrinityNativeJobAccess.id(job), amounts,
                pending -> ae2craftingtime$fits(pending.entrySet().stream().collect(
                        java.util.stream.Collectors.toMap(Map.Entry::getKey,
                                entry -> BigInteger.valueOf(entry.getValue()))))).entrySet()) {
            ProfilerBridge.start(network, cpu, entry.getKey(), entry.getValue(), tick());
            IntegrationLog.cpu("data_energistics", "cpu-dispatch");
        }
        ae2craftingtime$checkUnsupported(cpu);
    }

    @Unique private boolean ae2craftingtime$eligible(Object cpu, IGrid grid) {
        return cpu != null && !ae2craftingtime$stale(TrinityNativeJobAccess.job(this), cpu)
                && TrinityNativeJobAccess.number(cpu) > 0 && grid != null && grid.getPivot() != null
                && ProfilerBridge.trackingEnabled(cpu);
    }

    @Unique private boolean ae2craftingtime$stale(Object job, Object cpu) {
        if (ae2craftingtime$generation == null
                || ae2craftingtime$generation.equals(ProfilerBridge.recoveryStamp(cpu))) return false;
        if (job != null && ae2craftingtime$accounting.reject(TrinityNativeJobAccess.id(job))) {
            ae2craftingtime$recovery.clear();
            ae2craftingtime$restored = false;
            ae2craftingtime$warned = true;
            ProfilerBridge.discardUnsupportedJob(cpu);
        }
        return true;
    }

    @Unique private AEKey ae2craftingtime$outputKey(Object job) {
        var output = TrinityNativeJobAccess.output(job);
        return output == null ? null : output.what();
    }

    @Unique private boolean ae2craftingtime$fits(Map<AEKey, BigInteger> amounts) {
        return TrinityQuantityBounds.fits(amounts, key -> key.getId().toString(), AEKey::getAmountPerUnit);
    }

    @Unique private void ae2craftingtime$checkUnsupported(Object cpu) {
        if (ae2craftingtime$accounting.unsupported() && !ae2craftingtime$warned) {
            ae2craftingtime$warned = true;
            ProfilerBridge.discardUnsupportedJob(cpu);
            LoggerFactory.getLogger("ae2craftingtime").warn(
                    "Trinity job profiling disabled: committed quantity exceeds supported long range");
        }
    }

    @Unique private void ae2craftingtime$observe(Runnable observation) {
        try { observation.run(); }
        catch (RuntimeException error) {
            Object cpu;
            try { cpu = TrinityNativeJobAccess.cpu(this); }
            catch (RuntimeException ignored) { cpu = null; }
            if (cpu != null) ProfilerBridge.discardUnsupportedJob(cpu);
            try {
                var job = TrinityNativeJobAccess.job(this);
                if (job != null) ae2craftingtime$accounting.reject(TrinityNativeJobAccess.id(job));
            } catch (RuntimeException ignored) { /* The native error remains diagnostic-only. */ }
            if (!ae2craftingtime$warned) {
                ae2craftingtime$warned = true;
                LoggerFactory.getLogger("ae2craftingtime").warn("Trinity job observer rejected a changed contract", error);
            }
        }
    }

    @Unique private static long tick() { return TickHandler.instance().getCurrentTick(); }
}
