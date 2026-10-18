package com.arena.pekaclient.render.blockparticles;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.BlockParticlePolicy;
import net.minecraft.client.particle.EntityDiggingFX;

/** Runtime predicate injected only at EffectRenderer.addEffect(EntityFX). */
public final class BlockParticleHooks {

    private BlockParticleHooks() {
    }

    /**
     * EntityDiggingFX is vanilla's shared block-break/block-hit crack particle family. Every other EntityFX subtype
     * reaches vanilla unchanged, including particles from entities, potions, weather, portals, and mods.
     */
    public static boolean shouldSuppress(Object effect) {
        return BlockParticlePolicy.shouldSuppress(AppleSettings.noBlockParticles, effect instanceof EntityDiggingFX);
    }
}
