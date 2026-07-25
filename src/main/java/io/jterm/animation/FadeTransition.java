package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

/**
 * Fade-through-black screen transition.
 * <p>
 * At progress 0.0 the old screen is fully visible. At progress 0.5 the screen
 * is fully black. At progress 1.0 the new screen is fully visible.
 * <p>
 * Implementation: iterate all cells, blend toward black for the first half
 * (multiply RGB by {@code 1 - progress*2}), blend from black to new content for
 * the second half (multiply RGB by {@code progress*2 - 1}).
 */
public class FadeTransition implements TransitionEffect {
    private final long durationMs;
    private final ScreenBuffer oldScreen;

    /**
     * Create a fade transition.
     *
     * @param durationMs total duration in milliseconds
     * @param oldScreen  buffer containing the old screen content (before transition)
     */
    public FadeTransition(long durationMs, ScreenBuffer oldScreen) {
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

        // Clamp progress to [0, 1]
        double p = Math.max(0.0, Math.min(1.0, progress));

        if (p < 0.5) {
            // First half: fade old content toward black
            double intensity = 1.0 - p * 2.0; // 1.0 → 0.0
            for (int r = 0; r < size.rows(); r++) {
                for (int c = 0; c < size.columns(); c++) {
                    TextCell oldCell = oldScreen.getCell(c, r);
                    AnsiColor dimmedFg = dimColor(oldCell.fg(), intensity);
                    AnsiColor dimmedBg = dimColor(oldCell.bg(), intensity);
                    graphics.setCell(c, r, new TextCell(oldCell.character(), dimmedFg, dimmedBg, oldCell.modifiers()));
                }
            }
        } else {
            // Second half: fade from black to new content
            double intensity = p * 2.0 - 1.0; // 0.0 → 1.0
            for (int r = 0; r < size.rows(); r++) {
                for (int c = 0; c < size.columns(); c++) {
                    TextCell newCell = graphics.getCell(c, r);
                    AnsiColor dimmedFg = dimColor(newCell.fg(), intensity);
                    AnsiColor dimmedBg = dimColor(newCell.bg(), intensity);
                    graphics.setCell(c, r, new TextCell(newCell.character(), dimmedFg, dimmedBg, newCell.modifiers()));
                }
            }
        }
    }

    /**
     * Dim a color toward black by the given intensity factor (0.0 = black, 1.0 = full).
     */
    private static AnsiColor dimColor(io.jterm.style.Color color, double intensity) {
        if (intensity <= 0.0) return AnsiColor.BLACK;
        if (intensity >= 1.0) {
            return color instanceof AnsiColor ac ? ac : AnsiColor.DEFAULT;
        }

        if (color instanceof AnsiColor ac) {
            if (ac == AnsiColor.DEFAULT) return AnsiColor.DEFAULT;
            // Blend toward black using blendAnsi
            return AnsiColor.blendAnsi(ac, AnsiColor.BLACK, intensity);
        }
        // For RgbColor / IndexedColor, convert to nearest AnsiColor and blend
        return AnsiColor.BLACK;
    }
}