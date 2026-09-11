package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.mc1201.net.CpuTtcPacketCodec;
import com.ctux.ae2craftingtime.mc1201.net.CpuTtcRequestC2S;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CpuTtcRequests {
    public static boolean enabled() { return true; }
    public static void send(CpuTtcPacketCodec.Request request) { PacketDistributor.sendToServer(new CpuTtcRequestC2S(request)); }
    private CpuTtcRequests() { }
}
