package io.jterm.widget.animation;

import io.jterm.animation.AnimatedBackground;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.widget.AbstractComponent;

import java.util.Random;

/**
 * A 3D perspective warp starfield background. Stars stream from the center
 * toward the viewer, getting brighter and larger as they approach. This is a
 * {@link io.jterm.animation.AnimatedBackground} implementation that can
 * also be used as a regular component.
 */
public class WarpStarfield extends AbstractComponent implements AnimatedBackground {
    private static final int MIN_FPS = 1;
    private static final int MAX_FPS = 60;
    private static final long NS_PER_MS = 1_000_000L;

    private static final double MIN_DEPTH = 0.1;
    private static final double MAX_DEPTH = 10.0;
    private static final double DEFAULT_WARP_SPEED = 0.05;

    private final Random random = new Random();
    private final int starCount;
    private TerminalSize preferredSize;

    private volatile int targetFps = 15;
    private volatile double warpSpeed = DEFAULT_WARP_SPEED;
    private volatile boolean paused = false;
    private volatile boolean running = false;

    private volatile long lastTickNanos = -1;
    private volatile long accumulatedNs;
    private volatile long frameElapsedMs;
    private volatile int frame;

    private Star[] stars;
    private volatile int centerX;
    private volatile int centerY;

    public WarpStarfield(TerminalSize preferredSize) {
        this(preferredSize, 175);
    }

    public WarpStarfield(TerminalSize preferredSize, int starCount) {
        this.preferredSize = preferredSize;
        this.starCount = Math.max(1, starCount);
    }

    public void setTargetFps(int fps) {
        this.targetFps = Math.max(MIN_FPS, Math.min(MAX_FPS, fps));
    }

    public int getTargetFps() {
        return targetFps;
    }

    public void setWarpSpeed(double speed) {
        this.warpSpeed = Math.max(0.001, speed);
    }

    public double getWarpSpeed() {
        return warpSpeed;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public int getFrame() {
        return frame;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getStarCount() {
        return stars == null ? 0 : stars.length;
    }

    public void resetFrame() {
        frame = 0;
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
        centerX = newSize.columns() / 2;
        centerY = newSize.rows() / 2;
        if (stars == null || stars.length != starCount) {
            stars = new Star[starCount];
            for (int i = 0; i < starCount; i++) {
                stars[i] = createStar();
            }
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

        if (stars == null) {
            onResize(size);
        }

        var bg = theme.background();
        Color dim = blend(AnsiColor.WHITE, bg, 0.25);
        Color medium = blend(AnsiColor.WHITE, bg, 0.55);
        Color bright = blend(AnsiColor.WHITE, bg, 0.85);
        Color cyanTint = random.nextDouble() < 0.10
                ? blend(AnsiColor.BRIGHT_CYAN, bg, 0.7)
                : bright;

        for (Star star : stars) {
            advanceStar(star, warpSpeed);

            var current = project(star.x, star.y, star.z);
            var previous = project(star.x, star.y, star.z + warpSpeed * 2);

            if (!current.isValid(size) && !previous.isValid(size)) continue;

            double b = brightnessForDepth(star.z);
            Color fg;
            if (star.z > 7.0) {
                fg = dim;
            } else if (star.z > 3.0) {
                fg = medium;
            } else if (star.z > 1.0) {
                fg = bright;
            } else {
                fg = cyanTint;
            }

            char glyph = glyphForDepth(star.z);
            var cell = star.z < 3.0
                    ? new TextCell(glyph, fg, bg, SGR.BOLD)
                    : new TextCell(glyph, fg, bg);

            if (previous.isValid(size) && current.isValid(size)) {
                graphics.drawLine(previous.column(), previous.row(),
                        current.column(), current.row(), cell);
            } else if (current.isValid(size)) {
                graphics.setCell(current.column(), current.row(), cell);
            }
        }
    }

    /** Project a 3D normalized point to 2D screen coordinates. */
    public TerminalPosition project(double x, double y, double z) {
        int sx = (int) Math.round(centerX + (x / z) * centerX);
        int sy = (int) Math.round(centerY + (y / z) * centerY);
        return new TerminalPosition(sx, sy);
    }

    /** Returns a brightness value in [0, 1] based on depth. */
    public double brightnessForDepth(double z) {
        return 1.0 - (Math.max(MIN_DEPTH, Math.min(z, MAX_DEPTH)) / MAX_DEPTH);
    }

    /** Choose a glyph character based on depth. */
    public char glyphForDepth(double z) {
        if (z > 7.0) return '.';
        if (z > 3.0) return '+';
        if (z > 1.0) return '\u2727';
        return '\u2605';
    }

    /** Move a star toward the viewer; respawn it once it passes through. */
    public void advanceStar(Star star, double speed) {
        star.z -= speed;
        if (star.z < MIN_DEPTH) {
            star.z = MAX_DEPTH;
            star.x = random.nextDouble() * 2.0 - 1.0;
            star.y = random.nextDouble() * 2.0 - 1.0;
        }
    }

    /** Create a new star at a random 3D position. */
    public Star createStar() {
        double x = random.nextDouble() * 2.0 - 1.0;
        double y = random.nextDouble() * 2.0 - 1.0;
        double z = MIN_DEPTH + random.nextDouble() * (MAX_DEPTH - MIN_DEPTH);
        return new Star(x, y, z);
    }

    private static Color blend(Color a, Color b, double aWeight) {
        if (a instanceof AnsiColor ac && b instanceof AnsiColor bc) {
            return aWeight >= 0.5 ? ac : bc;
        }
        return a;
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

    /** Mutable 3D star state used internally and exposed for testing. */
    public static final class Star {
        double x;
        double y;
        double z;

        Star(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
