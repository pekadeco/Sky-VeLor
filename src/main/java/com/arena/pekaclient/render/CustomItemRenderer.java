package com.arena.pekaclient.render;

import com.arena.pekaclient.feature.FirstPersonItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * First-person item renderer with Sky VeLor's size and animation-tempo settings applied.
 *
 * <p>A plain subclass of the vanilla {@link ItemRenderer}. {@link FirstPersonItemSetup} installs one
 * instance into the field that Minecraft 1.8.9 actually renders from, so the inherited state machine
 * ({@code itemToRender}, {@code equippedProgress}, the map/bow/block/eat branches) keeps running
 * exactly as vanilla wrote it. Only the visual position, size, and animation-tempo settings below are layered on top.
 *
 * <p>Both settings are strictly visual and default to vanilla:
 * <ul>
 *   <li><b>Position and size</b> wrap {@code renderItemInFirstPerson} in a renderer-space translation and
 *       uniform scale. Translation is applied before scaling, so X/Y/Z stay stable as size changes. The
 *       transform is outside the vanilla push/pop pair and leaves the matrix stack exactly as it was found.
 *       Swords, tools, blocks, bows, maps and the bare arm share this one visual call. {@code renderOverlays}
 *       (water, fire and pumpkin screens) is separate and deliberately unaffected.</li>
 *   <li><b>Animation speed</b> rescales the per-tick step of the equip transition, so the item
 *       slides out and back in faster or slower. It never touches swing progress, item-use counts or
 *       any server-visible timer, so attack speed, blocking, eating and bow draw are unchanged.</li>
 * </ul>
 *
 * <p>ForgeGradle reobfuscates this class, so the overrides, the {@code super} calls and every
 * Minecraft member reference are remapped to SRG names automatically. Nothing here matches a
 * Minecraft member by its development name.
 */
public class CustomItemRenderer extends ItemRenderer {

    private static final Logger LOG = LogManager.getLogger("SkyVeLor");

    /**
     * The two progress fields of the vanilla renderer, or null when they could not be identified.
     *
     * <p>They are private, so a subclass cannot read them directly, and their names differ between
     * the development workspace and the reobfuscated release jar. Rather than hardcode either name,
     * {@link #bindProgressFields()} identifies them structurally: the vanilla renderer declares
     * exactly two non-static float fields, and its own public {@code resetEquippedProgress()} zeroes
     * one of them. That gives a deterministic answer under any mapping, without guessing.
     */
    private Field equippedProgressField;
    private Field prevEquippedProgressField;
    private boolean bindingAttempted;

    public CustomItemRenderer(Minecraft mcIn) {
        super(mcIn);
    }

    // ------------------------------------------------------------------ item scale

