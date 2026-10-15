package com.arena.pekaclient.render;

/**
 * Minimal vector icons drawn from primitives (no textures), so they stay crisp
 * at any GUI scale and need no resource pack.
 */
public final class Icons {

    public static final int HOME = 0;
    public static final int PALETTE = 1;
    public static final int BOLT = 2;
    public static final int GEAR = 3;
    public static final int SEARCH = 4;
    public static final int CHEVRON = 5;
    public static final int CHECK = 6;
    public static final int HUD = 7;
    public static final int LEAF = 8;
    public static final int TOWER = 9;
    public static final int SWORD = 10;
    public static final int FISH = 11;

    private Icons() {
    }

    /** Four-sided outline of a rectangle, using lines of thickness th. */
    private static void outline(float x, float y, float w, float h, float th, int color) {
        UIRender.line(x, y, x + w, y, th, color);
        UIRender.line(x + w, y, x + w, y + h, th, color);
        UIRender.line(x + w, y + h, x, y + h, th, color);
        UIRender.line(x, y + h, x, y, th, color);
    }

    /** Draws an icon centred on (cx, cy) fitting a square of side {@code s}. */
    public static void draw(int type, float cx, float cy, float s, int color) {
        float h = s * 0.5f;
        float th = Math.max(1f, s * 0.13f);
        switch (type) {
            case HOME:
                UIRender.triangle(cx, cy - h, cx - h, cy - 0.1f * h, cx + h, cy - 0.1f * h, color);
                UIRender.rect(cx - 0.62f * h, cy - 0.1f * h, 1.24f * h, 0.95f * h, color);
                break;
            case PALETTE:
                UIRender.ring(cx, cy, h, 0.78f * h, color);
                float d = s * 0.14f;
                UIRender.circle(cx - 0.4f * h, cy + 0.1f * h, d, color);
                UIRender.circle(cx - 0.1f * h, cy - 0.35f * h, d, color);
                UIRender.circle(cx + 0.3f * h, cy - 0.25f * h, d, color);
                break;
            case BOLT:
                UIRender.triangle(cx + 0.45f * h, cy - h, cx - 0.45f * h, cy + 0.12f * h, cx + 0.05f * h, cy + 0.12f * h, color);
                UIRender.triangle(cx - 0.05f * h, cy - 0.12f * h, cx + 0.45f * h, cy - 0.12f * h, cx - 0.45f * h, cy + h, color);
                break;
            case GEAR:
                UIRender.ring(cx, cy, h * 0.7f, h * 0.36f, color);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4.0;
                    float ca = (float) Math.cos(a);
                    float sa = (float) Math.sin(a);
                    UIRender.line(cx + ca * h * 0.55f, cy + sa * h * 0.55f,
                            cx + ca * h, cy + sa * h, th * 1.6f, color);
                }
                break;
            case SEARCH:
                float rr = h * 0.62f;
                float ox = cx - h * 0.18f;
                float oy = cy - h * 0.18f;
                UIRender.ring(ox, oy, rr, rr - th * 1.2f, color);
                UIRender.line(ox + rr * 0.72f, oy + rr * 0.72f, cx + h * 0.85f, cy + h * 0.85f, th * 1.4f, color);
                break;
            case HUD:
                float hx0 = cx - h;
                float hy0 = cy - 0.7f * h;
                float hx1 = cx + h;
                float hy1 = cy + 0.7f * h;
                UIRender.line(hx0, hy0, hx1, hy0, th, color);
                UIRender.line(hx1, hy0, hx1, hy1, th, color);
                UIRender.line(hx1, hy1, hx0, hy1, th, color);
                UIRender.line(hx0, hy1, hx0, hy0, th, color);
                UIRender.rect(hx0 + 0.35f * h, hy0 + 0.35f * h, 0.9f * h, 0.3f * h, color);
                break;
            case LEAF:
                UIRender.line(cx, cy + h, cx, cy - 0.1f * h, th, color);
                UIRender.line(cx, cy - 0.1f * h, cx - 0.85f * h, cy - 0.45f * h, th, color);
                UIRender.line(cx - 0.85f * h, cy - 0.45f * h, cx - 0.2f * h, cy - 0.95f * h, th, color);
                UIRender.line(cx - 0.2f * h, cy - 0.95f * h, cx, cy - 0.1f * h, th, color);
                UIRender.line(cx, cy - 0.1f * h, cx + 0.85f * h, cy - 0.45f * h, th, color);
                UIRender.line(cx + 0.85f * h, cy - 0.45f * h, cx + 0.2f * h, cy - 0.95f * h, th, color);
                UIRender.line(cx + 0.2f * h, cy - 0.95f * h, cx, cy - 0.1f * h, th, color);
                break;
            case TOWER:
                outline(cx - 0.6f * h, cy - 0.2f * h, 1.2f * h, 1.2f * h, th, color);
                for (int i = 0; i < 3; i++) {
                    outline(cx - 0.6f * h + i * 0.45f * h, cy - h, 0.3f * h, 0.8f * h, th, color);
                }
                break;
            case SWORD:
                float sx = cx - 0.2f * h;
                float sy = cy + 0.2f * h;
                UIRender.line(sx, sy, cx + 0.8f * h, cy - 0.8f * h, th * 1.3f, color);
                UIRender.line(sx - 0.25f * h, sy - 0.25f * h, sx + 0.25f * h, sy + 0.25f * h, th * 1.2f, color);
                UIRender.line(sx, sy, cx - 0.8f * h, cy + 0.8f * h, th * 1.6f, color);
                break;
            case FISH:
                UIRender.line(cx + 0.8f * h, cy, cx - 0.2f * h, cy - 0.6f * h, th, color);
                UIRender.line(cx - 0.2f * h, cy - 0.6f * h, cx - 0.5f * h, cy, th, color);
                UIRender.line(cx - 0.5f * h, cy, cx - 0.2f * h, cy + 0.6f * h, th, color);
                UIRender.line(cx - 0.2f * h, cy + 0.6f * h, cx + 0.8f * h, cy, th, color);
                UIRender.line(cx - 0.5f * h, cy, cx - 0.95f * h, cy - 0.45f * h, th, color);
                UIRender.line(cx - 0.5f * h, cy, cx - 0.95f * h, cy + 0.45f * h, th, color);
                UIRender.line(cx - 0.95f * h, cy - 0.45f * h, cx - 0.95f * h, cy + 0.45f * h, th, color);
                UIRender.rect(cx + 0.35f * h, cy - 0.15f * h, th, th, color);
                break;
            case CHEVRON:
                UIRender.line(cx - 0.55f * h, cy - 0.22f * h, cx, cy + 0.3f * h, th, color);
                UIRender.line(cx, cy + 0.3f * h, cx + 0.55f * h, cy - 0.22f * h, th, color);
                break;
            case CHECK:
                UIRender.line(cx - 0.6f * h, cy + 0.02f * h, cx - 0.15f * h, cy + 0.5f * h, th, color);
                UIRender.line(cx - 0.15f * h, cy + 0.5f * h, cx + 0.65f * h, cy - 0.5f * h, th, color);
                break;
            default:
                break;
        }
    }
}
