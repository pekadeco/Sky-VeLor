package com.arena.pekaclient.assist.visual;

/**
 * Mapping-free rules for the short render-only Aspect of the End/Void transition.
 *
 * <p>This class intentionally has no player, world, packet, controller, or input dependency. It can only recognize
 * an already-observed local position discontinuity and calculate a HUD veil alpha; it cannot start or steer a
 * teleport.</p>
 */
public final class AspectTeleportTransitionPolicy {

    /** Keep an observed local Aspect use armed through ordinary server response latency (about 600 ms at 20 TPS). */
    public static final int ARM_TICKS = 12;
    /** Exact use observation keeps this low enough to cover short AOTE/AOTV teleports without treating a walk as one. */
    public static final double MIN_TELEPORT_DISTANCE = 1.5D;
    public static final long FADE_IN_NANOS = 45_000_000L;
    public static final long DURATION_NANOS = 340_000_000L;
    /** Intentionally visible, but still below an opaque screen flash. */
    public static final float MAX_VEIL_ALPHA = .28F;

    private AspectTeleportTransitionPolicy() {
    }

    /** Recognizes the visible AOTE/AOTV item names without allocating stripped or lower-cased strings. */
    public static boolean isAspectTeleportItem(String displayName) {
        return containsToken(displayName, "aspect of the end") || containsToken(displayName, "aspect of the void");
    }

    /** A finite completed position jump is eligible only after the renderer has armed from an observed Aspect use. */
    public static boolean isLargePositionJump(double fromX, double fromY, double fromZ,
                                              double toX, double toY, double toZ) {
        if (!finite(fromX) || !finite(fromY) || !finite(fromZ)
                || !finite(toX) || !finite(toY) || !finite(toZ)) {
            return false;
        }
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        return dx * dx + dy * dy + dz * dz >= MIN_TELEPORT_DISTANCE * MIN_TELEPORT_DISTANCE;
    }

    /**
     * Calculates a gentle fade-in/ease-out veil. It is zero outside the fixed short duration, so a delayed or lost
     * render event cannot leave the screen dimmed.
     */
    public static float veilAlpha(long elapsedNanos) {
        if (elapsedNanos < 0L || elapsedNanos >= DURATION_NANOS) return 0F;
        if (elapsedNanos < FADE_IN_NANOS) {
            return MAX_VEIL_ALPHA * smoothStep(elapsedNanos / (float) FADE_IN_NANOS);
        }
        float fadeOut = (elapsedNanos - FADE_IN_NANOS) / (float) (DURATION_NANOS - FADE_IN_NANOS);
        return MAX_VEIL_ALPHA * (1F - smoothStep(fadeOut));
    }

    /** Case-insensitive text search that ignores Minecraft formatting pairs without allocating on a tick path. */
    static boolean containsToken(String source, String token) {
        if (source == null || token == null || token.length() == 0) return false;
        int matched = 0;
        for (int i = 0, n = source.length(); i < n; i++) {
            char c = source.charAt(i);
            if (c == '\u00a7' && i + 1 < n) {
                i++;
                continue;
            }
            char expected = token.charAt(matched);
            if (equalsIgnoreAscii(c, expected)) {
                matched++;
                if (matched == token.length()) return true;
            } else {
                matched = equalsIgnoreAscii(c, token.charAt(0)) ? 1 : 0;
            }
        }
        return false;
    }

    private static float smoothStep(float value) {
        float t = value < 0F ? 0F : (value > 1F ? 1F : value);
        return t * t * (3F - 2F * t);
    }

    private static boolean equalsIgnoreAscii(char left, char right) {
        if (left == right) return true;
        if (left >= 'A' && left <= 'Z') left = (char) (left + ('a' - 'A'));
        return left == right;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
