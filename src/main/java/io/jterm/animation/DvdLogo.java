package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Animated bouncing DVD logo background.
 *
 * <p>A small "DVD" text block bounces diagonally around the terminal. On
 * corner hits it changes to a new ANSI color, and a fading trail follows the
 * previous positions.</p>
 */
public class DvdLogo implements AnimatedBackground {

    private static final int TARGET_FPS = 15;
    private static final String LOGO = "DVD";
    private static final int LOGO_WIDTH = LOGO.length();
    private static final int LOGO_HEIGHT = 1;

    private static final AnsiColor[] PALETTE = {
            AnsiColor.WHITE,
            AnsiColor.YELLOW,
            AnsiColor.GREEN,
            AnsiColor.CYAN,
            AnsiColor.BLUE,
            AnsiColor.MAGENTA,
            AnsiColor.RED,
            AnsiColor.BRIGHT_YELLOW,
            AnsiColor.BRIGHT_GREEN,
            AnsiColor.BRIGHT_CYAN,
            AnsiColor.BRIGHT_BLUE,
            AnsiColor.BRIGHT_MAGENTA,
            AnsiColor.BRIGHT_RED,
    };

    // Maximum trail length; older positions fade further.
    private static final int MAX_TRAIL = 20;

    private volatile boolean running;
    private volatile TerminalSize lastSize;

    private int x;
    private int y;
    private int dx = 1;
    private int dy = 1;
    private int colorIndex;

    private final Deque<int[]> trail = new ArrayDeque<>();

    public DvdLogo(TerminalSize preferredSize) {
        onResize(preferredSize);
        this.x = 1;
        this.y = 1;
        this.colorIndex = 0;
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        lastSize = size;
        if (size.columns() <= 0 || size.rows() <= 0) {
            return;
        }

        int cols = size.columns();
        int rows = size.rows();

        // If the terminal is too small to fit the logo at all, render what we can.
        int maxX = Math.max(0, cols - LOGO_WIDTH);
        int maxY = Math.max(0, rows - LOGO_HEIGHT);

        // Determine if the next move would hit a wall or corner before moving.
        int nextX = x + dx;
        int nextY = y + dy;
        boolean hitWallX = nextX > maxX || nextX < 0;
        boolean hitWallY = nextY > maxY || nextY < 0;
        boolean hitCorner = hitWallX && hitWallY;

        // Bounce before moving so the current position stays valid.
        if (hitWallX) {
            dx = -dx;
        }
        if (hitWallY) {
            dy = -dy;
        }

        if (hitCorner) {
            colorIndex = (colorIndex + 1) % PALETTE.length;
        }

        // Move the logo for the next frame.
        x += dx;
        y += dy;

        // Clamp after move (defensive for extremely small sizes).
        x = Math.max(0, Math.min(x, maxX));
        y = Math.max(0, Math.min(y, maxY));

        // Fill background with black.
        var black = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, cols, rows, black);

        // Record current position in the trail.
        trail.addFirst(new int[]{x, y});
        while (trail.size() > MAX_TRAIL) {
            trail.removeLast();
        }

        // Draw the trail, older positions fading.
        int i = 0;
        for (int[] pos : trail) {
            int tx = pos[0];
            int ty = pos[1];
            double age = (double) i / (trail.size());
            var trailColor = pickTrailColor(age);
            drawLogo(graphics, tx, ty, trailColor, SGR.DIM);
            i++;
        }

        // Draw the bright logo head on top.
        drawLogo(graphics, x, y, PALETTE[colorIndex], SGR.BOLD);
    }

    private void drawLogo(TextGraphics graphics, int x, int y, AnsiColor fg, SGR sgr) {
        for (int i = 0; i < LOGO_WIDTH; i++) {
            int cx = x + i;
            if (cx < 0) continue;
            char ch = LOGO.charAt(i);
            graphics.setCell(cx, y, new TextCell(ch, fg, AnsiColor.BLACK, sgr));
        }
    }

    private AnsiColor pickTrailColor(double age) {
        AnsiColor current = PALETTE[colorIndex];
        if (age >= 0.8) {
            return AnsiColor.BRIGHT_BLACK;
        } else if (age >= 0.5) {
            return AnsiColor.blendAnsi(AnsiColor.BRIGHT_BLACK, current, 0.4);
        } else if (age >= 0.2) {
            return AnsiColor.blendAnsi(AnsiColor.BRIGHT_BLACK, current, 0.7);
        } else {
            return current;
        }
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

    /** Visible for tests: current logo position. */
    public int[] getPosition() {
        return new int[]{x, y};
    }

    /** Visible for tests: current velocity direction. */
    public int[] getDirection() {
        return new int[]{dx, dy};
    }

    /** Visible for tests: current logo color. */
    public AnsiColor getColor() {
        return PALETTE[colorIndex];
    }

    /** Visible for tests: set the logo position directly. */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Visible for tests: set the velocity direction directly. */
    public void setDirection(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }
}
