package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

/**
 * Fade-through-black screen transition.
 * <p>
 * At progress 0.0 the old screen is fully visible. At progress 0.5 the screen
 * is fully black. At progress 1.0 the new screen is fully visible.
 * <p>
 * Implementation uses discrete dimming steps via SGR modifiers rather than
 * color blending, because the 16-color ANSI palette has too few shades to
 * produce a smooth blend. This approach works on all terminals.
 * <p>
 * The dimming schedule across progress [0.0, 0.5):
 * <ul>
 *   <li>[0.00, 0.25) — old content at full intensity (no dim)</li>
 *   <li>[0.25, 0.40) — old content with DIM modifier</li>
 *   <li>[0.40, 0.50) — old content as black-on-black (invisible)</li>
 * </ul>
 * And the brightening schedule across progress [0.5, 1.0]:
 * <ul>
 *   <li>[0.50, 0.60) — new content as black-on-black (invisible)</li>
 *   <li>[0.60, 0.75) — new content with DIM modifier</li>
 *   <li>[0.75, 1.00] — new content at full intensity (no dim)</li>
 * </ul>
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
    public int targetFps() {
        // Lower FPS = fewer full-screen refreshes over the wire.
        // 10 FPS × 600ms = 6 frames total. Enough for the 6 discrete steps.
        return 10;
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size, double progress) {
        if (size.rows() <= 0 || size.columns() <= 0) return;

        double p = Math.max(0.0, Math.min(1.0, progress));

        if (p < 0.5) {
            // First half: fade old content toward black in 3 discrete steps
            DimLevel dim;
            if (p < 0.25) {
                dim = DimLevel.FULL;
            } else if (p < 0.40) {
                dim = DimLevel.DIM;
            } else {
                dim = DimLevel.BLACK;
            }
            for (int r = 0; r < size.rows(); r++) {
                for (int c = 0; c < size.columns(); c++) {
                    TextCell oldCell = oldScreen.getCell(c, r);
                    graphics.setCell(c, r, applyDim(oldCell, dim));
                }
            }
        } else {
            // Second half: fade from black to new content in 3 discrete steps
            DimLevel dim;
            if (p < 0.60) {
                dim = DimLevel.BLACK;
            } else if (p < 0.75) {
                dim = DimLevel.DIM;
            } else {
                dim = DimLevel.FULL;
            }
            for (int r = 0; r < size.rows(); r++) {
                for (int c = 0; c < size.columns(); c++) {
                    TextCell newCell = graphics.getCell(c, r);
                    graphics.setCell(c, r, applyDim(newCell, dim));
                }
            }
        }
    }

    /**
     * Apply a dimming level to a cell.
     * <ul>
     *   <li>FULL — cell unchanged</li>
     *   <li>DIM — cell with SGR.DIM modifier added (terminal renders at half intensity)</li>
     *   <li>BLACK — cell with both fg and bg set to BLACK (invisible)</li>
     * </ul>
     */
    private static TextCell applyDim(TextCell cell, DimLevel level) {
        return switch (level) {
            case FULL -> cell;
            case DIM -> {
                java.util.EnumSet<SGR> mods = cell.modifiers() != null
                    ? java.util.EnumSet.copyOf(cell.modifiers())
                    : java.util.EnumSet.noneOf(SGR.class);
                mods.add(SGR.DIM);
                yield new TextCell(cell.character(), cell.fg(), cell.bg(), mods);
            }
            case BLACK -> new TextCell(cell.character(), AnsiColor.BLACK, AnsiColor.BLACK, cell.modifiers());
        };
    }

    private enum DimLevel { FULL, DIM, BLACK }
}