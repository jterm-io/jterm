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

    public VoronoiCells(TerminalSize preferredSize) {
        onResize(preferredSize);
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        lastSize = size;
        if (size.columns() <= 0 || size.rows() <= 0) return;

        renderAtTime(graphics, size);
        time += 0.05;
    }

    /** Visible for tests. */
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
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        if (newSize.columns() <= 0 || newSize.rows() <= 0) return;
        seeds = new Seed[SEED_COUNT];
        for (int i = 0; i < SEED_COUNT; i++) {
            seeds[i] = new Seed(newSize, random, PALETTE[i % PALETTE.length]);
        }
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

    /** Visible for tests. */
    public double getTime() {
        return time;
    }

    /** Visible for tests. */
    public Seed[] getSeeds() {
        return seeds;
    }

    /** Visible for tests. */
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

        public double getX() { return x; }
        public double getY() { return y; }
        public double getVx() { return vx; }
        public double getVy() { return vy; }
        public AnsiColor getColor() { return color; }
    }
}
