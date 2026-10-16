package com.arena.pekaclient.render;

import com.arena.pekaclient.feature.FirstPersonItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;

/**
 * Replaces Minecraft's first-person item renderer with one that can scale the held item.
 *
 * <p>This is a plain subclass of the vanilla {@link ItemRenderer}. It is installed by swapping the
 * single renderer instance that {@link Minecraft} holds (see {@link FirstPersonItemSetup}), so every
 * vanilla call path keeps working: the inherited {@code updateEquippedItem()} still advances this
 * instance's own equip state, and the inherited block/item/bow/map rendering is untouched. Nothing
 * is delegated to a second renderer, which would desynchronise the equip animation.
 *
 * <p>ForgeGradle reobfuscates this class for the release jar, so the overridden method and the
 * {@code super} call are remapped to their SRG names automatically. No method names are matched by
 * string, which keeps the release jar working in the obfuscated runtime.
 */
public class CustomItemRenderer extends ItemRenderer {

    public CustomItemRenderer(Minecraft mcIn) {
        super(mcIn);
    }

    /**
     * Renders the held item, optionally wrapped in a uniform scale.
     *
     * <p>At the default 100% the vanilla method is called with no extra matrix operations at all, so
     * the appearance is bit-for-bit the vanilla one. When a scale is active the transform is applied
     * around a push/pop pair; the vanilla renderer pushes and pops its own matrices inside, and those
     * inherit the scale, so swords, tools, blocks, bows and maps all scale together and the matrix
     * stack is left exactly as it was found.
     */
    @Override
    public void renderItemInFirstPerson(float partialTicks) {
        float scale = FirstPersonItem.getItemScale();
        if (scale <= 0.0f || scale == 1.0f) {
            super.renderItemInFirstPerson(partialTicks);
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
        try {
            super.renderItemInFirstPerson(partialTicks);
        } finally {
            GlStateManager.popMatrix();
        }
    }
}
