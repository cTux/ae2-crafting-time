package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.TimeEstimate;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * Builds the delayed warning message. The status word is a separate red
 * translatable so translation order never breaks, and the output name links
 * to the locate command while its record exists.
 */
public final class DelayedChatText {
    public static MutableComponent delayedMessage(String outputName, UUID recordId, long idleTicks,
            double typicalTicks) {
        var idleSeconds = (long) Math.ceil(Math.max(0, idleTicks) / 20.0);
        return Component.translatable("text.ae2craftingtime.chat.delayed",
                name(outputName, recordId),
                Component.translatable("text.ae2craftingtime.chat.delayed.word")
                        .withStyle(ChatFormatting.RED),
                Component.translatable("text.ae2craftingtime.value.whole_seconds", idleSeconds),
                TimeEstimate.formatTicks(typicalTicks));
    }

    private static Component name(String outputName, UUID recordId) {
        var name = Component.literal(outputName);
        if (recordId == null) {
            return name;
        }
        return name.withStyle(style -> style.withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/ae2craftingtime locate " + recordId))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("text.ae2craftingtime.chat.delayed.hint"))));
    }

    private DelayedChatText() {
    }
}
