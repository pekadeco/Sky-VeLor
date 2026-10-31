package com.arena.pekaclient.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minecraft-free visibility and diagnostic policy for the replacement Default Chat renderer. Closed and open chat
 * both honor the remembered selected tab; the bounded debug history backs /lunaria chatdebug.
 */
public final class ChatVisibilityAudit {

    public static final int HISTORY_LIMIT = 15;

    private ChatVisibilityAudit() {
    }

    public enum Reason {
        CLOSED_ACTIVE_TAB("visible/closed-active-tab"),
        CLOSED_TAB_FILTER("hidden/closed-tab-filter"),
        OPEN_ACTIVE_TAB("visible/open-active-tab"),
        OPEN_TAB_FILTER("hidden/open-tab-filter"),
        FADE_EXPIRED("hidden/closed-fade-expired"),
        LINE_LIMIT("hidden/line-limit-or-scroll"),
        CACHE_MISSING("hidden/not-in-chat-cache");

        private final String text;

        Reason(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    public static final class Decision {
        public final boolean visible;
        public final Reason reason;

        private Decision(boolean visible, Reason reason) {
            this.visible = visible;
            this.reason = reason;
        }

        public String describe() {
            return reason.text();
        }
    }

    /** One compact group/message in the command's newest-first 15-entry history. */
    public static final class Entry {
        public final int groupId;
        public final String clean;
        public final int count;

        private Entry(int groupId, String clean, int count) {
            this.groupId = groupId;
            this.clean = clean == null ? "" : clean;
            this.count = Math.max(1, count);
        }
    }

    /** Bounded, newest-first history. A compact replacement updates its existing group instead of adding a fake line. */
    public static final class History {
        private final List<Entry> entries = new ArrayList<Entry>();

        public void record(int groupId, String clean, int count) {
            if (groupId != 0) {
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).groupId == groupId) {
                        entries.set(i, new Entry(groupId, clean, count));
                        return;
                    }
                }
            }
            entries.add(0, new Entry(groupId, clean, count));
            while (entries.size() > HISTORY_LIMIT) {
                entries.remove(entries.size() - 1);
            }
        }

        public List<Entry> entries() {
            return Collections.unmodifiableList(entries);
        }

        public void clear() {
            entries.clear();
        }
    }

    /**
     * Computes a status for one group. {@code linesBefore} is the number of rows closer to the chat bottom, and
     * {@code visibleLines} is Minecraft's current unfocused/focused line capacity. Open mode deliberately ignores
     * closed-chat fading; players may reopen chat to inspect old cached text.
     */
    public static Decision decide(boolean chatOpen, boolean inCache, boolean matchesActiveTab,
                                  int ageTicks, int fadeSeconds, int linesBefore, int visibleLines) {
        if (!inCache) {
            return new Decision(false, Reason.CACHE_MISSING);
        }
        if (!matchesActiveTab) {
            return new Decision(false, chatOpen ? Reason.OPEN_TAB_FILTER : Reason.CLOSED_TAB_FILTER);
        }
        if (!chatOpen && fadeSeconds > 0 && ageTicks >= fadeSeconds * 20) {
            return new Decision(false, Reason.FADE_EXPIRED);
        }
        if (visibleLines <= 0 || linesBefore >= visibleLines) {
            return new Decision(false, Reason.LINE_LIMIT);
        }
        return new Decision(true, chatOpen ? Reason.OPEN_ACTIVE_TAB : Reason.CLOSED_ACTIVE_TAB);
    }
}
