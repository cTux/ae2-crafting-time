package com.ctux.ae2craftingtime.testdriver;

import com.ctux.ae2craftingtime.testdriver.mixin.ChatComponentAccessor;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.platform.win32.WinUser.KEYBDINPUT;
import net.minecraft.client.Minecraft;

/** Sends the same real modifier clicks used by the plan, status, and Tree screens. */
final class StatsInteraction {
    private boolean keyboardUsed;
    private int clickPhase;
    private boolean clicked;
    private int chatCount;
    private long nextStatsClick;
    private long clickedAt;

    void next() { clicked = false; }

    boolean click(Minecraft minecraft, UiSnapshot snapshot, String output, boolean reset) {
        return click(minecraft, snapshot, output, reset, true);
    }

    boolean clickWithoutStats(Minecraft minecraft, UiSnapshot snapshot, String output, boolean reset) {
        return click(minecraft, snapshot, output, reset, false);
    }

    private boolean click(Minecraft minecraft, UiSnapshot snapshot, String output, boolean reset, boolean expectResponse) {
        var chat = ((ChatComponentAccessor) minecraft.gui.getChat()).ae2craftingtime_test_driver$messages();
        if (!clicked) {
            if (System.nanoTime() < nextStatsClick) return false;
            if (!DriverPlatform.focus(minecraft)) {
                releaseKeys();
                clickPhase = 0;
                return false;
            }
            if (clickPhase++ == 0) {
                chatCount = chat.size();
                key(0x11, false);
                if (reset) key(0x12, false);
                return false;
            }
            if (!DriverPlatform.modifiers(minecraft, reset)) return false;
            var row = snapshot.rows().stream().filter(r -> r.outputId().equals(output)).findFirst().orElseThrow();
            DriverPlatform.click(minecraft, row.cell().centerX(), row.cell().centerY());
            releaseKeys();
            clicked = true;
            clickedAt = System.nanoTime();
            clickPhase = 0;
        }
        String expected = reset ? "Cleared TTC stats for " + output : output + " x1:";
        long matches = chat.size() > chatCount ? chat.subList(0, chat.size() - chatCount).stream()
                .filter(message -> message.content().getString().contains(expected)).count() : 0;
        if (matches > 1) throw new IllegalStateException("Duplicated stats response: " + expected);
        if (!expectResponse) {
            if (matches != 0) throw new IllegalStateException("Disabled read boundary sent stats action: " + expected);
            return System.nanoTime() - clickedAt > java.util.concurrent.TimeUnit.SECONDS.toNanos(1);
        }
        boolean received = matches == 1;
        if (received) nextStatsClick = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(
                com.ctux.ae2craftingtime.core.PlayerMessageRateLimit.COOLDOWN_MILLIS);
        return received;
    }

    void releaseKeys() {
        if (keyboardUsed) {
            key(0x12, true);
            key(0x11, true);
        }
    }

    private void key(int code, boolean release) {
        // CodexVM is Windows; Minecraft's existing JNA dependency avoids AWT's cached headless state.
        var input = new INPUT();
        input.type = new DWORD(INPUT.INPUT_KEYBOARD);
        input.input.setType(KEYBDINPUT.class);
        input.input.ki.wVk = new WORD(0);
        input.input.ki.wScan = new WORD(code == 0x11 ? 0x1d : 0x38);
        input.input.ki.dwFlags = new DWORD(KEYBDINPUT.KEYEVENTF_SCANCODE | (release ? KEYBDINPUT.KEYEVENTF_KEYUP : 0));
        keyboardUsed = true;
        if (User32.INSTANCE.SendInput(new DWORD(1), new INPUT[] {input}, input.size()).intValue() != 1) {
            throw new IllegalStateException("Native modifier input was rejected");
        }
    }

}
