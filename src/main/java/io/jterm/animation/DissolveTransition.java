package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;

import java.util.Random;

/**
 * Classic BBS dissolve transition.
 * <p>
 * Random cells scatter to the new screen in a random order. At progress p,
 * a fraction p of cells show new content, the rest show old content. Uses a
 * deterministic {@link Random} seeded at construction for reproducibility.
 */
public class DissolveTransition implements TransitionEffect {
    private final long durationMs;
    private final ScreenBuffer oldScreen;
    private final long seed;

    /**
     * Create a dissolve transition with a seed for deterministic behavior.
     *
     * @param durationMs total duration in milliseconds
     * @param oldScreen  buffer containing the old screen content
     * @param seed       random seed for reproducible dissolve pattern
     */
    public DissolveTransition(long durationMs, ScreenBuffer oldScreen, long seed) {
        this.durationMs = durationMs;
        this.oldScreen = oldScreen;
        this.seed = seed;
    }

    @Override
    /** Returns the transition duration in milliseconds.
 * @return the duration in ms */
    public long durationMs() {
        return durationMs;
    }

    @Override
    /** Renders a single frame of the transition.
     * @param graphics the graphics context
     * @param size the terminal size
     * @param progress the transition progress (0.0 to 1.0) */
    public void renderFrame(TextGraphics graphics, TerminalSize size, double progress) {
        if (size.rows() <= 0 || size.columns() <= 0) return;

        double p = Math.max(0.0, Math.min(1.0, progress));
        int totalCells = size.rows() * size.columns();
        int cellsToReveal = (int) Math.round(p * totalCells);

        // Create shuffled index array
        int[] indices = new int[totalCells];
        for (int i = 0; i < totalCells; i++) indices[i] = i;
        shuffle(indices, new Random(seed));

        // Capture new content
        var newBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                newBuffer.setCell(c, r, graphics.getCell(c, r));

        // First `cellsToReveal` cells (in shuffled order) show new content,
        // the rest show old content.
        boolean[] showNew = new boolean[totalCells];
        for (int i = 0; i < cellsToReveal && i < totalCells; i++) {
            showNew[indices[i]] = true;
        }

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                int idx = r * size.columns() + c;
                if (showNew[idx]) {
                    graphics.setCell(c, r, newBuffer.getCell(c, r));
                } else {
                    graphics.setCell(c, r, oldScreen.getCell(c, r));
                }
            }
        }
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