package io.jterm.style;

/** Classic 16-color ANSI palette. Maximum terminal compatibility. */
public enum AnsiColor implements Color {
    BLACK(0, 0, 0, 0), RED(1, 170, 0, 0), GREEN(2, 0, 170, 0), YELLOW(3, 170, 85, 0),
    BLUE(4, 0, 0, 170), MAGENTA(5, 170, 0, 170), CYAN(6, 0, 170, 170), WHITE(7, 170, 170, 170),
    BRIGHT_BLACK(8, 85, 85, 85), BRIGHT_RED(9, 255, 85, 85), BRIGHT_GREEN(10, 85, 255, 85),
    BRIGHT_YELLOW(11, 255, 255, 85), BRIGHT_BLUE(12, 85, 85, 255), BRIGHT_MAGENTA(13, 255, 85, 255),
    BRIGHT_CYAN(14, 85, 255, 255), BRIGHT_WHITE(15, 255, 255, 255),
    DEFAULT(-1, 0, 0, 0);

    private final int index;
    private final int r, g, b;

    AnsiColor(int index, int r, int g, int b) {
        this.index = index;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /** Returns the RGB representation of this ANSI color. */
    public RgbColor toRgb() {
        return new RgbColor(r, g, b);
    }

    /** Returns the ANSI color closest to the given RGB values. */
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

    /** Blends two ANSI colors and returns the nearest ANSI color in the palette. */
    public static AnsiColor blendAnsi(AnsiColor a, AnsiColor b, double aWeight) {
        if (aWeight <= 0.0) return b;
        if (aWeight >= 1.0) return a;
        int r = (int) (a.r + (b.r - a.r) * aWeight);
        int g = (int) (a.g + (b.g - a.g) * aWeight);
        int bl = (int) (a.b + (b.b - a.b) * aWeight);
        return nearest(r, g, bl);
    }

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
