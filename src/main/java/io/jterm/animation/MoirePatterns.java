package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

/**
 * Animated Moire pattern background.
 *
 * <p>Two square dot grids rotate at different angles and speeds. Where the
 * grids align, the dots overlap and the cells glow bright; where they miss, the
 * cells fall back to dim. The slow relative rotation creates shifting
 * interference fringes across the terminal.</p>
 */
public class MoirePatterns implements AnimatedBackground {

    private static final int TARGET_FPS = 9;

    // Grid spacing in terminal cells.
    private static final int GRID_SPACING = 4;

    // Rotation speed in radians per frame for each grid.
    private static final double ANGLE_A_SPEED = 0.03;
    private static final double ANGLE_B_SPEED = -0.045;

    // Character ramp from dim overlap to bright overlap.
    private static final char[] CHARS = {'.', '+', '#', '*'};

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private volatile double angleA;
    private volatile double angleB;
    private volatile int frame;

    /**
     * Constructs a new MoirePatterns instance.
     * @param preferredSize the preferred size
     */
    public MoirePatterns(TerminalSize preferredSize) {
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

        frame++;
        angleA += ANGLE_A_SPEED;
        angleB += ANGLE_B_SPEED;

        int cols = size.columns();
        int rows = size.rows();
        double cx = cols / 2.0;
        double cy = rows / 2.0;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                double value = overlapValue(x, y, cx, cy);
                graphics.setCell(x, y, cellFor(value, frame));
            }
        }
    }

    /**
     * Computes how closely the two rotated grids coincide at (x, y).
     * Higher values mean the cell is close to a dot in both grids.
     */
    double overlapValue(int x, int y, double cx, double cy) {
        double dx = x - cx;
        double dy = y - cy;

        // Distance from nearest grid point in grid A.
        double da = distanceToGrid(dx, dy, angleA);
        // Distance from nearest grid point in grid B.
        double db = distanceToGrid(dx, dy, angleB);

        // Overlap is strongest when both grids place a dot near this cell.
        double overlap = Math.exp(-da * da / 0.8) * Math.exp(-db * db / 0.8);

        // Add a weak single-grid contribution so non-overlapping areas still
        // show the rotating grids rather than falling completely black.
        double single = 0.25 * Math.exp(-Math.min(da * da, db * db) / 0.9);
        return Math.min(1.0, overlap + single);
    }

    /**
     * Rotates (x, y) by angle and returns the Euclidean distance to the nearest
     * lattice point of a square grid with the configured spacing.
     */
    private double distanceToGrid(double dx, double dy, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double rx = dx * cos - dy * sin;
        double ry = dx * sin + dy * cos;
        double nearestX = Math.round(rx / GRID_SPACING) * GRID_SPACING;
        double nearestY = Math.round(ry / GRID_SPACING) * GRID_SPACING;
        double ddx = rx - nearestX;
        double ddy = ry - nearestY;
        return Math.sqrt(ddx * ddx + ddy * ddy);
    }

    private TextCell cellFor(double value, int frame) {
        int level = Math.min(CHARS.length - 1, (int) (value * CHARS.length));
        char ch = CHARS[level];

        // Brighter colors where the grids overlap; dimmer cyan/blue where they don't.
        Color fg = pickForeground(level);
        Color bg = pickBackground(level);

        SGR sgr = level >= 2 ? SGR.BOLD : SGR.DIM;
        // Subtle frame-to-frame shimmer on the brightest cells.
        if (level == CHARS.length - 1 && (frame % 3) == 0) {
            sgr = SGR.BOLD;
        }
        return new TextCell(ch, fg, bg, sgr);
    }

    private Color pickForeground(int level) {
        return switch (level) {
            case 0 -> AnsiColor.BLUE;
            case 1 -> AnsiColor.CYAN;
            case 2 -> AnsiColor.BRIGHT_CYAN;
            default -> AnsiColor.BRIGHT_WHITE;
        };
    }

    private Color pickBackground(int level) {
        return switch (level) {
            case 0 -> AnsiColor.BLACK;
            case 1 -> AnsiColor.BLACK;
            case 2 -> AnsiColor.BLUE;
            default -> AnsiColor.CYAN;
        };
    }

    @Override
    /**
     * Reallocates internal buffers for the new terminal size.
     * @param newSize the new size
     */
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
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
     * Returns the current frame counter, visible for tests.
     *
     * @return the frame counter
     */
    public int getFrame() {
        return frame;
    }

    /**
     * Returns the current angle of the first grid, visible for tests.
     *
     * @return the first grid angle
     */
    public double getAngleA() {
        return angleA;
    }

    /**
     * Returns the current angle of the second grid, visible for tests.
     *
     * @return the second grid angle
     */
    public double getAngleB() {
        return angleB;
    }
}
