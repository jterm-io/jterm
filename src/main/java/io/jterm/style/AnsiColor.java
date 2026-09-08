package io.jterm.style;

/**
 * Classic 16-color ANSI palette. Maximum terminal compatibility.
 *
 * <p>Constants: BLACK(0), RED(1), GREEN(2), YELLOW(3), BLUE(4), MAGENTA(5),
 * CYAN(6), WHITE(7), BRIGHT_BLACK(8), BRIGHT_RED(9), BRIGHT_GREEN(10),
 * BRIGHT_YELLOW(11), BRIGHT_BLUE(12), BRIGHT_MAGENTA(13), BRIGHT_CYAN(14),
 * BRIGHT_WHITE(15), DEFAULT(-1, terminal default colors).</p>
 */
public enum AnsiColor implements Color {
    /** Black — palette index 0. */
    BLACK(0, 0, 0, 0),
    /** Red — palette index 1. */
    RED(1, 170, 0, 0),
    /** Green — palette index 2. */
    GREEN(2, 0, 170, 0),
    /** Yellow — palette index 3. */
    YELLOW(3, 170, 85, 0),
    /** Blue — palette index 4. */
    BLUE(4, 0, 0, 170),
    /** Magenta — palette index 5. */
    MAGENTA(5, 170, 0, 170),
    /** Cyan — palette index 6. */
    CYAN(6, 0, 170, 170),
    /** White — palette index 7. */
    WHITE(7, 170, 170, 170),
    /** Bright black (dark gray) — palette index 8. */
    BRIGHT_BLACK(8, 85, 85, 85),
    /** Bright red — palette index 9. */
    BRIGHT_RED(9, 255, 85, 85),
    /** Bright green — palette index 10. */
    BRIGHT_GREEN(10, 85, 255, 85),
    /** Bright yellow — palette index 11. */
    BRIGHT_YELLOW(11, 255, 255, 85),
    /** Bright blue — palette index 12. */
    BRIGHT_BLUE(12, 85, 85, 255),
    /** Bright magenta — palette index 13. */
    BRIGHT_MAGENTA(13, 255, 85, 255),
    /** Bright cyan — palette index 14. */
    BRIGHT_CYAN(14, 85, 255, 255),
    /** Bright white — palette index 15. */
    BRIGHT_WHITE(15, 255, 255, 255),
    /** Terminal default colors (no explicit SGR color), index -1. Not valid for backgrounds. */
    DEFAULT(-1, 0, 0, 0);

    private final int index;
    private final int r, g, b;

    AnsiColor(int index, int r, int g, int b) {
        this.index = index;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
     * Returns the RGB representation of this ANSI color.
     *
     * @return the equivalent 24-bit {@link RgbColor}
     */
    public RgbColor toRgb() {
        return new RgbColor(r, g, b);
    }

    /**
     * Returns the ANSI color closest to the given RGB values.
     *
     * @param r red component, 0-255
     * @param g green component, 0-255
     * @param b blue component, 0-255
     * @return the nearest palette color (never {@link #DEFAULT})
     */
    public static AnsiColor nearest(int r, int g, int b) {
        AnsiColor best = BLACK;
        double bestDist = Double.MAX_VALUE;
        for (AnsiColor c : values()) {
            if (c == DEFAULT) continue;
            double dist = (c.r - r) * (c.r - r) + (c.g - g) * (c.g - g) + (c.b - b) * (c.b - b);
            if (dist < bestDist) {
                bestDist = dist;
                best = c;
            }
        }
        return best;
    }

    /**
     * Blends two ANSI colors and returns the nearest ANSI color in the palette.
     *
     * @param a       first color
     * @param b       second color
     * @param aWeight blend weight of {@code a}, 0.0 (all {@code b}) to 1.0 (all {@code a})
     * @return the blended color, snapped to the nearest palette entry
     */
    public static AnsiColor blendAnsi(AnsiColor a, AnsiColor b, double aWeight) {
        if (aWeight <= 0.0) return b;
        if (aWeight >= 1.0) return a;
        int r = (int) (a.r + (b.r - a.r) * aWeight);
        int g = (int) (a.g + (b.g - a.g) * aWeight);
        int bl = (int) (a.b + (b.b - a.b) * aWeight);
        return nearest(r, g, bl);
    }

    /**
     * Return the ANSI foreground escape sequence for this color.
     *
     * @return the SGR foreground parameter bytes
     */
    @Override
    public byte[] fgSequence() {
        return switch (index) {
            case 0 -> "30".getBytes();
            case 1 -> "31".getBytes();
            case 2 -> "32".getBytes();
            case 3 -> "33".getBytes();
            case 4 -> "34".getBytes();
            case 5 -> "35".getBytes();
            case 6 -> "36".getBytes();
            case 7 -> "37".getBytes();
            case 8 -> "90".getBytes();
            case 9 -> "91".getBytes();
            case 10 -> "92".getBytes();
            case 11 -> "93".getBytes();
            case 12 -> "94".getBytes();
            case 13 -> "95".getBytes();
            case 14 -> "96".getBytes();
            case 15 -> "97".getBytes();
            default -> "39".getBytes(); // DEFAULT
        };
    }

    /**
     * Return the ANSI background escape sequence for this color.
     *
     * @return the SGR background parameter bytes
     */
    @Override
    public byte[] bgSequence() {
        return switch (index) {
            case 0 -> "40".getBytes();
            case 1 -> "41".getBytes();
            case 2 -> "42".getBytes();
            case 3 -> "43".getBytes();
            case 4 -> "44".getBytes();
            case 5 -> "45".getBytes();
            case 6 -> "46".getBytes();
            case 7 -> "47".getBytes();
            case 8 -> "100".getBytes();
            case 9 -> "101".getBytes();
            case 10 -> "102".getBytes();
            case 11 -> "103".getBytes();
            case 12 -> "104".getBytes();
            case 13 -> "105".getBytes();
            case 14 -> "106".getBytes();
            case 15 -> "107".getBytes();
            default -> "49".getBytes(); // DEFAULT
        };
    }
}
