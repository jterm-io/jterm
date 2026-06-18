package io.jterm.util;

/** Unicode box-drawing and special characters for borders and lines. */
public final class Symbols {
    private Symbols() {}

    // Single line
    public static final String H_LINE = "─";   // \u2500
    public static final String V_LINE = "│";   // \u2502
    public static final String TL_CORNER = "┌"; // \u250C
    public static final String TR_CORNER = "┐"; // \u2510
    public static final String BL_CORNER = "└"; // \u2514
    public static final String BR_CORNER = "┘"; // \u2518
    public static final String T_JUNCTION = "┬"; // \u252C
    public static final String B_JUNCTION = "┴"; // \u2534
    public static final String L_JUNCTION = "├"; // \u251C
    public static final String R_JUNCTION = "┤"; // \u2524
    public static final String CROSS = "┼";      // \u253C

    // Double line
    public static final String H_DOUBLE = "═";   // \u2550
    public static final String V_DOUBLE = "║";   // \u2551
    public static final String TL_DOUBLE = "╔";  // \u2554
    public static final String TR_DOUBLE = "╗";  // \u2557
    public static final String BL_DOUBLE = "╚";  // \u255A
    public static final String BR_DOUBLE = "╝";  // \u255D

    // Rounded
    public static final String TL_ROUNDED = "╭";  // \u256D
    public static final String TR_ROUNDED = "╮";  // \u256E
    public static final String BL_ROUNDED = "╰";  // \u2570
    public static final String BR_ROUNDED = "╯";  // \u256F

    // Blocks for chart sub-cell resolution
    public static final String BLOCK_UPPER_HALF = "▀"; // \u2580 — fills upper half of cell
    public static final String BLOCK_LOWER_HALF = "▄"; // \u2584 — fills lower half of cell

    // Misc
    public static final String BLOCK_SOLID = "█";  // \u2588
    public static final String BLOCK_LIGHT = "░";  // \u2591
    public static final String BLOCK_MEDIUM = "▒"; // \u2592
    public static final String BLOCK_DARK = "▓";   // \u2593
    public static final String ARROW_UP = "↑";     // \u2191
    public static final String ARROW_DOWN = "↓";   // \u2193
    public static final String ARROW_LEFT = "←";   // \u2190
    public static final String ARROW_RIGHT = "→";  // \u2192
    public static final String CHECKMARK = "✓";    // \u2713
    public static final String X_MARK = "✗";       // \u2717
    public static final String ELLIPSIS = "…";     // \u2026
}
