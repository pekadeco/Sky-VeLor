package com.arena.pekaclient.dungeon;

/** Maps native map-pixel coordinates into the visible minimap, using Better NEF's player-centered rotation. */
final class DungeonMapProjection {
    private final boolean rotated;
    private final float pivotX;
    private final float pivotY;
    private final double cos;
    private final double sin;
    private final float zoom;

    DungeonMapProjection(boolean rotated, float pivotX, float pivotY, float angle) {
        this(rotated, pivotX, pivotY, angle, 1.0F);
    }

    DungeonMapProjection(boolean rotated, float pivotX, float pivotY, float angle, float zoom) {
        this.rotated = rotated;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        double radians = Math.toRadians(angle);
        this.cos = Math.cos(radians);
        this.sin = Math.sin(radians);
        // Values above 1 deliberately crop the map viewport for content zoom-in; the widget bounds stay fixed.
        this.zoom = Math.max(0.25F, Math.min(3.0F, zoom));
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

    float[] project(float rawX, float rawY) {
        if (!rotated) {
            return new float[]{64.0F + (rawX - 64.0F) * zoom,
                    64.0F + (rawY - 64.0F) * zoom};
        }
        double dx = rawX - pivotX;
        double dy = rawY - pivotY;
        return new float[]{(float) (64.0D + zoom * (dx * cos - dy * sin)),
                (float) (64.0D + zoom * (dx * sin + dy * cos))};
    }
}
