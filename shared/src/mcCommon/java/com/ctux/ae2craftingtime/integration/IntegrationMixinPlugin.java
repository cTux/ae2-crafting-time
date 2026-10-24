package com.ctux.ae2craftingtime.integration;

import com.ctux.ae2craftingtime.core.IntegrationCatalog;
import com.ctux.ae2craftingtime.core.IntegrationContract;
import com.ctux.ae2craftingtime.core.IntegrationSelection;
import com.ctux.ae2craftingtime.core.PresentationHooks;
import com.ctux.ae2craftingtime.core.AelisPlanTtc;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Lives outside Mixin's reserved package; shared by every config in this process. */
public final class IntegrationMixinPlugin implements IMixinConfigPlugin {
    private static final class Startup {
        private static final IntegrationSelection SELECTION = new IntegrationSelection(IntegrationCatalog.CANDIDATES,
                IntegrationPlatform.TARGET, IntegrationPlatform.isClient(), IntegrationPlatform::version,
                candidate -> IntegrationContract.check(candidate.contract(), IntegrationMixinPlugin::bytecode),
                decision -> {
                    var logger = LoggerFactory.getLogger("ae2craftingtime/integrations");
                    var message = "dependency={} version={} adapter={} reason={} rejected={} (selection only)";
                    if (decision.reason().equals("no_compatible_variant")) {
                        logger.warn(message, decision.dependency(), decision.installedVersion(), decision.variant(),
                                decision.reason(), decision.rejected());
                    } else {
                        logger.info(message, decision.dependency(), decision.installedVersion(), decision.variant(),
                                decision.reason(), decision.rejected());
                    }
                });
    }

    public static Map<String, IntegrationSelection.Decision> snapshot() {
        return Startup.SELECTION.snapshot();
    }

    static IntegrationContract.ClassInfo bytecode(String internalName) {
        // ModLauncher rejects getClassNode(name, false); resource reads also avoid transformations.
        return read(internalName, MixinService.getService()::getResourceAsStream);
    }

    static IntegrationContract.ClassInfo read(String internalName, Function<String, InputStream> resources) {
        try (var input = resources.apply(internalName + ".class")) {
            if (input == null) return null;
            var node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return describe(node);
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot inspect adapter target " + internalName, failure);
        }
    }

    static IntegrationContract.ClassInfo describe(ClassNode node) {
        var members = new HashMap<String, List<String>>();
        node.fields.forEach(field -> members.computeIfAbsent("field:" + field.name, key -> new ArrayList<>())
                .add(field.desc));
        node.methods.forEach(method -> members.computeIfAbsent("method:" + method.name, key -> new ArrayList<>())
                .add(method.desc));
        return new IntegrationContract.ClassInfo(node.superName, members);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return select(Startup.SELECTION, mixinClassName);
    }

    static boolean select(IntegrationSelection selection, String mixinClassName) {
        try {
            return selection.shouldApply(mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1));
        } catch (RuntimeException failure) {
            // Optional configs log-and-skip Exceptions. Bootstrap faults must instead stop startup.
            throw new MixinInitialisationError("Cannot select integration for " + mixinClassName, failure);
        }
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (mixin.endsWith(".AelisPlanScreenMixin")) {
            AelisPlanTtc.screenReady(aelisScreenReady(node));
        } else if (mixin.endsWith(".AelisPlanLayoutMixin")) {
            AelisPlanTtc.layoutReady(aelisLayoutReady(node));
        } else if (mixin.endsWith(".AbstractTableRendererMixin")) {
            PresentationHooks.tableRenderer(tableReady(node));
        } else if (mixin.endsWith(".CraftConfirmScreenMixin")) {
            PresentationHooks.planInput(inputReady(node));
        } else if (mixin.endsWith(".CraftingCPUScreenMixin")) {
            PresentationHooks.statusInput(inputReady(node));
        } else if (mixin.endsWith(".CPUSelectionListMixin")) {
            PresentationHooks.cpuCard(cpuReady(node));
        }
    }

    static boolean aelisScreenReady(ClassNode node) {
        return calls(method(node, "drawFG"), "drawTtc") == 1
                && calls(method(node, "drawBG"), "summaryHeight") == 1
                && node.methods.stream().anyMatch(m -> calls(m, "ttcTooltip") == 3);
    }

    static boolean aelisLayoutReady(ClassNode node) {
        return calls(method(node, "fit"), "panelCap") == 1
                && calls(method(node, "columns"), "columnEnd") == 1;
    }

    static boolean tableReady(ClassNode node) {
        var render = method(node, "render");
        var scroll = method(node, "getScrollableRows", "(I)I");
        boolean newest = render != null && render.desc.contains("GuiGraphicsExtractor");
        return render != null && scroll != null
                && calls(render, "cellWidth") == 5 && calls(render, "cellHeight") == 4
                && calls(render, "columnPitch") == 1 && calls(render, "rowPitch") == 1
                && calls(render, "columnCount") == 1 && calls(render, "indexColumns") == 1
                && calls(render, "textScale") == (newest ? 2 : 3)
                && calls(render, "inverseX") == 1 && calls(render, "inverseY") == 1
                && calls(render, "visibleRows") == 1 && calls(render, "lineHeight") == 2
                && calls(render, "drawTextWithShadow") == 1
                && calls(render, "beginTtcColors") == 1
                && calls(render, "centeringHeight") == 1 && calls(render, "fullRowTooltip") == 1
                && calls(scroll, "scrollRows") == 1;
    }

    static boolean inputReady(ClassNode node) {
        var method = method(node, "ae2craftingtime$clickedTableIndex");
        return calls(method, "of") == 1 && calls(method, "entryAt") == 1
                && calls(method(node, "ae2craftingtime$clickedEntry"), "clickedTableIndex") == 1
                && calls(method(node, "ae2craftingtime$handleClickedStats"), "clickedEntry") == 1;
    }

    static boolean cpuReady(ClassNode node) {
        return calls(method(node, "drawBackgroundLayer"), "drawTtc") == 1
                && calls(method(node, "getTooltip", "(II)Lappeng/client/gui/Tooltip;"), "fullTtcTooltip") == 1;
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElse(null);
    }

    private static MethodNode method(ClassNode node, String name, String descriptor) {
        return node.methods.stream().filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .findFirst().orElse(null);
    }

    private static int calls(MethodNode method, String suffix) {
        if (method == null) return 0;
        int found = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && (call.name.equals(suffix) || call.name.endsWith("$" + suffix))) found++;
        }
        return found;
    }
}
