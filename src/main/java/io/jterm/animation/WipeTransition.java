package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

/**
 * Top-to-bottom wipe transition.
 * <p>
 * A horizontal line sweeps from row 0 to the bottom. Above the sweep line:
 * new screen content. Below the sweep line: old screen content. The sweep
 * line itself uses a full-block character ('█') in a highlight color.
 */
public class WipeTransition implements TransitionEffect {
    private final long durationMs;
    private final ScreenBuffer oldScreen;
    private final AnsiColor sweepColor;

    /**
     * Create a wipe transition with a default sweep color (bright cyan).
     *
     * @param durationMs total duration in milliseconds
     * @param oldScreen  buffer containing the old screen content
     */
    public WipeTransition(long durationMs, ScreenBuffer oldScreen) {
        this(durationMs, oldScreen, AnsiColor.BRIGHT_CYAN);
    }

    /**
     * Create a wipe transition with a custom sweep-line color.
     *
     * @param durationMs total duration in milliseconds
     * @param oldScreen  buffer containing the old screen content
     * @param sweepColor color of the sweeping line character
     */
    public WipeTransition(long durationMs, ScreenBuffer oldScreen, AnsiColor sweepColor) {
        this.durationMs = durationMs;
        this.oldScreen = oldScreen;
        this.sweepColor = sweepColor;
    }

    @Override
    public long durationMs() {
        return durationMs;
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size, double progress) {
        if (size.rows() <= 0 || size.columns() <= 0) return;

        double p = Math.max(0.0, Math.min(1.0, progress));
        int sweepRow = (int) Math.round(p * size.rows());

        // Capture the new content before we overwrite
        var size2 = size;
        var newBuffer = new ScreenBuffer(size2);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                newBuffer.setCell(c, r, graphics.getCell(c, r));

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (r < sweepRow) {
                    // Above sweep line: new content (already in graphics, keep it)
                    graphics.setCell(c, r, newBuffer.getCell(c, r));
                } else if (r == sweepRow) {
                    // Sweep line: block character in highlight color
                    graphics.setCell(c, r, new TextCell('\u2588', sweepColor, AnsiColor.BLACK));
                } else {
                    // Below sweep line: old content
                    graphics.setCell(c, r, oldScreen.getCell(c, r));
                }
            }
        }
    }
}