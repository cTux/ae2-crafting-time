package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.PlanRecurrenceChunk;
import net.minecraft.client.Minecraft;
import appeng.menu.me.crafting.CraftConfirmMenu;

public final class PlanStoredVariantsClient {
    public static void receive(PlanRecurrenceChunk chunk, long updateRevision) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof CraftConfirmMenu menu) {
            ((RecurrentPlanMenu) menu).ae2craftingtime$applyStoredVariants(chunk, updateRevision);
        }
    }

    private PlanStoredVariantsClient() {}
}
