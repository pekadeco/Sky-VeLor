package com.arena.pekaclient.chat;

/** Pure geometry used by the live default-chat renderer and headless rounded/group-bound tests. */
public final class ChatRenderGeometry {
    private ChatRenderGeometry() {
    }

    /** Lightweight immutable-style bounds used by headless grouped-line checks. */
    public static final class Bounds {
        public final float x;
        public final float y;
        public final float w;
        public final float h;

        public Bounds(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = Math.max(0f, w);
            this.h = Math.max(0f, h);
        }

        public float bottom() { return y + h; }
        public boolean contains(float px, float py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    public static final class Rect {
        public float x;
        public float y;
        public float w;
        public float h;
        public float radius;

        public Rect set(float nextX, float nextY, float nextW, float nextH, float requestedRadius) {
            x = nextX;
            y = nextY;
            w = Math.max(0f, nextW);
            h = Math.max(0f, nextH);
            radius = Math.max(0f, Math.min(requestedRadius, Math.min(w, h) * .5f));
            return this;
        }

        public boolean contains(float px, float py) {
            return px >= x && py >= y && px <= x + w && py <= y + h;
        }
    }

    /** A rounded background has one contiguous rectangle and a radius that cannot self-intersect at any GUI scale. */
    public static Rect background(Rect out, float x, float bottom, float width, float height, float radius) {
        if (out == null) out = new Rect();
        return out.set(x, bottom - Math.max(0f, height), width, height, clampRadius(radius, width, height));
    }

    /** Computes a full-width grouped highlight rectangle for one or more wrapped chat rows, without inter-row gaps. */
    public static Bounds groupBounds(float x, float y, float width, float lineHeight, float lineGap,
                                     int lines, float verticalPad) {
        int count = Math.max(1, lines);
        float row = Math.max(0f, lineHeight);
        float gap = Math.max(0f, lineGap);
        float pad = Math.max(0f, verticalPad);
        return new Bounds(x, y, Math.max(0f, width), count * row + Math.max(0, count - 1) * gap + pad * 2f);
    }

    public static float clampRadius(float requestedRadius, float width, float height) {
        return Math.max(0f, Math.min(Math.max(0f, requestedRadius), Math.min(Math.max(0f, width),
                Math.max(0f, height)) * .5f));
    }

    /** Extends a message group over each wrapped row, preserving full row-width highlight coverage. */
    public static Rect include(Rect out, float x, float y, float width, float height) {
        if (out == null) out = new Rect();
        if (out.w <= 0f || out.h <= 0f) return out.set(x, y, width, height, 0f);
        float left = Math.min(out.x, x);
        float top = Math.min(out.y, y);
        float right = Math.max(out.x + out.w, x + Math.max(0f, width));
        float bottom = Math.max(out.y + out.h, y + Math.max(0f, height));
        return out.set(left, top, right - left, bottom - top, 0f);
    }
}
