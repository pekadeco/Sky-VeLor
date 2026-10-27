package com.arena.pekaclient.assist.visual;

import com.arena.pekaclient.config.AssistSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;

/**
 * Observes a completed Aspect of the End/Void position change and starts a temporary render-only camera path.
 *
 * <p>A local right-click/use-key observation arms a short window before the ordinary server-authoritative position
 * response arrives. The response remains vanilla; this class only passes its already-observed before/after positions
 * to the fail-closed renderer bridge. It never writes player coordinates, rotation, key state, controller state, or
 * camera state, and it never sends packets.</p>
 */
public final class AspectTeleportTransitionRenderer {

    private final AspectTeleportTransitionObservation observation = new AspectTeleportTransitionObservation();
    private Object lastWorld;
    private boolean useKeyWasDown;

    /** Called from the ordinary END client tick after vanilla has applied any server-authoritative position update. */
    public void tick(Minecraft minecraft) {
        if (minecraft == null || minecraft.theWorld == null || minecraft.thePlayer == null) {
            reset();
            return;
        }
        if (!AssistSettings.smoothAspectTeleport || minecraft.gameSettings == null
                || minecraft.gameSettings.thirdPersonView != 0 || minecraft.getRenderViewEntity() != minecraft.thePlayer) {
            // The render-coordinate path is intentionally first-person and player-view only; no third-person or
            // alternate camera entity state is altered as a fallback.
            reset();
            return;
        }

        if (lastWorld != minecraft.theWorld) {
            // A destination path from a prior world must never briefly render into a freshly joined world.
            observation.reset();
            AspectTeleportCameraHooks.clear();
            lastWorld = minecraft.theWorld;
        }

        EntityPlayerSP player = minecraft.thePlayer;
        ItemStack held = player.getHeldItem();
        boolean holdingAspect = held != null && AspectTeleportTransitionPolicy.isAspectTeleportItem(held.getDisplayName());
        // Mouse observations below cover ordinary clicks immediately. This edge check also covers the bound use-item
        // key and launcher/input paths that do not emit a Forge MouseEvent.
        boolean useKeyDown = minecraft.gameSettings.keyBindUseItem != null
                && minecraft.gameSettings.keyBindUseItem.isKeyDown();
        if (holdingAspect && useKeyDown && !useKeyWasDown) {
            observation.arm();
        }
        useKeyWasDown = useKeyDown;

        if (observation.observe(minecraft.theWorld, player.posX, player.posY, player.posZ)) {
            AspectTeleportCameraHooks.begin(observation.transitionFromX(), observation.transitionFromY(),
                    observation.transitionFromZ(), observation.transitionToX(), observation.transitionToY(),
                    observation.transitionToZ());
        }
    }

    /**
     * Records an ordinary local use click without consuming, cancelling, or replaying it. This makes the transition
     * reliable for a quick AOTE/AOTV click even when the key state has already been released by the next client tick.
     */
    public void observeUse(Minecraft minecraft) {
        if (!AssistSettings.smoothAspectTeleport || minecraft == null || minecraft.thePlayer == null
                || minecraft.gameSettings == null || minecraft.gameSettings.thirdPersonView != 0
                || minecraft.getRenderViewEntity() != minecraft.thePlayer) return;
        ItemStack held = minecraft.thePlayer.getHeldItem();
        if (held != null && AspectTeleportTransitionPolicy.isAspectTeleportItem(held.getDisplayName())) {
            observation.arm();
        }
    }

    private void reset() {
        observation.reset();
        lastWorld = null;
        useKeyWasDown = false;
        AspectTeleportCameraHooks.clear();
    }
}
