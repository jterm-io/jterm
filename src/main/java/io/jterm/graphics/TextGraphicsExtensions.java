package io.jterm.graphics;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.style.TextCell;

/** Adds sub-graphics factory method to TextGraphics. */
public class TextGraphicsExtensions {
    public static TextGraphics subGraphics(TextGraphics parent, TerminalPosition offset, TerminalSize size) {
        return new ClippedTextGraphics(parent, offset, size);
    }
}
