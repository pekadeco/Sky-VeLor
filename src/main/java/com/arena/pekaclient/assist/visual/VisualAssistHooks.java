package com.arena.pekaclient.assist.visual;

import com.arena.pekaclient.config.AssistSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

/** Mapping-neutral branch used by the narrow EntityRenderer core hook. */
public final class VisualAssistHooks {
    private VisualAssistHooks() {
    }

    /** Only skips vanilla's local hurt-camera transform; damage, knockback, packets, and entities stay untouched. */
    public static boolean suppressHurtCamera() {
        return AssistSettings.noHurtCamera;
    }

    /** Pure fail-closed gate retained for headless regression coverage. */
    public static boolean shouldUseVisualFallback(boolean enabled, boolean coreHookActive) {
        return enabled && !coreHookActive;
    }

    /**
     * Last-resort client-only visual fallback when the narrow EntityRenderer hook was unavailable. Minecraft derives
     * the camera roll from the local player's hurtTime, so clearing that short-lived visual counter removes only the
     * tilt. It never sends a packet and never changes health, velocity, knockback, or server damage handling.
     */
    public static void zeroFallbackVisualTilt(Minecraft mc) {
        if (!shouldUseVisualFallback(AssistSettings.noHurtCamera, VisualAssistHookStatus.isHurtCameraActive())) return;
        if (mc == null) return;
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;
        VisualAssistHookStatus.hurtCameraFallbackReady();
        if (player.hurtTime > 0) player.hurtTime = 0;
    }
}
