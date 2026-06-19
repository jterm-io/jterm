package io.jterm.style;

import java.nio.charset.StandardCharsets;

/** Low-level ANSI escape sequence utilities. */
public final class AnsiCodes {
    public static final String CSI = "\033[";   // Control Sequence Introducer
    public static final String OSC = "\033]";     // Operating System Command
    public static final String RESET = CSI + "0m";
    public static final String CLEAR_SCREEN = CSI + "2J";
    public static final String CLEAR_LINE = CSI + "2K";
    public static final String HIDE_CURSOR = CSI + "?25l";
    public static final String SHOW_CURSOR = CSI + "?25h";
    public static final String ENTER_ALT_SCREEN = CSI + "?1049h";
    public static final String EXIT_ALT_SCREEN = CSI + "?1049l";
    public static final String DISABLE_AUTOWRAP = CSI + "?7l";
    public static final String ENABLE_AUTOWRAP = CSI + "?7h";

    private AnsiCodes() {}

    public static byte[] cursorTo(int row, int col) {
        return (CSI + (row + 1) + ";" + (col + 1) + "H").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] enable(SGR sgr) {
        return (CSI + sgr.code + "m").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] disable(SGR sgr) {
        // SGR disable codes: bold/dim/faint=22, italic=23, underline=24, blink=25, reverse=27, hidden=28, strikethrough=29
        int disableCode = switch (sgr) {
            case BOLD, DIM -> 22;
            case ITALIC -> 23;
            case UNDERLINE -> 24;
            case BLINK -> 25;
            case REVERSE -> 27;
            case HIDDEN -> 28;
            case STRIKETHROUGH -> 29;
        };
        return (CSI + disableCode + "m").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] setForeground(Color color) {
        return (CSI + new String(color.fgSequence(), StandardCharsets.UTF_8) + "m").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] setBackground(Color color) {
        return (CSI + new String(color.bgSequence(), StandardCharsets.UTF_8) + "m").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] reset() {
        return RESET.getBytes(StandardCharsets.UTF_8);
    }
}
