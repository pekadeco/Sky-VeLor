package com.arena.pekaclient.render;

import com.arena.pekaclient.config.AppleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Installs {@link CustomItemRenderer} where Minecraft 1.8.9 actually renders the held item from.
 *
 * <p><b>Which field matters.</b> There are two holders of the renderer, and they are easy to confuse:
 * <pre>
 *   Minecraft.itemRenderer        private, non-final   - created in the Minecraft constructor
 *   EntityRenderer.itemRenderer   public,  final       - captured from mcIn.getItemRenderer()
 * </pre>
 * Every call on the render path goes through the second one. In {@code EntityRenderer},
 * {@code updateCameraAndRender} calls {@code this.itemRenderer.updateEquippedItem()} and
 * {@code renderHand} calls {@code this.itemRenderer.renderItemInFirstPerson(partialTicks)} followed by
 * {@code renderOverlays}. {@code Minecraft.itemRenderer} is only the construction site and the getter,
 * so replacing it alone changes nothing on screen: the setting moves, the config saves, and the game
 * keeps drawing with the vanilla instance {@code EntityRenderer} captured at startup. Both fields are
 * therefore pointed at the same custom instance, which also keeps every reader of
 * {@code mc.getItemRenderer()} consistent with what is drawn.
 *
 * <p><b>Why fields are found by type and not by name.</b> The release jar is reobfuscated to SRG
 * names, so at runtime neither field is called {@code itemRenderer}. A lookup by the literal
 * development name throws {@link NoSuchFieldException} in a real game, and if that exception is
 * swallowed the feature silently does nothing. Matching on the declared field whose type is
 * {@link ItemRenderer} works in the development workspace and in the obfuscated release alike, because
 * the {@code ItemRenderer.class} reference in this class is remapped by ForgeGradle too. Each of the
 * two classes declares exactly one such field, so the match is unambiguous.
 *
 * <p><b>Timing.</b> {@code EntityRenderer} is built during {@code Minecraft.startGame()}, so this can
 * only run once the client exists. {@link #init()} is idempotent and leaves itself retryable while the
 * renderer is still absent; {@code PekaClientMod} re-invokes it from the client tick until it reports
 * installed.
 *
 * <p>Failures are logged instead of swallowed, so a broken install is visible in the log rather than
 * presenting as a setting that appears to do nothing.
 */
public final class FirstPersonItemSetup {

    private static final Logger LOG = LogManager.getLogger("SkyVeLor");

    private static boolean installed;
    private static boolean gaveUp;
    private static ItemRenderer originalItemRenderer;

    private FirstPersonItemSetup() {
    }

    /**
     * Installs the custom renderer. Cheap and safe to call repeatedly: once installed, or once the
     * install has definitively failed, further calls return immediately.
     */
    public static void init() {
        if (installed || gaveUp) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return; // Not constructed yet; the next call will retry.
        }
        EntityRenderer entityRenderer = mc.entityRenderer;
        if (entityRenderer == null) {
            return; // startGame() has not built it yet; the next call will retry.
        }

        try {
            Field rendererField = findRendererField(EntityRenderer.class);
            if (rendererField == null) {
                giveUp("no ItemRenderer field found on EntityRenderer");
                return;
            }
            rendererField.setAccessible(true);
            Object current = rendererField.get(entityRenderer);
            if (!(current instanceof ItemRenderer)) {
                giveUp("EntityRenderer holds " + (current == null ? "null" : current.getClass().getName())
                        + " instead of an ItemRenderer");
                return;
            }
            originalItemRenderer = (ItemRenderer) current;

            ItemRenderer custom = new CustomItemRenderer(mc);
            write(entityRenderer, rendererField, custom);

            if (rendererField.get(entityRenderer) != custom) {
                giveUp("writing EntityRenderer." + rendererField.getName() + " did not stick");
                return;
            }

            // Keep Minecraft's own field pointing at the same instance so mc.getItemRenderer() and
            // anything else that reads it agrees with what is actually being drawn.
            Field mcField = findRendererField(Minecraft.class);
            if (mcField != null) {
                mcField.setAccessible(true);
                write(mc, mcField, custom);
                if (mcField.get(mc) != custom) {
                    LOG.warn("First-person item customization: EntityRenderer is patched but Minecraft.{}"
                            + " still holds the vanilla renderer", mcField.getName());
                }
            } else {
                LOG.warn("First-person item customization: no ItemRenderer field found on Minecraft;"
                        + " the held item is customized but mc.getItemRenderer() still returns the original");
            }

            installed = true;
            LOG.info("First-person item customization installed on EntityRenderer.{} (Item scale {}%,"
                            + " animation speed {}%)",
                    rendererField.getName(),
                    AppleSettings.effectiveFirstPersonItemScale(),
                    AppleSettings.effectiveFirstPersonAnimSpeed());
        } catch (Throwable t) {
            giveUp("unexpected failure: " + t);
            LOG.warn("First-person item customization could not be installed", t);
        }
    }

    /** The one field on {@code type} whose declared type is ItemRenderer, or null when there is none. */
    private static Field findRendererField(Class<?> type) {
        Field found = null;
        for (Field f : type.getDeclaredFields()) {
            if (!ItemRenderer.class.equals(f.getType())) {
                continue;
            }
            // Prefer a writable field if a class somehow declares more than one.
            if (!Modifier.isFinal(f.getModifiers())) {
                return f;
            }
            if (found == null) {
                found = f;
            }
        }
        return found;
    }

    /** Assigns {@code value}, clearing {@code final} first on Java 8 when the field requires it. */
    private static void write(Object owner, Field field, Object value) throws IllegalAccessException {
        if (Modifier.isFinal(field.getModifiers())) {
            clearFinal(field);
        }
        field.set(owner, value);
    }

    /**
     * Clears the {@code final} modifier so {@code EntityRenderer.itemRenderer} can be reassigned once.
     * This is the standard Java 8 route and the project targets Java 8 exactly.
     */
    private static void clearFinal(Field field) {
        try {
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (Exception e) {
            // Leave it to the following set(), whose outcome init() verifies and reports.
            LOG.debug("Could not clear the final modifier on {}: {}", field.getName(), e.toString());
        }
    }

    private static void giveUp(String reason) {
        gaveUp = true;
        LOG.warn("First-person item customization disabled: {}", reason);
    }

    /** The vanilla renderer that was replaced, or null when installation did not happen. */
    public static ItemRenderer getOriginalItemRenderer() {
        return originalItemRenderer;
    }

    /** True only when the custom renderer is really in place on the field that renders. */
    public static boolean isInstalled() {
        return installed;
    }

    /** True when installation failed for good, so the client tick stops retrying. */
    public static boolean hasGivenUp() {
        return gaveUp;
    }
}
