package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.core.TtcSymbols;
import com.ctux.ae2craftingtime.core.TtcEstimate;
import com.ctux.ae2craftingtime.core.TtcChatEstimate;
import com.ctux.ae2craftingtime.core.TimeEstimate;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import java.util.Optional;

/** Builds a local display copy; the server and stored chat retain their original components. */
public final class TtcComponents {
    public static MutableComponent text(String key, Object... args) {
        return decorate(Component.translatable(key, args));
    }

    public static MutableComponent time(String value) {
        return ClientOptionsRuntime.current().features().enabled(OptionFeature.SHOW_EMOJI)
                ? prefix(TtcSymbols.Symbol.TIME, Component.literal(value)) : Component.literal(value);
    }

    public static MutableComponent estimate(TtcEstimate value) {
        return estimate(value, ClientOptionsRuntime.current().features().enabled(OptionFeature.INSTANT_STATUS),
                ClientOptionsRuntime.current().features().enabled(OptionFeature.SHOW_EMOJI));
    }

    public static MutableComponent estimate(TtcEstimate value, boolean instant, boolean emoji) {
        if (instant && value.instant()) {
            var label = Component.translatable("text.ae2craftingtime.instant");
            return (emoji ? Component.literal("🥇 ").append(label) : label)
                    .withStyle(style -> style.withColor(0x55FF55).withBold(false));
        }
        var label = Component.literal(TimeEstimate.format(value));
        return emoji ? prefix(TtcSymbols.Symbol.TIME, label) : label;
    }

    public static MutableComponent semanticEstimate(TtcEstimate value) {
        return Component.translatableWithFallback(TtcChatEstimate.key(value), "%1$s",
                TimeEstimate.format(value), Double.toString(value.durationMs()));
    }

    public static MutableComponent decorate(Component source) {
        return decorate(source, ClientOptionsRuntime.current().features().enabled(OptionFeature.SHOW_EMOJI));
    }

    public static MutableComponent decorate(Component source, boolean showEmoji) {
        if (alreadyDecorated(source)) return source.copy();
        var style = TtcHover.decorate(source.getStyle(), value -> decorate(value, showEmoji));
        MutableComponent result;
        if (source.getContents() instanceof TranslatableContents text) {
            var estimate = TtcChatEstimate.read(text.getKey(), text.getArgs());
            if (estimate.isPresent()) {
                var display = estimate(estimate.get(), ClientOptionsRuntime.current().features().enabled(OptionFeature.INSTANT_STATUS), showEmoji);
                display.setStyle(display.getStyle().applyTo(style));
                for (var sibling : source.getSiblings()) display.append(decorate(sibling, showEmoji));
                return display;
            }
            var args = text.getArgs().clone();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component nested) args[i] = decorate(nested, showEmoji);
                else if (showEmoji && TtcSymbols.timeArgument(text.getKey(), i) && args[i] != null)
                    args[i] = prefix("?".equals(args[i].toString()) ? TtcSymbols.Symbol.INFO
                            : TtcSymbols.Symbol.TIME, Component.literal(args[i].toString()));
            }
            result = Component.translatableWithFallback(text.getKey(), text.getFallback(), args)
                    .setStyle(style);
            for (var sibling : source.getSiblings()) result.append(decorate(sibling, showEmoji));
            var symbol = showEmoji ? TtcSymbols.heading(text.getKey()) : null;
            if (symbol != null) return prefix(symbol, result);
            return result;
        }
        result = MutableComponent.create(source.getContents()).setStyle(style);
        for (var sibling : source.getSiblings()) result.append(decorate(sibling, showEmoji));
        return result;
    }

    public static TranslatableContents translation(Component source) {
        if (source.getContents() instanceof TranslatableContents text) return text;
        if (alreadyDecorated(source) && source.getSiblings().get(1).getContents() instanceof TranslatableContents text)
            return text;
        return null;
    }

    /** Match only symbol glyphs to a crafting row's foreground at render time. */
    public static Component rowSymbolsInTextColor(Component source, int color) {
        var result = Component.empty();
        var foreground = source.getStyle().getColor();
        if (foreground == null && source.getSiblings().size() > 1)
            foreground = source.getSiblings().get(1).getStyle().getColor();
        var textColor = foreground == null ? color : foreground.getValue();
        source.visit((style, part) -> {
            var symbol = false;
            for (var candidate : TtcSymbols.Symbol.values()) {
                if (part.equals(candidate.glyph() + " ")) symbol = true;
            }
            result.append(Component.literal(part).setStyle(symbol ? style.withColor(textColor) : style));
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return result;
    }

    public static Component fitWithEllipsis(Component text, Font font, int width) {
        return fitWithEllipsis(text, font.getSplitter(), width);
    }

    static Component fitWithEllipsis(Component text, net.minecraft.client.StringSplitter splitter, int width) {
        if (splitter.stringWidth(text) <= width) return text;
        var ellipsis = Component.literal("...").setStyle(text.getStyle());
        int available = width - (int) Math.ceil(splitter.stringWidth(ellipsis));
        if (available < 0) return Component.empty();
        var preview = Component.empty();
        splitter.headByWidth(text, available, net.minecraft.network.chat.Style.EMPTY).visit((style, part) -> {
            preview.append(Component.literal(part).setStyle(style));
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return preview.append(ellipsis);
    }

    private static boolean alreadyDecorated(Component source) {
        if (source.getSiblings().size() < 2) return false;
        var first = source.getSiblings().get(0);
        for (var symbol : TtcSymbols.Symbol.values()) {
            if ((symbol.glyph() + " ").equals(first.getString())) return true;
        }
        return false;
    }

    private static MutableComponent prefix(TtcSymbols.Symbol symbol, Component value) {
        return Component.empty().append(Component.literal(symbol.glyph() + " ")
                .withStyle(style -> style.withColor(TextColor.fromRgb(symbol.color())))).append(value);
    }

    private TtcComponents() { }
}
