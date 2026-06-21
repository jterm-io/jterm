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

    /**
     * Draws a line with ASCII "anti-aliasing": instead of placing the same
     * character at every Bresenham point, picks a glyph whose visual position
     * within the cell approximates where the ideal line passes through.
     *
     * <p>Character selection is based on the entry and exit direction of the
     * line at each cell:
     * <ul>
     *   <li>Straight horizontal segment → {@code -}</li>
     *   <li>Straight vertical segment → {@code |}</li>
     *   <li>Diagonal segment → {@code \} or {@code /}</li>
     *   <li>Corner (horizontal↔vertical transition) → {@code .} (bottom) or
     *       {@code '} (top), depending on which part of the cell the line clips</li>
     *   <li>Diagonal↔straight transition → {@code .} or {@code '}</li>
     *   <li>General fallback → {@code *}</li>
     * </ul>
     *
     * @param x0   start column
     * @param y0   start row
     * @param x1   end column
     * @param y1   end row
     * @param cell template cell (character is overridden per-point; fg/bg preserved)
     */
    public void drawLineSmooth(int x0, int y0, int x1, int y1, TextCell cell) {
        // Collect Bresenham path first so we can look at neighbours
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
        // Draw each point with a context-aware character
        for (int i = 0; i < path.size(); i++) {
            int[] pt = path.get(i);
            int[] prev = i > 0 ? path.get(i - 1) : null;
            int[] next = i < path.size() - 1 ? path.get(i + 1) : null;
            char ch = smoothLineChar(pt, prev, next);
            buffer.setCell(pt[0], pt[1], cell.withCharacter(ch));
        }
    }

    /**
     * Picks a glyph for a Bresenham point based on the entry and exit
     * directions of the line through that cell.
     */
    static char smoothLineChar(int[] pt, int[] prev, int[] next) {
        // Entry / exit direction vectors
        int enDx = prev != null ? Integer.compare(pt[0], prev[0]) : 0;
        int enDy = prev != null ? Integer.compare(pt[1], prev[1]) : 0;
        int exDx = next != null ? Integer.compare(next[0], pt[0]) : 0;
        int exDy = next != null ? Integer.compare(next[1], pt[1]) : 0;

        // Endpoints: use the single direction
        if (prev == null) return dirChar(exDx, exDy, exDx, exDy);
        if (next == null) return dirChar(enDx, enDy, enDx, enDy);

        boolean enH = enDx != 0 && enDy == 0;  // entry horizontal
        boolean enV = enDx == 0 && enDy != 0;  // entry vertical
        boolean enD = enDx != 0 && enDy != 0;  // entry diagonal
        boolean exH = exDx != 0 && exDy == 0;
        boolean exV = exDx == 0 && exDy != 0;
        boolean exD = exDx != 0 && exDy != 0;

        // Straight through — same type in and out
        if (enH && exH) return '-';
        if (enV && exV) return '|';
        if (enD && exD && enDx == exDx && enDy == exDy)
            return (exDx * exDy > 0) ? '\\' : '/';

        // Corner: horizontal ↔ vertical transition
        if ((enH && exV) || (enV && exH)) {
            // Line clips a corner of the cell.
            // If it trends downward → line passes through bottom → '.'
            // If it trends upward  → line passes through top    → '''
            if (exDy > 0 || enDy > 0) return '.';
            return '\'';
        }

        // Diagonal ↔ straight transition: light character at the boundary
        if (enD || exD) {
            if (exDy > 0 || enDy > 0) return '.';
            if (exDy < 0 || enDy < 0) return '\'';
            return '*';
        }

        return '*';
    }

    /** Returns the character for a straight segment in the given direction. */
    static char dirChar(int dx, int dy, int sdx, int sdy) {
        if (dx != 0 && dy == 0) return '-';
        if (dx == 0 && dy != 0) return '|';
        if (dx != 0 && dy != 0) return (dx * dy > 0) ? '\\' : '/';
        return '*';
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
