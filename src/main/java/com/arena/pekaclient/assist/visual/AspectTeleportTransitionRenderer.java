package com.arena.pekaclient.assist.visual;

import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;

/**
 * Short HUD-space visual ease for a completed Aspect of the End/Void position change.
 *
 * <p>A local right-click/use-key observation arms a small window before the server-authoritative position response
 * arrives. The renderer only observes that completed response and paints a visible, bounded blue veil. It never
 * writes player coordinates, rotation, key state, controller state, or camera state, and it never sends packets.</p>
 */
public final class AspectTeleportTransitionRenderer {

    private final AspectTeleportTransitionObservation observation = new AspectTeleportTransitionObservation();
    private boolean useKeyWasDown;
    private long transitionStartedNanos = Long.MIN_VALUE;

    /** Called from the ordinary END client tick after vanilla has applied any server-authoritative position update. */
    public void tick(Minecraft minecraft) {
        if (minecraft == null || minecraft.theWorld == null || minecraft.thePlayer == null) {
            reset();
            return;
        }
        if (!AssistSettings.smoothAspectTeleport) {
            reset();
            return;
        }

        EntityPlayerSP player = minecraft.thePlayer;
        ItemStack held = player.getHeldItem();
        boolean holdingAspect = held != null && AspectTeleportTransitionPolicy.isAspectTeleportItem(held.getDisplayName());
        // Mouse observations below cover ordinary clicks immediately. This edge check also covers the bound use-item
        // key and launcher/input paths that do not emit a Forge MouseEvent.
        boolean useKeyDown = minecraft.gameSettings != null && minecraft.gameSettings.keyBindUseItem != null
                && minecraft.gameSettings.keyBindUseItem.isKeyDown();
        if (holdingAspect && useKeyDown && !useKeyWasDown) {
            observation.arm();
        }
        useKeyWasDown = useKeyDown;

        if (observation.observe(minecraft.theWorld, player.posX, player.posY, player.posZ)) {
            transitionStartedNanos = System.nanoTime();
        }
    }

    /**
     * Records an ordinary local use click without consuming, cancelling, or replaying it. This makes the transition
     * reliable for a quick AOTE/AOTV click even when the key state has already been released by the next client tick.
     */
    public void observeUse(Minecraft minecraft) {
        if (!AssistSettings.smoothAspectTeleport || minecraft == null || minecraft.thePlayer == null) return;
        ItemStack held = minecraft.thePlayer.getHeldItem();
        if (held != null && AspectTeleportTransitionPolicy.isAspectTeleportItem(held.getDisplayName())) {
            observation.arm();
        }
    }

    /** Draws only the current short-lived HUD veil. No world render state or camera coordinates are changed. */
    public void render(ScaledResolution resolution) {
        if (!AssistSettings.smoothAspectTeleport || resolution == null || transitionStartedNanos == Long.MIN_VALUE) return;
        float alpha = AspectTeleportTransitionPolicy.veilAlpha(System.nanoTime() - transitionStartedNanos);
        if (alpha <= 0F) {
            transitionStartedNanos = Long.MIN_VALUE;
            return;
        }
        float width = resolution.getScaledWidth();
        float height = resolution.getScaledHeight();
        if (width <= 0F || height <= 0F) return;
        UIRender.beginFrame();
        try {
            // The stronger base masks the abrupt snap; the restrained blue wash preserves the Aspect visual identity.
            UIRender.rect(0F, 0F, width, height, UIRender.withAlpha(Theme.DIM, alpha));
            UIRender.rect(0F, 0F, width, height, UIRender.withAlpha(Theme.ACCENT_BLUE, alpha * .24F));
        } finally {
            UIRender.endFrame();
        }
    }

    private void reset() {
        observation.reset();
        useKeyWasDown = false;
        transitionStartedNanos = Long.MIN_VALUE;
    }
}
