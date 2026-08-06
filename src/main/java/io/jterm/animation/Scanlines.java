package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.Random;

/**
 * Animated CRT scanline background.
 *
 * <p>Draws alternating dim/bright horizontal rows to mimic the dark gaps between
 * CRT scanlines, plus a slowly-sweeping bright line that travels down the screen.
 * A small random chance per frame creates a brief brightness flicker, like an
 * aging monitor losing sync.</p>
 *
 * <p>This is an {@link AnimatedBackground} only, not a {@link io.jterm.widget.Component},
 * so it can render full-frame behind a login screen without widget lifecycle overhead.</p>
 */
public class Scanlines implements AnimatedBackground {

    private static final int TARGET_FPS = 12;

    // Period of the moving bright scanline (rows).
    private static final int SWEEP_PERIOD = 8;

    // Chance per frame of a brief brightness flicker.
    private static final double FLICKER_CHANCE = 0.04;

    private final Random random = new Random();

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private volatile int frame;

    /**
     * Constructs a new Scanlines instance.
     * @param preferredSize the preferred size
     */
    public Scanlines(TerminalSize preferredSize) {
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

        int rows = size.rows();
        int cols = size.columns();

        // Occasional whole-frame brightness shift for the flicker effect.
        boolean flicker = random.nextDouble() < FLICKER_CHANCE;

        int sweepRow = (frame / 2) % Math.max(1, rows);

        for (int y = 0; y < rows; y++) {
            boolean isEvenRow = (y % 2) == 0;
            Color rowColor = rowColor(y, sweepRow, flicker, isEvenRow);
            TextCell cell = new TextCell(' ', AnsiColor.BLACK, rowColor,
                    flicker ? SGR.BOLD : SGR.DIM);

            for (int x = 0; x < cols; x++) {
                graphics.setCell(x, y, cell);
            }
        }
    }

    private Color rowColor(int y, int sweepRow, boolean flicker, boolean isEvenRow) {
        Color base = isEvenRow ? AnsiColor.BLACK : AnsiColor.BRIGHT_BLACK;

        // The bright CRT line is one row wide and dims above/below by one row.
        int distance = Math.abs(y - sweepRow);
        if (distance == 0) {
            base = flicker ? AnsiColor.WHITE : AnsiColor.BRIGHT_WHITE;
        } else if (distance == 1) {
            base = AnsiColor.WHITE;
        }

        // Flicker brightens every row for one frame.
        if (flicker && distance > 1) {
            base = (base == AnsiColor.BLACK) ? AnsiColor.BRIGHT_BLACK : AnsiColor.WHITE;
        }

        return base;
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

    /** Visible for tests: current frame counter. */
    public int getFrame() {
        return frame;
    }
}
