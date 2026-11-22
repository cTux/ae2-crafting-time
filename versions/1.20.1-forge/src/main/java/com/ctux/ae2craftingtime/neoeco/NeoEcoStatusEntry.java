package com.ctux.ae2craftingtime.neoeco;

import appeng.api.stacks.AEKey;
import com.ctux.ae2craftingtime.core.TtcEstimate;
import java.math.BigInteger;

public interface NeoEcoStatusEntry {
    AEKey key();
    BigInteger active();
    BigInteger pending();
    TtcEstimate ae2craftingtime$estimate();
    void ae2craftingtime$estimate(TtcEstimate value);
}
