package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

/**
 * Animated DNA double helix background. Two intertwined sine-wave strands
 * rotate around each other vertically, with connecting rungs between them.
 */
public class DNAHelix implements AnimatedBackground {

    private static final int TARGET_FPS = 10;
    private static final char STRAND_GLYPH = 'o';
    private static final char RUNG_GLYPH = '-';

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private volatile double time;

    /**
     * Constructs a new DNAHelix instance.
     * @param preferredSize the preferred size
     */
    public DNAHelix(TerminalSize preferredSize) {
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

        renderAtTime(graphics, size, time);
        time += 0.08;
    }

    /** Visible for tests to avoid wall-clock time. */
    public void renderAtTime(TextGraphics graphics, TerminalSize size, double timeSeconds) {
        if (size.columns() <= 0 || size.rows() <= 0) return;

        // Black background.
        TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bg);

        int cols = size.columns();
        int rows = size.rows();
        int centerX = cols / 2;
        double amplitude = Math.max(2.0, cols * 0.15);
        double frequency = Math.PI * 2.0 / Math.max(1, rows * 0.5);
        double speed = 0.5;

        for (int y = 0; y < rows; y++) {
            // Strand 1: phase = 0
            double phase1 = y * frequency + timeSeconds * speed;
            int x1 = (int) Math.round(centerX + amplitude * Math.sin(phase1));
            // Strand 2: phase = PI (opposite)
            double phase2 = y * frequency + timeSeconds * speed + Math.PI;
            int x2 = (int) Math.round(centerX + amplitude * Math.sin(phase2));

            // Clamp to bounds.
            x1 = Math.max(0, Math.min(cols - 1, x1));
            x2 = Math.max(0, Math.min(cols - 1, x2));

            // Determine depth: if sin component is negative, strand is "behind" → dim.
            double depth1 = Math.sin(phase1);
            double depth2 = Math.sin(phase2);

            // Draw strand 1.
            AnsiColor color1 = depth1 >= 0 ? AnsiColor.BRIGHT_GREEN : AnsiColor.GREEN;
            TextCell cell1 = depth1 >= 0
                    ? new TextCell(STRAND_GLYPH, color1, AnsiColor.BLACK, SGR.BOLD)
                    : new TextCell(STRAND_GLYPH, color1, AnsiColor.BLACK);
            graphics.setCell(x1, y, cell1);

            // Draw strand 2.
            AnsiColor color2 = depth2 >= 0 ? AnsiColor.BRIGHT_CYAN : AnsiColor.CYAN;
            TextCell cell2 = depth2 >= 0
                    ? new TextCell(STRAND_GLYPH, color2, AnsiColor.BLACK, SGR.BOLD)
                    : new TextCell(STRAND_GLYPH, color2, AnsiColor.BLACK);
            graphics.setCell(x2, y, cell2);

            // Draw connecting rung every 3rd row.
            if (y % 3 == 0 && x1 != x2) {
                int lo = Math.min(x1, x2);
                int hi = Math.max(x1, x2);
                TextCell rung = new TextCell(RUNG_GLYPH, AnsiColor.BRIGHT_BLACK, AnsiColor.BLACK);
                for (int x = lo + 1; x < hi; x++) {
                    graphics.setCell(x, y, rung);
                }
            }
        }
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

    /** Visible for tests. */
    public double getTime() {
        return time;
    }
}
