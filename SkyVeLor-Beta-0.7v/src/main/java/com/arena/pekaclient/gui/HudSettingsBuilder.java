package com.arena.pekaclient.gui;

import com.arena.pekaclient.hud.BalanceSource;
import com.arena.pekaclient.hud.DollarsHud;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.InventoryHud;
import com.arena.pekaclient.hud.MiningHud;
import com.arena.pekaclient.hud.PlayerDollHud;
import com.arena.pekaclient.hud.StatsHud;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleSegmented;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
import com.arena.pekaclient.widget.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Builds the settings controls for one HUD element. The HUD tab of the hub and the HUD editor's
 * inspector both use this, so the two always offer the same options.
 *
 * Each item is a label and a control. A null label means the control is a full-width action.
 */
final class HudSettingsBuilder {

    static final float CONTROL_W = 150f;
    static final float CONTROL_H = 22f;

    /** One row: an optional label and its control. */
    static final class Item {
        final String label;
        final Widget control;

        Item(String label, Widget control) {
            this.label = label;
            this.control = control;
        }
    }

    private final List<Item> items = new ArrayList<Item>();
    private final Runnable changed;

    private HudSettingsBuilder(Runnable changed) {
        this.changed = changed;
    }

    /**
     * @param element      the element to edit
     * @param amountField  the text field used for a typed balance (only the balance element uses it)
     * @param changed      called after any value changes (the editor marks the layout dirty; the hub passes a no-op)
     * @param applyBalance called when "Set balance" is pressed
     * @param withShow     whether to include a Show toggle at the top
     */
    static List<Item> build(HudElement element, Widget amountField, Runnable changed,
                            Runnable applyBalance, boolean withShow) {
        HudSettingsBuilder b = new HudSettingsBuilder(changed);
        if (withShow) {
            b.toggle("Show", () -> element.enabled, v -> element.enabled = v);
        }
        b.specific(element, amountField, applyBalance);
        b.common(element);
        return b.items;
    }

    private void specific(HudElement element, Widget amountField, Runnable applyBalance) {
        if (element instanceof MiningHud) {
            final MiningHud m = (MiningHud) element;
            dropdown("Label", MiningHud.LABELS, null, () -> m.label, v -> m.label = v);
            segmented("Window", MiningHud.WINDOW_NAMES, () -> m.windowIndex, v -> m.windowIndex = v);
            segmented("Decimals", MiningHud.DECIMAL_NAMES, () -> m.decimals, v -> m.decimals = v);
        } else if (element instanceof DollarsHud) {
            final DollarsHud d = (DollarsHud) element;
            dropdown("Label", DollarsHud.LABELS, null, () -> d.label, v -> d.label = v);
            dropdown("Format", DollarsHud.FORMATS, null, () -> d.format, v -> d.format = v);
            toggle("Coin icon", () -> d.coinIcon, v -> d.coinIcon = v);
            segmented("Source", new String[]{"Auto", "Manual"},
                    () -> BalanceSource.manualMode ? 1 : 0, v -> BalanceSource.manualMode = v == 1);
            items.add(new Item("Amount", amountField));
            items.add(new Item(null, new AppleButton("Set balance", CONTROL_W, CONTROL_H, true, applyBalance)));
        } else if (element instanceof InventoryHud) {
            final InventoryHud inv = (InventoryHud) element;
            toggle("Armour slots", () -> inv.showArmor, v -> inv.showArmor = v);
        } else if (element instanceof PlayerDollHud) {
            final PlayerDollHud doll = (PlayerDollHud) element;
            toggle("Spin", () -> doll.spin, v -> doll.spin = v);
            slider("Angle", -180, 180, 5, "\u00b0", () -> doll.angle, v -> doll.angle = v);
        } else if (element instanceof StatsHud) {
            final StatsHud st = (StatsHud) element;
            toggle("Show FPS", () -> st.showFps, v -> st.showFps = v);
            toggle("Show ping", () -> st.showPing, v -> st.showPing = v);
        }
    }

    private void common(final HudElement e) {
        slider("Scale", 50, 200, 5, "%", () -> Math.round(e.scale * 100f), v -> e.scale = v / 100f);
        slider("Opacity", 20, 100, 5, "%", () -> e.opacity, v -> e.opacity = v);
        slider("Radius", 0, 16, 1, "px", () -> e.radius, v -> e.radius = v);
        segmented("Text size", HudElement.TEXT_SIZE_NAMES, () -> e.textSize, v -> e.textSize = v);
        toggle("Background", () -> e.background, v -> e.background = v);
        toggle("Border", () -> e.border, v -> e.border = v);
        dropdown("Accent", HudElement.accentNames(), HudElement.accentDots(), () -> e.accent, v -> e.accent = v);
        items.add(new Item(null, new AppleButton("Reset position", CONTROL_W, CONTROL_H, false, () -> {
            e.resetPosition();
            changed.run();
        })));
        items.add(new Item(null, new AppleButton("Reset style", CONTROL_W, CONTROL_H, false, () -> {
            e.resetStyle();
            changed.run();
        })));
    }

    private void toggle(String label, Supplier<Boolean> get, Consumer<Boolean> set) {
        items.add(new Item(label, new AppleToggle(get, v -> {
            set.accept(v);
            changed.run();
        })));
    }

    private void slider(String label, int min, int max, int step, String suffix,
                        IntSupplier get, IntConsumer set) {
        items.add(new Item(label, new AppleSlider(min, max, step, suffix, CONTROL_W, get, v -> {
            set.accept(v);
            changed.run();
        })));
    }

    private void segmented(String label, String[] options, IntSupplier get, IntConsumer set) {
        items.add(new Item(label, new AppleSegmented(options, CONTROL_W, CONTROL_H, get, v -> {
            set.accept(v);
            changed.run();
        })));
    }

    private void dropdown(String label, String[] options, int[] dots, IntSupplier get, IntConsumer set) {
        items.add(new Item(label, new AppleDropdown(options, dots, CONTROL_W, CONTROL_H, get, v -> {
            set.accept(v);
            changed.run();
        })));
    }
}
