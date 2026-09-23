package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ClientConfig;
import com.ctux.ae2craftingtime.core.ClientConfigFile;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.core.TtcColor;
import com.ctux.ae2craftingtime.mc1201.net.WarningPreferenceC2S;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import org.slf4j.LoggerFactory;

/** Client-only loaded state; the screen edits a copy and applies it after a successful save. */
public final class ClientOptionsRuntime {
    private static ClientConfig current = new ClientConfig();
    private static Path path;

    public static void initialize(Path configDirectory) {
        path = configDirectory.resolve("ae2craftingtime-client.toml");
        try {
            current = ClientConfigFile.load(path, configDirectory.resolve("ae2craftingtime-common.toml"));
        } catch (IOException error) {
            LoggerFactory.getLogger("ae2craftingtime").warn("Could not read client options; using defaults", error);
            current = new ClientConfig();
        }
        TtcBadge.BACKGROUND = badgeBackground();
    }

    public static ClientConfig current() { return current; }

    public static boolean enabled(OptionFeature feature) { return current.features().enabled(feature); }

    public static int ttcColor(long seconds, long min, long max) {
        if (!enabled(OptionFeature.TTC_COLORS)) return current.color(ClientConfig.Color.TOTAL);
        return TtcColor.forSeconds(seconds, min, max,
                current.color(ClientConfig.Color.FAST), current.color(ClientConfig.Color.MIDDLE),
                current.color(ClientConfig.Color.SLOW));
    }

    public static int badgeBackground() {
        return current.badgeOpacity() << 24 | current.color(ClientConfig.Color.BADGE);
    }

    public static void apply(ClientConfig changed) throws IOException {
        ClientConfigFile.save(path, changed);
        current = changed.copy();
        TtcBadge.BACKGROUND = badgeBackground();
        syncWarningPreference();
    }

    public static void syncWarningPreference() {
        if (Minecraft.getInstance().getConnection() != null) {
            StatsNetwork.sendToServer(new WarningPreferenceC2S(
                    current.features().enabled(OptionFeature.RECEIVE_CRAFT_WARNINGS)));
        }
    }

    private ClientOptionsRuntime() {
    }
}
