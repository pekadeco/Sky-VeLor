package com.arena.pekaclient.feature;

/** Proves the No block particles option cannot suppress a non-block particle or do anything while off. */
public final class BlockParticlePolicyTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("off keeps block digging particles", !BlockParticlePolicy.shouldSuppress(false, true));
        check("off keeps every other particle", !BlockParticlePolicy.shouldSuppress(false, false));
        check("on suppresses confirmed block digging particles", BlockParticlePolicy.shouldSuppress(true, true));
        check("on keeps non-block particles", !BlockParticlePolicy.shouldSuppress(true, false));

        System.out.println();
        System.out.println("block particle policy: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
