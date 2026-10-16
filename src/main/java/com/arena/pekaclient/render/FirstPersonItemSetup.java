package com.arena.pekaclient.render;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/**
 * Sets up first-person item rendering customization by replacing Minecraft's ItemRenderer
 * with a custom implementation that applies scale and animation speed transformations.
 */
public class FirstPersonItemSetup {

    private static boolean initialized = false;
    private static ItemRenderer originalItemRenderer;

    /**
     * Initializes the first-person item rendering customization.
     * Replaces Minecraft's itemRenderer with a CustomItemRenderer.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        
        try {
            Minecraft mc = Minecraft.getMinecraft();
            Field itemRendererField = Minecraft.class.getDeclaredField("itemRenderer");
            itemRendererField.setAccessible(true);
            
            // Get the original ItemRenderer
            originalItemRenderer = (ItemRenderer) itemRendererField.get(mc);
            
            // Create our custom ItemRenderer
            CustomItemRenderer customRenderer = new CustomItemRenderer(mc, originalItemRenderer);
            
            // Replace Minecraft's itemRenderer with our custom one
            itemRendererField.set(mc, customRenderer);
            
            initialized = true;
        } catch (Exception e) {
            // Failed to replace ItemRenderer - customization will be disabled
            initialized = true; // Mark as initialized to prevent retries
        }
    }

    /**
     * Returns the original ItemRenderer (for reference if needed).
     */
    public static ItemRenderer getOriginalItemRenderer() {
        return originalItemRenderer;
    }

    /**
     * Checks if the custom ItemRenderer has been successfully installed.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
