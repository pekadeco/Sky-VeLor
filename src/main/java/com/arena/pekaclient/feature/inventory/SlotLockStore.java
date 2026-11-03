package com.arena.pekaclient.feature.inventory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Local, versioned persistence for player-inventory slot locks and multi-source hotbar bindings. */
public final class SlotLockStore {
    public static final int SLOT_COUNT = 40;
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_INVENTORY_START = 9;
    public static final int MAIN_INVENTORY_END = 35;

    private static final Logger LOGGER = Logger.getLogger("Lunaria");
    private static final String VERSION = "1";

    private final boolean[] locked = new boolean[SLOT_COUNT];
    private final int[] boundTo = new int[SLOT_COUNT];
    private File file;
    private boolean loaded;
    private boolean failed;
    private String failureReason = "";

    public SlotLockStore() {
        Arrays.fill(boundTo, -1);
    }

    /** Loads the current Lunaria file, with no compatibility fallback. */
    public synchronized void load(File target) {
        load(target, new File[0]);
    }

    /**
     * Loads the current target when it exists; otherwise reads the first intact legacy source without altering it.
     * All later mutations write only {@code target}. This makes a prior-release file a read-only migration input,
     * preserves it for rollback, and prevents an old interrupted-write file from being promoted in place.
     */
    public synchronized void load(File target, File... legacyReadTargets) {
        if (loaded) return;
        if (target == null) {
            fail("Slot-lock settings path is unavailable.");
            loaded = true;
            return;
        }
        file = target;
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            fail("Could not create the slot-lock settings folder.");
            loaded = true;
            return;
        }

        // A current-target recovery file always takes precedence over any historical source. It belongs to this
        // target only, so promoting it cannot modify a compatibility input.
        File temp = new File(file.getPath() + ".tmp");
        if (temp.isFile()) {
            Properties recovery = readStrict(temp);
            if (recovery != null) {
                apply(recovery);
                try {
                    try {
                        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException unsupported) {
                        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    loaded = true;
                    return;
                } catch (IOException | SecurityException ex) {
                    LOGGER.log(Level.WARNING, "Could not promote recovered slot-lock settings " + temp, ex);
                }
                fail("Could not recover the slot-lock settings. Inventory actions involving player slots are restricted until repair.");
                loaded = true;
                return;
            }
            if (file.isFile()) {
                Properties main = readStrict(file);
                if (main != null) apply(main);
            }
            fail("The slot-lock recovery file is malformed. The source files were preserved; risky inventory actions are restricted.");
            loaded = true;
            return;
        }

        if (file.isFile()) {
            Properties values = readStrict(file);
            if (values == null) {
                fail("The slot-lock settings file is malformed or unreadable. Risky inventory actions are restricted.");
            } else {
                apply(values);
            }
            loaded = true;
            return;
        }

        File legacy = firstLegacyFile(legacyReadTargets, target);
        if (legacy != null) {
            Properties values = readStrict(legacy);
            if (values == null) {
                fail("A legacy slot-lock settings file is malformed or unreadable. The source file was preserved; risky inventory actions are restricted.");
            } else {
                apply(values);
            }
        }
        loaded = true;
    }

    private static File firstLegacyFile(File[] candidates, File target) {
        if (candidates == null) return null;
        for (File candidate : candidates) {
            if (candidate == null || !candidate.isFile()) continue;
            if (target != null && candidate.equals(target)) continue;
            return candidate;
        }
        return null;
    }

    /**
     * A binding is stored on both endpoints, as in Better NEF. Each endpoint is automatically
     * protected while the pair exists; the relation itself is the persisted source of that lock.
     */
    public synchronized boolean isLocked(int index) {
        ensureLoaded();
        if (!isValidIndex(index)) return false;
        if (locked[index] || boundTo[index] >= 0) return true;
        // Keep legacy one-way settings safe until they are normalized by load().
        if (isHotbarIndex(index)) {
            for (int source = MAIN_INVENTORY_START; source < SLOT_COUNT; source++) {
                if (boundTo[source] == index) return true;
            }
        }
        return false;
    }

    /** Returns only a user-created lock, excluding the automatic lock implied by a binding. */
    public synchronized boolean isExplicitlyLocked(int index) {
        ensureLoaded();
        return isValidIndex(index) && locked[index];
    }

    public synchronized int getBoundTo(int index) {
        ensureLoaded();
        return isValidIndex(index) ? boundTo[index] : -1;
    }

