package com.ctux.ae2craftingtime.integration.jei;

import guideme.Guides;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

@JeiPlugin
public final class GuideMeJeiPlugin implements IModPlugin {
    private static final Identifier GUIDE_ID = Identifier.fromNamespaceAndPath("ae2craftingtime", "guide");
    private static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath("ae2craftingtime", "guideme");
    private static final DefaultArtifactVersion LAST_GUIDEME_WITHOUT_JEI =
            new DefaultArtifactVersion("26.1.12-beta");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        if (needsLocalFallback()) {
            var guide = createGuide();
            registration.registerSubtypeInterpreter(
                    VanillaTypes.ITEM_STACK,
                    guide.getItem(),
                    (ISubtypeInterpreter<ItemStack>) (stack, context) -> stack.getComponentsPatch());
        }
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        if (needsLocalFallback()) {
            registration.addExtraItemStacks(List.of(createGuide()));
        }
    }

    private static ItemStack createGuide() {
        return Guides.createGuideItem(GUIDE_ID);
    }

    private static boolean needsLocalFallback() {
        return ModList.get().getModContainerById("guideme")
                .map(mod -> mod.getModInfo().getVersion().compareTo(LAST_GUIDEME_WITHOUT_JEI) <= 0)
                .orElse(false);
    }
}
