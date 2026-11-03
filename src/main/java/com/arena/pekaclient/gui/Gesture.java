package com.arena.pekaclient.gui;

import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.Widget;

/** Implemented by a compact composite control whose horizontal drag changes a value instead of scrolling. */
interface HorizontalDragWidget {
}

/**
 * Decides what a press-and-drag means on a scrollable list. Used for mouse and touch alike.
 *
 * - A press that never moves past {@link #THRESHOLD} is a tap. The widget under it activates on release.
 * - A horizontal drag that starts on a slider changes the slider.
 * - Any other drag scrolls the list. The press is cancelled, so a swipe never activates a button,
 *   toggle, segment or dropdown.
 */
final class Gesture {

    /** Pixels the pointer must move before the gesture is decided. */
    static final float THRESHOLD = 5f;

    static final int PENDING = 0;
    static final int SLIDER = 1;
    static final int SCROLL = 2;

    /** Fling is only kept if the pointer was moving this recently when released (nanoseconds). */
    private static final long FLING_WINDOW_NS = 80_000_000L;

    Widget target;
    int kind;
    float startX;
    float startY;
    float startScroll;
    private float lastY;
    private long lastNs;
    private float velocity;

    void begin(Widget widget, float x, float y, float scroll) {
        target = widget;
        kind = PENDING;
        startX = x;
        startY = y;
        startScroll = scroll;
        lastY = y;
        lastNs = System.nanoTime();
        velocity = 0f;
    }

    /** Classifies the gesture from the pointer position. Returns the kind after this move. */
    int move(float x, float y) {
        if (kind == PENDING) {
            float dx = x - startX;
            float dy = y - startY;
            if (Math.abs(dx) > THRESHOLD || Math.abs(dy) > THRESHOLD) {
                boolean horizontalControl = target instanceof AppleSlider || target instanceof HorizontalDragWidget;
                kind = (horizontalControl && Math.abs(dx) >= Math.abs(dy)) ? SLIDER : SCROLL;
            }
        }
        return kind;
    }

    /** Scroll value for a drag at pointer height y, using the scroll-speed factor (1.0 = 1:1). */
    float scrollAt(float y, float speed) {
        return startScroll + (startY - y) * speed;
    }

    /** Records the pointer speed while scrolling, for the fling on release. */
    void track(float y, float speed) {
        if (kind != SCROLL) {
            return;
        }
        long now = System.nanoTime();
        float dt = (now - lastNs) / 1.0e9f;
        if (dt > 0.002f) {
            float v = (lastY - y) / dt * speed;
            velocity = velocity * 0.5f + v * 0.5f;
            lastY = y;
            lastNs = now;
        }
    }

    /** Fling velocity on release, or 0 if the pointer had paused before lifting. */
    float flingVelocity() {
        if (kind != SCROLL) {
            return 0f;
        }
        return System.nanoTime() - lastNs < FLING_WINDOW_NS ? velocity : 0f;
    }
}
