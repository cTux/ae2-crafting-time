package com.ctux.ae2craftingtime.integration.jei;

import guideme.Guides;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

@JeiPlugin
public final class GuideMeJeiPlugin implements IModPlugin {
    private static final ResourceLocation GUIDE_ID =
            ResourceLocation.fromNamespaceAndPath("ae2craftingtime", "guide");
    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath("ae2craftingtime", "guideme");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        if (ModList.get().isLoaded("guideme")) {
            var guide = createGuide();
            registration.registerSubtypeInterpreter(
                    VanillaTypes.ITEM_STACK,
                    guide.getItem(),
                    (ISubtypeInterpreter<ItemStack>) (stack, context) -> stack.getComponentsPatch());
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
