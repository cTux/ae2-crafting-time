package com.ctux.ae2craftingtime.testdriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/** File rendezvous for the marked, loopback-only connected variant case. */
final class StoredVariantControl {
    private static String pendingAction;
    private static long pendingSequence;
    private static String acknowledgedAction;
    private static long acknowledgedSequence;

    static Path directory() { return CpuListTtcControl.directory().resolve("variant"); }

    static State state() {
        var p = read(directory().resolve("state.properties"));
        return new State(Boolean.parseBoolean(p.getProperty("ready", "false")), p.getProperty("epoch", ""),
                Long.parseLong(p.getProperty("ack", "0")), p.getProperty("action", ""),
                Integer.parseInt(p.getProperty("x", "0")), Integer.parseInt(p.getProperty("y", "0")),
                Integer.parseInt(p.getProperty("z", "0")), p.getProperty("player", ""),
                Integer.parseInt(p.getProperty("menu", "-1")), Long.parseLong(p.getProperty("revision", "0")));
    }

    static Command command() {
        var p = read(directory().resolve("command.properties"));
        return new Command(p.getProperty("epoch", ""), Long.parseLong(p.getProperty("sequence", "0")),
                p.getProperty("action", ""), p.getProperty("player", ""),
                Integer.parseInt(p.getProperty("menu", "-1")), Long.parseLong(p.getProperty("revision", "0")));
    }

    static boolean request(String action, UUID player, int menu, long revision) {
        var current = state();
        if (action.equals(acknowledgedAction) && current.ack() == acknowledgedSequence
                && current.player().equals(player.toString()) && current.menu() == menu
                && current.revision() == revision) return true;
        if (!action.equals(pendingAction)) {
            if (current.ack() == Long.MAX_VALUE) throw new IllegalStateException("Variant control sequence exhausted");
            pendingAction = action;
            pendingSequence = current.ack() + 1;
            var p = new Properties();
            p.setProperty("epoch", CpuListTtcControl.epoch());
            p.setProperty("sequence", Long.toString(pendingSequence));
            p.setProperty("action", action);
            p.setProperty("player", player.toString());
            p.setProperty("menu", Integer.toString(menu));
            p.setProperty("revision", Long.toString(revision));
            write(directory().resolve("command.properties"), p);
        }
        var next = state();
        if (!next.ready() || !next.epoch().equals(CpuListTtcControl.epoch())
                || next.ack() != pendingSequence || !next.action().equals(action)
                || !next.player().equals(player.toString()) || next.menu() != menu
                || next.revision() != revision) return false;
        pendingAction = null;
        acknowledgedAction = action;
        acknowledgedSequence = next.ack();
        return true;
    }

    static void publish(long ack, String action, net.minecraft.core.BlockPos terminal, UUID player,
            int menu, long revision) {
        var p = new Properties();
        p.setProperty("ready", "true");
        p.setProperty("epoch", CpuListTtcControl.epoch());
        p.setProperty("ack", Long.toString(ack));
        p.setProperty("action", action);
        p.setProperty("x", Integer.toString(terminal.getX()));
        p.setProperty("y", Integer.toString(terminal.getY()));
        p.setProperty("z", Integer.toString(terminal.getZ()));
        p.setProperty("player", player.toString());
        p.setProperty("menu", Integer.toString(menu));
        p.setProperty("revision", Long.toString(revision));
        write(directory().resolve("state.properties"), p);
    }

    record State(boolean ready, String epoch, long ack, String action, int x, int y, int z,
            String player, int menu, long revision) {}
    record Command(String epoch, long sequence, String action, String player, int menu, long revision) {
        boolean matches(UUID expectedPlayer, int expectedMenu, long expectedRevision, long previous) {
            return epoch.equals(CpuListTtcControl.epoch()) && previous < Long.MAX_VALUE && sequence == previous + 1
                    && player.equals(expectedPlayer.toString()) && menu == expectedMenu
                    && revision > 0 && revision == expectedRevision;
        }
    }

    private static Properties read(Path path) {
        var p = new Properties();
        if (!Files.isRegularFile(path)) return p;
        try (var in = Files.newInputStream(path)) {
            if (Files.size(path) > 65_536) throw new IllegalStateException("Variant control file too large");
            p.load(in);
            return p;
        } catch (IOException error) { throw new IllegalStateException(error); }
    }

    private static void write(Path path, Properties p) {
        try {
            Files.createDirectories(path.getParent());
            var temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(temporary)) { p.store(out, null); }
            DriverProgress.moveWithAccessDeniedRetry(temporary, path,
                    (source, target) -> Files.move(source, target, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE));
        } catch (IOException error) { throw new IllegalStateException(error); }
    }

    private StoredVariantControl() {}
}