    public synchronized int getSourceForHotbar(int hotbarIndex) {
        ensureLoaded();
        if (!isHotbarIndex(hotbarIndex)) return -1;
        int preferredSource = boundTo[hotbarIndex];
        if (isBindableSourceIndex(preferredSource) && boundTo[preferredSource] == hotbarIndex) {
            return preferredSource;
        }
        // Legacy settings may contain only source->hotbar values. Normalize the preferred
        // source in memory without discarding any additional source bound to the same hotbar.
        for (int index = MAIN_INVENTORY_START; index < SLOT_COUNT; index++) {
            if (boundTo[index] == hotbarIndex) return index;
        }
        return -1;
    }

    /** Returns all bound inventory/armor slots for a hotbar target in stable slot order. */
    public synchronized int[] getSourcesForHotbar(int hotbarIndex) {
        ensureLoaded();
        if (!isHotbarIndex(hotbarIndex)) return new int[0];
        int count = 0;
        for (int source = MAIN_INVENTORY_START; source < SLOT_COUNT; source++) {
            if (boundTo[source] == hotbarIndex) count++;
        }
        int[] sources = new int[count];
        int offset = 0;
        for (int source = MAIN_INVENTORY_START; source < SLOT_COUNT; source++) {
            if (boundTo[source] == hotbarIndex) sources[offset++] = source;
        }
        return sources;
    }

    public synchronized int lockedCount() {
        ensureLoaded();
        int count = 0;
        for (int index = 0; index < SLOT_COUNT; index++) {
            if (isLocked(index)) count++;
        }
        return count;
    }

    public synchronized int bindingCount() {
        ensureLoaded();
        int count = 0;
        for (int index = MAIN_INVENTORY_START; index < SLOT_COUNT; index++) {
            if (boundTo[index] >= 0) count++;
        }
        return count;
    }

    public synchronized boolean hasFailed() {
        ensureLoaded();
        return failed;
    }

    public synchronized String getFailureReason() {
        ensureLoaded();
        return failureReason;
    }

    public synchronized ChangeResult toggleLock(int index) {
        ensureLoaded();
        if (!isValidIndex(index)) return ChangeResult.INVALID_SLOT;
        if (failed) return ChangeResult.STORAGE_UNAVAILABLE;
        boolean[] oldLocked = locked.clone();
        int[] oldBindings = boundTo.clone();
        locked[index] = !locked[index];
        if (locked[index]) {
            clearBindingAt(index);
            if (isHotbarIndex(index)) clearBindingsToHotbar(index);
        }
        if (!saveCurrent()) return rollback(oldLocked, oldBindings);
        return locked[index] ? ChangeResult.LOCKED : ChangeResult.UNLOCKED;
    }

    public synchronized ChangeResult bind(int inventoryIndex, int hotbarIndex) {
        ensureLoaded();
        if (!isBindableSourceIndex(inventoryIndex) || hotbarIndex < 0 || hotbarIndex >= HOTBAR_SIZE) {
            return ChangeResult.INVALID_BINDING;
        }
        if (failed) return ChangeResult.STORAGE_UNAVAILABLE;
        if (locked[inventoryIndex] || locked[hotbarIndex]) return ChangeResult.LOCKED_SLOT;

        boolean[] oldLocked = locked.clone();
        int[] oldBindings = boundTo.clone();
        int current = boundTo[inventoryIndex];
        if (current == hotbarIndex) {
            clearBindingAt(inventoryIndex);
            ChangeResult result = saveCurrent() ? ChangeResult.UNBOUND : rollback(oldLocked, oldBindings);
            return result == ChangeResult.UNBOUND ? ChangeResult.UNBOUND : result;
        }
        clearBindingAt(inventoryIndex);
        // A hotbar slot may have several linked inventory/armor sources. Keep all of them
        // and make the newest source the target's default when using the swap gesture on the hotbar.
        boundTo[inventoryIndex] = hotbarIndex;
        boundTo[hotbarIndex] = inventoryIndex;
        if (!saveCurrent()) return rollback(oldLocked, oldBindings);
        return ChangeResult.BOUND;
    }

    /** Completes a hold-to-bind gesture after the source was toggled by the key-down event. */
    public synchronized ChangeResult bindFromLockGesture(int inventoryIndex, int hotbarIndex) {
        ensureLoaded();
        if (!isBindableSourceIndex(inventoryIndex) || hotbarIndex < 0 || hotbarIndex >= HOTBAR_SIZE) {
            return ChangeResult.INVALID_BINDING;
        }
        if (failed) return ChangeResult.STORAGE_UNAVAILABLE;
        if (locked[hotbarIndex]) return ChangeResult.LOCKED_SLOT;

        boolean[] oldLocked = locked.clone();
        int[] oldBindings = boundTo.clone();
        // The source->target relationship is retained for every bound item. The hotbar stores
        // only its preferred source so a swap on the target remains deterministic.
        clearBindingAt(inventoryIndex);
        locked[inventoryIndex] = false;
        boundTo[inventoryIndex] = hotbarIndex;
        boundTo[hotbarIndex] = inventoryIndex;
        if (!saveCurrent()) return rollback(oldLocked, oldBindings);
        return ChangeResult.BOUND;
    }

