package com.arena.pekaclient.gui;

import com.arena.pekaclient.widget.Widget;

import java.util.Locale;

/**
 * One entry in a settings list. Rows are laid out in a single vertical flow that the screen scrolls, and every row
 * is plain data, so search can reuse them.
 *
 * Kinds:
 *  - SECTION: a pinned label. Always visible. Rows after it belong to no group.
 *  - HEADER: a collapsible category. Click it to expand or collapse the rows under it (its group).
 *  - ITEM: a label, optional description and one control.
 *  - CUSTOM: a painter that draws its own content in the given rectangle.
 */
final class Row {

    static final int SECTION = 0;
    static final int ITEM = 1;
    static final int CUSTOM = 2;
    static final int HEADER = 3;

    /** Paints custom content (charts, stat cards) into the given card rectangle. */
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

    /** For ITEM and CUSTOM rows: the HEADER title this row belongs to, or null. */
    String group;
    /** False while the row's group is collapsed. Set by the screen before layout. */
    boolean visible = true;
    /** For HEADER rows: how many rows belong to the group. Set by the screen before layout. */
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

    /** A collapsible category. The title is shown as written. */
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
