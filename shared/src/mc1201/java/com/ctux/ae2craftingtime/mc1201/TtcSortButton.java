package com.ctux.ae2craftingtime.mc1201;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntSupplier;

public final class TtcSortButton extends IconButton {
    private final IntSupplier mode;

    public TtcSortButton(Runnable onPress, IntSupplier mode) {
        super(button -> onPress.run());
        this.mode = mode;
    }

    @Override
    protected Icon getIcon() {
        return switch (mode.getAsInt()) {
            case 1 -> Icon.ARROW_UP;
            case 2 -> Icon.ARROW_DOWN;
            default -> Icon.SORT_BY_AMOUNT;
        };
    }

    @Override
    public List<Component> getTooltipMessage() {
        return List.of(TtcText.sortTitle(), TtcText.sortMode(mode.getAsInt()));
    }
}
