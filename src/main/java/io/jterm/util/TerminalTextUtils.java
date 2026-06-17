package io.jterm.util;

/** Utilities for measuring and manipulating terminal text. */
public final class TerminalTextUtils {
    private TerminalTextUtils() {}

    public static boolean isControlChar(char c) {
        return c < 32 || c == 127;
    }

    public static boolean isPrintable(char c) {
        return !isControlChar(c) && c != 0x7f;
    }

    public static boolean isCharDoubleWidth(char c) {
        var block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.HANGUL_JAMO
            || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.KATAKANA
            || block == Character.UnicodeBlock.HIRAGANA;
    }

    /** Returns the display width of a string (accounts for double-width chars). */
    public static int getTrueWidth(String s) {
        int width = 0;
        for (char c : s.toCharArray()) {
            width += isCharDoubleWidth(c) ? 2 : 1;
        }
        return width;
    }

    /** Truncates a string to fit within maxColumns display columns. */
    public static String truncate(String s, int maxColumns) {
        if (getTrueWidth(s) <= maxColumns) return s;
        var sb = new StringBuilder();
        int width = 0;
        for (char c : s.toCharArray()) {
            int cw = isCharDoubleWidth(c) ? 2 : 1;
            if (width + cw > maxColumns) break;
            sb.append(c);
            width += cw;
        }
        return sb.toString();
    }
}
