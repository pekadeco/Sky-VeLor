package com.arena.pekaclient.chat;

import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bounded logical-message history for the one Default Chat renderer. Vanilla keeps a short wrapped draw cache;
 * this stores the original rich component once so tab changes can always rebuild their own filtered view.
 */
public final class ChatMessageHistory {

    public static final int DEFAULT_LIMIT = 500;

    /** One logical incoming message, retained in arrival order until the bounded ring evicts its oldest entry. */
    public static final class Entry {
        private final int id;
        private final int compactGroupId;
        private final IChatComponent original;
        private final String clean;
        private final String formatted;
        private int compactCount;
        private long timestamp;

        private Entry(int id, int compactGroupId, IChatComponent original, String clean, int compactCount,
                      long timestamp) {
            this.id = id;
            this.compactGroupId = compactGroupId;
            this.original = original == null ? null : original.createCopy();
            this.clean = clean == null ? "" : clean;
            this.formatted = original == null ? "" : original.getFormattedText();
            this.compactCount = Math.max(1, compactCount);
            this.timestamp = Math.max(0L, timestamp);
        }

        public int id() { return id; }
        public int compactGroupId() { return compactGroupId; }
        public String clean() { return clean; }
        public String formatted() { return formatted; }
        public int compactCount() { return compactCount; }
        public long timestamp() { return timestamp; }

        /** The renderer always receives a private copy, preserving the stored original for later compact updates. */
        public IChatComponent originalCopy() {
            return original == null ? null : original.createCopy();
        }

        private void refreshCompactCount(int count, long nextTimestamp) {
            compactCount = Math.max(1, count);
            timestamp = Math.max(0L, nextTimestamp);
        }
    }

    private final int limit;
    private final List<Entry> entries = new ArrayList<Entry>(); // oldest first
    private int nextId = 1;
    private long revision;

    public ChatMessageHistory() {
        this(DEFAULT_LIMIT);
    }

    public ChatMessageHistory(int limit) {
        this.limit = Math.max(1, limit);
    }

    /**
     * Records one incoming logical message. A compact reprint updates its existing high-id group rather than adding a
     * duplicate history item; all ordinary/blank lines receive independent entries even when vanilla assigns id zero.
     */
    public Entry record(int compactGroupId, IChatComponent original, String clean, int compactCount, long timestamp) {
        if (compactGroupId != 0) {
            Entry existing = entryForCompactGroup(compactGroupId);
            if (existing != null) {
                existing.refreshCompactCount(compactCount, timestamp);
                revision++;
                return existing;
            }
        }
        Entry created = new Entry(nextEntryId(), compactGroupId, original, clean, compactCount, timestamp);
        entries.add(created);
        while (entries.size() > limit) {
            entries.remove(0);
        }
        revision++;
        return created;
    }

    /** Finds a stored first/original component for a compact replacement before it is reprinted into vanilla chat. */
    public IChatComponent originalForCompactGroup(int compactGroupId) {
        Entry entry = entryForCompactGroup(compactGroupId);
        return entry == null ? null : entry.originalCopy();
    }

    public int size() { return entries.size(); }
    public long revision() { return revision; }

    public List<Entry> oldestFirst() {
        return Collections.unmodifiableList(new ArrayList<Entry>(entries));
    }

    public List<Entry> newestFirst() {
        List<Entry> newest = new ArrayList<Entry>(entries);
        Collections.reverse(newest);
        return Collections.unmodifiableList(newest);
    }

    public void clear() {
        if (!entries.isEmpty()) {
            entries.clear();
            revision++;
        }
    }

    private Entry entryForCompactGroup(int compactGroupId) {
        if (compactGroupId == 0) return null;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (entry.compactGroupId == compactGroupId) return entry;
        }
        return null;
    }

    private int nextEntryId() {
        int id = nextId++;
        if (nextId <= 0) nextId = 1;
        return id;
    }
}
