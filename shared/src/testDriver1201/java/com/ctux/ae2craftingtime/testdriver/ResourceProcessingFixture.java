package com.ctux.ae2craftingtime.testdriver;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

final class ResourceProcessingFixture {
    record Slot(int index, AEKey key, long amount, String inputId, String provider, String cpu,
            boolean dispatched, long heldAmount, long releasedAmount, boolean cancelled, long tick) { }

    private final StandardCraftFixture fixture;
    private final List<AEKey> outputs;
    private final List<Slot> slots = new ArrayList<>();
    private List<Future<appeng.api.networking.crafting.ICraftingPlan>> plans;
    private final boolean clearSamplesBeforeCase;
    private boolean patternsConfigured;
    private boolean submitted;
    private boolean storageValidated;

    ResourceProcessingFixture(StandardCraftFixture fixture, List<AEKey> outputs) {
        this(fixture, outputs, true);
    }

    ResourceProcessingFixture(StandardCraftFixture fixture, List<AEKey> outputs, boolean clearSamplesBeforeCase) {
        this.fixture = fixture;
        this.outputs = List.copyOf(outputs);
        this.clearSamplesBeforeCase = clearSamplesBeforeCase;
        fixture.configureResourceFixture();
    }

    boolean prepare(ServerPlayer player, FixtureMarker marker, boolean chemical) {
        if (!fixture.prepare(player, marker)) return false;
        if (!patternsConfigured) {
            fixture.configureResourcePatterns(player, outputs, chemical, clearSamplesBeforeCase);
            for (int index = 0; index < outputs.size(); index++) {
                var cpu = fixture.resourceCpus(player).get(index);
                var provider = fixture.resourceProviders().get(0);
                var input = index == 0 ? AEItemKey.of(Items.COBBLESTONE) : AEItemKey.of(Items.SAND);
                slots.add(new Slot(index, outputs.get(index), outputs.get(index).getAmountPerUnit(),
                        input.getId().toString(), provider.toShortString(), cpu.getBlockPos().toShortString(),
                        false, 0, 0, false, 0));
            }
            patternsConfigured = true;
            return false;
        }
        var service = fixture.cpu(player).getMainNode().getGrid().getCraftingService();
        var storage = fixture.cpu(player).getMainNode().getGrid().getStorageService().getInventory();
        storageValidated = outputs.stream().allMatch(key -> storage.insert(key, key.getAmountPerUnit(),
                Actionable.SIMULATE, IActionSource.ofMachine(fixture.cpu(player))) == key.getAmountPerUnit());
        return outputs.stream().allMatch(service::isCraftable) && storageValidated;
    }

    boolean submit(ServerPlayer player) {
        var cpus = fixture.resourceCpus(player);
        var service = fixture.cpu(player).getMainNode().getGrid().getCraftingService();
        if (plans == null) {
            plans = slots.stream().map(slot -> service.beginCraftingCalculation(player.level(),
                    () -> IActionSource.ofMachine(cpus.get(slot.index())), slot.key(), slot.amount(),
                    appeng.api.networking.crafting.CalculationStrategy.REPORT_MISSING_ITEMS)).toList();
            return false;
        }
        if (plans.stream().anyMatch(plan -> !plan.isDone())) return false;
        if (!submitted) {
            for (int index = 0; index < plans.size(); index++) {
                try {
                    var plan = plans.get(index).get();
                    if (plan.simulation()) throw new IllegalStateException("resource fixture plan is missing inputs");
                    var result = service.submitJob(plan, null, cpus.get(index).getCluster(), false,
                            IActionSource.ofMachine(cpus.get(index)));
                    if (!result.successful()) throw new IllegalStateException("resource fixture job was rejected: " + result);
                } catch (Exception error) {
                    throw new IllegalStateException("resource fixture calculation failed", error);
                }
            }
            submitted = true;
        }
        for (int index = 0; index < slots.size(); index++) {
            var current = slots.get(index);
            if (!current.dispatched() && fixture.consumeResourceInput(player, index)) {
                slots.set(index, new Slot(current.index(), current.key(), current.amount(), current.inputId(),
                        current.provider(), current.cpu(), true, current.amount(), 0, false,
                        player.level().getGameTime()));
            }
        }
        return slots.stream().allMatch(Slot::dispatched)
                && cpus.subList(0, slots.size()).stream().allMatch(cpu -> cpu.getCluster().isBusy());
    }

    boolean held(int slot) { return slots.get(slot).heldAmount() > 0 && !slots.get(slot).cancelled(); }

    boolean release(ServerPlayer player, int slot) {
        var current = slots.get(slot);
        if (!current.dispatched()) return false;
        if (current.heldAmount() > 0) {
            long inserted = fixture.cpu(player).getMainNode().getGrid().getStorageService().getInventory().insert(
                    current.key(), current.heldAmount(), Actionable.MODULATE,
                    IActionSource.ofMachine(fixture.resourceCpus(player).get(slot)));
            if (inserted < 0 || inserted > current.heldAmount()) {
                throw new IllegalStateException("resource fixture returned an invalid insertion amount");
            }
            slots.set(slot, afterInsertion(current, inserted));
        }
        return slots.get(slot).heldAmount() == 0
                && !fixture.resourceCpus(player).get(slot).getCluster().isBusy();
    }

    boolean releaseAll(ServerPlayer player) {
        boolean complete = true;
        for (var slot : slots) {
            if (!release(player, slot.index())) complete = false;
        }
        return complete && allReleased(slots);
    }

    boolean delayed(ServerPlayer player) {
        return slots.stream().allMatch(slot -> slot.dispatched() && slot.heldAmount() > 0
                && fixture.resourceDelayed(player, slot.key()));
    }

    boolean cancel(ServerPlayer player, int slot) {
        fixture.resourceCpus(player).get(slot).getCluster().craftingLogic.cancel();
        var current = slots.get(slot);
        slots.set(slot, new Slot(current.index(), current.key(), current.amount(), current.inputId(),
                current.provider(), current.cpu(), current.dispatched(), 0, current.releasedAmount(), true,
                current.tick()));
        return !fixture.resourceCpus(player).get(slot).getCluster().isBusy();
    }

    List<Slot> slots() { return List.copyOf(slots); }
    List<net.minecraft.core.BlockPos> providers() { return fixture.resourceProviders(); }
    boolean storageValidated() { return storageValidated; }

    static Slot afterInsertion(Slot current, long inserted) {
        if (inserted < 0 || inserted > current.heldAmount()) {
            throw new IllegalArgumentException("resource fixture returned an invalid insertion amount");
        }
        return new Slot(current.index(), current.key(), current.amount(), current.inputId(), current.provider(),
                current.cpu(), current.dispatched(), current.heldAmount() - inserted,
                current.releasedAmount() + inserted, current.cancelled(), current.tick());
    }

    static boolean allReleased(List<Slot> values) {
        return values.stream().allMatch(value -> value.heldAmount() == 0);
    }

    void close(ServerPlayer player) {
        close(player, true);
    }

    void close(ServerPlayer player, boolean clearSamples) {
        if (plans != null) plans.stream().filter(plan -> !plan.isDone()).forEach(plan -> plan.cancel(true));
        fixture.cleanupResourceFixture(player, outputs, clearSamples);
        plans = null;
        submitted = false;
        patternsConfigured = false;
        slots.clear();
    }
}
