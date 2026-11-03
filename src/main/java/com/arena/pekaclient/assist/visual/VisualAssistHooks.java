package com.arena.pekaclient.assist.visual;

import com.arena.pekaclient.config.AssistSettings;

/** Mapping-neutral helpers used by the narrow EntityRenderer hurt-camera core hook. */
public final class VisualAssistHooks {
    private VisualAssistHooks() {
    }

    /**
     * Applies the persisted 0-100% setting once to vanilla's hurt-camera tilt amplitude. This does not inspect or
     * mutate player state: damage tint, hurt timing, hit animation, knockback, health and packets stay vanilla.
     */
    public static float scaleHurtCameraTilt(float vanillaAmplitude) {
        return vanillaAmplitude * AssistSettings.hurtCameraShakeMultiplier();
    }
}
