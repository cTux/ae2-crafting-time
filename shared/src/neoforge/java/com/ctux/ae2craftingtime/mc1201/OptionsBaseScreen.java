package com.ctux.ae2craftingtime.mc1201;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** NeoForge's supported Minecraft versions paint the background in Screen. */
abstract class OptionsBaseScreen extends Screen {
    protected OptionsBaseScreen(Component title) {
        super(title);
    }
}
