package com.arena.pekaclient.render;

/**
 * Cached, allocation-free-after-warmup perimeter geometry for {@link UIRender}'s rounded rectangles.
 *
 * <p>The cache stores only coordinate geometry: colour, position, opacity and outline state remain inputs to the
 * renderer. That lets a card, button, chat surface and tooltip share a shape without constructing point lists every
 * frame. Four independent corner radii are clamped to half the shortest side, which guarantees adjacent corners
 * cannot cross or leave a gap. Coordinates are GUI pixels; Minecraft's regular GUI transform scales them for every
 * GUI scale.</p>
 */
public final class RoundedRectGeometry {
    /** 16 quarter-arc subdivisions keep normal 7–12 px Apple corners genuinely round at GUI scales 1–4. */
    public static final int MAX_SEGMENTS = 16;
    public static final int CACHE_CAPACITY = 128;

    /** Unit-circle points are built once for each permitted segment count, never in the render loop. */
    private static final float[][] UNIT_X = new float[MAX_SEGMENTS + 1][];
    private static final float[][] UNIT_Y = new float[MAX_SEGMENTS + 1][];
    private static final Geometry[] CACHE = new Geometry[CACHE_CAPACITY];
    private static final Geometry EMPTY;

    private static int cacheSize;
    private static int replaceCursor;
    private static int hits;
    private static int misses;

    static {
        for (int segments = 1; segments <= MAX_SEGMENTS; segments++) {
            int pointCount = 4 * (segments + 1);
            UNIT_X[segments] = new float[pointCount];
            UNIT_Y[segments] = new float[pointCount];
            for (int corner = 0; corner < 4; corner++) {
                for (int point = 0; point <= segments; point++) {
                    double degrees = 180.0 + 90.0 * corner + 90.0 * point / segments;
                    int index = corner * (segments + 1) + point;
                    UNIT_X[segments][index] = (float) Math.cos(Math.toRadians(degrees));
                    UNIT_Y[segments][index] = (float) Math.sin(Math.toRadians(degrees));
                }
            }
        }
        EMPTY = new Geometry(0f, 0f, 0f, 0f, 0f, 0f, 1);
    }

    private RoundedRectGeometry() {
    }

    /** Immutable cached perimeter. Points proceed continuously clockwise around a box with origin (0, 0). */
    public static final class Geometry {
        public final float width;
        public final float height;
        public final float topLeft;
        public final float topRight;
        public final float bottomRight;
        public final float bottomLeft;
        public final int segments;
        public final int pointCount;
        private final float[] x;
        private final float[] y;

        private Geometry(float width, float height, float topLeft, float topRight, float bottomRight,
                         float bottomLeft, int segments) {
            this.width = width;
            this.height = height;
            this.topLeft = topLeft;
            this.topRight = topRight;
            this.bottomRight = bottomRight;
            this.bottomLeft = bottomLeft;
            this.segments = segments;
            this.pointCount = 4 * (segments + 1);
            this.x = new float[pointCount];
            this.y = new float[pointCount];
            fillPerimeter();
        }

        private void fillPerimeter() {
            float[] unitX = UNIT_X[segments];
            float[] unitY = UNIT_Y[segments];
            for (int corner = 0; corner < 4; corner++) {
                float radius = corner == 0 ? topLeft : corner == 1 ? topRight
                        : corner == 2 ? bottomRight : bottomLeft;
                float centerX = (corner == 0 || corner == 3) ? radius : width - radius;
                float centerY = corner < 2 ? radius : height - radius;
                for (int point = 0; point <= segments; point++) {
                    int out = corner * (segments + 1) + point;
                    x[out] = centerX + radius * unitX[out];
                    y[out] = centerY + radius * unitY[out];
                }
            }
        }

        public float x(int point) {
            return x[Math.max(0, Math.min(pointCount - 1, point))];
        }

        public float y(int point) {
            return y[Math.max(0, Math.min(pointCount - 1, point))];
        }
    }

    /** Returns geometry cached by width, height, all four radii and the selected quality level. */
    public static Geometry get(float width, float height, float topLeft, float topRight, float bottomRight,
                               float bottomLeft, int requestedSegments) {
        float safeWidth = Math.max(0f, width);
        float safeHeight = Math.max(0f, height);
        if (safeWidth <= 0f || safeHeight <= 0f) {
            return EMPTY;
        }
        float limit = Math.min(safeWidth, safeHeight) * .5f;
        float tl = clampRadius(topLeft, limit);
        float tr = clampRadius(topRight, limit);
        float br = clampRadius(bottomRight, limit);
        float bl = clampRadius(bottomLeft, limit);
        int segments = Math.max(1, Math.min(MAX_SEGMENTS, requestedSegments));
        for (int i = 0; i < cacheSize; i++) {
            Geometry candidate = CACHE[i];
            if (candidate != null && same(candidate, safeWidth, safeHeight, tl, tr, br, bl, segments)) {
                hits++;
                return candidate;
            }
        }
        Geometry created = new Geometry(safeWidth, safeHeight, tl, tr, br, bl, segments);
        if (cacheSize < CACHE_CAPACITY) {
            CACHE[cacheSize++] = created;
        } else {
            CACHE[replaceCursor] = created;
            replaceCursor = (replaceCursor + 1) % CACHE_CAPACITY;
        }
        misses++;
        return created;
    }

    /**
     * Uses one requested segment per GUI-pixel of the largest corner radius, then applies the selected bounded cap.
     * The former radius/2 policy visibly faceted a 12 px window corner at the persisted Balanced/Fast choices.
     */
    public static int segmentsFor(float largestRadius, int qualityCap) {
        int requested = Math.max(1, (int) Math.ceil(Math.max(0f, largestRadius)));
        return Math.max(1, Math.min(MAX_SEGMENTS, Math.min(Math.max(1, qualityCap), requested)));
    }

    public static int cacheSize() {
        return cacheSize;
    }

    public static int cacheHits() {
        return hits;
    }

    public static int cacheMisses() {
        return misses;
    }

    /** Headless regression support. Live GUI code never calls this and never clears warm geometry each frame. */
    public static void clearCacheForTests() {
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = null;
        }
        cacheSize = 0;
        replaceCursor = 0;
        hits = 0;
        misses = 0;
    }

    private static boolean same(Geometry geometry, float width, float height, float tl, float tr, float br, float bl,
                                int segments) {
        return geometry.segments == segments
                && Float.floatToIntBits(geometry.width) == Float.floatToIntBits(width)
                && Float.floatToIntBits(geometry.height) == Float.floatToIntBits(height)
                && Float.floatToIntBits(geometry.topLeft) == Float.floatToIntBits(tl)
                && Float.floatToIntBits(geometry.topRight) == Float.floatToIntBits(tr)
                && Float.floatToIntBits(geometry.bottomRight) == Float.floatToIntBits(br)
                && Float.floatToIntBits(geometry.bottomLeft) == Float.floatToIntBits(bl);
    }

    private static float clampRadius(float radius, float maximum) {
        return Math.max(0f, Math.min(maximum, radius));
    }
}
