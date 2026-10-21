package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.crafting.execution.CraftingSubmitResult;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.fish_dan_.data_energistics.blockentity.trinity.TrinityDataCoreBlockEntity;
import com.fish_dan_.data_energistics.common.crafting.trinity.execution.cpu.TrinityDataCoreVirtualCpu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.concurrent.Future;

/** A real oversized AE2 calculation must be rejected by native CPU storage admission. */
final class DataEnergisticsTrinityAdmissionFixture extends DataEnergisticsTrinityFixture {
    private Future<ICraftingPlan> oversized;
    private boolean rejected;
    private int admissionCellSlot = -1;
    private long seededSand;
    private long sandBefore;
    private long sandInserted;
    private Map<String, Long> admissionAmounts = Map.of();

    @Override boolean admissionRejected() { return rejected; }

    @Override
    protected Map<String, Long> nativeVariantAmounts(IGrid grid) { return admissionAmounts; }

    @Override
    protected boolean finishVariant(ServerPlayer player, IGrid grid, Placement placement) {
        if (rejected) return true;
        var host = (TrinityDataCoreBlockEntity) player.serverLevel().getBlockEntity(placement.origin());
        var coordinator = host.getCpuPartitions().stream().filter(cpu -> cpu.number() == 0)
                .findFirst().orElseThrow();
        long capacity = coordinator.getAvailableStorage();
        if (capacity <= 0 || capacity >= 1_000_000)
            throw new IllegalStateException("formed Trinity CPU has no bounded finite storage admission case: " + capacity);
        var drive = (DriveBlockEntity) player.serverLevel().getBlockEntity(placement.terminal().offset(-2, -1, 0));
        if (drive == null || !drive.getMainNode().isActive() || drive.getMainNode().getGrid() != grid)
            return false;
        var cells = drive.getInternalInventory();
        if (admissionCellSlot < 0) {
            for (int slot = 0; slot < cells.size(); slot++) {
                if (cells.getStackInSlot(slot).isEmpty()) {
                    admissionCellSlot = slot;
                    cells.setItemDirect(slot, AEItems.ITEM_CELL_256K.stack());
                    grid.getStorageService().invalidateCache();
                    return false;
                }
            }
            throw new IllegalStateException("admission fixture needs an empty grid drive slot");
        }
        if (!cells.getStackInSlot(admissionCellSlot).is(AEItems.ITEM_CELL_256K.asItem()))
            throw new IllegalStateException("admission fixture cell was replaced");
        var input = AEItemKey.of(Items.SAND);
        var storage = grid.getStorageService().getInventory();
        var source = IActionSource.ofPlayer(player);
        long requiredSand = capacity + 1;
        if (seededSand == 0) {
            var cell = drive.getOriginalCellInventory(admissionCellSlot);
            if (cell == null) return false;
            long present = storage.getAvailableStacks().get(input);
            long missing = Math.max(0, requiredSand - present);
            if (cell.insert(input, missing, Actionable.SIMULATE, source) != missing
                    || cell.insert(input, missing, Actionable.MODULATE, source) != missing)
                throw new IllegalStateException("admission fixture could not store real oversized sand input: " + missing);
            seededSand = requiredSand;
            sandBefore = present;
            sandInserted = missing;
            grid.getStorageService().invalidateCache();
            return false;
        }
        long actualSand = storage.getAvailableStacks().get(input);
        if (actualSand < seededSand) return false;
        if (oversized == null) {
            oversized = grid.getCraftingService().beginCraftingCalculation(player.serverLevel(),
                    () -> IActionSource.ofPlayer(player), outputKey(), requiredSand,
                    CalculationStrategy.REPORT_MISSING_ITEMS);
            return false;
        }
        if (!oversized.isDone()) return false;
        ICraftingPlan plan;
        try { plan = oversized.get(); }
        catch (Exception error) { throw new IllegalStateException("native oversized AE2 plan failed", error); }
        if (plan.simulation() || !plan.missingItems().isEmpty()
                || plan.finalOutput().amount() != requiredSand)
            throw new IllegalStateException("oversized native plan is not a complete real craft: simulation="
                    + plan.simulation() + ", missing=" + plan.missingItems() + ", output="
                    + plan.finalOutput().amount() + ", sand=" + actualSand);
        if (plan.bytes() <= capacity)
            throw new IllegalStateException("oversized native plan did not exceed CPU storage: "
                    + plan.bytes() + " <= " + capacity);
        var result = coordinator.submitJob(grid, plan, IActionSource.ofPlayer(player), null);
        if (result != CraftingSubmitResult.CPU_TOO_SMALL
                || host.getCpuPartitions().stream().anyMatch(TrinityDataCoreVirtualCpu::isBusy)
                || ProfilerBridge.hasPending(new ProfileKey(ProfilerBridge.networkId(grid), outputId(placement, null)))
                || ProfilerBridge.stats(new ProfileKey(ProfilerBridge.networkId(grid), outputId(placement, null)))
                        .map(stats -> stats.sampleCount()).orElse(0) != 0)
            throw new IllegalStateException("oversized native plan was not rejected without profiling: " + result);
        long sandAfterRejection = storage.getAvailableStacks().get(input);
        if (sandAfterRejection != actualSand)
            throw new IllegalStateException("oversized native rejection changed stored input: "
                    + actualSand + " -> " + sandAfterRejection);
        admissionAmounts = Map.of("admissionCapacity", capacity, "requestedOutput", requiredSand,
                "sandBefore", sandBefore, "sandInserted", sandInserted, "sandAtPlan", actualSand,
                "planBytes", plan.bytes(), "planSimulation", plan.simulation() ? 1L : 0L,
                "planMissing", plan.missingItems().isEmpty() ? 0L : 1L,
                "nativeCpuTooSmall", 1L, "sandAfterRejection", sandAfterRejection);
        rejected = true;
        return true;
    }
}
