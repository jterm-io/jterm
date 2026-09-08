package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated lightning storm background. A dark sky is periodically torn open by a
 * jagged lightning bolt followed by a brief bright flash. Between strikes the
 * screen is black with a silhouetted landscape at the bottom and an occasional
 * faint rumble (subtle brightness shift across the sky).
 */
public class LightningStorm implements AnimatedBackground {

    private static final int TARGET_FPS = 8;
    private static final int MIN_STRIKE_INTERVAL = 10;
    private static final int MAX_STRIKE_INTERVAL = 50;
    private static final int FLASH_DURATION = 2;

    private final Random random = new Random();

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private volatile double time;

    private int framesUntilNextStrike;
    private int flashFrames;
    private int rumblePhase;
    private List<int[]> currentBolt;

    /**
     * Constructs a new LightningStorm instance.
     * @param preferredSize the preferred size
     */
    public LightningStorm(TerminalSize preferredSize) {
        onResize(preferredSize);
        scheduleNextStrike();
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

        renderAtTime(graphics, size, time, false);
        time += 0.12;
    }

    /**
     * Renders one frame of the animation at the given time.
     *
     * @param graphics the graphics to render into
     * @param size     the terminal size to render at
     * @param t        the animation time
     */
    public void renderAtTime(TextGraphics graphics, TerminalSize size, double t) {
        renderAtTime(graphics, size, t, true);
    }

    private void renderAtTime(TextGraphics graphics, TerminalSize size, double t, boolean testMode) {
        if (size.columns() <= 0 || size.rows() <= 0) {
            return;
        }

        int cols = size.columns();
        int rows = size.rows();

        // Sky background: black by default, with a very faint rumble gradient.
        int rumble = rumblePhase % 60;
        double rumbleStrength = rumble < 10 ? 0.05 + 0.05 * Math.sin(rumble * 0.6) : 0.0;
        AnsiColor skyBg = blendBlack(rumbleStrength);
        TextCell bg = new TextCell(' ', AnsiColor.BLACK, skyBg);
        graphics.fillRectangle(0, 0, cols, rows, bg);

        // Advance rumble phase.
        rumblePhase++;

        // Lightning strike logic (only in real-time mode).
        boolean flashing = false;
        if (!testMode) {
            if (flashFrames > 0) {
                flashFrames--;
                if (flashFrames <= 0) {
                    currentBolt = generateBolt(cols, rows);
                }
                flashing = true;
            } else if (framesUntilNextStrike <= 0) {
                currentBolt = generateBolt(cols, rows);
                flashFrames = FLASH_DURATION;
                framesUntilNextStrike = MIN_STRIKE_INTERVAL
                        + random.nextInt(MAX_STRIKE_INTERVAL - MIN_STRIKE_INTERVAL + 1);
                flashing = true;
            } else {
                framesUntilNextStrike--;
                if (currentBolt == null || random.nextDouble() < 0.05) {
                    currentBolt = null;
                }
            }
        }

        // Flash: brief full-screen bright yellow/white illumination.
        if (flashing) {
            TextCell flash = new TextCell(' ', AnsiColor.BRIGHT_WHITE, AnsiColor.BRIGHT_YELLOW);
            graphics.fillRectangle(0, 0, cols, rows, flash);
        }

        // Draw current lightning bolt on top of flash.
        if (currentBolt != null && !flashing) {
            TextCell bolt = new TextCell('\\', AnsiColor.BRIGHT_WHITE, AnsiColor.BLACK);
            for (int[] p : currentBolt) {
                int x = p[0];
                int y = p[1];
                if (x >= 0 && x < cols && y >= 0 && y < rows) {
                    graphics.setCell(x, y, bolt.withCharacter(boltChar(p)));
                }
            }
        }

        // Draw silhouetted landscape at the bottom.
        renderLandscape(graphics, cols, rows, flashing);
    }

