package net.minecraft.util;

public enum EnumChatFormatting {
    BLACK('0'), DARK_BLUE('1'), DARK_GREEN('2'), DARK_AQUA('3'), DARK_RED('4'), DARK_PURPLE('5'),
    GOLD('6'), GRAY('7'), DARK_GRAY('8'), BLUE('9'), GREEN('a'), AQUA('b'), RED('c'), LIGHT_PURPLE('d'),
    YELLOW('e'), WHITE('f'), OBFUSCATED('k'), BOLD('l'), STRIKETHROUGH('m'), UNDERLINE('n'), ITALIC('o'),
    RESET('r');

    private final char code;

    EnumChatFormatting(char code) { this.code = code; }

    public static String getTextWithoutFormattingCodes(String text) {
        if (text == null) { return null; }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == 0x00a7 && i + 1 < text.length()) { i++; continue; }
            sb.append(c);
        }
        return sb.toString();
    }
}
