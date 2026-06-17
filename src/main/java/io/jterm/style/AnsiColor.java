package io.jterm.style;

/** Classic 16-color ANSI palette. Maximum terminal compatibility. */
public enum AnsiColor implements Color {
    BLACK(0), RED(1), GREEN(2), YELLOW(3),
    BLUE(4), MAGENTA(5), CYAN(6), WHITE(7),
    BRIGHT_BLACK(8), BRIGHT_RED(9), BRIGHT_GREEN(10),
    BRIGHT_YELLOW(11), BRIGHT_BLUE(12), BRIGHT_MAGENTA(13),
    BRIGHT_CYAN(14), BRIGHT_WHITE(15),
    DEFAULT(-1);

    private final int index;

    AnsiColor(int index) { this.index = index; }

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
