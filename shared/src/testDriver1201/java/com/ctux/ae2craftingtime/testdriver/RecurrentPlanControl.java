package com.ctux.ae2craftingtime.testdriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

final class RecurrentPlanControl {
    private static String pendingAction;
    private static String acknowledgedAction;
    private static String acknowledgedEpoch;
    private static UUID acknowledgedPlayer;
    private static long pendingSequence;
    private static String pendingEpoch;
    private static UUID pendingPlayer;
    static String role() { return checkedRole(System.getProperty("ae2craftingtime.test.role", "alpha")); }
    static Path directory(String role) { return CpuListTtcControl.directory().resolve(checkedRole(role)); }
    static State state() { return state(role()); }
    static State state(String role) {
        var values = read(directory(role).resolve("state.properties"));
        return new State(Boolean.parseBoolean(values.getProperty("ready", "false")), values.getProperty("epoch", ""),
                Long.parseLong(values.getProperty("ack", "0")), values.getProperty("action", ""), values.getProperty("phase", ""),
                Integer.parseInt(values.getProperty("x", "0")), Integer.parseInt(values.getProperty("y", "0")),
                Integer.parseInt(values.getProperty("z", "0")), values.getProperty("player", ""),
                Boolean.parseBoolean(values.getProperty("recurrent", "false")), values.getProperty("turn", ""));
    }
    static Command command(String role) {
        var values = read(directory(role).resolve("command.properties"));
        return new Command(values.getProperty("epoch", ""), Long.parseLong(values.getProperty("sequence", "0")),
                values.getProperty("action", ""), values.getProperty("role", ""), values.getProperty("player", ""));
    }
    static boolean request(String action, UUID player) {
        var current = state();
        if (action.equals(acknowledgedAction) && CpuListTtcControl.epoch().equals(acknowledgedEpoch)
                && player.equals(acknowledgedPlayer)) return true;
        if (!action.equals(pendingAction) || !CpuListTtcControl.epoch().equals(pendingEpoch) || !player.equals(pendingPlayer)) {
            pendingAction = action;
            pendingEpoch = CpuListTtcControl.epoch();
            pendingPlayer = player;
            pendingSequence = current.ack() + 1;
            var values = new Properties(); values.setProperty("epoch", CpuListTtcControl.epoch());
            values.setProperty("sequence", Long.toString(pendingSequence)); values.setProperty("action", action);
            values.setProperty("role", role()); values.setProperty("player", player.toString());
            write(directory(role()).resolve("command.properties"), values);
        }
        var next = state();
        if (!next.ready() || !next.epoch().equals(CpuListTtcControl.epoch()) || next.ack() != pendingSequence || !action.equals(next.action()) || !player.toString().equals(next.player())) return false;
        pendingAction = null;
        acknowledgedAction = action;
        acknowledgedEpoch = next.epoch();
        acknowledgedPlayer = player;
        return true;
    }
    static void publish(String role, long ack, String action, String phase, net.minecraft.core.BlockPos terminal,
            UUID player, boolean recurrent, String turn) {
        var values = new Properties(); values.setProperty("ready", "true"); values.setProperty("epoch", CpuListTtcControl.epoch());
        values.setProperty("ack", Long.toString(ack)); values.setProperty("action", action); values.setProperty("phase", phase);
        values.setProperty("x", Integer.toString(terminal.getX())); values.setProperty("y", Integer.toString(terminal.getY()));
        values.setProperty("z", Integer.toString(terminal.getZ())); values.setProperty("player", player.toString());
        values.setProperty("recurrent", Boolean.toString(recurrent)); values.setProperty("turn", turn);
        write(directory(role).resolve("state.properties"), values);
    }
    private static String checkedRole(String value) { if (!value.equals("alpha") && !value.equals("beta")) throw new IllegalArgumentException("invalid recurrent role"); return value; }
    private static Properties read(Path path) { var result=new Properties(); if(!Files.isRegularFile(path))return result; try(var in=Files.newInputStream(path)){if(Files.size(path)>65536)throw new IllegalStateException("Recurrent control file exceeds 64 KiB");result.load(in);return result;}catch(IOException e){throw new IllegalStateException(e);} }
    private static void write(Path path, Properties values) { try { Files.createDirectories(path.getParent()); var tmp=path.resolveSibling(path.getFileName()+".tmp"); try(var out=Files.newOutputStream(tmp)){values.store(out,null);} Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); } catch(IOException e){throw new IllegalStateException(e);} }
    record State(boolean ready,String epoch,long ack,String action,String phase,int x,int y,int z,String player,boolean recurrent,String turn) {}
    record Command(String epoch,long sequence,String action,String role,String player) {}
    private RecurrentPlanControl() {}
}
