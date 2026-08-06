package io.jterm.graphics;

import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;

/** Helper to create sub-graphics. */
public class TextGraphicsHelper {
    /**
     * Create a TextGraphics backed by the given screen buffer.
     *
     * @param buffer the screen buffer to draw into
     *
     * @return a new TextGraphics instance
     */
    public static TextGraphics create(ScreenBuffer buffer) {
        return new TextGraphics(buffer);
    }

    /**
     * Return a sub-graphics for the given region of the parent graphics.
     *
     * @param graphics the parent graphics context
     * @param x         the x offset within the parent
     * @param y         the y offset within the parent
     * @param width     the width of the sub-region
     * @param height    the height of the sub-region
     *
     * @return a new TextGraphics clipped to the sub-region
     */
    public static TextGraphics sub(TextGraphics graphics, int x, int y, int width, int height) {
        var size = graphics.getSize();
        width = Math.min(width, size.columns() - x);
        height = Math.min(height, size.rows() - y);
        var sub = new ScreenBuffer(new TerminalSize(Math.max(0, width), Math.max(0, height)));
        return new TextGraphics(sub);
    }
}
