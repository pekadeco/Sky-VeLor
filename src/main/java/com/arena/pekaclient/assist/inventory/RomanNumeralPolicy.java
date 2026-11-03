package com.arena.pekaclient.assist.inventory;

import java.util.Locale;

/** Conservative Roman numeral conversion for tooltip level tokens, retaining all Minecraft formatting text. */
public final class RomanNumeralPolicy {
    private RomanNumeralPolicy() {
    }

    public static int parse(String value) {
        if (value == null) return -1;
        String roman = value.trim().toUpperCase(Locale.ROOT);
        if (roman.length() == 0 || roman.length() > 6) return -1;
        int result = 0;
        int previous = 0;
        for (int i = roman.length() - 1; i >= 0; i--) {
            int current = numeral(roman.charAt(i));
            if (current == 0) return -1;
            if (current < previous) result -= current;
            else {
                result += current;
                previous = current;
            }
        }
        // Canonical round trip rejects malformed forms such as IIX and VX.
        return encode(result).equals(roman) ? result : -1;
    }

    public static String replaceLevels(String input) {
        if (input == null || input.length() == 0) return input;
        StringBuilder out = null;
        int copied = 0;
        for (int i = 0; i < input.length();) {
            if (input.charAt(i) == '\u00a7' && i + 1 < input.length()) { i += 2; continue; }
            if (!isRoman(input.charAt(i))) { i++; continue; }
            int start = i;
            while (i < input.length() && isRoman(input.charAt(i))) i++;
            int end = i;
            boolean edgeBefore = start == 0 || !Character.isLetterOrDigit(input.charAt(start - 1));
            boolean edgeAfter = end == input.length() || !Character.isLetterOrDigit(input.charAt(end));
            int number = edgeBefore && edgeAfter ? parse(input.substring(start, end)) : -1;
            if (number < 1 || number > 50) continue;
            if (out == null) out = new StringBuilder(input.length() + 4);
            out.append(input, copied, start).append(number);
            copied = end;
        }
        return out == null ? input : out.append(input, copied, input.length()).toString();
    }

    private static boolean isRoman(char c) {
        return c == 'I' || c == 'V' || c == 'X' || c == 'L' || c == 'C' || c == 'D' || c == 'M';
    }

    private static int numeral(char c) {
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

    private static String encode(int value) {
        if (value < 1 || value > 3999) return "";
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (value >= values[i]) { out.append(symbols[i]); value -= values[i]; }
        }
        return out.toString();
    }
}
