package com.arena.pekaclient.feature.qol;

public final class RomanNumeralParserTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("parses six", RomanNumeralParser.parse("VI") == 6);
        check("parses subtractive four", RomanNumeralParser.parse("IV") == 4);
        check("parses subtractive nine", RomanNumeralParser.parse("IX") == 9);
        check("rejects noncanonical four", RomanNumeralParser.parse("IIII") == -1);
        check("rejects invalid letters", RomanNumeralParser.parse("Z") == -1);
        check("rejects empty text", RomanNumeralParser.parse("") == -1);
        check("rejects null", RomanNumeralParser.parse(null) == -1);
        check("converts unambiguous multi-letter suffix", "Sharpness 6".equals(RomanNumeralParser.replaceInString("Sharpness VI")));
        check("keeps formatting prefix", "§6Sharpness §e6".equals(RomanNumeralParser.replaceInString("§6Sharpness §eVI")));
        check("converts single level after known enchantment", "Protection 1".equals(RomanNumeralParser.replaceInString("Protection I")));
        check("does not convert ordinary pronoun", "I am ready".equals(RomanNumeralParser.replaceInString("I am ready")));
        check("does not rewrite a normal word", "DIVINE".equals(RomanNumeralParser.replaceInString("DIVINE")));
        check("does not rewrite invalid token", "Sharpness IIII".equals(RomanNumeralParser.replaceInString("Sharpness IIII")));
        check("does not convert large rarity-like number", "Sword CL".equals(RomanNumeralParser.replaceInString("Sword CL")));
        System.out.println("Roman numeral parser: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }
}
