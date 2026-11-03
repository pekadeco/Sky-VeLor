package com.arena.pekaclient.chat;

public final class RomanNumeralsTests {
    private static int passed;
    private static void check(String name, boolean ok) { if (!ok) throw new AssertionError(name); passed++; }
    public static void main(String[] args) {
        check("I", RomanNumerals.parse("I") == 1); check("IV", RomanNumerals.parse("IV") == 4);
        check("IX", RomanNumerals.parse("IX") == 9); check("XL", RomanNumerals.parse("XL") == 40);
        check("XC", RomanNumerals.parse("XC") == 90); check("CD", RomanNumerals.parse("CD") == 400);
        check("CM", RomanNumerals.parse("CM") == 900); check("MCMXCIX", RomanNumerals.parse("MCMXCIX") == 1999);
        check("3999", RomanNumerals.parse("MMMCMXCIX") == 3999); check("bad order", RomanNumerals.parse("IC") == -1);
        check("empty", RomanNumerals.parse("") == -1); check("null", RomanNumerals.parse(null) == -1);
        check("convert phrase", "Sharpness 6 and Power 4".equals(RomanNumerals.convertInText("Sharpness VI and Power IV")));
        check("preserve words", "CIVILIZATION".equals(RomanNumerals.convertInText("CIVILIZATION")));
        check("punctuation", "(9), 14!".equals(RomanNumerals.convertInText("(IX), XIV!")));
        check("null text", "".equals(RomanNumerals.convertInText(null)));
        System.out.println("RomanNumeralsTests: " + passed + "/" + passed + " passed");
    }
}
