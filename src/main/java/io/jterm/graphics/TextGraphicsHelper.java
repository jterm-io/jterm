package io.jterm.graphics;

import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;

/** Helper to create sub-graphics. */
public class TextGraphicsHelper {
    public static TextGraphics create(ScreenBuffer buffer) {
        return new TextGraphics(buffer);
    }

    public static TextGraphics sub(TextGraphics graphics, int x, int y, int width, int height) {
        var size = graphics.getSize();
        width = Math.min(width, size.columns() - x);
        height = Math.min(height, size.rows() - y);
        var sub = new ScreenBuffer(new TerminalSize(Math.max(0, width), Math.max(0, height)));
        return new TextGraphics(sub);
    }
}
