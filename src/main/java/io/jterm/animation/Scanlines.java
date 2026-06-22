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

    public Scanlines(TerminalSize preferredSize) {
        onResize(preferredSize);
    }

    @Override
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
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
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

    /** Visible for tests: current frame counter. */
    public int getFrame() {
        return frame;
    }
}
