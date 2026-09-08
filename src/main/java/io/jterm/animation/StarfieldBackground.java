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

import java.util.Random;

/**
 * A full-size animated star-field background. This is an implementation of
 * {@link AnimatedBackground} and {@link io.jterm.widget.Component}, so
 * it can be used either as a background window layer or as a panel component.
 */
public class StarfieldBackground extends AbstractComponent implements AnimatedBackground {
    private static final int MIN_FPS = 1;
    private static final int MAX_FPS = 60;
    private static final long NS_PER_MS = 1_000_000L;

    private final Random random = new Random();
    private TerminalSize preferredSize;
    private volatile int targetFps = 10;
    private volatile Color themeColor = AnsiColor.BRIGHT_CYAN;
    private volatile boolean paused = false;
    private volatile boolean running = false;

    private volatile long lastTickNanos = -1;
    private volatile long accumulatedNs;
    private volatile long frameElapsedMs;
    private volatile int frame;
    private volatile boolean firstRender = true;

    private Star[] stars;
    private int starCount;

    private Renderer renderer;

    /**
     * Functional interface for custom animation renderers.
     *
     * <p>The single abstract method {@link #render} receives the graphics
     * target and the elapsed time in milliseconds.
     */
    @FunctionalInterface
    public interface Renderer {

        /**
         * Renders one frame of the starfield animation.
         *
         * @param graphics the graphics target
         * @param elapsedMs elapsed time in milliseconds
         */
        void render(TextGraphics graphics, long elapsedMs);
    }

    /**
     * Constructs a new StarfieldBackground instance.
     * @param preferredSize the preferred size
     */
    public StarfieldBackground(TerminalSize preferredSize) {
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
     * Sets the theme color.
     * @param color the color
     */
    public void setThemeColor(Color color) {
        this.themeColor = color;
        invalidate();
    }

    /**
     * Returns the theme color.
     * @return the result
     */
    public Color getThemeColor() {
        return themeColor;
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
     * Resets the frame counter and animation time to zero.
     */
    public void resetFrame() {
        frame = 0;
        lastTickNanos = -1;
        accumulatedNs = 0;
        firstRender = true;
    }

    /**
     * Advances the animation by the given absolute wall-clock time in nanoseconds.
     * Call this from the GUI render loop or an {@link AnimationManager}.
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
        if (stars == null || stars.length != newSize.area()) {
            stars = new Star[newSize.area()];
            starCount = 0;
            firstRender = true;
        }
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

        var theme = ThemeManager.active();
        graphics.fillRectangle(0, 0, size.columns(), size.rows(),
                new TextCell(' ', theme.foreground(), theme.background()));

        if (renderer != null) {
            renderer.render(graphics, frameElapsedMs);
            return;
        }

        drawStarField(graphics, size);
    }

    private void drawStarField(TextGraphics graphics, TerminalSize size) {
        int area = size.area();
        if (stars == null || stars.length != area) {
            stars = new Star[area];
            starCount = 0;
        }

        int targetStars = Math.max(1, area / 16);
        if (firstRender) {
            while (starCount < targetStars && starCount < area) {
                int idx = random.nextInt(area);
                if (stars[idx] == null) {
                    stars[idx] = new Star(idx % size.columns(), idx / size.columns(),
                            1 + random.nextInt(10), random.nextDouble() < 0.15,
                            random.nextInt(4));
                    starCount++;
                }
            }
            firstRender = false;
        } else {
            if (starCount < targetStars && random.nextInt(10) == 0) {
                int idx = random.nextInt(area);
                if (stars[idx] == null) {
                    stars[idx] = new Star(random.nextInt(size.columns()),
                            idx / size.columns(),
                            1 + random.nextInt(10), random.nextDouble() < 0.15,
                            random.nextInt(4));
                    starCount++;
                }
            }
        }

        var theme = ThemeManager.active();
        Color dim = blend(themeColor, theme.background(), 0.35);
        Color medium = blend(themeColor, theme.background(), 0.65);

        for (int i = 0; i < area; i++) {
            Star star = stars[i];
            if (star == null) continue;

            int x = (star.x + frame / star.speed) % size.columns();
            int y = star.y;

            if (x < 0) x += size.columns();

            Color fg;
            char glyph;
            if (star.bright) {
                fg = themeColor;
                glyph = '*';
            } else {
                int phase = (star.phase + frame / star.speed) % 4;
                fg = phase < 2 ? medium : dim;
                glyph = '.';
            }
            graphics.setCell(x, y, new TextCell(glyph, fg, theme.background()));
        }
    }

    private static Color blend(Color a, Color b, double aWeight) {
        if (aWeight <= 0.0) return a;
        if (aWeight >= 1.0) return a;
        if (a instanceof AnsiColor ac && b instanceof AnsiColor bc) {
            return AnsiColor.blendAnsi(ac, bc, aWeight);
        }
        return aWeight >= 0.5 ? a : b;
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

    private static final class Star {
        final int x;
        final int y;
        final int speed;
        final boolean bright;
        final int phase;

        Star(int x, int y, int speed, boolean bright, int phase) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.bright = bright;
            this.phase = phase;
        }
    }
}
