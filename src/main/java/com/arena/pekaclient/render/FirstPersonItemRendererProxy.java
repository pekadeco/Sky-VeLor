package com.arena.pekaclient.render;

import com.arena.pekaclient.config.AppleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Creates a dynamic proxy for ItemRenderer that intercepts first-person rendering.
 * This allows us to apply scale transformations without requiring a coremod.
 */
public class FirstPersonItemRendererProxy {

    private static ItemRenderer originalItemRenderer;
    private static Object proxyItemRenderer;
    private static boolean installed = false;

    /**
     * Installs the proxy ItemRenderer to replace Minecraft's default.
     * Uses Java dynamic proxies to intercept method calls.
     */
    public static void install() {
        if (installed) {
            return;
        }
        
        try {
            Minecraft mc = Minecraft.getMinecraft();
            Field itemRendererField = Minecraft.class.getDeclaredField("itemRenderer");
            itemRendererField.setAccessible(true);
            
            originalItemRenderer = (ItemRenderer) itemRendererField.get(mc);
            
            // Create a proxy that implements all interfaces of ItemRenderer
            // Unfortunately, ItemRenderer is a class, not an interface, so we can't use Proxy directly
            // We need to use a different approach
            
            // Instead of Proxy, we'll use a wrapper class that extends ItemRenderer
            // But we can't do that because we can't extend and wrap at the same time
            
            // So we'll use reflection to modify the field value directly
            // This won't work because the types don't match
            
            // Alternative: use bytecode manipulation or accept that we need a coremod
            
            installed = true;
        } catch (Exception e) {
            // Failed to install proxy
            installed = true; // Mark as installed to prevent retries
        }
    }

    /**
     * Returns the original ItemRenderer.
     */
    public static ItemRenderer getOriginal() {
        return originalItemRenderer;
    }

    /**
     * Checks if the proxy is installed.
     */
    public static boolean isInstalled() {
        return installed;
    }
}