    public synchronized boolean clearAll() {
        ensureLoaded();
        if (failed) return false;
        boolean[] oldLocked = locked.clone();
        int[] oldBindings = boundTo.clone();
        Arrays.fill(locked, false);
        Arrays.fill(boundTo, -1);
        if (saveCurrent()) return true;
        System.arraycopy(oldLocked, 0, locked, 0, SLOT_COUNT);
        System.arraycopy(oldBindings, 0, boundTo, 0, SLOT_COUNT);
        fail("Could not save cleared slot-lock settings. Previous settings were restored in memory.");
        return false;
    }

    private ChangeResult rollback(boolean[] oldLocked, int[] oldBindings) {
        System.arraycopy(oldLocked, 0, locked, 0, SLOT_COUNT);
        System.arraycopy(oldBindings, 0, boundTo, 0, SLOT_COUNT);
        fail("Could not save slot-lock settings. Changes were rolled back; inventory actions are restricted until repair.");
        return ChangeResult.STORAGE_ERROR;
    }

    /** Removes one source link, or every link attached to a hotbar being manually locked. */
    private void clearBindingAt(int index) {
        if (!isValidIndex(index)) return;
        if (isHotbarIndex(index)) {
            boundTo[index] = -1;
            for (int source = MAIN_INVENTORY_START; source < SLOT_COUNT; source++) {
                if (boundTo[source] == index) boundTo[source] = -1;
            }
            return;
        }

        int target = boundTo[index];
        boundTo[index] = -1;
        if (isHotbarIndex(target)) refreshPreferredSource(target);
        // Clean up any old reciprocal-only references before the next save.
        for (int hotbar = 0; hotbar < HOTBAR_SIZE; hotbar++) {
            if (boundTo[hotbar] == index) refreshPreferredSource(hotbar);
        }
    }

    private void refreshPreferredSource(int hotbarIndex) {
        if (!isHotbarIndex(hotbarIndex)) return;
        int preferred = boundTo[hotbarIndex];
        if (isBindableSourceIndex(preferred) && boundTo[preferred] == hotbarIndex) return;
        boundTo[hotbarIndex] = -1;
        for (int source = MAIN_INVENTORY_START; source < SLOT_COUNT; source++) {
            if (boundTo[source] == hotbarIndex) {
                boundTo[hotbarIndex] = source;
                return;
            }
        }
    }

    private void clearBindingsToHotbar(int hotbarIndex) {
        if (!isHotbarIndex(hotbarIndex)) return;
        boundTo[hotbarIndex] = -1;
        for (int index = MAIN_INVENTORY_START; index < SLOT_COUNT; index++) {
            if (boundTo[index] == hotbarIndex) boundTo[index] = -1;
        }
    }

    private Properties readStrict(File source) {
        Properties values = new Properties();
        try (java.io.InputStream in = Files.newInputStream(source.toPath())) {
            values.load(in);
        } catch (IOException | IllegalArgumentException | SecurityException ex) {
            LOGGER.log(Level.WARNING, "Could not read slot-lock settings " + source, ex);
            return null;
        }
        if (!VERSION.equals(values.getProperty("version"))) return null;
        for (String key : values.stringPropertyNames()) {
            String value = values.getProperty(key);
            if ("version".equals(key)) continue;
            if (key.startsWith("locked.")) {
                int index = parseIndex(key.substring("locked.".length()));
                if (!isValidIndex(index) || !("true".equals(value) || "false".equals(value))) return null;
            } else if (key.startsWith("bound.")) {
                int index = parseIndex(key.substring("bound.".length()));
                int target = parseIndex(value);
                if (!isValidIndex(index) || (target != -1 && !isValidBindingEndpoint(index, target))) return null;
            } else {
                return null;
            }
        }
        boolean[] proposedLocked = new boolean[SLOT_COUNT];
        int[] proposedBindings = new int[SLOT_COUNT];
        Arrays.fill(proposedBindings, -1);
        for (String key : values.stringPropertyNames()) {
            if (key.startsWith("locked.")) proposedLocked[parseIndex(key.substring(7))] = Boolean.parseBoolean(values.getProperty(key));
            if (key.startsWith("bound.")) proposedBindings[parseIndex(key.substring(6))] = parseIndex(values.getProperty(key));
        }
        // Validate every source independently. Several inventory/armor slots may target the
        // same hotbar slot; the reverse hotbar entry is only the preferred source for target-side swaps.
        for (int target = 0; target < HOTBAR_SIZE; target++) {
            int preferred = proposedBindings[target];
            if (preferred >= 0) {
                if (!isBindableSourceIndex(preferred)) return null;
                int forward = proposedBindings[preferred];
                if (forward != -1 && forward != target) return null;
                if (proposedLocked[target] || proposedLocked[preferred]) return null;
            }
        }
        for (int sourceIndex = MAIN_INVENTORY_START; sourceIndex < SLOT_COUNT; sourceIndex++) {
            int target = proposedBindings[sourceIndex];
            if (target >= 0) {
                if (!isHotbarIndex(target)) return null;
                if (proposedLocked[sourceIndex] || proposedLocked[target]) return null;
            }
        }
        return values;
    }

