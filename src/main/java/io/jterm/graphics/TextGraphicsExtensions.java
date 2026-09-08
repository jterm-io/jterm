package io.jterm.graphics;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.style.TextCell;

/**
 * Adds sub-graphics factory method to TextGraphics.
 */
public class TextGraphicsExtensions {
    /**
     * Creates a new utility holder; not instantiable by callers.
     */
    public TextGraphicsExtensions() {
    }

    /**
     * Returns a graphics context clipped to the given region of the parent.
     *
     * @param parent the parent graphics context
     * @param offset the top-left offset of the sub-region within the parent
     * @param size   the size of the sub-region
     * @return a new TextGraphics clipped to the sub-region
     */
    public static TextGraphics subGraphics(TextGraphics parent, TerminalPosition offset, TerminalSize size) {
        return new ClippedTextGraphics(parent, offset, size);
    }
}