package com.arena.pekaclient.chat;

import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Bounded logical-message history for the one Default Chat renderer. Vanilla keeps a short wrapped draw cache;
 * this stores the original rich component once so tab changes can always rebuild their own filtered view.
 */
public final class ChatMessageHistory {

    public static final int DEFAULT_LIMIT = 500;

    /** One logical server arrival, retained until the bounded ring evicts its oldest entry. */
    public static final class Entry {
        private final int id;
        /** Immutable process-local arrival identity. It is authoritative for every tab and render ordering decision. */
        private final long sequence;
        private final int compactGroupId;
        private final IChatComponent original;
        private final String clean;
        private final String formatted;
        private int compactCount;
        /** Last arrival/update time used only for fade and arrival animation, never for ordering. */
        private long timestamp;

        private Entry(int id, long sequence, int compactGroupId, IChatComponent original, String clean,
                      int compactCount, long timestamp) {
            this.id = id;
            this.sequence = sequence;
            this.compactGroupId = compactGroupId;
            this.original = original == null ? null : original.createCopy();
            this.clean = clean == null ? "" : clean;
            this.formatted = original == null ? "" : original.getFormattedText();
            this.compactCount = Math.max(1, compactCount);
            this.timestamp = Math.max(0L, timestamp);
        }

        public int id() { return id; }
        public long sequence() { return sequence; }
        public int compactGroupId() { return compactGroupId; }
        public String clean() { return clean; }
        public String formatted() { return formatted; }
        public int compactCount() { return compactCount; }
        public long timestamp() { return timestamp; }

        /** The renderer always receives a private copy, preserving the stored original for later compact updates. */
        public IChatComponent originalCopy() {
            return original == null ? null : original.createCopy();
        }

        /** A compact reprint refreshes fade/count but deliberately retains this entry's arrival sequence and position. */
        private void refreshCompactCount(int count, long nextTimestamp) {
            compactCount = Math.max(1, count);
            timestamp = Math.max(0L, nextTimestamp);
        }
    }

    private static final Comparator<Entry> ARRIVAL_ASCENDING = new Comparator<Entry>() {
        @Override
        public int compare(Entry left, Entry right) {
            if (left.sequence < right.sequence) return -1;
            if (left.sequence > right.sequence) return 1;
            return left.id < right.id ? -1 : left.id == right.id ? 0 : 1;
        }
    };

    private final int limit;
    private final List<Entry> entries = new ArrayList<Entry>();
    private int nextId = 1;
    /** Never resets on disconnect/history clear: an entry sequence always increases during this client process. */
    private long nextArrivalSequence = 1L;
    private long revision;

    public ChatMessageHistory() {
        this(DEFAULT_LIMIT);
    }

    public ChatMessageHistory(int limit) {
        this.limit = Math.max(1, limit);
    }

    /**
     * Records one server-origin logical arrival. A compact reprint updates its existing high-id group rather than
     * creating a local/server echo duplicate; all ordinary/blank lines receive independent entries even when vanilla
     * assigns id zero.
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
        Entry created = new Entry(nextEntryId(), nextArrivalSequence(), compactGroupId, original, clean,
                compactCount, timestamp);
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

    /** Oldest-to-newest authoritative display order, sorted by immutable arrival sequence rather than timestamp. */
    public List<Entry> arrivalOrder() {
        List<Entry> ordered = new ArrayList<Entry>(entries);
        Collections.sort(ordered, ARRIVAL_ASCENDING);
        return Collections.unmodifiableList(ordered);
    }

    /** Newest-to-oldest sequence order for bounded diagnostics. */
    public List<Entry> newestArrivalFirst() {
        List<Entry> newest = new ArrayList<Entry>(arrivalOrder());
        Collections.reverse(newest);
        return Collections.unmodifiableList(newest);
    }

    /** Compatibility alias for callers that need oldest-to-newest display order. */
    public List<Entry> oldestFirst() {
        return arrivalOrder();
    }

    /** Compatibility alias for callers that need newest-to-oldest sequence order. */
    public List<Entry> newestFirst() {
        return newestArrivalFirst();
    }

    /** Clears retained entries on disconnect but deliberately does not reset the process-local arrival sequence. */
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

    private long nextArrivalSequence() {
        // Exhaustion would require more than nine quintillion server arrivals in one client process. Failing closed is
        // preferable to silently wrapping and violating the strict chronology contract.
        if (nextArrivalSequence <= 0L || nextArrivalSequence == Long.MAX_VALUE) {
            throw new IllegalStateException("Chat arrival sequence exhausted");
        }
        return nextArrivalSequence++;
    }
}
