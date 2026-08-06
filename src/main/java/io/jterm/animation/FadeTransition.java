package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

/**
 * Top-to-bottom sweep (curtain) fade transition.
 * <p>
 * A horizontal sweep line moves from row 0 (top) to the last row (bottom) over
 * the duration of the transition. The sweep pattern is a <b>curtain drop</b>:
 * <ul>
 *   <li><b>Above the sweep line:</b> the new screen content is fully visible.
 *   <li><b>Below the sweep line:</b> the old screen content remains unchanged.
 *   <li><b>At the sweep line:</b> a band of progressively dimmer block characters
 *       ('█' → '▓' → '▒' → '░' → ' ') creates a smooth fade edge between the
 *       two regions.
 * </ul>
 * <p>
 * 16-color ANSI terminals can't do true alpha blending, so the fade band uses
 * {@link AnsiColor#BRIGHT_BLACK} (dim gray) on block characters to simulate
 * the dimming.
 * <p>
 * Unlike the earlier random-pixel implementation, this version is purely
 * positional and therefore inherently deterministic — the {@code seed}
 * constructor parameter is retained only for API compatibility and has no
 * effect on the rendered output.
 */
public class FadeTransition implements TransitionEffect {
    private final long durationMs;
    private final ScreenBuffer oldScreen;
    @SuppressWarnings("unused")
    private final long seed;

    /**
     * Block characters from most-solid to least-solid, then space.
     * <pre>
     *   '█' → '▓' → '▒' → '░' → ' '  (full → faded → gone)
     * </pre>
     */
    private static final char[] FADE_CHARS = {'█', '▓', '▒', '░', ' '};
    /** Index of the empty/space fade level. */
    private static final int EMPTY_LEVEL = FADE_CHARS.length - 1;

    /** Number of rows occupied by the soft fade band at the sweep line. */
    private static final int BAND_ROWS = 4;

    /**
     * Create a fade transition with a default seed (deterministic behavior).
     *
     * @param durationMs total duration in milliseconds
     * @param oldScreen  buffer containing the old screen content
     */
    public FadeTransition(long durationMs, ScreenBuffer oldScreen) {
        this(durationMs, oldScreen, 12345L);
    }

    /**
     * Create a fade transition with an explicit seed.
     * <p>
     * The sweep pattern is positional and therefore already deterministic;
     * the seed is retained for API compatibility only and does not affect the
     * rendered output.
     *
     * @param durationMs total duration in milliseconds
     * @param oldScreen  buffer containing the old screen content
     * @param seed       ignored — kept for API compatibility
     */
    public FadeTransition(long durationMs, ScreenBuffer oldScreen, long seed) {
        this.durationMs = durationMs;
        this.oldScreen = oldScreen;
        this.seed = seed;
    }

    @Override
    /** Returns the transition duration in milliseconds.
 * @return the duration in ms */
    public long durationMs() { return durationMs; }

    @Override
    public int targetFps() { return 10; }

    @Override
    /** Renders a single frame of the fade transition.
 * @param graphics the graphics context
 * @param size the terminal size
 * @param progress the transition progress (0.0 to 1.0) */
    public void renderFrame(TextGraphics graphics, TerminalSize size, double progress) {
        if (size.rows() <= 0 || size.columns() <= 0) return;
        double p = Math.max(0.0, Math.min(1.0, progress));

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

        // Capture the NEW content that the graphics buffer already holds,
        // because we're about to overwrite parts of it below.
        var newBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                newBuffer.setCell(c, r, graphics.getCell(c, r));

        // Sweep line position, measured as a fractional row index.
        // At p=0 the sweep is at the top row; at p=1 it's at the bottom.
        // The boundary guards above handle the exact endpoints.
        double halfBand = (BAND_ROWS - 1) / 2.0;
        double sweepPos = p * size.rows();

        // The fade band covers [sweepPos - halfBand, sweepPos + halfBand].
        // Within the band, rows closer to the sweep line are more solid,
        // rows further away are dimmer — producing a smooth curtain edge.
        for (int r = 0; r < size.rows(); r++) {
            double dist = r - sweepPos;  // negative = above (new), positive = below (old)
            if (dist <= -halfBand) {
                // Above the band: new content, fully visible.
                for (int c = 0; c < size.columns(); c++)
                    graphics.setCell(c, r, newBuffer.getCell(c, r));
            } else if (dist >= halfBand) {
                // Below the band: old content, untouched.
                for (int c = 0; c < size.columns(); c++)
                    graphics.setCell(c, r, oldScreen.getCell(c, r));
            } else {
                // Inside the fade band: pick a block character based on the
                // distance from the sweep line. dist == 0 → '█' (solid),
                // dist == ±halfBand → ' ' (edge of the band).
                int level = bandLevel(dist, halfBand);
                var fadeCell = bandCell(level);
                for (int c = 0; c < size.columns(); c++)
                    graphics.setCell(c, r, fadeCell);
            }
        }
    }

    /**
     * Map a row's distance from the sweep line (within the band) to a fade
     * level. {@code dist == 0} → level 0 (solid '█'); {@code |dist| == halfBand}
     * → level 4 (space ' ').
     */
    private static int bandLevel(double dist, double halfBand) {
        double t = Math.abs(dist) / halfBand;          // 0.0 (center) → 1.0 (edge)
        int level = (int) Math.round(t * EMPTY_LEVEL);
        return Math.max(0, Math.min(EMPTY_LEVEL, level));
    }

    /**
     * Build the block-character cell used at a given fade band level.
     * Level 0 = solid '█', level 4 = space ' ', all in BRIGHT_BLACK so the
     * band reads as a dim edge between old and new content.
     */
    private static TextCell bandCell(int level) {
        char ch = FADE_CHARS[level];
        return new TextCell(String.valueOf(ch), AnsiColor.BRIGHT_BLACK,
                            AnsiColor.BLACK, java.util.EnumSet.noneOf(
                                io.jterm.style.SGR.class));
    }
}