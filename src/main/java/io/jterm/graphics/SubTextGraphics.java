package io.jterm.graphics;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

/** A TextGraphics that translates coordinates to a sub-region of a parent graphics. */
public class SubTextGraphics extends TextGraphics {
    private final TextGraphics parent;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public SubTextGraphics(TextGraphics parent, int x, int y, int width, int height) {
        super(nullScreenBuffer(x, y, width, height));
        if (parent == null) throw new IllegalArgumentException("parent graphics required");
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public TerminalSize getSize() {
        return new TerminalSize(width, height);
    }

    @Override
    public void setCell(int x, int y, TextCell cell) {
        if (x < 0 || y < 0 || x >= width || y >= height) return;
        parent.setCell(this.x + x, this.y + y, cell);
    }

    @Override
    public TextCell getCell(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return TextCell.EMPTY;
        return parent.getCell(this.x + x, this.y + y);
    }

    @Override
    public void fillRectangle(int x, int y, int width, int height, TextCell cell) {
        int x0 = Math.max(this.x + x, this.x);
        int y0 = Math.max(this.y + y, this.y);
        int x1 = Math.min(this.x + x + width, this.x + this.width);
        int y1 = Math.min(this.y + y + height, this.y + this.height);
        parent.fillRectangle(x0, y0, x1 - x0, y1 - y0, cell);
    }

    @Override
    public void drawRectangle(int x, int y, int width, int height, TextCell borderCell) {
        int x0 = Math.max(this.x + x, this.x);
        int y0 = Math.max(this.y + y, this.y);
        int x1 = Math.min(this.x + x + width - 1, this.x + this.width - 1);
        int y1 = Math.min(this.y + y + height - 1, this.y + this.height - 1);
        parent.drawRectangle(x0, y0, x1 - x0 + 1, y1 - y0 + 1, borderCell);
    }

    @Override
    public void drawLine(int x0, int y0, int x1, int y1, TextCell cell) {
        parent.drawLine(this.x + x0, this.y + y0, this.x + x1, this.y + y1, cell);
    }

    @Override
    public void drawString(int x, int y, String text, TextCell template) {
        parent.drawString(this.x + x, this.y + y, text, template);
    }

    @Override
    public void drawString(int x, int y, String text, Color fg, Color bg, SGR... mods) {
        parent.drawString(this.x + x, this.y + y, text, fg, bg, mods);
    }

    @Override
    public void putCell(int x, int y, TextCell cell) {
        setCell(x, y, cell);
    }

    @Override
    public TextGraphics getGraphics(int x, int y, int width, int height) {
        return new SubTextGraphics(parent, this.x + x, this.y + y, width, height);
    }

    private static io.jterm.screen.ScreenBuffer nullScreenBuffer(int x, int y, int width, int height) {
        return new io.jterm.screen.ScreenBuffer(new TerminalSize(Math.max(1, width), Math.max(1, height)));
    }
}
