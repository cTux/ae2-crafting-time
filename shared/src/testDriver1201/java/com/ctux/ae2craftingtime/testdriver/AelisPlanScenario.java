package com.ctux.ae2craftingtime.testdriver;

import appeng.api.stacks.AEItemKey;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.crafting.CraftAmountScreen;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import com.ctux.ae2craftingtime.core.AelisPlanTtc;
import com.ctux.ae2craftingtime.core.IntegrationRead;
import com.ctux.ae2craftingtime.core.OptionFeature;
import com.ctux.ae2craftingtime.integration.IntegrationMixinPlugin;
import com.ctux.ae2craftingtime.mc1201.ClientOptionsRuntime;
import com.ctux.ae2craftingtime.testdriver.mixin.CraftAmountScreenAccessor;
import com.ctux.ae2craftingtime.testdriver.mixin.MEStorageScreenAccessor;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Real AELIS plan first; explicit display fixtures then exercise exact-quantity boundaries. */
final class AelisPlanScenario implements AutoCloseable {
    static final List<String> CHECKS = List.of("aelis-plan", "row-ttc", "total-ttc", "tooltip", "exact-rows",
            "filters", "scroll", "tree", "option-off", "narrow", "native-view", "fixture-restored");
    private static final String SCREEN = "com.appliedenhancements.client.plan.AelisCraftConfirmScreen";
    private final StandardCraftFixture fixture = new StandardCraftFixture();
    private final StableFrames<Object> frames = new StableFrames<>(5);
    private final boolean originalRows = ClientOptionsRuntime.current().features().enabled(OptionFeature.PLAN_ROWS);
    private final boolean originalTotal = ClientOptionsRuntime.current().features().enabled(OptionFeature.PLAN_TOTAL);
    private CompletableFuture<Boolean> operation;
    private SuiteFixture pristine;
    private Object plannerOption;
    private Boolean originalPlanner;
    private int stage;
    private boolean sent;
    private List<?> original;
    private CraftConfirmScreen screen;
    private long frame = -1;

    String checkpoint() { return "phase=" + (stage == 0 ? "PREPARE" : "AELIS") + " aelis-plan=" + stage; }

