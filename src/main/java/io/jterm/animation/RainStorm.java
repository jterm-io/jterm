package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.Random;

/**
 * Animated rain storm background. Rain characters fall diagonally with a
 * slight wind drift. Occasional lightning flashes briefly illuminate the
 * full screen with a white/cyan tint.
 */
public class RainStorm implements AnimatedBackground {

    private static final int TARGET_FPS = 10;
    private static final int DROP_COUNT = 100;

    private final Random random = new Random();

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private volatile double time;

    private Drop[] drops;
    private int flashFrames;        // remaining frames of lightning flash
    private int flashCooldown;      // frames until next flash attempt

    public RainStorm(TerminalSize preferredSize) {
        onResize(preferredSize);
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        lastSize = size;
        if (size.columns() <= 0 || size.rows() <= 0) return;

        renderAtTime(graphics, size, time, false);
        time += 0.1;
    }

    /** Visible for tests to avoid wall-clock time. */
    public void renderAtTime(TextGraphics graphics, TerminalSize size, double t) {
        renderAtTime(graphics, size, t, true);
    }

    private void renderAtTime(TextGraphics graphics, TerminalSize size, double t, boolean testMode) {
        if (size.columns() <= 0 || size.rows() <= 0) return;

        ensureDrops(size);

        // Black background.
        TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bg);

        // Lightning flash check (only in renderFrame mode, not test mode).
        boolean flashing = false;
        if (!testMode) {
            if (flashFrames > 0) {
                flashFrames--;
                flashing = true;
            } else if (flashCooldown <= 0 && random.nextDouble() < 0.02) {
                flashFrames = 2 + random.nextInt(2); // 2-3 frames
                flashing = true;
            } else {
                flashCooldown--;
            }
        }

        if (flashing) {
            // Fill screen with bright white/cyan background.
            TextCell flash = new TextCell(' ', AnsiColor.BRIGHT_WHITE, AnsiColor.BRIGHT_CYAN);
            graphics.fillRectangle(0, 0, size.columns(), size.rows(), flash);
        }

        // Draw rain drops.
        for (Drop drop : drops) {
            if (drop == null) continue;
            drop.advance();
            int x = drop.screenX();
            int y = drop.screenY();
            if (x < 0 || x >= size.columns() || y < 0 || y >= size.rows()) continue;

            char glyph = drop.glyph();
            AnsiColor fg = flashing ? AnsiColor.BRIGHT_WHITE : AnsiColor.CYAN;
            TextCell cell = new TextCell(glyph, fg, AnsiColor.BLACK);
            graphics.setCell(x, y, cell);
        }
    }

    private void ensureDrops(TerminalSize size) {
        if (drops != null && drops.length == DROP_COUNT) return;
        drops = new Drop[DROP_COUNT];
        for (int i = 0; i < DROP_COUNT; i++) {
            drops[i] = new Drop(size, random);
        }
    }

    @Override
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        if (newSize.columns() <= 0 || newSize.rows() <= 0) return;
        drops = new Drop[DROP_COUNT];
        for (int i = 0; i < DROP_COUNT; i++) {
            drops[i] = new Drop(newSize, random);
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
    public Drop[] getDrops() {
        return drops;
    }

    /** Visible for tests. */
    public int getDropCount() {
        return drops == null ? 0 : drops.length;
    }

    /** Rain drop state. */
    public static final class Drop {
        private final int cols;
        private final int rows;
        private int x;
        private int y;
        private int speed;     // 1-3
        private int driftCounter;
        private char glyph;

        Drop(TerminalSize size, Random random) {
            this.cols = size.columns();
            this.rows = size.rows();
            this.x = random.nextInt(Math.max(1, cols));
            this.y = random.nextInt(Math.max(1, rows));
            this.speed = 1 + random.nextInt(3);
            this.driftCounter = random.nextInt(3);
            char[] glyphs = {'|', '/', '.'};
            this.glyph = glyphs[random.nextInt(glyphs.length)];
        }

        void advance() {
            y += speed;
            driftCounter++;
            if (driftCounter >= 3) {
                x--;
                driftCounter = 0;
            }
            // Respawn at top when reaching bottom.
            if (y >= rows) {
                y = 0;
                x = cols - 1;
            }
            // Respawn at right when drifting off left.
            if (x < 0) {
                x = cols - 1;
                y = 0;
            }
        }

        int screenX() {
            return x;
        }

        int screenY() {
            return y;
        }

        char glyph() {
            return glyph;
        }

        int speed() {
            return speed;
        }
    }
}
