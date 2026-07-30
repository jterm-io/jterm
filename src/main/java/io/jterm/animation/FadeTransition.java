package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

import java.util.Random;

/**
 * Gradual fade screen transition.
 * <p>
 * 16-color ANSI terminals can't do true alpha blending, so we simulate a fade
 * by progressively replacing characters with dimmer block characters:
 * <pre>
 *   '█' → '▓' → '▒' → '░' → ' '  (full → faded → gone)
 * </pre>
 * <p>
 * First half (0.0 → 0.5): fade old content out. Each cell has a random threshold
 * (from a seeded shuffle). As progress increases, cells past their threshold
 * progressively dim through the block characters until they become spaces.
 * At 0.5 everything is faded to spaces/dim blocks.
 * <p>
 * Second half (0.5 → 1.0): fade new content in. Reverse of the first half —
 * cells start faded and progressively restore to full brightness. At 1.0 the
 * new screen is fully visible.
 */
public class FadeTransition implements TransitionEffect {
    private final long durationMs;
    private final ScreenBuffer oldScreen;
    private final long seed;

    /** Block characters from most-solid to least-solid, then space. */
    private static final char[] FADE_CHARS = {'█', '▓', '▒', '░', ' '};
    /** Index of the empty/space fade level. */
    private static final int EMPTY_LEVEL = FADE_CHARS.length - 1;

    /**
     * Create a fade transition with a default seed for deterministic behavior.
     *
     * @param durationMs total duration in milliseconds
     * @param oldScreen  buffer containing the old screen content
     */
    public FadeTransition(long durationMs, ScreenBuffer oldScreen) {
        this(durationMs, oldScreen, 12345L);
    }

    /**
     * Create a fade transition with an explicit seed.
     *
     * @param durationMs total duration in milliseconds
     * @param oldScreen  buffer containing the old screen content
     * @param seed       random seed for reproducible fade pattern
     */
    public FadeTransition(long durationMs, ScreenBuffer oldScreen, long seed) {
        this.durationMs = durationMs;
        this.oldScreen = oldScreen;
        this.seed = seed;
    }

    @Override
    public long durationMs() { return durationMs; }

    @Override
    public int targetFps() { return 10; }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size, double progress) {
        if (size.rows() <= 0 || size.columns() <= 0) return;
        double p = Math.max(0.0, Math.min(1.0, progress));

        int totalCells = size.rows() * size.columns();

        // Boundary guarantees: at the exact endpoints, show full content.
        if (p <= 0.0) {
            for (int r = 0; r < size.rows(); r++)
                for (int c = 0; c < size.columns(); c++)
                    graphics.setCell(c, r, oldScreen.getCell(c, r));
            return;
        }
        if (p >= 1.0) {
            // New content is already in the graphics buffer — nothing to do.
            return;
        }

        // Build a shuffled index array so the fade looks organic, not a sweep.
        int[] indices = new int[totalCells];
        for (int i = 0; i < totalCells; i++) indices[i] = i;
        shuffle(indices, new Random(seed));

        // Capture the NEW content that the graphics buffer already holds.
        var newBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                newBuffer.setCell(c, r, graphics.getCell(c, r));

        // Width of the soft "wave front" band, in fractions of total cells.
        // At least 4 cells wide so all intermediate block chars are visible,
        // at most 20% of the grid so it doesn't dominate small screens.
        double bandWidth = Math.min(0.2, Math.max(1.0 / totalCells * 4, 0.05));

        if (p <= 0.5) {
            // ── Fade OUT old content ──────────────────────────────────
            double fp = p / 0.5;  // normalized 0.0 → 1.0 within first half
            for (int i = 0; i < totalCells; i++) {
                int idx = indices[i];
                int col = idx % size.columns();
                int row = idx / size.columns();
                var oldCell = oldScreen.getCell(col, row);
                double threshold = (double) i / totalCells;
                double lead = fp - threshold;
                int level = fadeOutLevel(lead, bandWidth);
                graphics.setCell(col, row, fadedCell(oldCell, level));
            }
        } else {
            // ── Fade IN new content ───────────────────────────────────
            double fp = (p - 0.5) / 0.5;  // normalized 0.0 → 1.0 within second half
            for (int i = 0; i < totalCells; i++) {
                int idx = indices[i];
                int col = idx % size.columns();
                int row = idx / size.columns();
                var newCell = newBuffer.getCell(col, row);
                double threshold = (double) i / totalCells;
                double lead = fp - threshold;
                int level = fadeInLevel(lead, bandWidth);
                if (level <= 0) {
                    graphics.setCell(col, row, newCell);
                } else {
                    graphics.setCell(col, row, fadedCell(newCell, level));
                }
            }
        }
    }

    /**
     * Fade-out level from lead (distance of the cell's threshold ahead of the
     * wave front). lead ≤ 0 → level 0 (full brightness). lead > 0 → ramp
     * through block chars to space, reaching level 4 at lead ≥ bandWidth.
     */
    private static int fadeOutLevel(double lead, double bandWidth) {
        if (lead <= 0.0) return 0;
        if (lead >= bandWidth) return EMPTY_LEVEL;
        return Math.min(EMPTY_LEVEL, (int) Math.ceil(lead / bandWidth * EMPTY_LEVEL));
    }

    /**
     * Fade-in level from lead (distance of the cell's threshold ahead of the
     * wave front). lead ≤ 0 → level 4 (space, not yet restoring). lead > 0 →
     * ramp down to level 0 (full brightness), reaching it at lead ≥ bandWidth.
     */
    private static int fadeInLevel(double lead, double bandWidth) {
        if (lead <= 0.0) return EMPTY_LEVEL;
        if (lead >= bandWidth) return 0;
        return Math.max(0, EMPTY_LEVEL - (int) Math.ceil(lead / bandWidth * EMPTY_LEVEL));
    }

    /**
     * Creates a faded version of a cell at the given fade level.
     * Level 0 = full original character (full brightness).
     * Level 1-4 = block character at that dimness, using BRIGHT_BLACK (dim gray)
     * as the foreground color to simulate darkness.
     */
    private static TextCell fadedCell(TextCell original, int level) {
        if (level <= 0) {
            return original;
        }
        char ch = FADE_CHARS[Math.min(level, EMPTY_LEVEL)];
        return new TextCell(String.valueOf(ch), AnsiColor.BRIGHT_BLACK,
                            original.bg(), original.modifiers());
    }

    /** Fisher-Yates shuffle with the given Random. */
    private static void shuffle(int[] arr, Random rng) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }
}