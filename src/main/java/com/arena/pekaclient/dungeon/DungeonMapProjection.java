package com.arena.pekaclient.dungeon;

/** Maps native map-pixel coordinates into the visible minimap with player-centered rotation. */
final class DungeonMapProjection {
    private final boolean rotated;
    private final float pivotX;
    private final float pivotY;
    private final double cos;
    private final double sin;
    private final float angle;
    private final float zoom;

    DungeonMapProjection(boolean rotated, float pivotX, float pivotY, float angle) {
        this(rotated, pivotX, pivotY, angle, 1.0F);
    }

    DungeonMapProjection(boolean rotated, float pivotX, float pivotY, float angle, float zoom) {
        this.rotated = rotated;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.angle = angle;
        double radians = Math.toRadians(angle);
        this.cos = Math.cos(radians);
        this.sin = Math.sin(radians);
        // Values above 1 deliberately crop the map viewport for content zoom-in; the widget bounds stay fixed.
        // AppleSettings exposes up to 500%, so projection must not silently cut a valid saved/UI value back to 300%.
        this.zoom = Math.max(0.25F, Math.min(5.0F, zoom));
    }

    /**
     * Returns the base fit factor that keeps all four corners inside the viewport at every yaw.
     * The user-facing content zoom may multiply this factor afterward. The fit itself is based on
     * the farthest corner's radius from the player pivot, so turning cannot change the fit factor.
     */
    static float fitZoom(float pivotX, float pivotY) {
        float[] cornersX = {0.0F, 128.0F, 0.0F, 128.0F};
        float[] cornersY = {0.0F, 0.0F, 128.0F, 128.0F};
        double maxRadius = 0.0D;
        for (int i = 0; i < cornersX.length; i++) {
            double dx = cornersX[i] - pivotX;
            double dy = cornersY[i] - pivotY;
            double radius = Math.sqrt(dx * dx + dy * dy);
            if (radius > maxRadius) maxRadius = radius;
        }
        if (maxRadius <= 0.0D) return 1.0F;
        // Keep a four-pixel inset in native map coordinates at every rotation angle.
        return (float) Math.min(1.0D, 60.0D / maxRadius);
    }

    float zoom() {
        return zoom;
    }

    /** Rotation used by map-relative puzzle-label glyphs; static maps deliberately keep text level. */
    float textRotation() {
        return rotated ? angle : 0.0F;
    }

    float[] project(float rawX, float rawY) {
        float[] result = new float[2];
        project(rawX, rawY, result);
        return result;
    }

    /** Projects into caller-owned storage so map labels do not allocate an array for each candidate position. */
    void project(float rawX, float rawY, float[] result) {
        if (result == null || result.length < 2) throw new IllegalArgumentException("result must have two slots");
        if (!rotated) {
            result[0] = 64.0F + (rawX - 64.0F) * zoom;
            result[1] = 64.0F + (rawY - 64.0F) * zoom;
            return;
        }
        double dx = rawX - pivotX;
        double dy = rawY - pivotY;
        result[0] = (float) (64.0D + zoom * (dx * cos - dy * sin));
        result[1] = (float) (64.0D + zoom * (dx * sin + dy * cos));
    }
}
