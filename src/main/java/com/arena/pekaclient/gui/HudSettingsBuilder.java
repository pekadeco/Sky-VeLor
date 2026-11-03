package com.arena.pekaclient.gui;

import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.InventoryHud;
import com.arena.pekaclient.hud.MiningHud;
import com.arena.pekaclient.hud.PlayerDollHud;
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
 * Builds the settings controls for one HUD element. The HUD Hub Editor category and the HUD editor's
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
     * @param element  the element to edit
     * @param changed  called after any value changes (the editor marks the layout dirty; the hub passes a no-op)
     * @param withShow whether to include a Show toggle at the top
     */
    static List<Item> build(HudElement element, Runnable changed, boolean withShow) {
        HudSettingsBuilder b = new HudSettingsBuilder(changed);
        if (withShow) {
            b.toggle("Show", () -> element.enabled, v -> element.enabled = v);
        }
        b.specific(element);
        b.common(element);
        return b.items;
    }

    private void specific(HudElement element) {
        if (element instanceof MiningHud) {
            final MiningHud m = (MiningHud) element;
            dropdown("Label", MiningHud.LABELS, null, () -> m.label, v -> m.label = v);
            segmented("Window", MiningHud.WINDOW_NAMES, () -> m.windowIndex, v -> m.windowIndex = v);
            segmented("Decimals", MiningHud.DECIMAL_NAMES, () -> m.decimals, v -> m.decimals = v);
        } else if (element instanceof InventoryHud) {
            final InventoryHud inv = (InventoryHud) element;
            toggle("Armour slots", () -> inv.showArmor, v -> inv.showArmor = v);
            toggle("Transparent", () -> inv.transparent, v -> inv.transparent = v);
            gatedSlider("Transparency", 10, 90, 10, "%", () -> inv.transparency, v -> inv.transparency = v,
                    () -> inv.transparent);
        } else if (element instanceof PlayerDollHud) {
            final PlayerDollHud doll = (PlayerDollHud) element;
            toggle("Spin", () -> doll.spin, v -> doll.spin = v);
            toggle("Use angle", () -> doll.angleOn, v -> {
                doll.angleOn = v;
                if (!v) {
                    doll.angle = 0;
                }
            });
            gatedSlider("Angle", -180, 180, 5, "\u00b0", () -> doll.angle, v -> doll.angle = v, () -> doll.angleOn);
        } else if (element instanceof com.arena.pekaclient.fishing.FishingHud) {
            final com.arena.pekaclient.fishing.FishingHud f = (com.arena.pekaclient.fishing.FishingHud) element;
            toggle("Catches", () -> f.showCatches, v -> f.showCatches = v);
            toggle("Common Creature (CC)", () -> f.showCommonCreature, v -> f.showCommonCreature = v);
            toggle("Festival", () -> f.showFestival, v -> f.showFestival = v);
        } else if (element instanceof com.arena.pekaclient.fishing.SeaCreatureHud) {
            final com.arena.pekaclient.fishing.SeaCreatureHud s = (com.arena.pekaclient.fishing.SeaCreatureHud) element;
            toggle("Limit range", () -> s.limitRange, v -> s.limitRange = v);
            gatedSlider("Range", 16, 64, 8, " m", () -> s.range, v -> s.range = v, () -> s.limitRange);
            toggle("Limit rows", () -> s.limitRows, v -> s.limitRows = v);
            gatedSlider("Rows", 1, 8, 1, "", () -> s.maxRows, v -> s.maxRows = v, () -> s.limitRows);
        } else if (element instanceof com.arena.pekaclient.slayer.SlayerHud) {
            final com.arena.pekaclient.slayer.SlayerHud sl = (com.arena.pekaclient.slayer.SlayerHud) element;
            toggle("Limit drops", () -> sl.limitDrops, v -> sl.limitDrops = v);
            gatedSlider("Drops", 1, 4, 1, "", () -> sl.drops, v -> sl.drops = v, () -> sl.limitDrops);
            toggle("Profit", () -> sl.showProfit, v -> sl.showProfit = v);
            toggle("Boss timer", () -> sl.showBossTimer, v -> sl.showBossTimer = v);
        }
    }

    private void common(final HudElement e) {
        toggle("Use scale", () -> e.scaleOn, v -> {
            e.scaleOn = v;
            if (!v) {
                e.scale = 1f;
            }
        });
        gatedSlider("Scale", 50, 200, 5, "%", () -> Math.round(e.scale * 100f), v -> e.scale = v / 100f, () -> e.scaleOn);
        toggle("Use opacity", () -> e.opacityOn, v -> {
            e.opacityOn = v;
            if (!v) {
                e.opacity = 100;
            }
        });
        gatedSlider("Opacity", 20, 100, 5, "%", () -> e.opacity, v -> e.opacity = v, () -> e.opacityOn);
        toggle("Use radius", () -> e.radiusOn, v -> {
            e.radiusOn = v;
            if (!v) {
                e.radius = 10;
            }
        });
        gatedSlider("Radius", 0, 16, 1, "px", () -> e.radius, v -> e.radius = v, () -> e.radiusOn);
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

    /** A slider that only responds while its switch is on. Off: dimmed and locked. */
    private void gatedSlider(String label, int min, int max, int step, String suffix,
                             IntSupplier get, IntConsumer set, Supplier<Boolean> on) {
        items.add(new Item(label, new AppleSlider(min, max, step, suffix, CONTROL_W, get, v -> {
            set.accept(v);
            changed.run();
        }).gate(on)));
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
