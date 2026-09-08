package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.ArrayList;
import java.util.List;

/**
 * Animated spirograph / harmonograph background.
 *
 * <p>Draws parametric Lissajous-style curves that slowly evolve as the phases
 * advance each frame. Points are kept in a circular fade buffer: new points are
 * bright, old points dim and are eventually dropped, producing a continuously
 * redrawn glowing trail. Frequencies (a, b) are chosen so the pattern builds
 * complex, symmetrical spirograph-like shapes.</p>
 */
public class Spirograph implements AnimatedBackground {

    private static final int TARGET_FPS = 12;

    // Characters from oldest/dim to newest/bright.
    private static final char[] GLYPHS = {' ', '.', '+', 'o', '*'};

    // Number of trail points to keep. Older points fade out.
    private static final int TRAIL_LENGTH = 300;

    // Parametric frequencies for the Lissajous curve.
    private static final double FREQ_X = 3.0;
    private static final double FREQ_Y = 5.0;

    // Phase advance per frame; slow enough to be visually smooth.
    private static final double PHASE_SPEED = 0.04;

    // Trail point with its age in frames.
    private record TrailPoint(int x, int y, int age) {}

    private volatile boolean running;
    private volatile TerminalSize lastSize;

    private final List<TrailPoint> trail = new ArrayList<>(TRAIL_LENGTH);
    private double phaseX;
    private double phaseY;
    private int frame;

    /**
     * Constructs a new Spirograph instance.
     * @param preferredSize the preferred size
     */
    public Spirograph(TerminalSize preferredSize) {
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
        if (size.columns() <= 0 || size.rows() <= 0) {
            return;
        }

        frame++;
        phaseX += PHASE_SPEED * FREQ_X;
        phaseY += PHASE_SPEED * FREQ_Y;

        advanceTrail(size);
        drawTrail(graphics, size);
    }

    /**
     * Advances the parametric curve by one step and adds the new point to the
     * trail buffer, removing the oldest point when the buffer is full.
     */
    private void advanceTrail(TerminalSize size) {
        int cols = size.columns();
        int rows = size.rows();

        double cx = cols / 2.0;
        double cy = rows / 2.0;
        double radius = Math.min(cx, cy) * 0.85;

        // Parametric Lissajous curve with slowly shifting phases.
        double t = frame * 0.05;
        double x = radius * Math.cos(FREQ_X * t + phaseX);
        double y = radius * Math.sin(FREQ_Y * t + phaseY);

        // Convert to integer cell coordinates centered on the terminal.
        int px = (int) Math.round(cx + x);
        int py = (int) Math.round(cy + y);

        // Clamp to the terminal bounds.
        px = Math.max(0, Math.min(cols - 1, px));
        py = Math.max(0, Math.min(rows - 1, py));

        if (trail.size() >= TRAIL_LENGTH) {
            trail.remove(0);
        }
        trail.add(new TrailPoint(px, py, 0));

        // Age all existing points by one frame.
        for (int i = 0; i < trail.size(); i++) {
            TrailPoint p = trail.get(i);
            trail.set(i, new TrailPoint(p.x, p.y, p.age + 1));
        }
    }

    /**
     * Draws the trail into the graphics buffer. Older points are dim and use
     * fainter glyphs; newer points are bright and bold.
     */
    private void drawTrail(TextGraphics graphics, TerminalSize size) {
        // Start with a black background so older frames fade away.
        TextCell background = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        for (int y = 0; y < size.rows(); y++) {
            for (int x = 0; x < size.columns(); x++) {
                graphics.setCell(x, y, background);
            }
        }

        for (TrailPoint point : trail) {
            if (point.age >= GLYPHS.length) {
                continue;
            }
            // Newest points are at the end of the array; draw oldest first so
            // bright new points overwrite dim old ones at the same coordinate.
            TextCell cell = cellFor(point.age);
            graphics.setCell(point.x, point.y, cell);
        }
    }

    private TextCell cellFor(int age) {
        // age 0 -> last (brightest) glyph; increasing age -> dimmer glyphs.
        int index = Math.max(0, GLYPHS.length - 1 - age);
        char ch = GLYPHS[index];

        Color fg = switch (index) {
            case 0 -> AnsiColor.MAGENTA;
            case 1 -> AnsiColor.MAGENTA;
            case 2 -> AnsiColor.BRIGHT_MAGENTA;
            case 3 -> AnsiColor.BRIGHT_CYAN;
            default -> AnsiColor.BRIGHT_WHITE;
        };

        SGR sgr = (index >= 3) ? SGR.BOLD : SGR.DIM;
        return new TextCell(ch, fg, AnsiColor.BLACK, sgr);
    }

    @Override
    /**
     * Reallocates internal buffers for the new terminal size.
     * @param newSize the new size
     */
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        // Clear the trail so the curve doesn't carry stale coordinates from the
        // previous terminal size.
        trail.clear();
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
     * Returns the current frame counter, visible for tests.
     *
     * @return the frame counter
     */
    public int getFrame() {
        return frame;
    }

    /**
     * Returns the current X phase, visible for tests.
     *
     * @return the X phase
     */
    public double getPhaseX() {
        return phaseX;
    }

    /**
     * Returns the current Y phase, visible for tests.
     *
     * @return the Y phase
     */
    public double getPhaseY() {
        return phaseY;
    }

    /**
     * Returns a snapshot of the current trail points, visible for tests.
     *
     * @return an unmodifiable copy of the trail points
     */
    public List<TrailPoint> getTrail() {
        return List.copyOf(trail);
    }
}
