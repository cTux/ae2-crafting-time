package com.ctux.ae2craftingtime.mc1201;

import appeng.api.stacks.AEKey;

public final class AeKeyAmounts {
    public static long normalize(AEKey key, long amount) {
        var amountPerUnit = key.getAmountPerUnit();
        if (amountPerUnit <= 1) {
            return amount;
        }
        return amount * 1000L / amountPerUnit;
    }

    private AeKeyAmounts() {
    }
}
