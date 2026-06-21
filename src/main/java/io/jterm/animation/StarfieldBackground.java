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
    private volatile int targetFps = 30;
    private volatile Color themeColor = AnsiColor.BRIGHT_CYAN;
    private volatile boolean paused = false;
    private volatile boolean running = false;

    private volatile long lastTickNanos = -1;
    private volatile long accumulatedNs;
    private volatile long frameElapsedMs;
    private volatile int frame;

    private Star[] stars;
    private int starCount;

    private Renderer renderer;

    /** Functional interface for custom animation renderers. */
    @FunctionalInterface
    public interface Renderer {
        void render(TextGraphics graphics, long elapsedMs);
    }

    public StarfieldBackground(TerminalSize preferredSize) {
        this.preferredSize = preferredSize;
    }

    public void setTargetFps(int fps) {
        this.targetFps = Math.max(MIN_FPS, Math.min(MAX_FPS, fps));
    }

    public int getTargetFps() {
        return targetFps;
    }

    public void setThemeColor(Color color) {
        this.themeColor = color;
        invalidate();
    }

    public Color getThemeColor() {
        return themeColor;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
        invalidate();
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public int getFrame() {
        return frame;
    }

    public void resetFrame() {
        frame = 0;
        lastTickNanos = -1;
        accumulatedNs = 0;
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
    protected TerminalSize calculatePreferredSize() {
        return preferredSize;
    }

    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        onResize(size);
    }

    @Override
    public void onResize(TerminalSize newSize) {
        if (stars == null || stars.length != newSize.area()) {
            stars = new Star[newSize.area()];
            starCount = 0;
        }
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        renderFrame(graphics, getSize());
    }

    @Override
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

        int targetStars = Math.max(1, area / 20);
        if (frame <= 1) {
            while (starCount < targetStars && starCount < area) {
                int idx = random.nextInt(area);
                if (stars[idx] == null) {
                    stars[idx] = new Star(idx % size.columns(), idx / size.columns(),
                            1 + random.nextInt(4), random.nextDouble() < 0.15);
                    starCount++;
                }
            }
        } else {
            if (starCount < targetStars && random.nextInt(10) == 0) {
                int idx = random.nextInt(area);
                if (stars[idx] == null) {
                    stars[idx] = new Star(0, idx / size.columns(),
                            1 + random.nextInt(4), random.nextDouble() < 0.15);
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
                int phase = (frame / star.speed) % 4;
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

    private static final class Star {
        final int x;
        final int y;
        final int speed;
        final boolean bright;

        Star(int x, int y, int speed, boolean bright) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.bright = bright;
        }
    }
}