    boolean tick(Minecraft minecraft, FixtureMarker marker, Map<String, Boolean> checks,
            Consumer<String> capture, BiConsumer<Integer, Integer> mouse) throws Exception {
        if (stage == 0) {
            if (plannerOption == null) {
                plannerOption = Class.forName("com.appliedenhancements.Config").getField("ENABLE_AUTOMATIC_AELIS_PLANNER").get(null);
                originalPlanner = IntegrationRead.invoke(plannerOption, "get", Boolean.class);
                IntegrationRead.invoke(plannerOption, "set", Object.class, true);
                ClientOptionsRuntime.current().features().setEnabled(OptionFeature.PLAN_ROWS, true);
                ClientOptionsRuntime.current().features().setEnabled(OptionFeature.PLAN_TOTAL, true);
            }
            if (operation == null) {
                var id = minecraft.player.getUUID();
                operation = minecraft.getSingleplayerServer().submit(() -> {
                    var player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(id);
                    if (pristine == null) pristine = new SuiteFixture(player.serverLevel(), player, marker);
                    return fixture.prepare(player, marker);
                });
            }
            if (operation.isDone()) { boolean ready = operation.join(); operation = null; if (ready) next(); }
            return false;
        }
        if (stage == 1) {
            if (minecraft.screen == null && !sent) {
                minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(fixture.terminal).add(0, 0, -0.5), Direction.NORTH, fixture.terminal, false));
                sent = true;
            } else if (minecraft.screen instanceof MEStorageScreen<?> storage) {
                var entry = ((MEStorageScreenAccessor) storage).ae2craftingtime_test_driver$repo().getAllEntries().stream()
                        .filter(row -> row.getWhat().getId().toString().equals("minecraft:smooth_stone") && row.isCraftable())
                        .findFirst().orElse(null);
                if (entry != null && frames.observe(storage.getMenu().containerId)) { DriverPlatform.cloneEntry(storage, entry); next(); }
            }
            return false;
        }
        if (stage == 2) {
            if (minecraft.screen instanceof CraftAmountScreen amount && !sent) {
                var button = ((CraftAmountScreenAccessor) amount).ae2craftingtime_test_driver$next();
                DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4); sent = true;
            } else if (minecraft.screen != null && minecraft.screen.getClass().getName().equals(SCREEN)
                    && ((CraftConfirmScreen) minecraft.screen).getMenu().getPlan() != null) {
                screen = (CraftConfirmScreen) minecraft.screen;
                original = IntegrationRead.field(screen, "rows", List.class);
                if (!original.isEmpty()) { mouse.accept(0, 0); next(); }
            }
            return false;
        }
        if (stage == 12) return finish(minecraft, checks);
        var snapshot = UiObservationStore.latest();
        if (snapshot == null || snapshot.frame() == frame) return false;
        frame = snapshot.frame();
        if (!frames.observe(stage)) return false;
        if (stage == 3) {
            if (!AelisPlanTtc.ready() || !IntegrationMixinPlugin.snapshot().get("appliedenhancements").variant().equals("aelis-plan-114"))
                throw new IllegalStateException("AELIS presentation hooks are not ready");
            var totalText = snapshot.text().stream().filter(text -> text.key().equals("text.ae2craftingtime.total_ttc"))
                    .findFirst().orElse(null);
            if (totalText == null) return false;
            if (!ClientOptionsRuntime.current().badgeBackground() && !Integer.valueOf(0x404040).equals(totalText.color()))
                throw new IllegalStateException("AELIS total does not inherit the native readable foreground: " + totalText);
            checks.put("aelis-plan", true); checks.put("row-ttc", true); checks.put("total-ttc", true);
            capture.accept("aelis-plan.png");
            var visible = IntegrationRead.field(screen, "visible", List.class);
            int index = java.util.stream.IntStream.range(0, visible.size()).filter(i ->
                    IntegrationRead.invoke(visible.get(i), "craft", BigInteger.class).signum() > 0).findFirst().orElseThrow();
            mouse.accept(screen.getGuiLeft() + 60, screen.getGuiTop() + 119 + index * 22); next(); return false;
        }
        if (stage == 4) {
            if (snapshot.tooltip().stream().noneMatch(text -> text.key().equals("text.ae2craftingtime.ttc"))) return false;
            checks.put("tooltip", true); capture.accept("aelis-tooltip.png"); mouse.accept(0, 0);
            var constructor = original.get(0).getClass().getConstructors()[0];
            var rows = new ArrayList<Object>(original);
            rows.add(constructor.newInstance(AEItemKey.of(Items.DIRT), "Unknown history", "minecraft:dirt",
                    BigInteger.ZERO, BigInteger.ONE, BigInteger.ZERO, BigInteger.ZERO));
            rows.add(constructor.newInstance(AEItemKey.of(Items.COBBLESTONE), "Exact quantity beyond long", "minecraft:cobblestone",
                    BigInteger.ZERO, BigInteger.ONE.shiftLeft(64), BigInteger.ZERO, BigInteger.ZERO));
            rows.add(constructor.newInstance(AEItemKey.of(Items.GLASS), "Cyclic display fixture", "minecraft:glass",
                    BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE));
            for (int i = 0; i < 12; i++) rows.add(constructor.newInstance(AEItemKey.of(Items.STONE), "Stored fixture " + i,
                    "fixture:" + i, BigInteger.ONE, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO));
            set("rows", List.copyOf(rows)); set("cyclicColumn", true); IntegrationRead.invoke(screen, "refreshFilter", Object.class);
            next(); return false;
        }
        if (stage == 5) {
            if (snapshot.text().stream().noneMatch(text -> text.key().equals("text.ae2craftingtime.aelis.partial_total"))) return false;
            capture.accept("aelis-exact-rows.png"); checks.put("exact-rows", true);
            click(minecraft, "Cyclic"); next(); return false;
        }
        if (stage == 6) {
            if (IntegrationRead.field(screen, "visible", List.class).size() != 1) return false;
            checks.put("filters", true); capture.accept("aelis-filter.png"); click(minecraft, "All"); next(); return false;
        }
        if (stage == 7) {
            if (!sent) { var layout = IntegrationRead.field(screen, "layout", Object.class);
                DriverPlatform.click(minecraft, screen.getGuiLeft() + IntegrationRead.invoke(layout, "width", Integer.class) - 19,
                        screen.getGuiTop() + IntegrationRead.invoke(layout, "rowsBottom", Integer.class) - 3); sent = true; frames.reset(); return false; }
            if (IntegrationRead.field(screen, "scroll", Integer.class) <= 0) return false;
            checks.put("scroll", true); capture.accept("aelis-scroll.png"); click(minecraft, "Craft Tree"); next(); return false;
        }
        if (stage == 8) {
            if (!IntegrationRead.field(screen, "view", Object.class).toString().equals("TREE")) return false;
            checks.put("tree", true); capture.accept("aelis-tree.png"); click(minecraft, "Materials");
            ClientOptionsRuntime.current().features().setEnabled(OptionFeature.PLAN_ROWS, false);
            ClientOptionsRuntime.current().features().setEnabled(OptionFeature.PLAN_TOTAL, false);
            screen.resize(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
            next(); return false;
        }
        if (stage == 9) {
            if (IntegrationRead.invoke(IntegrationRead.field(screen, "layout", Object.class), "width", Integer.class) != 360)
                throw new IllegalStateException("Disabled column still reserves space");
            checks.put("option-off", true); capture.accept("aelis-off.png");
            ClientOptionsRuntime.current().features().setEnabled(OptionFeature.PLAN_ROWS, true);
            ClientOptionsRuntime.current().features().setEnabled(OptionFeature.PLAN_TOTAL, true);
            screen.resize(minecraft, 381, minecraft.getWindow().getGuiScaledHeight()); next(); return false;
        }
        if (stage == 10) {
            if (IntegrationRead.invoke(IntegrationRead.field(screen, "layout", Object.class), "width", Integer.class) != 360)
                throw new IllegalStateException("Narrow column did not fall back");
            checks.put("narrow", true); capture.accept("aelis-narrow.png");
            screen.resize(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
            click(minecraft, "AE2 view"); next(); return false;
        }
        if (stage == 11) {
            if (minecraft.screen == null || minecraft.screen.getClass() != CraftConfirmScreen.class) return false;
            checks.put("native-view", true); capture.accept("aelis-native-view.png"); minecraft.player.closeContainer(); next(); return false;
        }
        return false;
    }

    private boolean finish(Minecraft minecraft, Map<String, Boolean> checks) {
        if (operation == null) {
            var id = minecraft.player.getUUID();
            operation = minecraft.getSingleplayerServer().submit(() -> {
                pristine.restore(minecraft.getSingleplayerServer().getPlayerList().getPlayer(id));
                return pristine.samplesRestored();
            });
        }
        if (!operation.isDone()) return false;
        if (!operation.join()) throw new IllegalStateException("AELIS fixture samples were not restored");
        close(); checks.put("fixture-restored", true); return true;
    }

    private void set(String name, Object value) throws Exception {
        var field = screen.getClass().getDeclaredField(name); field.setAccessible(true); field.set(screen, value);
    }
    private void click(Minecraft minecraft, String name) {
        var button = screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                .filter(widget -> widget.getMessage().getString().equals(name)).findFirst().orElseThrow();
        DriverPlatform.click(minecraft, button.getX() + 4, button.getY() + 4);
    }
    private void next() { stage++; sent = false; frames.reset(); }
    @Override public void close() {
        ClientOptionsRuntime.current().features().setEnabled(OptionFeature.PLAN_ROWS, originalRows);
        ClientOptionsRuntime.current().features().setEnabled(OptionFeature.PLAN_TOTAL, originalTotal);
        if (originalPlanner != null) IntegrationRead.invoke(plannerOption, "set", Object.class, originalPlanner);
    }
}
