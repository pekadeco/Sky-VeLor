package com.arena.pekaclient.chat;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict Roman-numeral parser and text converter for bounded, standalone tokens. */
public final class RomanNumerals {
    private static final Pattern TOKEN = Pattern.compile("(?<![A-Za-z])(?i)(?=[MDCLXVI])(M{0,3}(?:CM|CD|D?C{0,3})(?:XC|XL|L?X{0,3})(?:IX|IV|V?I{0,3}))(?![A-Za-z])");
    private RomanNumerals() { }

    /** Returns 1..3999 for canonical Roman numerals; returns -1 for invalid input. */
    public static int parse(String value) {
        if (value == null || value.length() == 0) return -1;
        String s = value.toUpperCase(Locale.ROOT);
        if (!TOKEN.matcher(s).matches() || s.length() == 0) return -1;
        int total = 0, previous = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            int v = valueOf(s.charAt(i));
            if (v < previous) total -= v; else { total += v; previous = v; }
        }
        return total >= 1 && total <= 3999 ? total : -1;
    }

    /** Converts standalone canonical Roman tokens to decimal digits, preserving surrounding text. */
    public static String convertInText(String text) {
        if (text == null || text.isEmpty()) return text == null ? "" : text;
        Matcher m = TOKEN.matcher(text);
        StringBuffer out = new StringBuffer(text.length());
        while (m.find()) {
            String token = m.group();
            int number = parse(token);
            // Do not turn empty matches into text or rewrite ordinary words.
            String replacement = number > 0 ? Integer.toString(number) : token;
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static int valueOf(char c) {
        switch (c) { case 'I': return 1; case 'V': return 5; case 'X': return 10; case 'L': return 50; case 'C': return 100; case 'D': return 500; case 'M': return 1000; default: return 0; }
    }
}
