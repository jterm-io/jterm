package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

import java.util.Random;

/**
 * Animated snowfall background. Each snowflake drifts downward with a gentle
 * horizontal sway and occasional wind gusts. Flakes wrap from bottom to top
 * so the storm feels continuous.
 */
public class Snowfall implements AnimatedBackground {

    private static final int TARGET_FPS = 12;
    private static final char[] GLYPHS = {'*', '.', ',', ';'};
    private static final double GUST_CHANCE = 0.01;
    private static final int GUST_MIN_FRAMES = 5;
    private static final int GUST_MAX_FRAMES = 20;
    private static final double GUST_STRENGTH = 0.8;

    private final Random random = new Random();

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private volatile double time;

    private Snowflake[] flakes;
    private double windOffset;
    private int gustFrames;
    private double gustVelocity;

    /**
     * Constructs a new Snowfall instance.
     * @param preferredSize the preferred size
     */
    public Snowfall(TerminalSize preferredSize) {
        onResize(preferredSize);
        ensureFlakes(preferredSize);
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

        ensureFlakes(size);
        updateWind();

        // Black background.
        TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bg);

        // Apply persistent wind drift plus any active gust.
        windOffset += 0.02 + gustVelocity;

        for (Snowflake flake : flakes) {
            if (flake == null) continue;
            flake.advance(size, windOffset);
            int x = flake.screenX();
            int y = flake.screenY();
            if (x < 0 || x >= size.columns() || y < 0 || y >= size.rows()) continue;

            TextCell cell = new TextCell(flake.glyph(), flake.color(), AnsiColor.BLACK);
            graphics.setCell(x, y, cell);
        }

        time += 0.05;
    }

    private void updateWind() {
        if (gustFrames > 0) {
            gustFrames--;
        } else if (random.nextDouble() < GUST_CHANCE) {
            gustFrames = GUST_MIN_FRAMES + random.nextInt(GUST_MAX_FRAMES - GUST_MIN_FRAMES + 1);
            gustVelocity = (random.nextBoolean() ? 1 : -1) * GUST_STRENGTH * (0.5 + random.nextDouble());
        } else {
            gustVelocity = gustVelocity * 0.9; // decay
        }
    }

    private void ensureFlakes(TerminalSize size) {
        int targetCount = flakeCount(size);
        if (flakes != null && flakes.length == targetCount) return;
        flakes = new Snowflake[targetCount];
        for (int i = 0; i < targetCount; i++) {
            flakes[i] = new Snowflake(size, random);
        }
    }

    private int flakeCount(TerminalSize size) {
        int area = size.columns() * size.rows();
        if (area <= 0) return 0;
        // Scale roughly with terminal area, clamped to the requested range.
        int count = Math.max(50, (int) (area * 0.05));
        return Math.min(100, count);
    }

    @Override
    /**
     * Reallocates internal buffers for the new terminal size.
     * @param newSize the new size
     */
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        flakes = null;
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
     * Returns the accumulated animation time, visible for tests.
     *
     * @return the current animation time in seconds
     */
    public double getTime() {
        return time;
    }

    /**
     * Returns the live flake array, visible for tests.
     *
     * @return the current snowflakes (may be {@code null} before sizing)
     */
    public Snowflake[] getFlakes() {
        return flakes;
    }

    /**
     * Returns the number of active snowflakes, visible for tests.
     *
     * @return the flake count (0 before sizing)
     */
    public int getFlakeCount() {
        return flakes == null ? 0 : flakes.length;
    }

    /**
     * Returns the current horizontal wind offset, visible for tests.
     *
     * @return the wind offset
     */
    public double getWindOffset() {
        return windOffset;
    }

    /** Snowflake state. */
    public static final class Snowflake {
        private final TerminalSize size;
        private final Random random;
        private double x;
        private double y;
        private double speed;
        private double driftPhase;
        private double driftAmplitude;
        private char glyph;
        private AnsiColor color;

        Snowflake(TerminalSize size, Random random) {
            this.size = size;
            this.random = random;
            resetPosition();
            this.speed = 0.2 + random.nextDouble() * 0.6;
            this.driftPhase = random.nextDouble() * Math.PI * 2;
            this.driftAmplitude = 0.3 + random.nextDouble() * 1.2;
            this.glyph = GLYPHS[random.nextInt(GLYPHS.length)];
            this.color = random.nextBoolean() ? AnsiColor.WHITE : AnsiColor.BRIGHT_WHITE;
        }

        void resetPosition() {
            this.x = random.nextDouble() * Math.max(1, size.columns());
            this.y = random.nextDouble() * Math.max(1, size.rows());
        }

        void advance(TerminalSize size, double windOffset) {
            y += speed;
            x += Math.sin(driftPhase) * driftAmplitude + windOffset * 0.1;
            driftPhase += 0.05;

            // Wrap bottom to top.
            if (y >= size.rows()) {
                y = 0;
                resetPosition();
            }
            // Wrap horizontally.
            if (x < 0) {
                x += size.columns();
            } else if (x >= size.columns()) {
                x -= size.columns();
            }
        }

        int screenX() {
            return (int) Math.floor(x);
        }

        int screenY() {
            return (int) Math.floor(y);
        }

        char glyph() {
            return glyph;
        }

        AnsiColor color() {
            return color;
        }

        double speed() {
            return speed;
        }

        double y() {
            return y;
        }
    }
}
