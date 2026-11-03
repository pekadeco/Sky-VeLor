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

    /**
     * One message-backed background target. With no visible rows this returns false and clears the destination, so a
     * closed or empty chat can never leave a stale full-height panel behind. "Full area" changes vertical extent only.
     */
    public static boolean messageBackground(Rect out, float x, float messageTop, float messageBottom, float viewportTop,
                                            float chatWidth, float textWidth, boolean fitWidth, boolean fullArea,
                                            float radius) {
        if (out == null) return false;
        float top = Math.max(viewportTop, messageTop);
        float bottom = Math.max(top, messageBottom);
        if (chatWidth <= 0f || bottom <= top) {
            out.set(x, bottom, 0f, 0f, 0f);
            return false;
        }
        if (fullArea) top = Math.min(top, Math.max(0f, viewportTop));
        float width = fitWidth ? Math.min(chatWidth, Math.max(0f, textWidth + 6f)) : chatWidth;
        if (width <= 0f || bottom <= top) {
            out.set(x, bottom, 0f, 0f, 0f);
            return false;
        }
        out.set(x, top, width, bottom - top, radius);
        return true;
    }

    /**
     * Eases a non-empty message-background target without flicker while messages arrive, scroll, or fade. Empty is
     * intentionally immediate: the last fully faded line cannot leave an orphaned rounded rectangle.
     */
    public static final class BackgroundTransition {
        private static final float DURATION = .14f;
        private boolean active;
        private float startX;
        private float startY;
        private float startW;
        private float startH;
        private float targetX;
        private float targetY;
        private float targetW;
        private float targetH;
        private float elapsed;
        public final Rect rect = new Rect();

        public boolean update(Rect target, boolean visible, float seconds) {
            if (!visible || target == null || target.w <= 0f || target.h <= 0f) {
                clear();
                return false;
            }
            if (!active) {
                // A new line grows the shape upward from its baseline rather than popping a full rectangle in.
                active = true;
                rect.set(target.x, target.y + target.h, target.w, 0f, target.radius);
                copyTarget(target);
                elapsed = 0f;
            }
            if (different(target)) {
                startX = rect.x;
                startY = rect.y;
                startW = rect.w;
                startH = rect.h;
                copyTarget(target);
                elapsed = 0f;
            }
            elapsed = Math.min(DURATION, elapsed + clamp(seconds, 0f, .1f));
            float progress = easeOut(elapsed / DURATION);
            rect.set(lerp(startX, targetX, progress), lerp(startY, targetY, progress),
                    lerp(startW, targetW, progress), lerp(startH, targetH, progress), target.radius);
            return true;
        }

        public void clear() {
            active = false;
            elapsed = 0f;
            rect.set(0f, 0f, 0f, 0f, 0f);
        }

        private boolean different(Rect target) {
            return Math.abs(target.x - targetX) > .01f || Math.abs(target.y - targetY) > .01f
                    || Math.abs(target.w - targetW) > .01f || Math.abs(target.h - targetH) > .01f;
        }

        private void copyTarget(Rect target) {
            targetX = target.x;
            targetY = target.y;
            targetW = target.w;
            targetH = target.h;
            startX = rect.x;
            startY = rect.y;
            startW = rect.w;
            startH = rect.h;
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

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * amount;
    }

    private static float easeOut(float value) {
        float t = clamp(value, 0f, 1f);
        return 1f - (1f - t) * (1f - t);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : value > max ? max : value;
    }
}
