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
    public void drawLine(int x0, int y0, int x1, int y1, TextCell cell) {
        // TextGraphics.drawLine writes to this.buffer only — we must delegate
        // to setCell so each point also reaches the parent buffer.
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0, y = y0;
        while (true) {
            setCell(x, y, cell);
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx) { err += dx; y += sy; }
        }
    }

    @Override
    public void drawLineSmooth(int x0, int y0, int x1, int y1, TextCell cell) {
        // Collect Bresenham path, then write each point via setCell so both
        // the local and parent buffers receive the smoothed character.
        java.util.List<int[]> path = new java.util.ArrayList<>();
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int cx = x0, cy = y0;
        while (true) {
            path.add(new int[]{cx, cy});
            if (cx == x1 && cy == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; cx += sx; }
            if (e2 < dx) { err += dx; cy += sy; }
        }
        var table = ShapeVectorTable.instance();
        for (int i = 0; i < path.size(); i++) {
            int[] pt = path.get(i);
            int[] prev = i > 0 ? path.get(i - 1) : null;
            int[] next = i < path.size() - 1 ? path.get(i + 1) : null;
            double[] samplingVec = computeLineSamplingVector(pt, prev, next);
            char ch = table.findBestChar(samplingVec, 0.01);
            setCell(pt[0], pt[1], cell.withCharacter(ch));
        }
    }

    @Override
    public void drawRectangle(int x, int y, int width, int height, TextCell borderCell) {
        int maxCol = x + width - 1;
        int maxRow = y + height - 1;
        for (int c = x; c <= maxCol; c++) {
            setCell(c, y, borderCell);
            setCell(c, maxRow, borderCell);
        }
        for (int r = y + 1; r < maxRow; r++) {
            setCell(x, r, borderCell);
            setCell(maxCol, r, borderCell);
        }
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
