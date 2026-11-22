package com.ctux.ae2craftingtime.neoeco;

import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import com.ctux.ae2craftingtime.mc1201.TtcComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class NeoEcoStatusText {
    private static final String TTC = "text.ae2craftingtime.neoeco_status_row";

    private NeoEcoStatusText() {}

    public static Component estimate(NeoEcoStatusEntry row) {
        var value = row.ae2craftingtime$estimate();
        if (value == null || !ClientOptionsRuntime.enabled(OptionFeature.STATUS_ROWS)) return null;
        return Component.translatableWithFallback(TTC, "%1$s", TtcComponents.estimate(value));
    }

    public static boolean isEstimate(Component line) {
        return line.getContents() instanceof TranslatableContents text && TTC.equals(text.getKey());
    }
}
