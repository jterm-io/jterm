package io.jterm.graphics;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.TextCell;

/** Graphics view that writes through to a parent buffer at an offset. */
public class ClippedTextGraphics extends TextGraphics {
    private final TextGraphics parent;
    private final TerminalPosition offset;

    public ClippedTextGraphics(TextGraphics parent, TerminalPosition offset, TerminalSize size) {
        super(new ScreenBuffer(size));
        this.parent = parent;
        this.offset = offset;
    }

    @Override
    public void setCell(int x, int y, TextCell cell) {
        if (x < 0 || y < 0 || x >= getSize().columns() || y >= getSize().rows()) return;
        super.setCell(x, y, cell);
        parent.setCell(offset.column() + x, offset.row() + y, cell);
    }

    @Override
    public void fillRectangle(int x, int y, int width, int height, TextCell cell) {
        for (int r = Math.max(0, y); r < y + height && r < getSize().rows(); r++) {
            for (int c = Math.max(0, x); c < x + width && c < getSize().columns(); c++) {
                setCell(c, r, cell);
            }
        }
    }

    @Override
    public void drawString(int x, int y, String text, TextCell template) {
        int col = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (col >= 0 && col < getSize().columns() && y >= 0 && y < getSize().rows()) {
                setCell(col, y, template.withCharacter(c));
            }
            col += isCharDoubleWidth(c) ? 2 : 1;
        }
    }

    private static boolean isCharDoubleWidth(char c) {
        var block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }
}
