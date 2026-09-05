package com.ctux.ae2craftingtime.integration.jei;

import guideme.Guides;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

@JeiPlugin
public final class GuideMeJeiPlugin implements IModPlugin {
    private static final Identifier GUIDE_ID = Identifier.fromNamespaceAndPath("ae2craftingtime", "guide");
    private static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath("ae2craftingtime", "guideme");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        if (ModList.get().isLoaded("guideme")) {
            var guide = createGuide();
            registration.registerSubtypeInterpreter(
                    guide.getItem(), (stack, context) -> stack.getComponentsPatch());
        }
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        if (ModList.get().isLoaded("guideme")) {
            registration.addExtraItemStacks(List.of(createGuide()));
        }
    }

    private static ItemStack createGuide() {
        return Guides.createGuideItem(GUIDE_ID);
    }
}
