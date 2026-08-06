package io.jterm.widget.animation;

import io.jterm.animation.AnimatedBackground;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;

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
    private static final double DEFAULT_WARP_SPEED = 0.075;

    private final Random random = new Random();
    private final int starCount;
    private TerminalSize preferredSize;

    private volatile int targetFps = 10;
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

    /**
     * Creates a WarpStarfield with the given preferred size and 60 stars.
     *
     * @param preferredSize the initial preferred terminal size
     */
    public WarpStarfield(TerminalSize preferredSize) {
        this(preferredSize, 60);
    }

    /**
     * Creates a WarpStarfield with the given preferred size and star count.
     *
     * @param preferredSize the initial preferred terminal size
     * @param starCount     the number of stars to render; clamped to at least 1
     */
    public WarpStarfield(TerminalSize preferredSize, int starCount) {
        this.preferredSize = preferredSize;
        this.starCount = Math.max(1, starCount);
    }

    /**
     * Sets the target frame rate for the animation, clamped to {@code [1, 60]}.
     *
     * @param fps the desired frames per second
     */
    public void setTargetFps(int fps) {
        this.targetFps = Math.max(MIN_FPS, Math.min(MAX_FPS, fps));
    }

    /** Returns the current target frame rate in frames per second. */
    public int getTargetFps() {
        return targetFps;
    }

    /**
     * Sets the warp speed (depth decrement per tick); clamped to a minimum of 0.001.
     *
     * @param speed the new warp speed
     */
    public void setWarpSpeed(double speed) {
        this.warpSpeed = Math.max(0.001, speed);
    }

    /** Returns the current warp speed. */
    public double getWarpSpeed() {
        return warpSpeed;
    }

    /**
     * Pauses or resumes the animation.
     *
     * @param paused {@code true} to pause, {@code false} to resume
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /** Returns whether the animation is currently paused. */
    public boolean isPaused() {
        return paused;
    }

    /** Returns the current frame counter value. */
    public int getFrame() {
        return frame;
    }

    /** Returns the X coordinate of the starfield center (viewport center). */
    public int getCenterX() {
        return centerX;
    }

    /** Returns the Y coordinate of the starfield center (viewport center). */
    public int getCenterY() {
        return centerY;
    }

    /** Returns the number of stars currently allocated, or 0 before the first resize. */
    public int getStarCount() {
        return stars == null ? 0 : stars.length;
    }

    /** Resets the frame counter and timing accumulator to their initial state. */
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

    /**
     * Returns the preferred terminal size supplied at construction.
     *
     * @return the preferred size
     */
    @Override
    protected TerminalSize calculatePreferredSize() {
        return preferredSize;
    }

    /**
     * Updates bounds and re-initializes star positions for the new size.
     *
     * @param position the new position
     * @param size     the new size
     */
    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        onResize(size);
    }

    /**
     * Recomputes the viewport center and (re)allocates stars when the size changes.
     *
     * @param newSize the new terminal size
     */
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

    /**
     * Renders the current animation frame to the component's own bounds.
     *
     * @param graphics the text-graphics target
     */
    @Override
    protected void drawComponent(TextGraphics graphics) {
        renderFrame(graphics, getSize());
    }

    /**
     * Renders the starfield to the given graphics target and size, advancing
     * each star and drawing it with a depth-based glyph/color.
     *
     * @param graphics the text-graphics target
     * @param size     the area to render into
     */
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
        // Same color palette as StarfieldBackground — cyan-based with dim/medium blends
        Color dim = blend(AnsiColor.BRIGHT_CYAN, bg, 0.30);
        Color medium = blend(AnsiColor.BRIGHT_CYAN, bg, 0.60);
        Color bright = AnsiColor.BRIGHT_CYAN;

        for (Star star : stars) {
            advanceStar(star, warpSpeed);

            var pos = project(star.x, star.y, star.z);
            if (!pos.isValid(size)) continue;

            // Color by depth — closer = brighter
            Color fg;
            char glyph;
            if (star.z > 6.0) {
                fg = dim;
                glyph = '.';
            } else if (star.z > 3.0) {
                fg = medium;
                glyph = '+';
            } else if (star.z > 1.0) {
                fg = bright;
                glyph = '*';
            } else {
                fg = bright;
                glyph = '#';
            }

            graphics.setCell(pos.column(), pos.row(),
                    new TextCell(glyph, fg, bg));
        }
    }

    /** Project a 3D normalized point to 2D screen coordinates. */
    public TerminalPosition project(double x, double y, double z) {
        // Narrower FOV — stars stay closer to center, more focused stream
        int sx = (int) Math.round(centerX + (x / z) * centerX * 2.5);
        int sy = (int) Math.round(centerY + (y / z) * centerY * 2.5);
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
        if (z > 1.0) return '*';
        return '#';
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
        if (aWeight <= 0.0) return a;
        if (aWeight >= 1.0) return a;
        if (a instanceof AnsiColor ac && b instanceof AnsiColor bc) {
            return AnsiColor.blendAnsi(ac, bc, aWeight);
        }
        return aWeight >= 0.5 ? a : b;
    }

    /** Returns the configured target frame rate. */
    @Override
    public int targetFps() {
        return targetFps;
    }

    /** Marks the animation as running. */
    @Override
    public void start() {
        running = true;
    }

    /** Marks the animation as stopped. */
    @Override
    public void stop() {
        running = false;
    }

    /** Returns whether the animation is currently running. */
    @Override
    public boolean isRunning() {
        return running;
    }

    /** Mutable 3D star state used internally and exposed for testing. */
    public static final class Star {
        double x;
        double y;
        double z;

        /**
         * Creates a star at the given 3D coordinates.
         *
         * @param x the X coordinate in normalized space
         * @param y the Y coordinate in normalized space
         * @param z the Z depth coordinate
         */
        Star(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
