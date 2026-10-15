package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.Anim;

/**
 * Base class for touch- and mouse-friendly controls.
 *
 * Interaction model (works with touch because Pojav maps taps to left clicks):
 *  - onPress  : finger/mouse down
 *  - onDrag   : pointer moved while down (only sliders react to it)
 *  - onRelease: pointer up; {@code activate} is false when the gesture turned into a scroll
 */
public abstract class Widget {

    public float x, y, w, h;
    public boolean enabled = true;

    protected final Anim hover = new Anim(0f);
    protected final Anim press = new Anim(0f);
    protected boolean pressed;

    public boolean contains(int mx, int my) {
        return enabled && mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Updates the hover target from the pointer and returns whether it is over this widget. */
    protected boolean hovered(int mx, int my) {
        boolean hov = contains(mx, my);
        hover.to(hov ? 1f : 0f);
        return hov;
    }

    public void update(float dt) {
        hover.update(dt, 16f);
        press.update(dt, 22f);
    }

    public abstract void draw(int mx, int my);

    public void onPress(int mx, int my) {
        pressed = true;
        press.to(1f);
    }

    public void onDrag(int mx, int my) {
    }

    public void onRelease(int mx, int my, boolean activate) {
        pressed = false;
        press.to(0f);
    }
}
