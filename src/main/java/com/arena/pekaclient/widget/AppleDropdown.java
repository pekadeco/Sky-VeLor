package com.arena.pekaclient.widget;

import com.arena.pekaclient.gui.GuiDebugOverlay;
import com.arena.pekaclient.render.GuiMotion;
import com.arena.pekaclient.render.GuiStyle;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class AppleDropdown extends Widget {
    private static final float ROW_H = 21f;
    private final String[] options;
    private final int[] dots;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private float popupX;
    private float popupY;
    private float popupW;
    private float popupH;
    private float popupRowHeight = ROW_H;
    private int firstVisible;
    private int visibleCount;
    /** Optional flow reservation supplied by a screen with a measured dropdown row. */
    private boolean popupReservation;
    private float reservationX;
    private float reservationY;
    private float reservationW;
    private float reservationH;
    private final float[] rowHover;
    private long popupOpenedAt;
    private long lastPopupFrame;

    public AppleDropdown(String[] options, int[] dots, float width, float height,
                         IntSupplier getter, IntConsumer setter) {
        super(width, height);
        this.options = options == null ? new String[0] : options.clone();
        this.dots = dots == null ? null : dots.clone();
        this.getter = getter;
        this.setter = setter;
        this.rowHover = new float[this.options.length];
    }

    private int selected() {
        if (options.length == 0 || getter == null) {
            return 0;
        }
        return Math.max(0, Math.min(options.length - 1, getter.getAsInt()));
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        beginAnimatedDraw(0.025f, 0.04f, 0.02f);
        float hover = enabled ? hoverAmount : 0f;
        int fill = UIRender.mix(Theme.buttonFill, Theme.buttonHover, hover * 0.5f);
        float radius = GuiStyle.radius(GuiStyle.RADIUS_FIELD, Theme.radius, w, h);
        UIRender.roundRect(x, y, w, h, radius, fill, fill, 0.4f);
        String label = options.length == 0 ? "—" : options[selected()];
        if (label == null) label = "";
        float leftPad = dots == null ? 8f : 20f;
        float textRoom = Math.max(0f, w - leftPad - 22f);
        label = fitText(label, textRoom);
        if (dots != null && options.length > 0) {
            int index = selected();
            if (index < dots.length) {
                UIRender.circle(x + 10f, y + h * 0.5f, 3.5f, dots[index]);
            }
        }
        UIRender.text(font(), label, x + leftPad, y + (h - textHeight()) * 0.5f,
                Theme.textPrimary, 1f, false);
        float cx = x + w - 10f;
        float cy = y + h * 0.5f;
        UIRender.line(cx - 3f, cy - 1f, cx, cy + 2f, 1.2f, Theme.textSecondary);
        UIRender.line(cx, cy + 2f, cx + 3f, cy - 1f, 1.2f, Theme.textSecondary);
        drawBorder(radius, Theme.border);
        endAnimatedDraw();
    }

    /** Maximum reserved editor-flow height when this control is expanded; actual popup is capped to ten options. */
    public float reservedPopupHeight() {
        float rowHeight = Math.max(ROW_H, textHeight() + 6f);
        return Math.min(10, options.length) * rowHeight + (options.length == 0 ? 0f : 3f);
    }

    /**
     * Pins the next popup draw to an already-reserved flow area. This prevents an expanded editor dropdown from
     * overlaying a later row while retaining the regular adaptive placement for every other screen.
     */
    public void reservePopup(float x, float y, float width, float height) {
        popupReservation = true;
        reservationX = x;
        reservationY = y;
        reservationW = Math.max(1f, width);
        reservationH = Math.max(0f, height);
    }

    public void onExpand() {
        firstVisible = 0;
        popupOpenedAt = System.nanoTime();
        lastPopupFrame = 0L;
        for (int i = 0; i < rowHover.length; i++) {
            rowHover[i] = 0f;
        }
    }

    public void drawPopup(int mouseX, int mouseY, float top, float bottom, float right, float left) {
        if (options.length == 0 || w <= 0f) {
            popupH = 0f;
            visibleCount = 0;
            return;
        }
        popupW = w;
        popupRowHeight = Math.max(ROW_H, textHeight() + 6f);
        float maxHeight = Math.max(0f, bottom - top - 8f);
        visibleCount = Math.max(1, Math.min(options.length, (int) (maxHeight / popupRowHeight)));
        visibleCount = Math.min(visibleCount, 10);
        firstVisible = Math.max(0, Math.min(firstVisible, options.length - visibleCount));
        popupH = visibleCount * popupRowHeight;
        boolean useReservation = popupReservation && reservationH + .01f >= popupH;
        if (useReservation) {
            popupW = Math.min(popupW, reservationW);
            popupX = clamp(reservationX, left + 4f, Math.max(left + 4f, right - popupW - 4f));
            popupY = clamp(reservationY, top + 4f, Math.max(top + 4f, bottom - popupH - 4f));
        } else {
            popupX = clamp(x, left + 4f, Math.max(left + 4f, right - popupW - 4f));
            float belowY = y + h + 3f;
            float aboveY = y - popupH - 3f;
            float belowSpace = bottom - belowY;
            float aboveSpace = y - top;
            if (belowSpace >= popupH || belowSpace >= aboveSpace) {
                popupY = clamp(belowY, top + 4f, Math.max(top + 4f, bottom - popupH - 4f));
            } else {
                popupY = clamp(aboveY, top + 4f, Math.max(top + 4f, bottom - popupH - 4f));
            }
        }
        popupReservation = false;

        long now = System.nanoTime();
        float dt = lastPopupFrame == 0L ? 1f / 60f : Math.min(0.1f, (now - lastPopupFrame) / 1.0e9f);
        lastPopupFrame = now;
        float reveal = 1f;
        if (animationsActive() && popupOpenedAt != 0L) {
            reveal = (now - popupOpenedAt) / 180000000f;
            reveal = UIRender.clamp01(reveal);
            reveal = 1f - (1f - reveal) * (1f - reveal) * (1f - reveal);
            if (reveal >= 0.999f) {
                popupOpenedAt = 0L;
            }
        } else {
            popupOpenedAt = 0L;
        }
        popupY += (1f - reveal) * 5f;
        GuiDebugOverlay.recordControl("AppleDropdown popup", popupX, popupY, popupW, popupH);
        float previousAlpha = UIRender.alphaMul;
        UIRender.alphaMul = previousAlpha * reveal;
        float radius = GuiStyle.radius(GuiStyle.RADIUS_POPUP, Theme.radius, popupW, popupH);
        UIRender.shadow(popupX, popupY, popupW, popupH, radius);
        UIRender.roundRect(popupX, popupY, popupW, popupH, radius, Theme.popup, Theme.popup, 0.5f);
        drawBorderAt(popupX, popupY, popupW, popupH, radius, Theme.border);
        int current = selected();
        for (int row = 0; row < visibleCount; row++) {
            int index = firstVisible + row;
            float rowY = popupY + row * popupRowHeight;
            GuiDebugOverlay.recordRow("AppleDropdown option " + index, popupX, rowY, popupW, popupRowHeight);
            boolean hot = mouseX >= popupX && mouseX < popupX + popupW
                    && mouseY >= rowY && mouseY < rowY + popupRowHeight;
            float targetHover = hot ? 1f : 0f;
            if (!animationsActive()) {
                rowHover[index] = targetHover;
            } else {
                rowHover[index] = ease(rowHover[index], targetHover, dt, GuiMotion.HOVER_SETTLE_MS);
            }
            float rowScale = animationsActive() ? 1f + rowHover[index] * 0.012f : 1f;
            boolean rowTransform = Math.abs(rowScale - 1f) > 0.0005f;
            if (rowTransform) {
                UIRender.pushScaleAt(popupX + popupW * 0.5f, rowY + popupRowHeight * 0.5f, rowScale);
            }
            if (index == current) {
                UIRender.roundRect(popupX + 3f, rowY + 1f, popupW - 6f, popupRowHeight - 2f,
                        Math.max(1f, radius - 2f), Theme.cardHover, Theme.cardHover, 0.2f);
                UIRender.roundRect(popupX + 3f, rowY + 4f, 2f, popupRowHeight - 8f,
                        1f, Theme.accent, Theme.accent, 0f);
            } else if (rowHover[index] > 0.001f) {
                int rowFill = UIRender.mix(Theme.popup, Theme.buttonHover, rowHover[index]);
                UIRender.roundRect(popupX + 3f, rowY + 1f, popupW - 6f, popupRowHeight - 2f,
                        Math.max(1f, radius - 2f), rowFill, rowFill, 0.2f);
            }
            String option = options[index] == null ? "" : options[index];
            float pad = dots == null ? 10f : 22f;
            float maxTextW = Math.max(0f, popupW - pad - 8f);
            option = fitText(option, maxTextW);
            if (dots != null && index < dots.length) {
                UIRender.circle(popupX + 12f, rowY + popupRowHeight * 0.5f, 3.5f, dots[index]);
            }
            UIRender.text(font(), option, popupX + pad, rowY + (popupRowHeight - textHeight()) * 0.5f,
                    index == current ? Theme.textPrimary : Theme.textSecondary, 1f, false);
            if (rowTransform) {
                UIRender.popTransform();
            }
        }
        UIRender.alphaMul = previousAlpha;
    }

    private static void drawBorderAt(float x, float y, float w, float h, float radius, int color) {
        UIRender.roundRectRing(x, y, w, h, radius, 1f, color);
    }

    public int optionAt(int mouseX, int mouseY) {
        if (visibleCount <= 0 || mouseX < popupX || mouseX >= popupX + popupW
                || mouseY < popupY || mouseY >= popupY + popupH) {
            return -1;
        }
        int row = (int) ((mouseY - popupY) / popupRowHeight);
        int result = firstVisible + row;
        return result >= 0 && result < options.length ? result : -1;
    }

    public void choose(int index) {
        if (setter != null && index >= 0 && index < options.length) {
            if (index != selected()) {
                clickPulse = 1f;
            }
            setter.accept(index);
        }
    }

    @Override
    public void onRelease(int mouseX, int mouseY, boolean activate) {
        super.onRelease(mouseX, mouseY, activate);
    }
}
