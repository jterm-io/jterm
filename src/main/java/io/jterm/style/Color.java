package io.jterm.style;

/**
 * Terminal color abstraction. Implementations: AnsiColor (16), IndexedColor (256), RgbColor (24-bit).
 */
public sealed interface Color permits AnsiColor, IndexedColor, RgbColor {
    /**
     * SGR byte sequence for foreground (e.g. "38;2;255;128;0" for RGB).
     *
     * @return the SGR foreground parameter bytes
     */
    byte[] fgSequence();

    /**
     * SGR byte sequence for background (e.g. "48;2;255;128;0" for RGB).
     *
     * @return the SGR background parameter bytes
     */
    byte[] bgSequence();
}
