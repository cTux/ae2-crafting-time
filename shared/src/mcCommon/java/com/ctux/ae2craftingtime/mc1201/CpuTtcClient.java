package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.CpuTtcCache;
import com.ctux.ae2craftingtime.mc1201.net.CpuTtcPacketCodec;

import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;
import appeng.menu.me.crafting.CraftingStatusMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class CpuTtcClient {
    private static final AtomicLong NEXT_SESSION = new AtomicLong();
    private static final CpuTtcCache CACHE = new CpuTtcCache();
    private static CraftingStatusMenu activeMenu;

    public static void open(CraftingStatusMenu menu) {
        activeMenu = menu;
        CACHE.open(NEXT_SESSION.getAndIncrement(), menu.containerId);
    }

    public static void refresh(List<CpuTtcCache.CpuView> views) {
        if (!CpuTtcRequests.enabled() || !active()) {
            clear();
            return;
        }
        CACHE.refresh(views, now()).ifPresent(request -> CpuTtcRequests.send(
                new CpuTtcPacketCodec.Request(request.containerId(), request.session(), request.sequence(),
                        request.serials())));
    }

    public static void receive(CpuTtcPacketCodec.Snapshot snapshot) {
        if (!active()) {
            clear();
            return;
        }
        CACHE.apply(snapshot.session(), snapshot.sequence(), snapshot.entries(), now());
    }

    public static OptionalLong seconds(int serial) {
        if (!active()) {
            clear();
            return OptionalLong.empty();
        }
        return CACHE.seconds(serial, now());
    }

    public static void clear() {
        CACHE.clear();
        activeMenu = null;
    }

    public static void close(CraftingStatusMenu menu) {
        if (activeMenu == menu) clear();
    }

    private static boolean active() {
        var minecraft = Minecraft.getInstance();
        return activeMenu != null && minecraft.getConnection() != null
                && minecraft.screen instanceof AbstractContainerScreen<?> screen
                && screen.getMenu() == activeMenu;
    }

    private static long now() { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()); }

    private CpuTtcClient() { }
}
