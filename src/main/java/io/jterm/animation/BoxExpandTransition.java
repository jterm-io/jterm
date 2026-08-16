package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.EnumSet;

/**
 * Box-expand transition: a rectangle grows from the center outward.
 * <p>
 * At progress 0.0, a single cell appears at the center of the screen showing
 * the new content. As progress increases, the rectangle expands symmetrically
 * in all directions — up, down, left, and right — revealing more of the new
 * screen content inside the box. Outside the box, the old screen content
 * remains visible. The leading edge of the expansion uses a bright border
 * (block characters in a highlight color) for a crisp visual edge.
 * <p>
 * At progress 1.0, the box fills the entire screen and the new content is
 * fully visible.
 */
public class BoxExpandTransition implements TransitionEffect {
    private final long durationMs;
    private final ScreenBuffer oldScreen;
    private final AnsiColor borderColor;

    /**
     * Create a box-expand transition with a default border color (bright cyan).
     *
     * @param durationMs total duration in milliseconds
     * @param oldScreen  buffer containing the old screen content
     */
    public BoxExpandTransition(long durationMs, ScreenBuffer oldScreen) {
        this(durationMs, oldScreen, AnsiColor.BRIGHT_CYAN);
    }

    /**
     * Create a box-expand transition with a custom border color.
     *
     * @param durationMs  total duration in milliseconds
     * @param oldScreen   buffer containing the old screen content
     * @param borderColor color of the expanding box border
     */
    public BoxExpandTransition(long durationMs, ScreenBuffer oldScreen, AnsiColor borderColor) {
        this.durationMs = durationMs;
        this.oldScreen = oldScreen;
        this.borderColor = borderColor;
    }

    @Override
    public long durationMs() {
        return durationMs;
    }

    @Override
    public int targetFps() {
        return 30;
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size, double progress) {
        if (size.rows() <= 0 || size.columns() <= 0) return;
        double p = Math.max(0.0, Math.min(1.0, progress));

        // At progress 0: show old screen entirely
        if (p <= 0.0) {
            for (int r = 0; r < size.rows(); r++)
                for (int c = 0; c < size.columns(); c++)
                    graphics.setCell(c, r, oldScreen.getCell(c, r));
            return;
        }
        // At progress 1: new content is already in the buffer — nothing to do
        if (p >= 1.0) return;

        // Capture the new content before we overwrite
        var newBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                newBuffer.setCell(c, r, graphics.getCell(c, r));

        // Center of the screen
        int centerCol = size.columns() / 2;
        int centerRow = size.rows() / 2;

        // The box expands from 1x1 at p=0 to full screen at p=1,
        // maintaining the screen's aspect ratio throughout.
        int maxHalfWidth = Math.max(centerCol, size.columns() - 1 - centerCol);
        int maxHalfHeight = Math.max(centerRow, size.rows() - 1 - centerRow);

        // Scale both dimensions by the same progress factor so the box
        // keeps the same width:height ratio as the terminal.
        int halfWidth = (int) Math.ceil(p * maxHalfWidth);
        int halfHeight = (int) Math.ceil(p * maxHalfHeight);
        if (halfWidth < 1) halfWidth = 1;
        if (halfHeight < 1) halfHeight = 1;

        int boxLeft = centerCol - halfWidth;
        int boxRight = centerCol + halfWidth;
        int boxTop = centerRow - halfHeight;
        int boxBottom = centerRow + halfHeight;

        // Border cell: full block in border color
        var borderCell = new TextCell("\u2588", borderColor, AnsiColor.BLACK,
                EnumSet.noneOf(SGR.class));

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                boolean insideBox = c >= boxLeft && c <= boxRight && r >= boxTop && r <= boxBottom;
                if (insideBox) {
                    // Check if this cell is on the border edge of the box
                    boolean onBorder = c == boxLeft || c == boxRight || r == boxTop || r == boxBottom;
                    if (onBorder) {
                        // Only draw the border for cells that are within the screen bounds
                        // and where the box edge hasn't reached the screen edge yet
                        boolean leftReached = boxLeft <= 0;
                        boolean rightReached = boxRight >= size.columns() - 1;
                        boolean topReached = boxTop <= 0;
                        boolean bottomReached = boxBottom >= size.rows() - 1;

                        // Draw border unless that edge has reached the screen boundary
                        if ((c == boxLeft && !leftReached) || (c == boxRight && !rightReached)
                                || (r == boxTop && !topReached) || (r == boxBottom && !bottomReached)) {
                            graphics.setCell(c, r, borderCell);
                        } else {
                            graphics.setCell(c, r, newBuffer.getCell(c, r));
                        }
                    } else {
                        // Inside the box: new content
                        graphics.setCell(c, r, newBuffer.getCell(c, r));
                    }
                } else {
                    // Outside the box: old content
                    graphics.setCell(c, r, oldScreen.getCell(c, r));
                }
            }
        }
    }
}