package com.ctux.ae2craftingtime.mc1201;

import appeng.api.stacks.AEKey;
import com.ctux.ae2craftingtime.core.ProfileAmounts;
import com.ctux.ae2craftingtime.core.ProfileUnit;

public final class AeKeyAmounts {
    public static long normalize(AEKey key, long amount) {
        var outputId = key.getId().toString();
        var normalized = ProfileAmounts.normalize(outputId, key.getAmountPerUnit(), amount);
        IntegrationLog.normalized(outputId);
        return normalized;
    }

    public static ProfileUnit unit(AEKey key) {
        return ProfileAmounts.unit(key.getId().toString(), key.getAmountPerUnit());
    }

    private AeKeyAmounts() {
    }
}
