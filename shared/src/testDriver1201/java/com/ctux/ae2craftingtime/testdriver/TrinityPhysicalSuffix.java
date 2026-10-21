package com.ctux.ae2craftingtime.testdriver;

/** Decides which real furnace stack is isolated while one output is returned. */
final class TrinityPhysicalSuffix {
    enum Source { INPUT, OUTPUT }

    static boolean twoGlassReady(int furnaceGlass, int buffered) {
        return furnaceGlass == 2 && buffered == 0;
    }

    static Source source(int furnaceSand, int furnaceGlass, int buffered) {
        if (buffered != 0 || furnaceSand < 0 || furnaceGlass < 1
                || furnaceSand + furnaceGlass != 2)
            throw new IllegalStateException("Trinity furnace lacks one physical output and one suffix");
        return furnaceGlass == 2 ? Source.OUTPUT : Source.INPUT;
    }

    static boolean isolated(int furnaceSand, int furnaceGlass, int buffered) {
        return furnaceSand == 0 && furnaceGlass == 0 && buffered == 1;
    }

    private TrinityPhysicalSuffix() {}
}
