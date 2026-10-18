package com.arena.pekaclient.chat;

import com.arena.pekaclient.gui.GuiLayoutToolkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shared geometry, wrapping, whole-message grouping, hit testing, scrolling and fade math for Chat QoL surfaces. */
public final class ChatPipeline {
    public interface Wrapper {
        List<String> wrap(String formattedText, int width);
    }

    /** One immutable source message. `groupId` survives every layout/rebuild while this message remains in history. */
    public static final class Message {
        public final Object component;
        public final String formatted;
        public final String plain;
        /** Kept for older callers; groupId is the explicit semantic name used by wrapped rows/blocks. */
        public final int id;
        public final int groupId;
        public final long receivedTick;

        public Message(Object component, String formatted, String plain, int id, long receivedTick) {
            this.component = component;
            this.formatted = formatted == null ? "" : formatted;
            this.plain = ChatText.clean(plain == null ? this.formatted : plain);
            this.id = id;
            this.groupId = id;
            this.receivedTick = receivedTick;
        }
    }

    /** One visual wrapped row, linked back to its persistent message-group ID. */
    public static final class Row {
        public final Message message;
        public final String formatted;
        public final int line;
        public final int groupId;
        public final GuiLayoutToolkit.Rect bounds = new GuiLayoutToolkit.Rect();

        Row(Message message, String formatted, int line) {
            this.message = message;
            this.formatted = formatted == null ? "" : formatted;
            this.line = line;
            this.groupId = message == null ? 0 : message.groupId;
        }
    }

    /** All wrapped rows belonging to one message. Bounds deliberately cover the full wrapped group. */
    public static final class Block {
        public final Message message;
        public final int groupId;
        public final List<Row> rows = new ArrayList<Row>();
        public final GuiLayoutToolkit.Rect bounds = new GuiLayoutToolkit.Rect();

        Block(Message message) {
            this.message = message;
            this.groupId = message == null ? 0 : message.groupId;
        }
    }

    public static final class Hit {
        public final Block block;
        public final Row row;

        Hit(Block block, Row row) {
            this.block = block;
            this.row = row;
        }

        public Message message() {
            return block == null ? null : block.message;
        }

        public int groupId() {
            return block == null ? 0 : block.groupId;
        }
    }

    private final List<Message> messages = new ArrayList<Message>();
    private final List<Block> blocks = new ArrayList<Block>();
    private final GuiLayoutToolkit.Scroll scroll = new GuiLayoutToolkit.Scroll();
    private final GuiLayoutToolkit.Rect viewport = new GuiLayoutToolkit.Rect();
    private int limit = 100;
    private int rowHeight = 9;
    private int lineSpacing;
    private int width = 1;
    private boolean open;
    private Hit hovered;

    public void add(Message message) {
        if (message == null) return;
        messages.add(0, message);
        while (messages.size() > limit) messages.remove(messages.size() - 1);
    }

    public void clear() {
        messages.clear();
        blocks.clear();
        scroll.reset();
        hovered = null;
    }

    public int messageCount() { return messages.size(); }
    public List<Message> messages() { return Collections.unmodifiableList(messages); }
    public List<Block> blocks() { return Collections.unmodifiableList(blocks); }
    public GuiLayoutToolkit.Rect viewport() { return viewport; }
    public GuiLayoutToolkit.Scroll scroll() { return scroll; }
    public Hit hovered() { return hovered; }

    public void setLimit(int next) {
        limit = Math.max(10, Math.min(1000, next));
        while (messages.size() > limit) messages.remove(messages.size() - 1);
    }

    public void setOpen(boolean value) { open = value; }
    public boolean isOpen() { return open; }

    /** Exact same method is called for all native chat targets. Newest content anchors at the viewport bottom. */
    public void layout(int x, int y, int w, int h, int scaledTextWidth, int nextRowHeight, int nextLineSpacing,
                       Wrapper wrapper) {
        viewport.set(x, y, Math.max(1, w), Math.max(1, h));
        width = Math.max(1, scaledTextWidth);
        rowHeight = Math.max(1, nextRowHeight);
        lineSpacing = Math.max(-2, nextLineSpacing);
        blocks.clear();
        float totalHeight = 0f;
        for (Message message : messages) {
            Block block = new Block(message);
            List<String> lines = wrapper == null ? Collections.singletonList(message.formatted)
                    : wrapper.wrap(message.formatted, width);
            if (lines == null || lines.isEmpty()) lines = Collections.singletonList("");
            for (int line = 0; line < lines.size(); line++) {
                block.rows.add(new Row(message, lines.get(line), line));
            }
            float blockHeight = block.rows.size() * rowHeight + Math.max(0, block.rows.size() - 1) * lineSpacing;
            block.bounds.set(x, 0, w, blockHeight);
            totalHeight += blockHeight;
            blocks.add(block);
        }
        scroll.setMaximum(Math.max(0f, totalHeight - viewport.h));
        float cursor = viewport.y + viewport.h + scroll.value();
        for (Block block : blocks) {
            cursor -= block.bounds.h;
            block.bounds.set(x, cursor, w, block.bounds.h);
            float rowY = cursor;
            for (Row row : block.rows) {
                row.bounds.set(x, rowY, w, rowHeight);
                rowY += rowHeight + lineSpacing;
            }
        }
    }

    public void wheel(float notches, boolean shift, float multiplier) {
        scroll.wheel(notches, shift, multiplier, rowHeight * 3f);
    }

    public void update(float seconds, boolean smooth) {
        scroll.update(seconds, smooth);
    }

    /** x intentionally spans the whole message group/window, not just visible glyph pixels. */
    public Hit hit(float mouseX, float mouseY) {
        hovered = null;
        if (!viewport.contains(mouseX, mouseY)) return null;
        for (Block block : blocks) {
            if (!block.bounds.contains(mouseX, mouseY)) continue;
            for (Row row : block.rows) {
                if (row.bounds.contains(mouseX, mouseY)) {
                    hovered = new Hit(block, row);
                    return hovered;
                }
            }
            if (!block.rows.isEmpty()) {
                hovered = new Hit(block, block.rows.get(0));
                return hovered;
            }
        }
        return null;
    }

    public boolean shouldFade(Message message, long currentTick, int fadeSeconds) {
        return !(open || fadeSeconds <= 0) && currentTick - message.receivedTick > fadeSeconds * 20L;
    }

    public float fadeAlpha(Message message, long currentTick, int fadeSeconds) {
        if (open || fadeSeconds <= 0) return 1f;
        long age = Math.max(0L, currentTick - message.receivedTick);
        long start = Math.max(0, fadeSeconds * 20L - 20L);
        if (age <= start) return 1f;
        return GuiLayoutToolkit.clamp(1f - (age - start) / 20f, 0f, 1f);
    }
}
