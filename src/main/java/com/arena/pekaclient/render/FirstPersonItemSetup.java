package com.arena.pekaclient.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Installs {@link CustomItemRenderer} in place of Minecraft's vanilla first-person item renderer.
 *
 * <p><b>Why the field is located by type and not by name.</b> The release jar is reobfuscated to SRG
 * names, so at runtime the field is not called {@code itemRenderer}. Looking it up by the literal
 * MCP name always throws {@link NoSuchFieldException} in a real game, and the failure is invisible
 * because the renderer simply never gets replaced. Matching on the declared field whose type is
 * {@link ItemRenderer} works in both the deobfuscated development workspace and the obfuscated
 * release runtime, because the {@code ItemRenderer.class} reference in this class is remapped by
 * ForgeGradle along with everything else.
 *
 * <p>Failures are logged rather than swallowed, so a broken install is visible in the log instead of
 * silently turning the setting into a no-op.
 */
public final class FirstPersonItemSetup {

    private static final Logger LOG = LogManager.getLogger("SkyVeLor");

    private static boolean attempted;
    private static boolean installed;
    private static ItemRenderer originalItemRenderer;

    private FirstPersonItemSetup() {
    }

    /** Installs the custom renderer. Safe to call more than once; only the first call does work. */
    public static void init() {
        if (attempted) {
            return;
        }
        attempted = true;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            LOG.warn("First-person item customization: Minecraft instance unavailable, feature disabled");
            return;
        }
        try {
            Field field = findItemRendererField();
            if (field == null) {
                LOG.warn("First-person item customization: no ItemRenderer field found on Minecraft, feature disabled");
                return;
            }
            field.setAccessible(true);
            Object current = field.get(mc);
            if (!(current instanceof ItemRenderer)) {
                LOG.warn("First-person item customization: unexpected renderer type {}, feature disabled",
                        current == null ? "null" : current.getClass().getName());
                return;
            }
            originalItemRenderer = (ItemRenderer) current;

            ItemRenderer replacement = new CustomItemRenderer(mc);
            clearFinal(field);
            field.set(mc, replacement);

            installed = field.get(mc) == replacement;
            if (installed) {
                LOG.info("First-person item customization installed (Minecraft.{})", field.getName());
            } else {
                LOG.warn("First-person item customization: field write did not stick, feature disabled");
            }
        } catch (Throwable t) {
            installed = false;
            LOG.warn("First-person item customization could not be installed: " + t, t);
        }
    }

    /** Finds the Minecraft field that holds the ItemRenderer, independent of the mapping in use. */
    private static Field findItemRendererField() {
        Field fallback = null;
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (!ItemRenderer.class.equals(f.getType())) {
                continue;
            }
            // Prefer a non-final field: it can be written without touching the modifiers field.
            if (!Modifier.isFinal(f.getModifiers())) {
                return f;
            }
            if (fallback == null) {
                fallback = f;
            }
        }
        return fallback;
    }

    /** Clears the {@code final} modifier on Java 8 so a final field can be reassigned once. */
    private static void clearFinal(Field field) {
        if (!Modifier.isFinal(field.getModifiers())) {
            return;
        }
        try {
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (Exception e) {
            // The set() below will report the real outcome if this was not enough.
            LOG.debug("Could not clear the final modifier on {}: {}", field.getName(), e.toString());
        }
    }

    /** The vanilla renderer that was replaced, or null when installation did not happen. */
    public static ItemRenderer getOriginalItemRenderer() {
        return originalItemRenderer;
    }

    /** True only when the custom renderer is actually installed and active. */
    public static boolean isInstalled() {
        return installed;
    }
}
