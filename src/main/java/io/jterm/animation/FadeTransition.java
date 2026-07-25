package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

/**
 * Fade-through-black screen transition.
 * <p>
 * Simple 3-step transition: old screen → black hold → new screen.
 * <p>
 * 16-color ANSI terminals can't dim smoothly, so we use a simple approach:
 * show old content, hold black for enough frames to be visible, then show new.
 * The key is timing — black must be held long enough to register as a deliberate
 * transition, not a glitch.
 */
public class FadeTransition implements TransitionEffect {
    private final long durationMs;
    private final ScreenBuffer oldScreen;

    public FadeTransition(long durationMs, ScreenBuffer oldScreen) {
        this.durationMs = durationMs;
        this.oldScreen = oldScreen;
    }

    @Override
    public long durationMs() { return durationMs; }

    @Override
    public int targetFps() { return 8; }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size, double progress) {
        if (size.rows() <= 0 || size.columns() <= 0) return;
        double p = Math.max(0.0, Math.min(1.0, progress));

        if (p < 0.33) {
            // First third: show old screen content unchanged
            for (int r = 0; r < size.rows(); r++) {
                for (int c = 0; c < size.columns(); c++) {
                    graphics.setCell(c, r, oldScreen.getCell(c, r));
                }
            }
        } else if (p < 0.67) {
            // Middle third: solid black
            for (int r = 0; r < size.rows(); r++) {
                for (int c = 0; c < size.columns(); c++) {
                    var src = oldScreen.getCell(c, r);
                    graphics.setCell(c, r, new TextCell(src.character(), AnsiColor.BLACK, AnsiColor.BLACK, src.modifiers()));
                }
            }
        } else {
            // Last third: show new screen content (already in graphics buffer, leave as-is)
        }
    }
}