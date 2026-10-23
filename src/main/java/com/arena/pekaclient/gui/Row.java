package com.arena.pekaclient.gui;

import com.arena.pekaclient.widget.Widget;

import java.util.Locale;

/**
 * One entry in the Hub's single vertical flow.
 *
 * <p>This deliberately keeps the small, historical Hub model: a {@link #HEADER} owns the direct rows below it
 * until the next header or section. It is an expandable category, not a nested feature tree or a second navigation
 * bar. Search can reuse the same rows because their visibility and geometry are plain data.</p>
 */
final class Row {
    static final int SECTION = 0;
    static final int ITEM = 1;
    static final int CUSTOM = 2;
    static final int HEADER = 3;

    /** Paints custom content into a card rectangle when a caller needs a non-widget row. */
    interface Painter {
        void paint(float x, float y, float w, float h);
    }

    final int kind;
    final String label;
    final String desc;
    final Widget control;
    final Painter painter;
    final float height;
    float y;
    /** Measured text column retained for legacy focused-editor row layout. */
    float labelX;
    float labelWidth;

    /** For ITEM and CUSTOM rows: the HEADER title this row belongs to, or null. */
    String group;
    /** False while the row's enclosing category is collapsed. Set by the screen before layout. */
    boolean visible = true;
    /** For HEADER rows: the direct rows contained in that category. */
    int children;

    private Row(int kind, String label, String desc, Widget control, Painter painter, float height) {
        this.kind = kind;
        this.label = label;
        this.desc = desc;
        this.control = control;
        this.painter = painter;
        this.height = height;
    }

    static Row section(String title) {
        return new Row(SECTION, title.toUpperCase(Locale.ROOT), null, null, null, 20f);
    }

    /** A historical-style collapsible category header. */
    static Row header(String title) {
        return new Row(HEADER, title, null, null, null, 32f);
    }

    static Row item(String label, String desc, Widget control) {
        return new Row(ITEM, label, desc, control, null, 40f);
    }

    static Row custom(float height, Painter painter) {
        return new Row(CUSTOM, null, null, null, painter, height);
    }
}
