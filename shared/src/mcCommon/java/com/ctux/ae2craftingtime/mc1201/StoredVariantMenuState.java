package com.ctux.ae2craftingtime.mc1201;

import appeng.api.networking.IGrid;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.me.helpers.StackWatcher;
import appeng.menu.me.crafting.CraftingPlanSummary;
import com.ctux.ae2craftingtime.core.PlanStoredVariantDetector;
import com.ctux.ae2craftingtime.mc1201.mixin.StorageServiceAccessor;
import com.ctux.ae2craftingtime.mc1201.net.PlanStoredVariantsS2C;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/** Transient, menu-owned observation; callbacks only mark it dirty. */
public final class StoredVariantMenuState implements IStorageWatcherNode {
    private CraftingPlanSummary summary;
    private StackWatcher<IStorageWatcherNode> watcher;
    private final com.ctux.ae2craftingtime.core.PlanStoredVariantLifecycle lifecycle =
            new com.ctux.ae2craftingtime.core.PlanStoredVariantLifecycle();
    private final com.ctux.ae2craftingtime.core.PlanStoredVariantUpdateState updates =
            new com.ctux.ae2craftingtime.core.PlanStoredVariantUpdateState();

    public void close() {
        releaseWatcher();
        summary = null;
        updates.reset(0);
        lifecycle.close();
    }

    public void broadcast(CraftingPlanSummary current, IGrid currentGrid, ServerPlayer player,
            int containerId, long summaryRevision, boolean valid) {
        var items = new HashSet<Object>();
        if (current != null && current != summary) {
            for (var entry : current.getEntries()) {
                if (entry.getWhat() instanceof AEItemKey item
                        && com.ctux.ae2craftingtime.core.PlanStoredVariantLifecycle.show(true,
                                entry.getMissingAmount(), true, true)) {
                    items.add(item.getPrimaryKey());
                }
            }
        }
        switch (lifecycle.broadcast(current, currentGrid, valid, items)) {
            case CLOSE -> { close(); return; }
            case NONE -> { return; }
            case CLEAR -> {
                send(player, containerId, summaryRevision, Set.of());
                releaseWatcher();
                return;
            }
            case INSTALL -> {
                releaseWatcher();
                summary = current;
                updates.reset(current.getEntries().size());
                if (!lifecycle.observes()) return;
                watcher = new StackWatcher<>(
                        ((StorageServiceAccessor) currentGrid.getStorageService()).ae2craftingtime$interestManager(), this);
                watcher.setWatchAll(true);
            }
            case REFRESH -> { }
        }
        refresh(current, currentGrid, player, containerId, summaryRevision);
    }

    private void releaseWatcher() {
        if (watcher != null) watcher.destroy();
        watcher = null;
    }

    private void refresh(CraftingPlanSummary current, IGrid currentGrid, ServerPlayer player,
            int containerId, long summaryRevision) {
        var rows = new ArrayList<PlanStoredVariantDetector.Missing<AEKey, Object>>();
        for (var entry : current.getEntries()) {
            rows.add(entry.getWhat() instanceof AEItemKey item
                    ? new PlanStoredVariantDetector.Missing<>(item, item.getPrimaryKey(), entry.getMissingAmount())
                    : null);
        }
        var available = new ArrayList<PlanStoredVariantDetector.Stored<AEKey, Object>>();
        for (var stack : currentGrid.getStorageService().getInventory().getAvailableStacks()) {
            if (stack.getKey() instanceof AEItemKey item) {
                available.add(new PlanStoredVariantDetector.Stored<>(item, item.getPrimaryKey(), stack.getLongValue()));
            }
        }
        send(player, containerId, summaryRevision, PlanStoredVariantDetector.detect(rows, available));
    }

    private void send(ServerPlayer player, int containerId, long summaryRevision, Set<Integer> rows) {
        var entries = summary.getEntries().size();
        for (var update : updates.replace(rows)) {
            StatsNetwork.sendTo(player, new PlanStoredVariantsS2C(update.revision(),
                    new com.ctux.ae2craftingtime.core.PlanRecurrenceChunk(
                            containerId, summaryRevision, entries, update.offset(), update.rowCount(), update.mask())));
        }
        if (updates.exhausted()) {
            releaseWatcher();
            lifecycle.suspend();
        }
    }

    @Override public void updateWatcher(IStackWatcher ignored) {}

    @Override public void onStackChange(AEKey what, long amount) {
        if (what instanceof AEItemKey item) lifecycle.changed(item.getPrimaryKey());
    }
}