    private static boolean isValidBindingEndpoint(int index, int target) {
        if (isBindableSourceIndex(index)) return isHotbarIndex(target);
        if (isHotbarIndex(index)) return isBindableSourceIndex(target);
        return false;
    }

    private void apply(Properties values) {
        Arrays.fill(locked, false);
        Arrays.fill(boundTo, -1);
        for (String key : values.stringPropertyNames()) {
            if (key.startsWith("locked.")) locked[parseIndex(key.substring(7))] = Boolean.parseBoolean(values.getProperty(key));
            if (key.startsWith("bound.")) boundTo[parseIndex(key.substring(6))] = parseIndex(values.getProperty(key));
        }
        int[] storedBindings = boundTo.clone();
        Arrays.fill(boundTo, -1);
        // Restore each source link first; then preserve the stored preferred source per hotbar
        // when valid, falling back to another linked source for legacy source-only settings.
        for (int source = MAIN_INVENTORY_START; source < SLOT_COUNT; source++) {
            int target = storedBindings[source];
            if (isHotbarIndex(target)) boundTo[source] = target;
        }
        for (int target = 0; target < HOTBAR_SIZE; target++) {
            int preferred = storedBindings[target];
            if (isBindableSourceIndex(preferred) && boundTo[preferred] == target) {
                boundTo[target] = preferred;
                continue;
            }
            if (isBindableSourceIndex(preferred) && boundTo[preferred] < 0) {
                // Normalize the legacy one-way form where only the hotbar stored its source.
                boundTo[preferred] = target;
                boundTo[target] = preferred;
                continue;
            }
            for (int source = MAIN_INVENTORY_START; source < SLOT_COUNT; source++) {
                if (boundTo[source] == target) {
                    boundTo[target] = source;
                    break;
                }
            }
        }
    }

    private boolean saveCurrent() {
        if (file == null) return false;
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) return false;
        File temp = new File(file.getPath() + ".tmp");
        try (FileOutputStream stream = new FileOutputStream(temp);
             Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
            writer.write("# Lunaria slot locks and bindings");
            writer.write(System.lineSeparator());
            writer.write("version=" + VERSION);
            writer.write(System.lineSeparator());
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (locked[i]) {
                    writer.write("locked." + i + "=true");
                    writer.write(System.lineSeparator());
                }
                if (boundTo[i] >= 0) {
                    writer.write("bound." + i + "=" + boundTo[i]);
                    writer.write(System.lineSeparator());
                }
            }
            writer.flush();
            stream.getFD().sync();
        } catch (IOException | SecurityException ex) {
            LOGGER.log(Level.WARNING, "Could not write slot-lock temp file " + temp, ex);
            return false;
        }
        try {
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | SecurityException ex) {
            LOGGER.log(Level.WARNING, "Could not replace slot-lock file " + file, ex);
            return false;
        }
    }

    private void ensureLoaded() {
        if (loaded) return;
        File config = new File(System.getProperty("user.dir", "."), "config");
        load(new File(config, "lunaria/slot-locks.properties"),
                new File(config, "skyvelor/slot-locks.properties"));
    }

    private static boolean isValidIndex(int index) {
        return index >= 0 && index < SLOT_COUNT;
    }

    private static boolean isMainInventoryIndex(int index) {
        return index >= MAIN_INVENTORY_START && index <= MAIN_INVENTORY_END;
    }

    private static boolean isBindableSourceIndex(int index) {
        return index >= MAIN_INVENTORY_START && index < SLOT_COUNT;
    }

    private static boolean isHotbarIndex(int index) {
        return index >= 0 && index < HOTBAR_SIZE;
    }

    private static int parseIndex(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private void fail(String reason) {
        failed = true;
        failureReason = reason == null ? "Slot-lock storage failed." : reason;
        LOGGER.warning("[Lunaria] " + failureReason);
    }

    public enum ChangeResult {
        LOCKED,
        UNLOCKED,
        BOUND,
        UNBOUND,
        LOCKED_SLOT,
        INVALID_SLOT,
        INVALID_BINDING,
        STORAGE_ERROR,
        STORAGE_UNAVAILABLE
    }
}
