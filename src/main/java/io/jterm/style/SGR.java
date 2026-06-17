package io.jterm.style;

/** Select Graphic Rendition modifiers. Applied per-cell alongside colors. */
public enum SGR {
    BOLD(1),
    DIM(2),
    ITALIC(3),
    UNDERLINE(4),
    BLINK(5),
    REVERSE(7),
    HIDDEN(8),
    STRIKETHROUGH(9);

    public final int code;

    SGR(int code) { this.code = code; }
}
