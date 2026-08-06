package io.jterm.style;

/** 24-bit true color. Low terminal support but best fidelity. */
public record RgbColor(int r, int g, int b) implements Color {
    public RgbColor {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
            throw new IllegalArgumentException("RGB values must be 0-255");
    }

    /**
     * Return the ANSI foreground escape sequence for this color.
     *
     * @return the SGR foreground parameter bytes
     */
    @Override
    public byte[] fgSequence() { return ("38;2;" + r + ";" + g + ";" + b).getBytes(); }

    /**
     * Return the ANSI background escape sequence for this color.
     *
     * @return the SGR background parameter bytes
     */
    @Override
    public byte[] bgSequence() { return ("48;2;" + r + ";" + g + ";" + b).getBytes(); }
}