    private void renderLandscape(TextGraphics graphics, int cols, int rows, boolean flashing) {
        if (rows < 3 || cols < 2) {
            return;
        }
        int horizon = rows - 2;

        AnsiColor fg = flashing ? AnsiColor.BRIGHT_BLACK : AnsiColor.BLACK;
        AnsiColor bg = flashing ? AnsiColor.BRIGHT_YELLOW : AnsiColor.BLACK;

        // Jagged mountain silhouette.
        int[] heights = new int[cols];
        int h = horizon;
        for (int x = 0; x < cols; x++) {
            h += random.nextInt(3) - 1;
            h = Math.max(horizon - 3, Math.min(horizon + 1, h));
            heights[x] = h;
        }

        for (int x = 0; x < cols; x++) {
            int colHeight = heights[x];
            for (int y = colHeight; y < rows; y++) {
                char glyph = landscapeChar(x, y, colHeight, rows, cols);
                graphics.setCell(x, y, new TextCell(glyph, fg, bg));
            }
        }
    }

    private char landscapeChar(int x, int y, int horizonY, int rows, int cols) {
        // Bottom edge uses box-drawing horizontal line.
        if (y == rows - 1) {
            return '─';
        }
        // Occasional tree spikes.
        if (y == horizonY && x % 7 == 3 && x < cols - 1) {
            return '▲';
        }
        return '█';
    }

    private char boltChar(int[] p) {
        // Choose zig-zag slash based on local direction. The bolt points travel
        // downward, so prefer forward slashes when moving right and backslashes
        // when moving left.
        if (p.length > 2) {
            return p[2] > 0 ? '\\' : '/';
        }
        return random.nextBoolean() ? '\\' : '/';
    }

    private List<int[]> generateBolt(int cols, int rows) {
        List<int[]> path = new ArrayList<>();
        int startX = Math.max(1, Math.min(cols - 2, random.nextInt(cols)));
        int endX = Math.max(1, Math.min(cols - 2, startX + random.nextInt(21) - 10));
        int endY = rows / 2 + random.nextInt(Math.max(1, rows / 3));

        int x = startX;
        int y = 0;
        path.add(new int[]{x, y, 0});

        while (y < endY) {
            y++;
            // Jitter x as we descend.
            if (random.nextBoolean()) {
                x += random.nextInt(3) - 1;
            }
            x = Math.max(1, Math.min(cols - 2, x));

            int direction = Integer.compare(endX, x);
            path.add(new int[]{x, y, direction});
        }
        return path;
    }

    private AnsiColor blendBlack(double weight) {
        if (weight <= 0.0) {
            return AnsiColor.BLACK;
        }
        // Blend black toward dark blue for a faint rumble glow.
        int r = (int) (weight * 25);
        int g = (int) (weight * 25);
        int b = (int) (weight * 85);
        return AnsiColor.nearest(r, g, b);
    }

    @Override
    /**
     * Reallocates internal buffers for the new terminal size.
     * @param newSize the new size
     */
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        currentBolt = null;
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
     * Returns the current animation time.
     *
     * @return the animation time
     */
    public double getTime() {
        return time;
    }

    /**
     * Returns the number of remaining flash frames.
     *
     * @return the remaining flash frame count
     */
    public int getFlashFrames() {
        return flashFrames;
    }

    /**
     * Returns the number of frames until the next lightning strike.
     *
     * @return the frame count until the next strike
     */
    public int getFramesUntilNextStrike() {
        return framesUntilNextStrike;
    }

    /**
     * Returns the currently displayed lightning bolt.
     *
     * @return the bolt as a list of {x, y} points, or {@code null} if none
     */
    public List<int[]> getCurrentBolt() {
        return currentBolt;
    }

    private void scheduleNextStrike() {
        framesUntilNextStrike = MIN_STRIKE_INTERVAL
                + random.nextInt(MAX_STRIKE_INTERVAL - MIN_STRIKE_INTERVAL + 1);
    }
}
