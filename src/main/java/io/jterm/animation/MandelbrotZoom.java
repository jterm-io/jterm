package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

/**
 * Animated Mandelbrot set zoom background.
 *
 * <p>Renders the Mandelbrot set at a fixed terminal resolution and slowly zooms
 * into the well-known minibrot near (-0.745, 0.113). After zooming deeply the
 * view resets and the cycle repeats. Iteration count drives both a character
 * ramp and an ANSI color ramp, keeping CPU usage modest at terminal sizes.</p>
 */
public class MandelbrotZoom implements AnimatedBackground {

    private static final int TARGET_FPS = 10;

    // Character ramp from outside (space) to inside (#) of the set.
    private static final char[] GLYPHS = {' ', '.', ':', '-', '=', '+', '*', '#'};

    private static final int MAX_ITERATIONS = 40;

    // Interesting point with a small minibrot.
    private static final double CENTER_X = -0.745;
    private static final double CENTER_Y = 0.113;

    // Initial viewport width in the complex plane.
    private static final double INITIAL_RANGE = 2.5;

    // How much the range shrinks per frame (very slow zoom).
    private static final double ZOOM_FACTOR = 0.96;

    // After the view gets this narrow, reset.
    private static final double MIN_RANGE = 0.0005;

    private volatile boolean running;
    private volatile TerminalSize lastSize;

    private double range;

    /**
     * Constructs a new MandelbrotZoom instance.
     * @param preferredSize the preferred size
     */
    public MandelbrotZoom(TerminalSize preferredSize) {
        onResize(preferredSize);
        this.range = INITIAL_RANGE;
    }

    @Override
    /**
     * Renders one frame of the animation into the given graphics buffer.
     * @param graphics the graphics
     * @param size the size
     */
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        lastSize = size;
        if (size.columns() <= 0 || size.rows() <= 0) {
            return;
        }

        // Terminal cells are roughly 2:1 (height:width), so to fill the
        // full screen without visual distortion we stretch the Y range
        // by the cell aspect ratio.
        int cols = size.columns();
        int rows = size.rows();
        double aspect = 2.0; // terminal cell height / width

        double halfRangeX = range / 2.0;
        double halfRangeY = halfRangeX * rows * aspect / cols;

        double minX = CENTER_X - halfRangeX;
        double maxX = CENTER_X + halfRangeX;
        double minY = CENTER_Y - halfRangeY;
        double maxY = CENTER_Y + halfRangeY;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                double cx = map(x, 0, cols - 1, minX, maxX);
                double cy = map(y, 0, rows - 1, minY, maxY);

                int iterations = mandelbrotIterations(cx, cy, MAX_ITERATIONS);
                graphics.setCell(x, y, cellFor(iterations));
            }
        }

        range *= ZOOM_FACTOR;
        if (range < MIN_RANGE) {
            range = INITIAL_RANGE;
        }
    }

    private static int mandelbrotIterations(double cr, double ci, int maxIterations) {
        double zr = 0.0;
        double zi = 0.0;
        for (int i = 0; i < maxIterations; i++) {
            double zr2 = zr * zr;
            double zi2 = zi * zi;
            if (zr2 + zi2 > 4.0) {
                return i;
            }
            zi = 2.0 * zr * zi + ci;
            zr = zr2 - zi2 + cr;
        }
        return maxIterations;
    }

    private static TextCell cellFor(int iterations) {
        // Inside the set (hit max iterations) uses the brightest glyph/color.
        int level = Math.min(GLYPHS.length - 1, iterations / (MAX_ITERATIONS / GLYPHS.length + 1));
        if (iterations == MAX_ITERATIONS) {
            level = GLYPHS.length - 1;
        }

        char ch = GLYPHS[level];
        Color fg = switch (level) {
            case 0 -> AnsiColor.BLACK;
            case 1 -> AnsiColor.BLUE;
            case 2 -> AnsiColor.CYAN;
            case 3 -> AnsiColor.GREEN;
            case 4 -> AnsiColor.YELLOW;
            case 5 -> AnsiColor.BRIGHT_YELLOW;
            case 6 -> AnsiColor.BRIGHT_RED;
            default -> AnsiColor.BRIGHT_WHITE;
        };

        SGR sgr = (level >= GLYPHS.length / 2) ? SGR.BOLD : SGR.DIM;
        return new TextCell(ch, fg, AnsiColor.BLACK, sgr);
    }

    private static double map(int value, int srcMin, int srcMax, double dstMin, double dstMax) {
        if (srcMax == srcMin) {
            return (dstMin + dstMax) / 2.0;
        }
        double t = (value - srcMin) / (double) (srcMax - srcMin);
        return dstMin + t * (dstMax - dstMin);
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

    /** Visible for tests: set viewport range directly. */
    public void setRange(double range) {
        this.range = range;
    }

    /** Visible for tests: current viewport range in the complex plane. */
    public double getRange() {
        return range;
    }
}
