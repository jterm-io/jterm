package io.jterm.style;

/** Select Graphic Rendition modifiers. Applied per-cell alongside colors. */
public enum SGR {
    /** Bold or increased intensity. */
    BOLD(1),

    /** Dim/decreased intensity. */
    DIM(2),

    /** Italic. */
    ITALIC(3),

    /** Underlined text. */
    UNDERLINE(4),

    /** Slow blink. */
    BLINK(5),

    /** Reverse video: swap foreground and background. */
    REVERSE(7),

    /** Concealed text (not printed). */
    HIDDEN(8),

    /** Strikethrough. */
    STRIKETHROUGH(9);

    /** The numeric SGR parameter code. */
    public final int code;

    SGR(int code) { this.code = code; }
}