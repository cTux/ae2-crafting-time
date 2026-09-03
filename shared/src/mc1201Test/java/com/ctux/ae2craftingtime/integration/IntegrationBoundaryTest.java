package com.ctux.ae2craftingtime.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.ctux.ae2craftingtime.core.IntegrationCatalog;
import com.ctux.ae2craftingtime.core.IntegrationContract;
import com.ctux.ae2craftingtime.core.IntegrationSelection;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

class IntegrationBoundaryTest {
    @Test
    void asmReadsAnOverlappingContractWithoutDefiningAnyClasses() {
        var classes = new HashMap<String, IntegrationContract.ClassInfo>();
        var owner = "cn/dancingsnow/neoecoae/api/me/ECOCraftingCPULogic";
        var writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PRIVATE, "unused", "I", null, null).visitEnd();
        writer.visitMethod(Opcodes.ACC_PRIVATE, "recordPushedPattern",
                "(L" + owner + "$PendingPatternAccounting;)V", null, null).visitEnd();
        writer.visitMethod(Opcodes.ACC_PRIVATE, "recordPushedPattern",
                "(Lcn/dancingsnow/neoecoae/api/me/ExecutingCraftingJob;"
                        + "Lcn/dancingsnow/neoecoae/impl/crafting/fastpath/ECOExtractedPatternExecution;JZ)V",
                null, null).visitEnd();
        writer.visitEnd();
        classes.put(owner, IntegrationMixinPlugin.read(owner, resource -> {
            assertEquals(owner + ".class", resource);
            return new java.io.ByteArrayInputStream(writer.toByteArray());
        }));
        var selector = new IntegrationSelection(IntegrationCatalog.CANDIDATES, "1.20.1-forge", true,
                id -> "overlap-fixture", c -> IntegrationContract.check(c.contract(), classes::get), d -> {});
        assertTrue(selector.shouldApply("ECOCraftingCpuLogicMixin"));
        assertTrue(selector.shouldApply("NeoEcoLongBatchDispatchMixin"));
        assertFalse(selector.shouldApply("NeoEcoPendingDispatchMixin"));
        assertEquals(List.of("I"), classes.get(owner).members().get("field:unused"));
        assertNull(IntegrationMixinPlugin.read("absent", resource -> null));
        assertThrows(RuntimeException.class, () -> IntegrationMixinPlugin.read("malformed",
                resource -> new java.io.ByteArrayInputStream(new byte[] { 1, 2, 3 })));
        var failure = new java.io.IOException("fixture read failure");
        var thrown = assertThrows(java.io.UncheckedIOException.class, () -> IntegrationMixinPlugin.read("unreadable",
                resource -> new java.io.InputStream() {
                    @Override public int read() throws java.io.IOException { throw failure; }
                }));
        assertSame(failure, thrown.getCause());
        assertEquals("batched-long", selector.snapshot().get("neoecoae").variant());
        assertTrue(IntegrationMixinPlugin.select(selector, "example.NeoEcoLongBatchDispatchMixin"));
        var broken = new IntegrationSelection(IntegrationCatalog.CANDIDATES, "1.20.1-forge", true,
                id -> { throw new IllegalStateException("unavailable metadata"); }, c -> null, d -> {});
        var fatal = assertThrows(org.spongepowered.asm.launch.MixinInitialisationError.class,
                () -> IntegrationMixinPlugin.select(broken, "example.NeoEcoLongBatchDispatchMixin"));
        assertEquals("unavailable metadata", fatal.getCause().getMessage());
        assertTrue(broken.snapshot().isEmpty());
    }

    @Test
    void everyConfigUsesTheSamePluginAndOwnsAllOptionalHooks() throws Exception {
        var files = IntegrationPlatform.TARGET.equals("1.20.1-forge")
                ? List.of("ae2craftingtime.mixins.json", "ae2craftingtime-advancedae.mixins.json")
                : List.of("ae2craftingtime.mixins.json");
        var all = new HashSet<String>();
        var clients = new HashSet<String>();
        for (var file : files) {
            try (var input = getClass().getResourceAsStream("/" + file)) {
                var json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals(IntegrationMixinPlugin.class.getName(), json.get("plugin").getAsString());
                assertFalse(json.get("required").getAsBoolean());
                assertEquals(1, json.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
                json.getAsJsonArray("mixins").forEach(m -> assertTrue(all.add(m.getAsString())));
                if (json.has("client")) json.getAsJsonArray("client").forEach(m -> {
                    assertTrue(all.add(m.getAsString()));
                    clients.add(m.getAsString());
                });
            }
        }
        for (var candidate : IntegrationCatalog.CANDIDATES) {
            if (candidate.targets().contains(IntegrationPlatform.TARGET)) {
                assertTrue(all.containsAll(candidate.mixins()), candidate.variant());
                for (var mixin : candidate.mixins()) assertEquals(candidate.clientOnly(), clients.contains(mixin));
            }
        }
        for (var mixin : all) {
            var node = new ClassNode();
            try (var input = getClass().getResourceAsStream("/com/ctux/ae2craftingtime/mc1201/mixin/" + mixin + ".class")) {
                assertNotNull(input, mixin);
                new ClassReader(input).accept(node, ClassReader.SKIP_CODE);
            }
            boolean pseudo = node.invisibleAnnotations != null && node.invisibleAnnotations.stream()
                    .anyMatch(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Pseudo;"));
            if (pseudo) assertTrue(IntegrationCatalog.CANDIDATES.stream().anyMatch(c -> c.mixins().contains(mixin)), mixin);
        }
        // Config construction must be safe before loader metadata exists, including both Forge configs.
        for (int i = 0; i < files.size(); i++) {
            var plugin = new IntegrationMixinPlugin();
            plugin.onLoad("com.ctux.ae2craftingtime.mc1201.mixin");
            assertNull(plugin.getRefMapperConfig());
            assertNull(plugin.getMixins());
            plugin.acceptTargets(Set.of(), Set.of());
        }
    }
}
