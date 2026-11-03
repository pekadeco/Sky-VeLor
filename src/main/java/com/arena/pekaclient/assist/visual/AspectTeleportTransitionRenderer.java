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

    /**
     * Called at END client tick only to maintain input arming and its finite time limit. Position observation belongs
     * to {@link #renderTick(Minecraft)} so an S08 position update is seen on the frame that will render it.
     */
    public void tick(Minecraft minecraft) {
        if (!eligible(minecraft)) return;

        observation.advanceTick();
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
    }

    /**
     * Samples the already-applied local player position at RenderTick START, immediately before the world camera is
     * consumed. Repeated render frames update only the baseline and never consume the client-tick arm window.
     */
    public void renderTick(Minecraft minecraft) {
        if (!eligible(minecraft)) return;
        EntityPlayerSP player = minecraft.thePlayer;
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
        if (!eligible(minecraft)) return;
        ItemStack held = minecraft.thePlayer.getHeldItem();
        if (held != null && AspectTeleportTransitionPolicy.isAspectTeleportItem(held.getDisplayName())) {
            observation.arm();
        }
    }

    /** Fails closed outside the local first-person player view so alternate cameras can never inherit the path. */
    private boolean eligible(Minecraft minecraft) {
        if (minecraft == null || minecraft.theWorld == null || minecraft.thePlayer == null
                || !AssistSettings.smoothAspectTeleport || minecraft.gameSettings == null
                || minecraft.gameSettings.thirdPersonView != 0 || minecraft.getRenderViewEntity() != minecraft.thePlayer) {
            reset();
            return false;
        }
        if (lastWorld != minecraft.theWorld) {
            // A destination path from a prior world must never briefly render into a freshly joined world.
            observation.reset();
            AspectTeleportCameraHooks.clear();
            lastWorld = minecraft.theWorld;
        }
        return true;
    }

    private void reset() {
        observation.reset();
        lastWorld = null;
        useKeyWasDown = false;
        AspectTeleportCameraHooks.clear();
    }
}
