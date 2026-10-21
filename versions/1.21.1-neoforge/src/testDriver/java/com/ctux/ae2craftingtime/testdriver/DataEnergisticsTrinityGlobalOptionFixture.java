package com.ctux.ae2craftingtime.testdriver;

import com.ctux.ae2craftingtime.core.OptionFeature;

/** The same native stale-job cycle through the global Profiling switch. */
final class DataEnergisticsTrinityGlobalOptionFixture extends DataEnergisticsTrinityCancelFixture {
    @Override protected OptionFeature optionFeature() { return OptionFeature.PROFILING; }
}
