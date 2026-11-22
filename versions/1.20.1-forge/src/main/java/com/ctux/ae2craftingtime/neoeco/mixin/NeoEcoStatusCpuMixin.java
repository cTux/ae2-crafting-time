package com.ctux.ae2craftingtime.neoeco.mixin;

import com.ctux.ae2craftingtime.neoeco.NeoEcoStatusCpu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.api.me.ECOCraftingCPU", remap = false)
public abstract class NeoEcoStatusCpuMixin implements NeoEcoStatusCpu {}
