package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

import java.util.Random;

/**
 * Animated Voronoi cells background. Colored regions grow from random seed
 * points that drift slowly, with bright boundaries where the nearest seed
 * changes.
 */
public class VoronoiCells implements AnimatedBackground {

    private static final int TARGET_FPS = 8;
    private static final int SEED_COUNT = 8;
    private static final char INTERIOR_GLYPH = '\u00B0';  // 176
    private static final char BOUNDARY_GLYPH = '\u00B1';  // 177

    private final Random random = new Random();

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private volatile double time;

    private Seed[] seeds;

    private static final AnsiColor[] PALETTE = {
        AnsiColor.RED, AnsiColor.GREEN, AnsiColor.YELLOW,
        AnsiColor.BLUE, AnsiColor.MAGENTA, AnsiColor.CYAN,
        AnsiColor.BRIGHT_RED, AnsiColor.BRIGHT_GREEN
    };

    /**
     * Constructs a new VoronoiCells instance.
     * @param preferredSize the preferred size
     */
    public VoronoiCells(TerminalSize preferredSize) {
        onResize(preferredSize);
    }

    @Override
    /**
     * Renders one frame of the animation into the given graphics buffer.
     * @param graphics the graphics
     * @param size the size
     */
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        lastSize = size;
        if (size.columns() <= 0 || size.rows() <= 0) return;

        renderAtTime(graphics, size);
        time += 0.05;
    }

    /**
     * Renders one frame of the animation at the current internal time.
     * Visible for tests.
     *
     * @param graphics the text-graphics target
     * @param size     terminal size to render at
     */
    public void renderAtTime(TextGraphics graphics, TerminalSize size) {
        if (size.columns() <= 0 || size.rows() <= 0) return;

        ensureSeeds(size);

        // Black background.
        TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bg);

        int cols = size.columns();
        int rows = size.rows();

        // Advance seeds.
        for (Seed seed : seeds) {
            seed.advance();
        }

        // Compute nearest seed for each cell.
        int[][] nearest = new int[rows][cols];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                double bestDist = Double.MAX_VALUE;
                int bestIdx = 0;
                for (int i = 0; i < seeds.length; i++) {
                    double dx = x - seeds[i].x;
                    double dy = y - seeds[i].y;
                    double dist = dx * dx + dy * dy;
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestIdx = i;
                    }
                }
                nearest[y][x] = bestIdx;
            }
        }

        // Render cells, marking boundaries.
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int idx = nearest[y][x];
                boolean isBoundary = false;

                // Check right and down neighbors for boundary.
                if (x + 1 < cols && nearest[y][x + 1] != idx) {
                    isBoundary = true;
                } else if (y + 1 < rows && nearest[y + 1][x] != idx) {
                    isBoundary = true;
                }

                if (isBoundary) {
                    graphics.setCell(x, y, new TextCell(BOUNDARY_GLYPH, AnsiColor.BRIGHT_BLACK, AnsiColor.BLACK));
                } else {
                    AnsiColor color = seeds[idx].color;
                    graphics.setCell(x, y, new TextCell(INTERIOR_GLYPH, color, AnsiColor.BLACK));
                }
            }
        }
    }

    private void ensureSeeds(TerminalSize size) {
        if (seeds != null && seeds.length == SEED_COUNT) return;
        seeds = new Seed[SEED_COUNT];
        for (int i = 0; i < SEED_COUNT; i++) {
            seeds[i] = new Seed(size, random, PALETTE[i % PALETTE.length]);
        }
    }

    @Override
    /**
     * Reallocates internal buffers for the new terminal size.
     * @param newSize the new size
     */
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        if (newSize.columns() <= 0 || newSize.rows() <= 0) return;
        seeds = new Seed[SEED_COUNT];
        for (int i = 0; i < SEED_COUNT; i++) {
            seeds[i] = new Seed(newSize, random, PALETTE[i % PALETTE.length]);
        }
    }

    @Override
    /**
     * Starts the animation.
     */
    public void start() {
        running = true;
    }

    @Override
    /**
     * Stops the animation.
     */
    public void stop() {
        running = false;
    }

    @Override
    /**
     * Returns whether the running flag is set.
     * @return the result
     */
    public boolean isRunning() {
        return running;
    }

    @Override
    /**
     * Returns the target frame rate in frames per second.
     * @return the result
     */
    public int targetFps() {
        return TARGET_FPS;
    }

    @Override
    /**
     * Returns the last terminal size the animation was rendered at.
     * @return the result
     */
    public TerminalSize lastSize() {
        return lastSize;
    }

    /**
     * Returns the current animation time. Visible for tests.
     *
     * @return animation time in seconds
     */
    public double getTime() {
        return time;
    }

    /**
     * Returns the current seed points. Visible for tests.
     *
     * @return the seed array, or null before the first render
     */
    public Seed[] getSeeds() {
        return seeds;
    }

    /**
     * Returns the number of seed points. Visible for tests.
     *
     * @return the seed count, or 0 before the first render
     */
    public int getSeedCount() {
        return seeds == null ? 0 : seeds.length;
    }

    /** Seed point state. */
    public static final class Seed {
        private double x;
        private double y;
        private double vx;
        private double vy;
        private final AnsiColor color;
        private int cols;
        private int rows;

        Seed(TerminalSize size, Random random, AnsiColor color) {
            this.cols = size.columns();
            this.rows = size.rows();
            this.x = random.nextInt(Math.max(1, cols));
            this.y = random.nextInt(Math.max(1, rows));
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 0.1 + random.nextDouble() * 0.2;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
            this.color = color;
        }

        void advance() {
            x += vx;
            y += vy;
            // Bounce off edges.
            if (x <= 0) { x = 0; vx = -vx; }
            if (x >= cols - 1) { x = cols - 1; vx = -vx; }
            if (y <= 0) { y = 0; vy = -vy; }
            if (y >= rows - 1) { y = rows - 1; vy = -vy; }
        }

        /**
         * Returns the X coordinate.
         *
         * @return the X coordinate
         */
        public double getX() { return x; }

        /**
         * Returns the Y coordinate.
         *
         * @return the Y coordinate
         */
        public double getY() { return y; }

        /**
         * Returns the horizontal velocity.
         *
         * @return the horizontal velocity (cells per tick)
         */
        public double getVx() { return vx; }

        /**
         * Returns the vertical velocity.
         *
         * @return the vertical velocity (cells per tick)
         */
        public double getVy() { return vy; }

        /**
         * Returns the cell color.
         *
         * @return the color
         */
        public AnsiColor getColor() { return color; }
    }
}
