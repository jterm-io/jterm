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

    /** Sub-cell resolution for shape-vector line drawing. */
    private static final int SS_W = 20;  // sub-pixel columns per cell
    private static final int SS_H = 30;  // sub-pixel rows per cell (taller, like monospace)

    /**
     * Draws a line using shape-vector-based character selection. Instead of
     * placing the same character at every Bresenham point, this method computes
     * a 6D sampling vector for each cell the line passes through (representing
     * how the line occupies 6 sub-regions of the cell), then finds the ASCII
     * character whose shape best matches.
     *
     * <p>This produces smoother diagonal lines that follow the contour of the
     * line, similar to the technique described in
     * <a href="https://alexharri.com/blog/ascii-rendering">Alex Harri's ASCII rendering blog post</a>.
     *
     * @param x0   start column
     * @param y0   start row
     * @param x1   end column
     * @param y1   end row
     * @param cell template cell (character is overridden per-point; fg/bg preserved)
     */
    public void drawLineSmooth(int x0, int y0, int x1, int y1, TextCell cell) {
        // Collect Bresenham path
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
            buffer.setCell(pt[0], pt[1], cell.withCharacter(ch));
        }
    }

    /**
     * Computes a 6D sampling vector for a Bresenham point by supersampling
     * the line segment (from prev to next, passing through pt) at sub-cell
     * resolution and counting coverage per sub-region.
     *
     * The 6 sub-regions are arranged as a 2×3 grid:
     * <pre>
     *  ┌─────┬─────┐
     *  │  0  │  1  │   top
     *  ├─────┼─────┤
     *  │  2  │  3  │   middle
     *  ├─────┼─────┤
     *  │  4  │  5  │   bottom
     *  └─────┴─────┘
     * </pre>
     */
    static double[] computeLineSamplingVector(int[] pt, int[] prev, int[] next) {
        // Determine the line segment that passes through this cell.
        // We use prev→next (or pt→next for start, prev→pt for end) to get
        // the direction of the line through the cell.
        double segX0, segY0, segX1, segY1;
        if (prev != null && next != null) {
            segX0 = prev[0]; segY0 = prev[1];
            segX1 = next[0]; segY1 = next[1];
        } else if (prev != null) {
            segX0 = prev[0]; segY0 = prev[1];
            segX1 = pt[0];   segY1 = pt[1];
        } else if (next != null) {
            segX0 = pt[0];   segY0 = pt[1];
            segX1 = next[0]; segY1 = next[1];
        } else {
            // Single point — fill center
            double[] vec = new double[6];
            vec[2] = vec[3] = 0.5;
            return vec;
        }

        // Convert to sub-cell coordinates relative to pt (the cell origin).
        // Bresenham cell (cx, cy) corresponds to the cell area [cx, cx+1) × [cy, cy+1)
        // in continuous space. The line passes through the center of each cell,
        // so we offset by +0.5 cell units to center the line in the sub-cell grid.
        double lx0 = (segX0 - pt[0] + 0.5) * SS_W;
        double ly0 = (segY0 - pt[1] + 0.5) * SS_H;
        double lx1 = (segX1 - pt[0] + 0.5) * SS_W;
        double ly1 = (segY1 - pt[1] + 0.5) * SS_H;

        // Rasterize the line segment into the sub-cell grid and count hits per region
        // We use a supersampling approach: walk the line at fine granularity
        double[] vec = new double[6];
        int[] counts = new int[6];
        int totalSamples = 0;

        // Number of samples along the segment — enough for good coverage
        double segLen = Math.sqrt((lx1 - lx0) * (lx1 - lx0) + (ly1 - ly0) * (ly1 - ly0));
        int numSamples = Math.max(SS_W + SS_H, (int) Math.ceil(segLen * 3));
        // Also sample a thick line for better coverage — but keep it thin
        // to match the visual weight of ASCII characters like '-', '/', etc.
        int lineRadius = 1;

        // Sample a 1-pixel-wide line (no radius) to match the thinness of
        // characters like '-', '/', '|'.
        for (int s = 0; s <= numSamples; s++) {
            double t = (double) s / numSamples;
            double px = lx0 + (lx1 - lx0) * t;
            double py = ly0 + (ly1 - ly0) * t;

            int ix = (int) Math.round(px);
            int iy = (int) Math.round(py);
            if (ix < 0 || ix >= SS_W || iy < 0 || iy >= SS_H) continue;

            int regionIdx = subRegionIndex(ix, iy);
            counts[regionIdx]++;
            totalSamples++;
        }

        // Normalize: coverage of each region = fraction of sub-pixels in that
        // region that the line touches. Each region has (SS_W/2)*(SS_H/3) pixels.
        int regionPixels = (SS_W / 2) * (SS_H / 3);
        if (regionPixels > 0) {
            for (int d = 0; d < 6; d++) {
                vec[d] = Math.min(1.0, (double) counts[d] / regionPixels);
            }
        }

        return vec;
    }

    /**
     * Maps a sub-pixel coordinate to one of the 6 sub-regions.
     * Grid: 2 columns × 3 rows
     */
    private static int subRegionIndex(int sx, int sy) {
        int col = (sx * 2) / SS_W;      // 0 or 1
        int row = (sy * 3) / SS_H;      // 0, 1, or 2
        col = Math.min(1, Math.max(0, col));
        row = Math.min(2, Math.max(0, row));
        return row * 2 + col;
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