    @Override
    public void renderItemInFirstPerson(float partialTicks) {
        float scale = FirstPersonItem.getItemScale();
        float x = FirstPersonItem.getItemX();
        float y = FirstPersonItem.getItemY();
        float z = FirstPersonItem.getItemZ();
        if (!FirstPersonItem.hasVisualTransform()) {
            super.renderItemInFirstPerson(partialTicks);
            return;
        }
        GlStateManager.pushMatrix();
        // Translate before scaling so X/Y/Z remain predictable renderer-space offsets even when Size changes.
        // This is visual matrix state only: no player, inventory, packet, swing, or attack state is modified.
        GlStateManager.translate(x, y, z);
        if (scale > 0.0f && scale != 1.0f) {
            GlStateManager.scale(scale, scale, scale);
        }
        try {
            super.renderItemInFirstPerson(partialTicks);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    // ------------------------------------------------------------------ animation speed

    /**
     * Advances the equip transition at the configured tempo.
     *
     * <p>The vanilla step is {@code equippedProgress += clamp(target - equippedProgress, -0.4, +0.4)}.
     * This lets the vanilla method take its own step, then rescales the step it actually took around
     * the value it started from, and clamps the result so it cannot pass the target. The transition
     * therefore stays monotonic and converges on the same endpoint: above 100% it arrives sooner,
     * below 100% it takes longer, and there is no jump, reversal or overshoot. Because the rendered
     * value interpolates between {@code prevEquippedProgress} and {@code equippedProgress}, scaling
     * the gap scales the visible tempo.
     *
     * <p>At 100% this is a single {@code super} call with no reflection at all.
     */
    @Override
    public void updateEquippedItem() {
        float speed = FirstPersonItem.getAnimationSpeed();
        if (speed <= 0.0f || speed == 1.0f) {
            super.updateEquippedItem();
            return;
        }
        if (!bindingAttempted) {
            bindProgressFields();
        }
        if (equippedProgressField == null || prevEquippedProgressField == null) {
            super.updateEquippedItem();
            return;
        }
        try {
            super.updateEquippedItem();
            // The vanilla method assigns prevEquippedProgress = equippedProgress first, so `from` is
            // the value this tick started at and `to` is where the vanilla step put it.
            float from = prevEquippedProgressField.getFloat(this);
            float to = equippedProgressField.getFloat(this);
            float delta = to - from;
            if (delta == 0.0f) {
                return;
            }
            float scaled = from + delta * speed;
            if (delta > 0.0f) {
                scaled = Math.min(1.0f, Math.max(from, scaled));
            } else {
                scaled = Math.max(0.0f, Math.min(from, scaled));
            }
            equippedProgressField.setFloat(this, scaled);
        } catch (IllegalAccessException e) {
            LOG.warn("Held-item animation speed stopped working: {}", e.toString());
            equippedProgressField = null;
            prevEquippedProgressField = null;
        }
    }

    /**
     * Works out which private float field is {@code equippedProgress} and which is
     * {@code prevEquippedProgress}, without depending on either name.
     *
     * <p>Both fields are set to values that cannot occur naturally, the vanilla
     * {@code resetEquippedProgress()} is called, and whichever field it zeroed is
     * {@code equippedProgress}. The original values are restored afterwards, so binding has no effect
     * on what the player sees.
     */
    private void bindProgressFields() {
        bindingAttempted = true;
        try {
            Field[] floats = floatFields();
            if (floats == null) {
                return;
            }
            Field a = floats[0];
            Field b = floats[1];
            float keepA = a.getFloat(this);
            float keepB = b.getFloat(this);

            a.setFloat(this, 0.25f);
            b.setFloat(this, 0.75f);
            resetEquippedProgress();
            boolean aIsEquipped = a.getFloat(this) == 0.0f;
            boolean bIsEquipped = b.getFloat(this) == 0.0f;

            a.setFloat(this, keepA);
            b.setFloat(this, keepB);

            if (aIsEquipped == bIsEquipped) {
                LOG.warn("Held-item animation speed: resetEquippedProgress did not identify exactly one"
                        + " progress field ({} / {}); leaving the animation at vanilla speed",
                        aIsEquipped, bIsEquipped);
                return;
            }
            equippedProgressField = aIsEquipped ? a : b;
            prevEquippedProgressField = aIsEquipped ? b : a;
            LOG.info("Held-item animation speed bound to ItemRenderer.{} / {}",
                    equippedProgressField.getName(), prevEquippedProgressField.getName());
        } catch (Throwable t) {
            equippedProgressField = null;
            prevEquippedProgressField = null;
            LOG.warn("Held-item animation speed unavailable: " + t, t);
        }
    }

    /** The two non-static float fields declared by the vanilla ItemRenderer, made accessible. */
    private static Field[] floatFields() throws IllegalAccessException {
        Field first = null;
        Field second = null;
        for (Field f : ItemRenderer.class.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (Modifier.isStatic(mod) || f.getType() != float.class) {
                continue;
            }
            f.setAccessible(true);
            if (first == null) {
                first = f;
            } else if (second == null) {
                second = f;
            } else {
                LOG.warn("ItemRenderer declares more than two float fields; not rescaling the animation");
                return null;
            }
        }
        if (first == null || second == null) {
            LOG.warn("ItemRenderer does not declare two float fields; not rescaling the animation");
            return null;
        }
        return new Field[] {first, second};
    }
}
