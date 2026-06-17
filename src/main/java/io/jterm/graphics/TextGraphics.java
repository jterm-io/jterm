package io.jterm.graphics;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

/** Drawing primitives targeting a backing ScreenBuffer. */
public class TextGraphics {
    private final ScreenBuffer buffer;

    public TextGraphics(ScreenBuffer buffer) {
        this.buffer = buffer;
    }

    public TerminalSize getSize() {
        return buffer.size();
    }

    public void setCell(int x, int y, TextCell cell) {
        buffer.setCell(x, y, cell);
    }

    public TextCell getCell(int x, int y) {
        return buffer.getCell(x, y);
    }

    public void fillRectangle(int x, int y, int width, int height, TextCell cell) {
        for (int r = y; r < y + height && r < buffer.size().rows(); r++) {
            for (int c = x; c < x + width && c < buffer.size().columns(); c++) {
                buffer.setCell(c, r, cell);
            }
        }
    }

    public void drawRectangle(int x, int y, int width, int height, TextCell borderCell) {
        int maxRow = Math.min(y + height - 1, buffer.size().rows() - 1);
        int maxCol = Math.min(x + width - 1, buffer.size().columns() - 1);
        for (int c = x; c <= maxCol; c++) {
            buffer.setCell(c, y, borderCell);
            buffer.setCell(c, maxRow, borderCell);
        }
        for (int r = y; r <= maxRow; r++) {
            buffer.setCell(x, r, borderCell);
            buffer.setCell(maxCol, r, borderCell);
        }
    }

    public void drawLine(int x0, int y0, int x1, int y1, TextCell cell) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            buffer.setCell(x0, y0, cell);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    public void drawString(int x, int y, String text, TextCell template) {
        int col = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            buffer.setCell(col, y, template.withCharacter(c));
            col += isCharDoubleWidth(c) ? 2 : 1;
        }
    }

    public void drawString(int x, int y, String text, Color fg, Color bg, SGR... mods) {
        var modsSet = mods.length == 0 ? java.util.EnumSet.noneOf(SGR.class) : java.util.EnumSet.of(mods[0], mods);
        drawString(x, y, text, new TextCell(' ', fg, bg, modsSet.toArray(new SGR[0])));
    }

    public void putCell(int x, int y, TextCell cell) {
        buffer.setCell(x, y, cell);
    }

    public TextGraphics getGraphics(int x, int y, int width, int height) {
        return new SubTextGraphics(this, x, y, width, height);
    }

    private static boolean isCharDoubleWidth(char c) {
        var block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }
}
