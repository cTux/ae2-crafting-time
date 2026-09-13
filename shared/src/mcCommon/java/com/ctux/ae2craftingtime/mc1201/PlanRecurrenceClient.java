package com.ctux.ae2craftingtime.mc1201;

import appeng.menu.me.crafting.CraftConfirmMenu;
import com.ctux.ae2craftingtime.core.PlanRecurrenceChunk;
import net.minecraft.client.Minecraft;

public final class PlanRecurrenceClient {
    public static void receive(PlanRecurrenceChunk chunk) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof CraftConfirmMenu menu) {
            ((RecurrentPlanMenu) menu).ae2craftingtime$apply(chunk);
        }
    }

    private PlanRecurrenceClient() {}
}

