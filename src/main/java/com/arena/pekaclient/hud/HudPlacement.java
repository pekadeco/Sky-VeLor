package com.arena.pekaclient.hud;

/**
 * Where a HUD element sits on screen, owned by PekaClient.
 *
 * Each axis stores an anchor (START, CENTER or END) and an offset in GUI pixels. Positions
 * therefore keep their relation to the screen edges when the window or GUI scale changes.
 * The absolute position is always clamped so an element cannot end up off-screen.
 *
 * This class uses no Minecraft types, so the maths can be unit-tested on its own.
 */
public final class HudPlacement {

    /** Which screen edge (or the centre) an offset is measured from. */
    public enum Anchor {
        /** Left for X, top for Y. */
        START,
        /** Centre. The offset moves the element away from the centre. */
        CENTER,
        /** Right for X, bottom for Y. The offset is the distance inward from that edge. */
        END
    }

    /** Largest offset accepted from the config file. Anything beyond this is clamped. */
    public static final int MAX_OFFSET = 100000;

    /** Guide flags returned by {@link #setFromAbsolute}. */
    public static final int GUIDE_CENTER_X = 1;
    public static final int GUIDE_CENTER_Y = 2;
    public static final int GUIDE_LEFT = 4;
    public static final int GUIDE_RIGHT = 8;
    public static final int GUIDE_TOP = 16;
    public static final int GUIDE_BOTTOM = 32;

    public Anchor anchorX;
    public Anchor anchorY;
    public int offsetX;
    public int offsetY;

    public HudPlacement(Anchor anchorX, int offsetX, Anchor anchorY, int offsetY) {
        this.anchorX = requireAnchor(anchorX);
        this.anchorY = requireAnchor(anchorY);
        this.offsetX = clampOffset(offsetX);
        this.offsetY = clampOffset(offsetY);
    }

    public HudPlacement copy() {
        return new HudPlacement(anchorX, offsetX, anchorY, offsetY);
    }

    public void set(HudPlacement other) {
        this.anchorX = other.anchorX;
        this.anchorY = other.anchorY;
        this.offsetX = other.offsetX;
        this.offsetY = other.offsetY;
    }

    /**
     * Left edge in GUI pixels for an element {@code width} wide on a screen {@code screenW} wide.
     * Always within 0 .. screenW - width when the element fits.
     */
    public int left(int screenW, int width) {
        return clampAxis(rawPosition(anchorX, offsetX, screenW, width), screenW, width);
    }

    /** Top edge in GUI pixels. Same rules as {@link #left}. */
    public int top(int screenH, int height) {
        return clampAxis(rawPosition(anchorY, offsetY, screenH, height), screenH, height);
    }

    /**
     * Sets the placement from an absolute top-left corner, which is what a drag produces.
     *
     * When {@code snap} is true and the corner is within {@code threshold} pixels of the centre or an
     * edge, it snaps there. Otherwise the element keeps the spot where it was dropped. The anchor is
     * chosen as the nearer half of the screen, so the element stays in the same place relative to the
     * nearest edge when the screen changes size.
     *
     * @return guide flags for the lines the element snapped to (0 when none)
     */
    public int setFromAbsolute(int screenW, int screenH, int width, int height,
                               int left, int top, boolean snap, int threshold) {
        int guides = 0;
        int x = clampAxis(left, screenW, width);
        int y = clampAxis(top, screenH, height);

        int centreX = (screenW - width) / 2;
        int endX = screenW - width;
        if (snap && Math.abs(x - centreX) <= threshold) {
            anchorX = Anchor.CENTER;
            offsetX = clampOffset(x - centreX);
            guides |= GUIDE_CENTER_X;
        } else if (snap && Math.abs(x) <= threshold) {
            anchorX = Anchor.START;
            offsetX = 0;
            guides |= GUIDE_LEFT;
        } else if (snap && Math.abs(x - endX) <= threshold) {
            anchorX = Anchor.END;
            offsetX = 0;
            guides |= GUIDE_RIGHT;
        } else if (x + width / 2 > screenW / 2) {
            anchorX = Anchor.END;
            offsetX = clampOffset(endX - x);
        } else {
            anchorX = Anchor.START;
            offsetX = clampOffset(x);
        }

        int centreY = (screenH - height) / 2;
        int endY = screenH - height;
        if (snap && Math.abs(y - centreY) <= threshold) {
            anchorY = Anchor.CENTER;
            offsetY = clampOffset(y - centreY);
            guides |= GUIDE_CENTER_Y;
        } else if (snap && Math.abs(y) <= threshold) {
            anchorY = Anchor.START;
            offsetY = 0;
            guides |= GUIDE_TOP;
        } else if (snap && Math.abs(y - endY) <= threshold) {
            anchorY = Anchor.END;
            offsetY = 0;
            guides |= GUIDE_BOTTOM;
        } else if (y + height / 2 > screenH / 2) {
            anchorY = Anchor.END;
            offsetY = clampOffset(endY - y);
        } else {
            anchorY = Anchor.START;
            offsetY = clampOffset(y);
        }
        return guides;
    }

    /** Puts the element back in its default corner for the given anchors and offsets. */
    public void reset(Anchor ax, int ox, Anchor ay, int oy) {
        this.anchorX = requireAnchor(ax);
        this.anchorY = requireAnchor(ay);
        this.offsetX = clampOffset(ox);
        this.offsetY = clampOffset(oy);
    }

    /**
     * Converts an older position format (raw x and y plus centre flags, written by an earlier build) to
     * the current anchor model. Used only when loading an old config file.
     */
    public static HudPlacement fromLegacy(int x, int y, boolean centerX, boolean centerY) {
        Anchor ax;
        int ox;
        if (centerX) {
            ax = Anchor.CENTER;
            ox = x;
        } else if (x < 0) {
            ax = Anchor.END;
            ox = -x;
        } else {
            ax = Anchor.START;
            ox = x;
        }
        Anchor ay;
        int oy;
        if (centerY) {
            ay = Anchor.CENTER;
            oy = y;
        } else if (y < 0) {
            ay = Anchor.END;
            oy = -y;
        } else {
            ay = Anchor.START;
            oy = y;
        }
        return new HudPlacement(ax, ox, ay, oy);
    }

    static int rawPosition(Anchor anchor, int offset, int screen, int size) {
        switch (anchor) {
            case CENTER:
                return (screen - size) / 2 + offset;
            case END:
                return screen - size - offset;
            default:
                return offset;
        }
    }

    /** Clamps a left or top value so the element lies on the screen. Oversized elements pin to 0. */
    static int clampAxis(int value, int screen, int size) {
        int max = Math.max(0, screen - size);
        if (value < 0) {
            return 0;
        }
        return value > max ? max : value;
    }

    static int clampOffset(int value) {
        if (value > MAX_OFFSET) {
            return MAX_OFFSET;
        }
        if (value < -MAX_OFFSET) {
            return -MAX_OFFSET;
        }
        return value;
    }

    private static Anchor requireAnchor(Anchor a) {
        if (a == null) {
            throw new IllegalArgumentException("HUD anchor must not be null");
        }
        return a;
    }

    @Override
    public String toString() {
        return "HudPlacement{" + anchorX + " " + offsetX + ", " + anchorY + " " + offsetY + "}";
    }
}
