package com.arena.pekaclient.gui;

import com.arena.pekaclient.widget.Widget;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * One entry in the Hub's single flat vertical flow.
 *
 * <p>Feature presentation has only two forms: {@link #ITEM} is a simple feature or a dropdown-body control, and
 * {@link #HEADER} is a one-level feature dropdown. A header owns exactly its toggle and editor-button body rows;
 * it never nests another group or an option list.</p>
 */
final class Row {
    static final int SECTION = 0;
    static final int ITEM = 1;
    static final int HEADER = 2;

    final int kind;
    final String label;
    final String desc;
    final Widget control;
    final float height;
    /** A header's existing feature switch; used only for its accurate green/red status dot. */
    final Supplier<Boolean> status;
    float y;
    /** Measured text column assigned by AppleHubScreen's shared responsive row split. */
    float labelX;
    float labelWidth;

    /** False while this header's one-level body is closed. Set by the screen before layout. */
    boolean visible = true;
    /** For HEADER rows: exactly the two body rows owned by this dropdown. */
    int bodyRows;

    private Row(int kind, String label, String desc, Widget control, float height, Supplier<Boolean> status) {
        this.kind = kind;
        this.label = label;
        this.desc = desc;
        this.control = control;
        this.height = height;
        this.status = status;
    }

    static Row section(String title) {
        return new Row(SECTION, title.toUpperCase(Locale.ROOT), null, null, 20f, null);
    }

    /** A one-level feature dropdown. Its status comes from the exact existing toggle in its body. */
    static Row dropdown(String title, String description, Supplier<Boolean> status) {
        return new Row(HEADER, title, description, null, 42f, status);
    }

    /** A simple feature row, or one of the two direct body rows in a feature dropdown. */
    static Row item(String label, String desc, Widget control) {
        return new Row(ITEM, label, desc, control, 40f, null);
    }
}
