package com.ctux.ae2craftingtime.mc1201;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Shared locate command shape. Each loader registers the built tree on its
 * own command event and passes its own highlight sender. The tree is open to
 * every command source; the handler itself validates the player and record
 * ownership, and non-player sources get no answer.
 */
public final class ProviderLocateCommand {
    public static final String ROOT = "ae2craftingtime";
    public static final String LOCATE = "locate";
    public static final int HIGHLIGHT_SECONDS = 15;

    public static LiteralArgumentBuilder<CommandSourceStack> build(BiConsumer<CommandSourceStack, UUID> locate) {
        return Commands.literal(ROOT)
                .requires(source -> true)
                .then(Commands.literal(LOCATE)
                        .then(Commands.argument("record", StringArgumentType.string())
                                .executes(context -> {
                                    UUID id;
                                    try {
                                        id = UUID.fromString(StringArgumentType.getString(context, "record"));
                                    } catch (IllegalArgumentException ignored) {
                                        expired(context.getSource());
                                        return 0;
                                    }
                                    locate.accept(context.getSource(), id);
                                    return 1;
                                })));
    }

    /**
     * Resolves the clicking player, answers the expiry notice when the record
     * is missing or foreign, and hands owned records to the loader sender.
     */
    public static void locate(CommandSourceStack source, UUID id,
            BiConsumer<ServerPlayer, ProviderLocateRecords.LocateRecord> sender) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
            return;
        }
        var record = ProviderLocateRecords.ownedBy(player.getUUID(), id);
        if (record.isEmpty()) {
            player.sendSystemMessage(Component.translatable("text.ae2craftingtime.chat.delayed.expired"));
            return;
        }
        var located = record.get();
        sender.accept(player, located);
        player.sendSystemMessage(DelayedChatText.highlightingMessage(
                providerName(source.getLevel(), located.positions()), located.positions(),
                located.dimensionId()));
    }

    /**
     * Names the provider block at the first resolved position for the
     * highlight message. Anything unresolvable falls back to the generic
     * provider name so the message never breaks.
     */
    public static Component providerName(Level level, List<BlockPos> positions) {
        if (level != null && positions != null && !positions.isEmpty()) {
            try {
                return level.getBlockState(positions.get(0)).getBlock().getName();
            } catch (Exception ignored) {
                // Fall through to the generic provider name.
            }
        }
        return Component.translatable("text.ae2craftingtime.chat.provider");
    }

    private static void expired(CommandSourceStack source) {
        try {
            source.getPlayerOrException().sendSystemMessage(
                    Component.translatable("text.ae2craftingtime.chat.delayed.expired"));
        } catch (Exception ignored) {
            // Non-player sources get no answer.
        }
    }

    private ProviderLocateCommand() {
    }
}
