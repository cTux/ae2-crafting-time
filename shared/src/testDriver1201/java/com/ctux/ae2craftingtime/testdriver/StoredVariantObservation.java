package com.ctux.ae2craftingtime.testdriver;

import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.me.helpers.StackWatcher;
import appeng.menu.me.crafting.CraftConfirmMenu;
import appeng.menu.me.crafting.CraftingPlanSummary;
import com.ctux.ae2craftingtime.core.PlanRecurrenceChunk;
import com.ctux.ae2craftingtime.mc1201.RecurrentPlanMenu;
import com.ctux.ae2craftingtime.mc1201.mixin.StorageServiceAccessor;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

/** Driver-only observations of real callbacks, native plans and packet delivery. */
public final class StoredVariantObservation {
    private static final Map<Object, ServerState> SERVERS = new WeakHashMap<>();
    private static final Map<CraftingPlanSummary, ServerState> SUMMARIES = new WeakHashMap<>();
    private static final Map<CraftConfirmMenu, ClientState> CLIENTS = new WeakHashMap<>();
    private static final Map<StackWatcher<IStorageWatcherNode>, ServerState> WATCHERS = new WeakHashMap<>();
    private static int released;
    private static boolean enabled = System.getProperty("ae2ct.testDriver.serverScenario", "")
            .equals("stored-variant-plan-connected");

    static synchronized void enable(boolean value) { enabled = value; }

    private static final class ServerState {
        CraftingPlanSummary summary;
        IGrid grid;
        List<List<Long>> amounts;
        int nodes;
        int notifications;
        int refreshedNotifications;
        int refreshes;
        int idle;
        int sends;
        int menu;
        boolean released;
    }
    private record ClientState(long revision, List<List<Long>> amounts, int packets) {}

    public static synchronized void broadcast(Object owner, CraftingPlanSummary summary, IGrid grid,
            ServerPlayer player, int menu, long revision) {
        if (!enabled) return;
        if (summary == null || grid == null) return;
        var state = SERVERS.get(owner);
        if (state == null || state.summary != summary) {
            state = new ServerState();
            state.summary = summary; state.grid = grid; state.nodes = grid.size(); state.amounts = amounts(summary);
            state.menu = menu;
            SERVERS.put(owner, state); SUMMARIES.put(summary, state);
        }
        if (state.grid == grid && state.nodes != grid.size()) throw new IllegalStateException("Diagnostic changed grid topology");
        if (!state.amounts.equals(amounts(summary))) throw new IllegalStateException("Diagnostic changed native server amounts");
        if (player.containerMenu.containerId != menu || revision <= 0)
            throw new IllegalStateException("Diagnostic observed an unauthorized menu");
        if (state.refreshes > 0 && state.notifications == state.refreshedNotifications) state.idle++;
    }

    public static synchronized void changed(Object owner, AEKey key, long amount) {
        if (!enabled) return;
        var state = SERVERS.get(owner);
        if (state == null || !(key instanceof AEItemKey item)) return;
        if (state.summary.getEntries().stream().anyMatch(row -> row.getMissingAmount() > 0
                && row.getWhat() instanceof AEItemKey wanted && wanted.getPrimaryKey() == item.getPrimaryKey())) {
            state.notifications++;
            System.out.println("AE2CT variant notification key=" + key + " amount=" + amount);
        }
    }

    public static synchronized void refresh(Object owner) {
        if (!enabled) return;
        var state = SERVERS.get(owner);
        if (state == null) throw new IllegalStateException("Variant refresh preceded native summary");
        if (state.refreshes > 0 && state.notifications == state.refreshedNotifications)
            throw new IllegalStateException("Variant rescanned storage without a relevant notification");
        state.refreshedNotifications = state.notifications;
        state.refreshes++;
        System.out.println("AE2CT variant refresh=" + state.refreshes + " notifications=" + state.notifications);
    }

    public static synchronized void registered(Object owner, StackWatcher<IStorageWatcherNode> watcher) {
        if (!enabled) return;
        if (watcher != null) WATCHERS.put(watcher, SERVERS.get(owner));
    }

