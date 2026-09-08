package io.jterm.util;

/** Unicode box-drawing and special characters for borders and lines. */
public final class Symbols {
    private Symbols() {}

    // Single line

    /** Horizontal single line — U+2500. */
    public static final String H_LINE = "─";   // \u2500

    /** Vertical single line — U+2502. */
    public static final String V_LINE = "│";   // \u2502

    /** Top-left single-line corner — U+250C. */
    public static final String TL_CORNER = "┌"; // \u250C

    /** Top-right single-line corner — U+2510. */
    public static final String TR_CORNER = "┐"; // \u2510

    /** Bottom-left single-line corner — U+2514. */
    public static final String BL_CORNER = "└"; // \u2514

    /** Bottom-right single-line corner — U+2518. */
    public static final String BR_CORNER = "┘"; // \u2518

    /** Single-line T-junction pointing down (top edge) — U+252C. */
    public static final String T_JUNCTION = "┬"; // \u252C

    /** Single-line T-junction pointing up (bottom edge) — U+2534. */
    public static final String B_JUNCTION = "┴"; // \u2534

    /** Single-line T-junction pointing right (left edge) — U+251C. */
    public static final String L_JUNCTION = "├"; // \u251C

    /** Single-line T-junction pointing left (right edge) — U+2524. */
    public static final String R_JUNCTION = "┤"; // \u2524

    /** Single-line four-way junction (cross) — U+253C. */
    public static final String CROSS = "┼";      // \u253C

    // Double line

    /** Horizontal double line — U+2550. */
    public static final String H_DOUBLE = "═";   // \u2550

    /** Vertical double line — U+2551. */
    public static final String V_DOUBLE = "║";   // \u2551

    /** Top-left double-line corner — U+2554. */
    public static final String TL_DOUBLE = "╔";  // \u2554

    /** Top-right double-line corner — U+2557. */
    public static final String TR_DOUBLE = "╗";  // \u2557

    /** Bottom-left double-line corner — U+255A. */
    public static final String BL_DOUBLE = "╚";  // \u255A

    /** Bottom-right double-line corner — U+255D. */
    public static final String BR_DOUBLE = "╝";  // \u255D

    // Rounded

    /** Top-left rounded corner — U+256D. */
    public static final String TL_ROUNDED = "╭";  // \u256D

    /** Top-right rounded corner — U+256E. */
    public static final String TR_ROUNDED = "╮";  // \u256E

    /** Bottom-left rounded corner — U+2570. */
    public static final String BL_ROUNDED = "╰";  // \u2570

    /** Bottom-right rounded corner — U+256F. */
    public static final String BR_ROUNDED = "╯";  // \u256F

    // Blocks for chart sub-cell resolution

    /** Upper-half block: fills the upper half of the cell — U+2580. */
    public static final String BLOCK_UPPER_HALF = "▀"; // \u2580 — fills upper half of cell

    /** Lower-half block: fills the lower half of the cell — U+2584. */
    public static final String BLOCK_LOWER_HALF = "▄"; // \u2584 — fills lower half of cell

    // Misc

    /** Solid full-cell block — U+2588. */
    public static final String BLOCK_SOLID = "█";  // \u2588

    /** Light shade block — U+2591. */
    public static final String BLOCK_LIGHT = "░";  // \u2591

    /** Medium shade block — U+2592. */
    public static final String BLOCK_MEDIUM = "▒"; // \u2592

    /** Dark shade block — U+2593. */
    public static final String BLOCK_DARK = "▓";   // \u2593

    /** Arrow pointing up — U+2191. */
    public static final String ARROW_UP = "↑";     // \u2191

    /** Arrow pointing down — U+2193. */
    public static final String ARROW_DOWN = "↓";   // \u2193

    /** Arrow pointing left — U+2190. */
    public static final String ARROW_LEFT = "←";   // \u2190

    /** Arrow pointing right — U+2192. */
    public static final String ARROW_RIGHT = "→";  // \u2192

    /** Check mark — U+2713. */
    public static final String CHECKMARK = "✓";    // \u2713

    /** Ballot X (cross mark) — U+2717. */
    public static final String X_MARK = "✗";       // \u2717

    /** Horizontal ellipsis — U+2026. */
    public static final String ELLIPSIS = "…";     // \u2026
}