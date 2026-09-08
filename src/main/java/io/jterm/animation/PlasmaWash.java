package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.widget.AbstractComponent;

/**
 * A full-screen animated plasma color wash using classic sine-wave interference.
 * Renders with character-density shading so the effect stays readable behind
 * foreground BBS text.
 */
public class PlasmaWash extends AbstractComponent implements AnimatedBackground {
    private static final int MIN_FPS = 1;
    private static final int MAX_FPS = 60;
    private static final long NS_PER_MS = 1_000_000L;

    // Plasma wave frequencies from the task spec.
    private static final double X_FREQ = 0.10;
    private static final double Y_FREQ = 0.15;
    private static final double DIAG_FREQ = 0.08;
    private static final double RADIAL_FREQ = 0.10;

    private static final double TIME_X = 1.0;
    private static final double TIME_Y = 1.3;
    private static final double TIME_DIAG = 0.7;
    private static final double TIME_RADIAL = 1.1;

    private static final int SHADING_LEVELS = 6;
    private static final char[] SHADING_CHARS = {' ', '.', '\u2591', '\u2592', '\u2593', '\u2588'};

    private TerminalSize preferredSize;
    private volatile int targetFps = 15;
    private volatile boolean paused = false;
    private volatile boolean running = false;

    private volatile long lastTickNanos = -1;
    private volatile long accumulatedNs;
    private volatile long frameElapsedMs;
    private volatile int frame;
    private volatile double time;

    private volatile Color baseColor = AnsiColor.BLUE;

    private Renderer renderer;

    /**
     * Functional interface for custom animation renderers.
     *
     * <p>The single abstract method {@link #render} receives the graphics
     * target and the animation time in seconds.
     */
    @FunctionalInterface
    public interface Renderer {

        /**
         * Renders one frame of the plasma animation.
         *
         * @param graphics the graphics target
         * @param time the animation time in seconds
         */
        void render(TextGraphics graphics, double time);
    }

    /**
     * Constructs a new PlasmaWash instance.
     * @param preferredSize the preferred size
     */
    public PlasmaWash(TerminalSize preferredSize) {
        this.preferredSize = preferredSize;
    }

    /**
     * Sets the target fps.
     * @param fps the fps
     */
    public void setTargetFps(int fps) {
        this.targetFps = Math.max(MIN_FPS, Math.min(MAX_FPS, fps));
    }

    /**
     * Returns the target fps.
     * @return the result
     */
    public int getTargetFps() {
        return targetFps;
    }

    /**
     * Sets the base color.
     * @param color the color
     */
    public void setBaseColor(Color color) {
        this.baseColor = color;
        invalidate();
    }

    /**
     * Returns the base color.
     * @return the result
     */
    public Color getBaseColor() {
        return baseColor;
    }

