package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.Random;

/**
 * Animated terrain flyover background. Procedurally generated mountain
 * silhouettes scroll sideways in layered parallax, like flying over a landscape
 * at night. Stars twinkle in the sky above.
 *
 * <p>This is an {@link AnimatedBackground} only, not a component, matching the
 * style used by {@link OceanWaves}.</p>
 */
public class TerrainFlyover implements AnimatedBackground {

    private static final int TARGET_FPS = 10;

    // CP437-safe glyphs.
    private static final char BACK_GLYPH = '\u00B0';   // 176
    private static final char MID_GLYPH = '\u00B1';    // 177
    private static final char FRONT_GLYPH = '\u00DB'; // 219
    private static final char STAR_DIM = '.';
    private static final char STAR_BRIGHT = '*';

    private static final int STAR_COUNT = 30;

    private final Random random = new Random();

    private final Layer back;
    private final Layer mid;
    private final Layer front;

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private volatile double time;

    private Star[] stars;
    private TerminalSize currentSize;

    /**
     * Constructs a new TerrainFlyover instance.
     * @param preferredSize the preferred size
     */
    public TerrainFlyover(TerminalSize preferredSize) {
        this.back = new Layer(0.22, 0.12, 0.08, AnsiColor.BRIGHT_BLACK, BACK_GLYPH);
        this.mid = new Layer(0.42, 0.18, 0.18, AnsiColor.GREEN, MID_GLYPH);
        this.front = new Layer(0.62, 0.26, 0.28, AnsiColor.BRIGHT_GREEN, FRONT_GLYPH);
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
        time += 0.05;
    }

    /** Visible for tests to avoid wall-clock time. */
    public void renderAtTime(TextGraphics graphics, TerminalSize size, double timeSeconds) {
        if (size.columns() <= 0 || size.rows() <= 0) return;

        // Clear sky.
        TextCell sky = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), sky);

        ensureStars(size);

        // Draw back-to-front so nearer mountains overwrite farther ones.
        renderLayer(graphics, size, back, timeSeconds);
        renderLayer(graphics, size, mid, timeSeconds);
        renderLayer(graphics, size, front, timeSeconds);

        // Draw stars last, on top of back mountains but behind nothing.
        renderStars(graphics, size, timeSeconds);
    }

    private void renderLayer(TextGraphics graphics, TerminalSize size, Layer layer, double t) {
        int rows = size.rows();
        int cols = size.columns();

        int[] heights = new int[cols];
        for (int x = 0; x < cols; x++) {
            double scrollX = x + t * layer.speed * 20.0;
            double y = layer.baseFraction * rows
                    + layer.amplitudeFraction * rows * noise(scrollX / Math.max(1.0, cols / 6.0));
            int h = rows - 1 - (int) Math.round(y);
            heights[x] = Math.max(0, Math.min(rows - 1, h));
        }

        TextCell body = new TextCell(layer.glyph, layer.color, AnsiColor.BLACK);
        TextCell peak = new TextCell(layer.glyph, layer.color, AnsiColor.BLACK, SGR.BOLD);

        for (int x = 0; x < cols; x++) {
            int y = heights[x];
            // Mark local maxima as bold peaks.
            boolean isPeak = x > 0 && x < cols - 1
                    && heights[x - 1] > heights[x] && heights[x + 1] > heights[x];
            for (int r = y; r < rows; r++) {
                graphics.setCell(x, r, isPeak && r == y ? peak : body);
            }
        }
    }

    /** Sum-of-sines noise in the range [-1, 1]. */
    private static double noise(double x) {
        return 0.50 * Math.sin(x * 1.0)
                + 0.30 * Math.sin(x * 2.3)
                + 0.20 * Math.sin(x * 4.7);
    }

    private void ensureStars(TerminalSize size) {
        if (stars != null && currentSize != null
                && currentSize.columns() == size.columns()
                && currentSize.rows() == size.rows()) {
            return;
        }
        this.currentSize = size;
        stars = new Star[STAR_COUNT];
        for (int i = 0; i < STAR_COUNT; i++) {
            stars[i] = new Star(
                    random.nextInt(size.columns()),
                    random.nextInt(Math.max(1, size.rows() / 2)),
                    0.5 + random.nextDouble() * 1.5,
                    0.5 + random.nextDouble() * 2.0,
                    0.5 + random.nextDouble() * 1.5,
                    random.nextDouble() * Math.PI * 2
            );
        }
    }

    private void renderStars(TextGraphics graphics, TerminalSize size, double t) {
        if (stars == null) return;

        for (Star star : stars) {
            if (star.x < 0 || star.x >= size.columns() || star.y < 0 || star.y >= size.rows()) continue;

            double raw = star.baseBrightness + star.amplitude * Math.sin(t * star.speed + star.phase);
            int brightness = Math.max(0, Math.min(3, (int) Math.round(raw)));

            if (brightness <= 0) continue;

            TextCell cell;
            if (brightness <= 1) {
                cell = new TextCell(STAR_DIM, AnsiColor.BRIGHT_BLACK, AnsiColor.BLACK);
            } else if (brightness == 2) {
                cell = new TextCell(STAR_BRIGHT, AnsiColor.WHITE, AnsiColor.BLACK);
            } else {
                cell = new TextCell(STAR_BRIGHT, AnsiColor.BRIGHT_WHITE, AnsiColor.BLACK, SGR.BOLD);
            }
            graphics.setCell(star.x, star.y, cell);
        }
    }

    @Override
    /**
     * Reallocates internal buffers for the new terminal size.
     * @param newSize the new size
     */
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        // Force star regeneration on next render.
        this.stars = null;
        this.currentSize = null;
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

    /** Visible for tests. */
    public Layer[] getLayers() {
        return new Layer[]{back, mid, front};
    }

    /** Visible for tests. */
    public Star[] getStars() {
        return stars;
    }

    /**
     * Returns the star count.
     * @return the result
     */
    public int getStarCount() {
        return stars == null ? 0 : stars.length;
    }

    /** Layer parameters. */
    public static final class Layer {
        private final double baseFraction;
        private final double amplitudeFraction;
        private final double speed;
        private final AnsiColor color;
        private final char glyph;

        Layer(double baseFraction, double amplitudeFraction, double speed,
              AnsiColor color, char glyph) {
            this.baseFraction = baseFraction;
            this.amplitudeFraction = amplitudeFraction;
            this.speed = speed;
            this.color = color;
            this.glyph = glyph;
        }

        /** Returns the base height fraction.
 * @return the base fraction */

        public double baseFraction() { return baseFraction; }
        public double amplitudeFraction() { return amplitudeFraction; }
        /** Returns the wave speed.
 * @return the speed */
        public double speed() { return speed; }
        public AnsiColor color() { return color; }
        public char glyph() { return glyph; }
    }

    private static final class Star {
        final int x;
        final int y;
        final double baseBrightness;
        final double amplitude;
        final double speed;
        final double phase;

        Star(int x, int y, double baseBrightness, double amplitude, double speed, double phase) {
            this.x = x;
            this.y = y;
            this.baseBrightness = baseBrightness;
            this.amplitude = amplitude;
            this.speed = speed;
            this.phase = phase;
        }
    }
}
