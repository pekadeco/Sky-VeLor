package com.arena.pekaclient.assist.visual;

import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;

/**
 * Short HUD-space visual ease for an already-completed Aspect of the End/Void position change.
 *
 * <p>The renderer observes only the local client's finished positions. It never writes player coordinates, rotation,
 * key state, or controller state, and it never sends packets. A subtle dark-blue veil eases in and out over 220 ms to
 * make the camera discontinuity less abrupt while leaving the actual game camera and gameplay authoritative.</p>
 */
public final class AspectTeleportTransitionRenderer {

    private Object observedWorld;
    private boolean hasLastPosition;
    private double lastX;
    private double lastY;
    private double lastZ;
    private int armedTicks;
    private long transitionStartedNanos = Long.MIN_VALUE;

    /** Called from the ordinary END client tick after vanilla has applied any server-authoritative position update. */
    public void tick(Minecraft minecraft) {
        if (minecraft == null || minecraft.theWorld == null || minecraft.thePlayer == null) {
            reset();
            return;
        }
        EntityPlayerSP player = minecraft.thePlayer;
        if (!AssistSettings.smoothAspectTeleport) {
            observeWithoutTransition(minecraft.theWorld, player);
            transitionStartedNanos = Long.MIN_VALUE;
            armedTicks = 0;
            return;
        }
        if (minecraft.theWorld != observedWorld) {
            observeWithoutTransition(minecraft.theWorld, player);
            transitionStartedNanos = Long.MIN_VALUE;
            armedTicks = 0;
            return;
        }

        ItemStack held = player.getHeldItem();
        if (held != null && AspectTeleportTransitionPolicy.isAspectTeleportItem(held.getDisplayName())) {
            armedTicks = AspectTeleportTransitionPolicy.ARM_TICKS;
        }
        if (hasLastPosition && armedTicks > 0 && AspectTeleportTransitionPolicy.isLargePositionJump(
                lastX, lastY, lastZ, player.posX, player.posY, player.posZ)) {
            transitionStartedNanos = System.nanoTime();
        }
        if (held == null || !AspectTeleportTransitionPolicy.isAspectTeleportItem(held.getDisplayName())) {
            if (armedTicks > 0) armedTicks--;
        }
        lastX = player.posX;
        lastY = player.posY;
        lastZ = player.posZ;
        hasLastPosition = true;
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
            // A dark base hides the single-frame snap; a very light blue wash retains the Aspect's visual identity.
            UIRender.rect(0F, 0F, width, height, UIRender.withAlpha(Theme.DIM, alpha));
            UIRender.rect(0F, 0F, width, height, UIRender.withAlpha(Theme.ACCENT_BLUE, alpha * .16F));
        } finally {
            UIRender.endFrame();
        }
    }

    private void observeWithoutTransition(Object world, EntityPlayerSP player) {
        observedWorld = world;
        hasLastPosition = true;
        lastX = player.posX;
        lastY = player.posY;
        lastZ = player.posZ;
    }

    private void reset() {
        observedWorld = null;
        hasLastPosition = false;
        armedTicks = 0;
        transitionStartedNanos = Long.MIN_VALUE;
    }
}
