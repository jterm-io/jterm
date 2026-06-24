package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.Random;

/**
 * Animated CRT phosphor decay background. Characters appear at full brightness
 * and slowly fade through dim green to black, like text left glowing on an old
 * monochrome terminal. The grid holds a brightness level per cell; each frame
 * decays every lit cell and randomly excites new ones.
 *
 * <p>This implementation is {@link AnimatedBackground} only (not a
 * {@link io.jterm.widget.Component}) so it can avoid the component
 * lifecycle and focus on full-frame rendering behind a login screen.</p>
 */
public class PhosphorDecay implements AnimatedBackground {

    private static final int TARGET_FPS = 10;

    // Six brightness levels, 5 brightest, 0 black.
    private static final int MAX_LEVEL = 5;
    private static final int MIN_LEVEL = 0;

    private static final char[] GLYPHS = {
            ' ', '.', ':', 'o', 'O', '@'
    };

    private static final double EXCITE_CHANCE = 0.06;

    private final Random random;

    private volatile boolean running;
    private volatile TerminalSize lastSize;

    private int[][] grid;
    private TerminalSize currentSize;

    public PhosphorDecay(TerminalSize preferredSize) {
        this(preferredSize, new Random());
    }

    /** Visible for tests: inject a deterministic Random. */
    PhosphorDecay(TerminalSize preferredSize, Random rng) {
        this.random = rng;
        onResize(preferredSize);
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        lastSize = size;
        if (size.columns() <= 0 || size.rows() <= 0) return;

        decayAndRender(graphics, size);
    }

    private void decayAndRender(TextGraphics graphics, TerminalSize size) {
        ensureGrid(size);

        int rows = size.rows();
        int cols = size.columns();

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int level = grid[x][y];
                if (level > MIN_LEVEL) {
                    // Most cells decay by one level each frame. A few decay
                    // faster to give the fade a little organic variation.
                    if (random.nextDouble() < 0.15) {
                        level = Math.max(MIN_LEVEL, level - 2);
                    } else {
                        level--;
                    }
                    grid[x][y] = level;
                }

                // Randomly excite cells to full brightness.
                if (random.nextDouble() < EXCITE_CHANCE) {
                    grid[x][y] = MAX_LEVEL;
                }

                graphics.setCell(x, y, cellForLevel(grid[x][y]));
            }
        }
    }

    private TextCell cellForLevel(int level) {
        return switch (level) {
            case 0 -> new TextCell(GLYPHS[0], AnsiColor.BLACK, AnsiColor.BLACK);
            case 1 -> new TextCell(GLYPHS[1], AnsiColor.BRIGHT_BLACK, AnsiColor.BLACK);
            case 2 -> new TextCell(GLYPHS[2], AnsiColor.GREEN, AnsiColor.BLACK);
            case 3 -> new TextCell(GLYPHS[3], AnsiColor.GREEN, AnsiColor.BLACK);
            case 4 -> new TextCell(GLYPHS[4], AnsiColor.BRIGHT_GREEN, AnsiColor.BLACK);
            case 5 -> new TextCell(GLYPHS[5], AnsiColor.BRIGHT_GREEN, AnsiColor.BLACK, SGR.BOLD);
            default -> new TextCell(GLYPHS[0], AnsiColor.BLACK, AnsiColor.BLACK);
        };
    }

    private void ensureGrid(TerminalSize size) {
        if (currentSize != null
                && currentSize.columns() == size.columns()
                && currentSize.rows() == size.rows()) {
            return;
        }

        int[][] old = grid;
        int oldCols = currentSize == null ? 0 : currentSize.columns();
        int oldRows = currentSize == null ? 0 : currentSize.rows();

        grid = new int[size.columns()][size.rows()];
        currentSize = size;

        // Preserve existing brightness for overlapping regions.
        if (old != null) {
            int preserveCols = Math.min(oldCols, size.columns());
            int preserveRows = Math.min(oldRows, size.rows());
            for (int x = 0; x < preserveCols; x++) {
                System.arraycopy(old[x], 0, grid[x], 0, preserveRows);
            }
        }

        // Seed a few random bright cells so the first frame isn't entirely black.
        if (old == null) {
            int area = size.columns() * size.rows();
            int seeds = Math.min(area, Math.max(3, area / 20));
            for (int i = 0; i < seeds; i++) {
                int x = random.nextInt(Math.max(1, size.columns()));
                int y = random.nextInt(Math.max(1, size.rows()));
                grid[x][y] = MAX_LEVEL;
            }
        }
    }

    @Override
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        this.currentSize = null;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int targetFps() {
        return TARGET_FPS;
    }

    @Override
    public TerminalSize lastSize() {
        return lastSize;
    }

    /** Visible for tests: read the current grid. */
    public int[][] getGrid() {
        if (grid == null) return new int[0][0];
        int[][] copy = new int[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            copy[i] = grid[i].clone();
        }
        return copy;
    }

    /** Visible for tests: force a cell to a brightness level. */
    public void setCell(int x, int y, int level) {
        if (grid == null || currentSize == null) return;
        if (x < 0 || x >= currentSize.columns() || y < 0 || y >= currentSize.rows()) return;
        grid[x][y] = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }
}
