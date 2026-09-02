package com.ctux.ae2craftingtime.testdriver;

public final class VmTextureProbe {
    public static int height(boolean enabled, String scenario, String renderer, int width, int height) {
        return enabled && !scenario.isBlank() && renderer != null && renderer.startsWith("SVGA3D")
                && width == 16384 && height == 16384 ? 8192 : height;
    }

    private VmTextureProbe() { }
}
