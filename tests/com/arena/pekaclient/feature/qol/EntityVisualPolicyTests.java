package com.arena.pekaclient.feature.qol;

/** Regression tests for immediate dead-mob hiding and exact Flaming Fist texture matching. */
public final class EntityVisualPolicyTests {
    public static void main(String[] args) {
        check(!EntityVisualPolicy.shouldHideDyingMob(false, false, false, 20.0F, 0),
                "healthy mobs remain visible");
        check(EntityVisualPolicy.shouldHideDyingMob(false, false, false, 0.0F, 0),
                "mob vanishes on the same render as its health reaches zero");
        check(EntityVisualPolicy.shouldHideDyingMob(false, false, false, -1.0F, 0),
                "negative health is hidden");
        check(EntityVisualPolicy.shouldHideDyingMob(false, false, false, 0.0F, 1),
                "vanilla death animation is hidden as soon as deathTime advances");
        check(EntityVisualPolicy.shouldHideDyingMob(true, false, false, 10.0F, 0),
                "dead mobs are hidden");
        check(!EntityVisualPolicy.shouldHideDyingMob(false, true, false, 0.0F, 3),
                "player entities are never hidden by the mob option");
        check(!EntityVisualPolicy.shouldHideDyingMob(false, false, true, 0.0F, 3),
                "armor stands are never hidden by the mob option");

        check(EntityVisualPolicy.isFlamingFistTexture("e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzcxNzkzM2M0MGZiZjkzNmFhOTI4ODUxM2VmZTE5YmRhNDYwMWVmYzBlNGVjYWQyZTAyM2IwYzFkMjg0NDRiIn19fQ=="),
                "Better NEF HTTP Flaming Fist texture is recognized");
        check(EntityVisualPolicy.isFlamingFistTexture("e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHBzOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzc3MTc5MzNjNDBmYmY5MzZhYTkyODg1MTNlZmUxOWJkYTQ2MDFlZmMwZTRlY2FkMmUwMjNiMGMxZDI4NDQ0YiJ9fX0="),
                "Better NEF HTTPS Flaming Fist texture is recognized");
        check(!EntityVisualPolicy.isFlamingFistTexture(null), "null texture is not recognized");
        check(!EntityVisualPolicy.isFlamingFistTexture(""), "empty texture is not recognized");
        check(!EntityVisualPolicy.isFlamingFistTexture("unrelated-texture"),
                "unrelated skull texture is not hidden");
        System.out.println("EntityVisualPolicyTests: all 12 tests passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
