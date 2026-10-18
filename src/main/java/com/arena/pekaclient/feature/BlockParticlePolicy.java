package com.arena.pekaclient.feature;

/** Pure fail-closed predicate for the particle hook. Only a confirmed vanilla block-digging particle is suppressed. */
public final class BlockParticlePolicy {

    private BlockParticlePolicy() {
    }

    public static boolean shouldSuppress(boolean enabled, boolean isBlockDiggingParticle) {
        return enabled && isBlockDiggingParticle;
    }
}
