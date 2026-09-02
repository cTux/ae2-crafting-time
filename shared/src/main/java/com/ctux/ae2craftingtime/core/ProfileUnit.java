package com.ctux.ae2craftingtime.core;

import java.util.Locale;

public enum ProfileUnit {
    ITEM,
    MILLIBUCKET,
    MANA;

    public String translationKey() {
        return "text.ae2craftingtime.unit." + name().toLowerCase(Locale.ROOT);
    }
}