    public static synchronized void sent(ServerPlayer player, int container, long revision, java.util.Set<Integer> rows) {
        if (!enabled) return;
        if (!(player.containerMenu instanceof CraftConfirmMenu menu) || menu.getPlan() == null
                || menu.containerId != container || revision != ((RecurrentPlanMenu) menu).ae2craftingtime$summaryRevision())
            throw new IllegalStateException("Variant sent to wrong recipient/plan");
        var state = SUMMARIES.get(menu.getPlan());
        if (state != null) state.sends++;
        System.out.println("AE2CT variant send-batch player=" + player.getUUID() + " menu=" + menu.containerId
                + " revision=" + revision + " rows=" + rows);
    }

    public static synchronized void released(Object owner, StackWatcher<IStorageWatcherNode> watcher) {
        if (!enabled) return;
        if (watcher == null) return;
        var state = WATCHERS.remove(watcher);
        if (state == null) throw new IllegalStateException("Released watcher was never observed registered");
        if (((StorageServiceAccessor) state.grid.getStorageService()).ae2craftingtime$interestManager()
                .getAllStacksWatchers().contains(watcher)) throw new IllegalStateException("Destroyed watcher remains registered");
        state.released = true;
        released++;
        System.out.println("AE2CT variant watcher-released=" + released);
    }

    static synchronized boolean verifyServer(CraftingPlanSummary summary) {
        var state = SUMMARIES.get(summary);
        if (state == null || state.refreshes < 2 || state.idle < 2 || state.notifications < 2 || state.sends < 4) return false;
        if (state.refreshes > state.notifications + 1) throw new IllegalStateException("Variant refreshes were not coalesced");
        return true;
    }

    static synchronized boolean closed(int menu) {
        var states = SUMMARIES.values().stream().filter(state -> state.menu == menu).toList();
        return !states.isEmpty() && states.stream().allMatch(state -> state.released);
    }
    static synchronized boolean closed(CraftingPlanSummary summary) {
        var state = SUMMARIES.get(summary);
        return state != null && state.released;
    }

    public static void installed(CraftConfirmMenu menu) {
        if (!enabled) return;
        if (!menu.isClientSide()) return;
        if (menu.getPlan() != null && menu.getPlan().getEntries().stream().anyMatch(entry ->
                ((com.ctux.ae2craftingtime.mc1201.RecurrentPlanEntry) entry).ae2craftingtime$storedVariant()))
            throw new IllegalStateException("Native plan installation retained stale variant flags");
        CLIENTS.put(menu, new ClientState(((RecurrentPlanMenu) menu).ae2craftingtime$summaryRevision(),
                amounts(menu.getPlan()), 0));
    }

    public static void receiving(PlanRecurrenceChunk chunk) {
        if (!enabled) return;
        var minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) throw new IllegalStateException("Variant packet ran outside client executor");
        if (minecraft.player == null || !(minecraft.player.containerMenu instanceof CraftConfirmMenu menu)
                || menu.getPlan() == null) return;
        if (!chunk.validFor(menu.containerId, ((RecurrentPlanMenu) menu).ae2craftingtime$summaryRevision(),
                menu.getPlan().getEntries().size())) return;
        var state = CLIENTS.get(menu);
        if (state == null || state.revision != chunk.revision()) throw new IllegalStateException("Variant packet preceded native plan");
        if (!state.amounts.equals(amounts(menu.getPlan()))) throw new IllegalStateException("Variant packet changed native amounts");
        CLIENTS.put(menu, new ClientState(state.revision, state.amounts, state.packets + 1));
        System.out.println("AE2CT variant received menu=" + menu.containerId + " revision=" + chunk.revision()
                + " offset=" + chunk.offset() + " mask=" + chunk.rows());
    }

    static boolean received(CraftConfirmMenu menu) {
        var state = CLIENTS.get(menu);
        return state != null && state.packets > 0;
    }

    private static List<List<Long>> amounts(CraftingPlanSummary summary) {
        return summary == null ? List.of() : summary.getEntries().stream().map(row ->
                List.of(row.getStoredAmount(), row.getCraftAmount(), row.getMissingAmount())).toList();
    }
    private StoredVariantObservation() {}
}
