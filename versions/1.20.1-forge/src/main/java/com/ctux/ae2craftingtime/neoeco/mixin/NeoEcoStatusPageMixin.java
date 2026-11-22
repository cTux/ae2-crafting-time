package com.ctux.ae2craftingtime.neoeco.mixin;

import appeng.api.networking.crafting.ICraftingCPU;
import com.ctux.ae2craftingtime.core.NeoEcoStatusRows;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import com.ctux.ae2craftingtime.mc1201.AeKeyAmounts;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.ServerOptionsRuntime;
import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusCpu;
import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusEntry;
import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusPayload;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.ListTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.gui.computation.ComputationCpuStatusPanel$ServerState", remap = false)
public abstract class NeoEcoStatusPageMixin {
    @WrapOperation(method = "snapshot", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lcn/dancingsnow/neoecoae/gui/computation/ComputationCpuStatusPanel$ServerState;"
                    + "page(Lappeng/api/networking/crafting/ICraftingCPU;Ljava/util/List;IILjava/util/Map;II)Lnet/minecraft/nbt/ListTag;"),
            require = 0)
    private ListTag ae2craftingtime$itemPage(@Coerce Object state, ICraftingCPU cpu, List<?> entries, int from,
            int to, Map<?, ?> visible, int visibleFrom, int visibleTo, Operation<ListTag> original) {
        var page = original.call(state, cpu, entries, from, to, visible, visibleFrom, visibleTo);
        if (!(cpu instanceof NeoEcoStatusCpu eco) || !cpu.isBusy()
                || !ProfilerBridge.tracksExternalJob(cpu) || ProfilerBridge.isSuspended(cpu)) return page;
        var grid = eco.getGrid();
        if (grid == null || page.size() != to - Math.min(from, to)) return page;
        var networkId = ProfilerBridge.networkId(grid);
        if (networkId.isEmpty()) return page;
        for (int index = 0; index < page.size(); index++) {
            if (!(entries.get(from + index) instanceof NeoEcoStatusEntry row)) continue;
            var key = row.key();
            if (key == null || !ServerOptionsRuntime.keyEnabled(key)) continue;
            var amount = NeoEcoStatusRows.remaining(row.active(), row.pending());
            if (amount <= 0) continue;
            var tag = page.getCompound(index);
            ProfilerBridge.stats(ProfilerBridge.key(networkId, key))
                    .flatMap(stats -> TimeEstimate.milliseconds(AeKeyAmounts.normalize(key, amount), stats))
                    .ifPresent(estimate -> NeoEcoStatusPayload.write(tag, estimate));
        }
        return page;
    }
}