    /**
     * Sets the paused.
     * @param paused the paused
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * Returns whether the paused flag is set.
     * @return the result
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Sets the renderer.
     * @param renderer the renderer
     */
    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
        invalidate();
    }

    /**
     * Returns the renderer.
     * @return the result
     */
    public Renderer getRenderer() {
        return renderer;
    }

    /**
     * Returns the frame.
     * @return the result
     */
    public int getFrame() {
        return frame;
    }

    /**
     * Returns the time.
     * @return the result
     */
    public double getTime() {
        return time;
    }

    /**
     * Resets the frame counter and animation time to zero.
     */
    public void resetFrame() {
        frame = 0;
        time = 0.0;
        lastTickNanos = -1;
        accumulatedNs = 0;
    }

    /**
     * Advances the animation by the given absolute wall-clock time in nanoseconds.
     *
     * @param nowNanos monotonically increasing time in nanoseconds
     */
    public void tick(long nowNanos) {
        if (paused) return;
        if (lastTickNanos < 0) {
            lastTickNanos = nowNanos;
            frame++;
            frameElapsedMs = 0;
            return;
        }
        long delta = nowNanos - lastTickNanos;
        if (delta < 0) delta = 0;
        lastTickNanos = nowNanos;
        accumulatedNs += delta;

        long frameNs = NS_PER_MS * 1000L / targetFps;
        if (accumulatedNs >= frameNs) {
            frameElapsedMs = delta / NS_PER_MS;
            accumulatedNs -= frameNs;
            if (accumulatedNs >= frameNs) {
                accumulatedNs = accumulatedNs % frameNs;
            }
            frame++;
            // Slow, lava-lamp speed: 0.08 radians per frame.
            time += 0.08;
        }
    }

    @Override
    /**
     * Computes the preferred size for this component.
     * @return the result
     */
    protected TerminalSize calculatePreferredSize() {
        return preferredSize;
    }

    @Override
    /**
     * Sets the bounds.
     * @param position the position
     * @param size the size
     */
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        onResize(size);
    }

    @Override
    /**
     * Reallocates internal buffers for the new terminal size.
     * @param newSize the new size
     */
    public void onResize(TerminalSize newSize) {
        // Plasma is computed on the fly; no state to reallocate.
    }

    @Override
    /**
     * Draws this component into the supplied graphics context.
     * @param graphics the graphics
     */
    protected void drawComponent(TextGraphics graphics) {
        renderFrame(graphics, getSize());
    }

    @Override
    /**
     * Renders one frame of the animation into the given graphics buffer.
     * @param graphics the graphics
     * @param size the size
     */
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        if (size.columns() <= 0 || size.rows() <= 0) return;

        if (renderer != null) {
            renderer.render(graphics, time);
            return;
        }

        var theme = ThemeManager.active();
        Color bg = theme.background();
        Color[] palette = buildPalette(bg);

        for (int y = 0; y < size.rows(); y++) {
            for (int x = 0; x < size.columns(); x++) {
                double value = plasmaValue(x, y, time);
                int level = valueToLevel(value);
                Color fg = palette[level];
                Color cellBg = level == 0 ? bg : blend(bg, palette[level], 0.3);
                graphics.setCell(x, y, new TextCell(SHADING_CHARS[level], fg, cellBg));
            }
        }
    }

    /**
     * Computes the raw plasma sine-wave sum at the given cell.
     * Result is in the range [-4.0, 4.0] for all x, y, time.
     */
    double plasmaValue(int x, int y, double t) {
        double v = Math.sin(x * X_FREQ + t * TIME_X)
                 + Math.sin(y * Y_FREQ + t * TIME_Y)
                 + Math.sin((x + y) * DIAG_FREQ + t * TIME_DIAG)
                 + Math.sin(Math.sqrt(x * x + y * y) * RADIAL_FREQ + t * TIME_RADIAL);
        // Clamp for safety against floating-point drift.
        return Math.max(-4.0, Math.min(4.0, v));
    }

    /**
     * Maps a raw plasma value [-4.0, 4.0] to a 0..1 normalized range.
     */
    double normalize(double value) {
        return (value + 4.0) / 8.0;
    }

    /**
     * Maps a raw plasma value to one of the shading levels.
     */
    int valueToLevel(double value) {
        double normalized = normalize(value);
        int level = (int) Math.floor(normalized * SHADING_LEVELS);
        return Math.max(0, Math.min(SHADING_LEVELS - 1, level));
    }

    /**
     * Builds a foreground palette mapping each shading level to a different
     * ANSI hue, blended toward the theme background so the plasma shows a
     * spectrum of colors rather than a single-hue gradient.
     */
    private Color[] buildPalette(Color bg) {
        // Spectrum of ANSI hues from cool to warm. Each shading level gets
        // a different base hue so the plasma shows multiple colors.
        AnsiColor[] hues = {
            AnsiColor.BLUE,       // level 0 — dimmest
            AnsiColor.CYAN,
            AnsiColor.GREEN,
            AnsiColor.YELLOW,
            AnsiColor.MAGENTA,
            AnsiColor.BRIGHT_CYAN  // level 5 — brightest
        };
        Color[] palette = new Color[SHADING_LEVELS];
        for (int i = 0; i < SHADING_LEVELS; i++) {
            double weight = 0.30 + (0.55 * i) / (SHADING_LEVELS - 1);
            palette[i] = blend(bg, hues[i], weight);
        }
        return palette;
    }

    /**
     * Blends two colors. When both are ANSI colors the result is selected by a
     * threshold so the output stays in the limited ANSI palette; RGB colors use
     * linear interpolation if supported.
     */
    static Color blend(Color a, Color b, double weight) {
        if (weight <= 0.0) return a;
        if (weight >= 1.0) return b;
        if (a instanceof AnsiColor ac && b instanceof AnsiColor bc) {
            return AnsiColor.blendAnsi(ac, bc, weight);
        }
        return weight < 0.5 ? a : b;
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
        return targetFps;
    }
}
