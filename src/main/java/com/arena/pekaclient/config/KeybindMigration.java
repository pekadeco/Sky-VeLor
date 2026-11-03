package com.arena.pekaclient.config;

import net.minecraft.client.settings.KeyBinding;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Migrates Controls records after public key-description IDs were renamed to Lunaria.
 *
 * Minecraft 1.8.9 stores bindings in options.txt under {@code key_<translation-key>}. It does not automatically
 * transfer an old description to a newly registered KeyBinding, so this tiny migration reads an explicit current
 * value first and only falls back to a historical ID when the current value is absent. It preserves unrelated lines
 * and leaves the previous file intact if an atomic replacement cannot be completed.
 */
public final class KeybindMigration {
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    public static final class Binding {
        final KeyBinding keyBinding;
        final String currentId;
        final List<String> historicIds;

        Binding(KeyBinding keyBinding, String currentId, String... historicIds) {
            this.keyBinding = keyBinding;
            this.currentId = currentId == null ? "" : currentId;
            this.historicIds = historicIds == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(Arrays.asList(((String[]) historicIds.clone())));
        }
    }

    private KeybindMigration() {
    }

    public static Binding binding(KeyBinding keyBinding, String currentId, String... historicIds) {
        return new Binding(keyBinding, currentId, historicIds);
    }

    /** Historic public IDs used by the prior release line. */
    public static String[] historicOpen() {
        return new String[]{"key.pekaclient.open"};
    }

    public static String[] historicLeap() {
        return new String[]{"key.pekaclient.leap"};
    }

    /** Friend base IDs which became Shortcut-specific controls in Lunaria. */
    public static String[] friendWardrobe() {
        return new String[]{"key.lunaria.wardrobe", "key.pekaclient.wardrobeShortcut"};
    }

    public static String[] friendPets() {
        return new String[]{"key.lunaria.pets", "key.pekaclient.petsShortcut"};
    }

    public static void migrate(File optionsFile, Binding... requested) {
        if (optionsFile == null || requested == null || requested.length == 0 || !optionsFile.isFile()) return;
        List<String> lines;
        try {
            lines = new ArrayList<String>(Files.readAllLines(optionsFile.toPath(), UTF_8));
        } catch (IOException | SecurityException ignored) {
            return;
        }
        boolean changed = apply(lines, requested);
        if (changed) writeAtomically(optionsFile, lines);
        // Current/legacy values are applied even if persistence failed, so this launch honours the player's control.
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    /** Package-visible pure parser used by the standalone migration test. */
    static boolean apply(List<String> lines, Binding... requested) {
        if (lines == null || requested == null) return false;
        boolean changed = false;
        for (Binding spec : requested) {
            if (spec == null || spec.keyBinding == null || spec.currentId.length() == 0) continue;
            Integer current = valueFor(lines, spec.currentId);
            if (current != null) {
                spec.keyBinding.setKeyCode(current.intValue());
                continue;
            }
            Integer historic = resolveValue(lines, null, spec.historicIds.toArray(new String[spec.historicIds.size()]));
            if (historic == null) continue;
            spec.keyBinding.setKeyCode(historic.intValue());
            lines.add(recordKey(spec.currentId) + ":" + historic.intValue());
            changed = true;
        }
        return changed;
    }

    /** Resolves a saved current ID first, then historical IDs in declared order. */
    static Integer resolveValue(List<String> lines, String currentId, String... historicIds) {
        Integer current = valueFor(lines, currentId);
        if (current != null) return current;
        if (historicIds != null) {
            for (String historic : historicIds) {
                Integer value = valueFor(lines, historic);
                if (value != null) return value;
            }
        }
        return null;
    }

    /** Last valid occurrence wins, matching options.txt's normal line-by-line loading behavior. */
    static Integer valueFor(List<String> lines, String keyId) {
        if (lines == null || keyId == null) return null;
        String target = recordKey(keyId);
        Integer value = null;
        for (String line : lines) {
            if (line == null) continue;
            int split = line.indexOf(':');
            if (split <= 0 || !target.equals(line.substring(0, split))) continue;
            try {
                value = Integer.valueOf(Integer.parseInt(line.substring(split + 1).trim()));
            } catch (NumberFormatException ignored) {
                // A malformed current record must not block a valid legacy binding.
            }
        }
        return value;
    }

    static String recordKey(String keyId) {
        return keyId != null && keyId.startsWith("key_") ? keyId : "key_" + keyId;
    }

    private static void writeAtomically(File target, List<String> lines) {
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory()) return;
        File temporary = new File(parent == null ? new File(".") : parent, target.getName() + ".lunaria.tmp");
        try {
            Files.write(temporary.toPath(), lines, UTF_8);
            try {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | SecurityException ignored) {
            // options.txt is Minecraft-owned. A failed migration must never prevent startup.
        }
    }
}
