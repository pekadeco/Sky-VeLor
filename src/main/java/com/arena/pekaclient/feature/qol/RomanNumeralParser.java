package com.arena.pekaclient.feature.qol;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts unambiguous Roman-numeral level tokens while preserving Minecraft formatting codes. */
public final class RomanNumeralParser {
    private static final Pattern VALID = Pattern.compile("^(?=.+)M{0,3}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$");
    private static final Pattern TOKEN = Pattern.compile("(?<![A-Za-z0-9])((?:\u00a7[0-9a-fk-or])*)([IVXLCDM]{1,6})(?![A-Za-z0-9])");
    private static final Set<String> LEVEL_WORDS = new HashSet<String>(Arrays.asList(
            "protection", "sharpness", "efficiency", "fortune", "silk", "touch", "unbreaking",
            "mending", "power", "punch", "flame", "infinity", "knockback", "looting", "smite",
            "bane", "arthropods", "respiration", "aqua", "affinity", "depth", "strider", "frost",
            "walker", "feather", "falling", "thorns", "growth", "rejuvenate", "wisdom", "ultimate",
            "wise", "compact", "cultivating", "champion", "expertise", "hecatomb", "mana", "vampirism",
            "syphon", "life", "steal", "execute", "ender", "slayer", "tier", "level", "floor"
    ));

    private RomanNumeralParser() {
    }

    public static int parse(String input) {
        if (input == null) return -1;
        String token = input.trim().toUpperCase(Locale.ROOT);
        if (token.isEmpty() || !VALID.matcher(token).matches()) return -1;
        int total = 0;
        for (int i = 0; i < token.length(); i++) {
            int current = valueOf(token.charAt(i));
            if (current == 0) return -1;
            if (i + 1 < token.length() && current < valueOf(token.charAt(i + 1))) total -= current;
            else total += current;
        }
        return total;
    }

    public static String replaceInString(String input) {
        if (input == null || input.isEmpty()) return input;
        Matcher matcher = TOKEN.matcher(input);
        StringBuilder out = null;
        int copiedThrough = 0;
        while (matcher.find()) {
            String roman = matcher.group(2);
            int value = parse(roman);
            if (value < 0 || value > 50 || !isSafeToken(input, matcher.start(2), roman, value)) continue;
            if (out == null) out = new StringBuilder(input.length() + 8);
            out.append(input, copiedThrough, matcher.start(2));
            out.append(value);
            copiedThrough = matcher.end(2);
        }
        if (out == null) return input;
        return out.append(input, copiedThrough, input.length()).toString();
    }

    private static boolean isSafeToken(String input, int tokenStart, String roman, int value) {
        if (roman.length() > 1) return true;
        String prefix = InventorySearchMatcher.normalize(input.substring(0, tokenStart)).trim();
        int split = Math.max(prefix.lastIndexOf(' '), prefix.lastIndexOf(':'));
        String previous = prefix.substring(split + 1).replaceAll("[^a-z]", "");
        return !previous.isEmpty() && LEVEL_WORDS.contains(previous) && value <= 10;
    }

    private static int valueOf(char c) {
        switch (c) {
            case 'I': return 1;
            case 'V': return 5;
            case 'X': return 10;
            case 'L': return 50;
            case 'C': return 100;
            case 'D': return 500;
            case 'M': return 1000;
            default: return 0;
        }
    }
}
