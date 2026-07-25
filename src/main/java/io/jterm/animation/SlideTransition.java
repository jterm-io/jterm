package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;

/**
 * Slide-up screen transition.
 * <p>
 * The old screen slides upward while the new screen enters from the bottom.
 * At progress p, the old screen is shifted up by {@code p * height} rows.
 * Cells that scroll off the top are gone; cells that enter from the bottom
 * are new content.
 */
public class SlideTransition implements TransitionEffect {
    private final long durationMs;
    private final ScreenBuffer oldScreen;

    /**
     * Create a slide transition.
     *
     * @param durationMs total duration in milliseconds
     * @param oldScreen  buffer containing the old screen content
     */
    public SlideTransition(long durationMs, ScreenBuffer oldScreen) {
        this.durationMs = durationMs;
        this.oldScreen = oldScreen;
    }

    @Override
    public long durationMs() {
        return durationMs;
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size, double progress) {
        if (size.rows() <= 0 || size.columns() <= 0) return;

        double p = Math.max(0.0, Math.min(1.0, progress));
        int shift = (int) Math.round(p * size.rows());

        // Capture new content
        var newBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                newBuffer.setCell(c, r, graphics.getCell(c, r));

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                // Source row in old buffer: r + shift
                int oldRow = r + shift;
                if (oldRow < size.rows()) {
                    // Old content from shifted position
                    graphics.setCell(c, r, oldScreen.getCell(c, oldRow));
                } else {
                    // New content enters from bottom
                    int newRow = oldRow - size.rows();
                    if (newRow < size.rows()) {
                        graphics.setCell(c, r, newBuffer.getCell(c, newRow));
                    } else {
                        // Out of bounds — should not happen with clamped progress
                        graphics.setCell(c, r, io.jterm.style.TextCell.EMPTY);
                    }
                }
            }
        }
    }
}