package com.arena.pekaclient.feature.qol;

/** Pure visibility checks for Better NEF-style entity cleanup. */
public final class EntityVisualPolicy {
    private static final String FLAMING_FIST_HTTP =
            "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzcxNzkzM2M0MGZiZjkzNmFhOTI4ODUxM2VmZTE5YmRhNDYwMWVmYzBlNGVjYWQyZTAyM2IwYzFkMjg0NDRiIn19fQ==";
    private static final String FLAMING_FIST_HTTPS =
            "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHBzOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzc3MTc5MzNjNDBmYmY5MzZhYTkyODg1MTNlZmUxOWJkYTQ2MDFlZmMwZTRlY2FkMmUwMjNiMGMxZDI4NDQ0YiJ9fX0=";

    private EntityVisualPolicy() { }

    /**
     * Hide mobs at the start of their death sequence, rather than rendering vanilla's 20-tick death animation.
     * Player entities and armor stands are deliberately excluded: neither should disappear due to zero health.
     */
    public static boolean shouldHideDyingMob(boolean entityIsDead, boolean isPlayer, boolean isArmorStand,
                                               float health, int deathTime) {
        if (isPlayer || isArmorStand) return false;
        return entityIsDead || health <= 0.0F || deathTime > 0;
    }

    /** Matches the exact HTTP/HTTPS skull texture payloads used by Better NEF's Flaming Fist detection. */
    public static boolean isFlamingFistTexture(String textureValue) {
        return FLAMING_FIST_HTTP.equals(textureValue) || FLAMING_FIST_HTTPS.equals(textureValue);
    }
}
