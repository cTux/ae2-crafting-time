package com.ctux.ae2craftingtime.testdriver;

import appeng.menu.me.crafting.CraftConfirmMenu;
import com.ctux.ae2craftingtime.core.PlanRecurrenceChunk;
import com.ctux.ae2craftingtime.mc1201.RecurrentPlanEntry;
import com.ctux.ae2craftingtime.mc1201.RecurrentPlanMenu;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;

public final class RecurrentPlanObservation {
    private static final Map<CraftConfirmMenu, NativePlan> PLANS = new WeakHashMap<>();
    private record NativePlan(long revision, List<Long> amounts, int chunks) {}

    public static void installed(CraftConfirmMenu menu) {
        if (!menu.isClientSide()) return;
        requireClientThread();
        var revision = ((RecurrentPlanMenu) menu).ae2craftingtime$summaryRevision();
        PLANS.put(menu, new NativePlan(revision, amounts(menu), 0));
        System.out.println("AE2CT native-plan player=" + menu.getPlayer().getUUID() + " menu=" + menu.containerId
                + " revision=" + revision + " thread=" + Thread.currentThread().getName());
    }

    public static void receiving(PlanRecurrenceChunk chunk) {
        requireClientThread();
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.containerMenu instanceof CraftConfirmMenu menu) || menu.getPlan() == null) return;
        var state = PLANS.get(menu);
        if (!chunk.validFor(menu.containerId, ((RecurrentPlanMenu) menu).ae2craftingtime$summaryRevision(), menu.getPlan().getEntries().size())) return;
        if (state == null || state.revision() != chunk.revision()) throw new IllegalStateException("Diagnostic preceded native setPlan");
        PLANS.put(menu, new NativePlan(state.revision(), state.amounts(), state.chunks() + 1));
        System.out.println("AE2CT diagnostic player=" + player.getUUID() + " menu=" + menu.containerId
                + " revision=" + chunk.revision() + " offset=" + chunk.offset() + " thread=" + Thread.currentThread().getName());
    }

    static boolean verify(CraftConfirmMenu menu) {
        var state = PLANS.get(menu);
        if (state == null) return false;
        if (!state.amounts().equals(amounts(menu))) throw new IllegalStateException("Diagnostic changed native missing quantities");
        boolean recurrent = menu.getPlan().getEntries().stream().anyMatch(e -> ((RecurrentPlanEntry) e).ae2craftingtime$recurrent());
        if (recurrent && state.chunks() == 0) return false;
        var carrier = (RecurrentPlanMenu) menu;
        var count = menu.getPlan().getEntries().size();
        var revision = carrier.ae2craftingtime$summaryRevision();
        var flags = flags(menu);
        for (int offset = 0; offset < count; offset += 256) {
            int rows = Math.min(256, count - offset);
            var bits = new java.util.BitSet(rows);
            for (int row = 0; row < rows; row++) if (flags.get(offset + row)) bits.set(row);
            var mask = new byte[32];
            var bytes = bits.toByteArray();
            System.arraycopy(bytes, 0, mask, 0, bytes.length);
            var duplicate = new PlanRecurrenceChunk(menu.containerId, revision, count, offset, rows, mask);
            if (!carrier.ae2craftingtime$apply(duplicate) || !carrier.ae2craftingtime$apply(duplicate)
                    || !flags.equals(flags(menu)) || !state.amounts().equals(amounts(menu)))
                throw new IllegalStateException("Duplicate diagnostic changed the native plan");
        }
        for (var bad : List.of(
                new PlanRecurrenceChunk(menu.containerId + 1, revision, count, 0, 1, new byte[32]),
                new PlanRecurrenceChunk(menu.containerId, revision - 1, count, 0, 1, new byte[32]),
                new PlanRecurrenceChunk(menu.containerId, revision + 1, count, 0, 1, new byte[32]),
                new PlanRecurrenceChunk(menu.containerId, revision, Integer.MAX_VALUE, 0, 256, new byte[32]),
                new PlanRecurrenceChunk(menu.containerId, revision, count, Integer.MAX_VALUE, 1, new byte[32]),
                new PlanRecurrenceChunk(menu.containerId, revision, count, 0, 1, new byte[31]))) {
            if (carrier.ae2craftingtime$apply(bad) || !flags.equals(flags(menu)) || !state.amounts().equals(amounts(menu)))
                throw new IllegalStateException("Malformed or stale diagnosis mutated the current native plan");
        }
        return true;
    }

    private static List<Long> amounts(CraftConfirmMenu menu) {
        return menu.getPlan().getEntries().stream().map(e -> e.getMissingAmount()).toList();
    }
    private static List<Boolean> flags(CraftConfirmMenu menu) {
        return menu.getPlan().getEntries().stream().map(e -> ((RecurrentPlanEntry) e).ae2craftingtime$recurrent()).toList();
    }
    private static void requireClientThread() {
        if (!Minecraft.getInstance().isSameThread()) throw new IllegalStateException("Plan observation is not on the client thread");
    }
    private RecurrentPlanObservation() {}
}
