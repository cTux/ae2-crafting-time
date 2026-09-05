package com.ctux.ae2craftingtime.integration.jei;

import guideme.Guides;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

@JeiPlugin
public final class GuideMeJeiPlugin implements IModPlugin {
    private static final ResourceLocation GUIDE_ID =
            ResourceLocation.fromNamespaceAndPath("ae2craftingtime", "guide");
    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath("ae2craftingtime", "guideme");
    private static final DefaultArtifactVersion LAST_GUIDEME_WITHOUT_JEI = new DefaultArtifactVersion("21.1.17");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        if (needsLocalFallback()) {
            var guide = createGuide();
            registration.registerSubtypeInterpreter(
                    VanillaTypes.ITEM_STACK,
                    guide.getItem(),
                    new ISubtypeInterpreter<>() {
                        @Override
                        public Object getSubtypeData(ItemStack stack, UidContext context) {
                            return stack.getComponentsPatch();
                        }

                        @Override
                        public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                            return stack.getComponentsPatch().toString();
                        }
                    });
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
