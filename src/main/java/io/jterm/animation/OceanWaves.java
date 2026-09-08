package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.ArrayList;
import java.util.List;

/**
 * Animated ocean waves background. Renders layered sine waves from back
 * (dim, high) to front (bright, low). Each wave has its own base line,
 * amplitude, frequency, speed and color. Water below a wave is filled with
 * a dim version of the wave color; sky above is black. Foam glyphs appear
 * at wave crests where the sine slope changes from rising to falling.
 *
 * <p>This implementation is {@link AnimatedBackground} only (not a
 * {@link io.jterm.widget.Component}) so it can avoid the component
 * lifecycle and focus on full-frame rendering behind a login screen.</p>
 */
public class OceanWaves implements AnimatedBackground {

    private static final char WAVE_SURFACE = '~';
    private static final char FOAM = '.';
    private static final char WATER_BODY = '=';
    private static final char SKY = ' ';

    private static final int TARGET_FPS = 10;

    private final List<Wave> waves = new ArrayList<>();
    private final Object waveLock = new Object();

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private volatile double time;

    /**
     * Constructs a new OceanWaves instance.
     * @param preferredSize the preferred size
     */
    public OceanWaves(TerminalSize preferredSize) {
        onResize(preferredSize);
    }

    private void rebuildWaves(TerminalSize size) {
        if (size == null) return;
        int rows = size.rows();
        int columns = size.columns();
        if (rows <= 0 || columns <= 0) return;

        synchronized (waveLock) {
            waves.clear();
            // Back (highest, dimmest) to front (lowest, brightest)
            waves.add(new Wave(
                    baseY(rows, 0.30), amplitude(rows, 0.08), frequency(columns, 0.35),
                    speed(0.30), AnsiColor.BLUE, dim(AnsiColor.BLUE)));
            waves.add(new Wave(
                    baseY(rows, 0.42), amplitude(rows, 0.10), frequency(columns, 0.55),
                    speed(0.45), AnsiColor.BRIGHT_BLUE, dim(AnsiColor.BRIGHT_BLUE)));
            waves.add(new Wave(
                    baseY(rows, 0.55), amplitude(rows, 0.12), frequency(columns, 0.75),
                    speed(0.60), AnsiColor.CYAN, dim(AnsiColor.CYAN)));
            waves.add(new Wave(
                    baseY(rows, 0.70), amplitude(rows, 0.14), frequency(columns, 0.95),
                    speed(0.75), AnsiColor.BRIGHT_CYAN, dim(AnsiColor.BRIGHT_CYAN)));
        }
    }

    private static int baseY(int rows, double fraction) {
        return (int) Math.round(rows * fraction);
    }

    private static double amplitude(int rows, double fraction) {
        return Math.max(0.5, rows * fraction);
    }

    private static double frequency(int columns, double factor) {
        return factor * Math.PI * 2.0 / Math.max(1, columns);
    }

    private static double speed(double factor) {
        return factor;
    }

    private static AnsiColor dim(AnsiColor color) {
        return switch (color) {
            case BRIGHT_BLUE -> AnsiColor.BLUE;
            case BRIGHT_CYAN -> AnsiColor.CYAN;
            case CYAN -> AnsiColor.BRIGHT_BLACK;
            default -> AnsiColor.BLACK;
        };
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
        time += 0.08;
    }

    /**
     * Visible for tests to avoid wall-clock time.
     *
     * @param graphics    graphics context to render into
     * @param size        terminal size to render at
     * @param timeSeconds animation time in seconds
     */
    public void renderAtTime(TextGraphics graphics, TerminalSize size, double timeSeconds) {
        if (size.columns() <= 0 || size.rows() <= 0) return;

        // Clear sky (black on black)
        TextCell sky = new TextCell(SKY, AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), sky);

        // Draw layers from back to front so nearer waves overwrite farther ones.
        List<Wave> snapshot;
        synchronized (waveLock) {
            snapshot = List.copyOf(waves);
        }

        for (Wave wave : snapshot) {
            renderLayer(graphics, size, wave, timeSeconds);
        }
    }

    private void renderLayer(TextGraphics graphics, TerminalSize size, Wave wave, double t) {
        int rows = size.rows();
        int cols = size.columns();

        int[] waveY = new int[cols];
        for (int x = 0; x < cols; x++) {
            double y = wave.baseY + wave.amplitude * Math.sin(x * wave.frequency + t * wave.speed);
            waveY[x] = Math.max(0, Math.min(rows - 1, (int) Math.round(y)));
        }

        TextCell surface = new TextCell(WAVE_SURFACE, wave.color, AnsiColor.BLACK);
        TextCell foam = new TextCell(FOAM, AnsiColor.BRIGHT_WHITE, AnsiColor.BLACK, SGR.BOLD);
        TextCell body = new TextCell(WATER_BODY, wave.dimColor, AnsiColor.BLACK);

        for (int x = 0; x < cols; x++) {
            int y = waveY[x];

            // Foam at local maxima (previous point was lower and next point is lower)
            boolean peak = false;
            if (x > 0 && x < cols - 1) {
                peak = waveY[x - 1] >= waveY[x] && waveY[x + 1] >= waveY[x];
            }

            for (int r = y; r < rows; r++) {
                if (r == y) {
                    graphics.setCell(x, r, peak ? foam : surface);
                } else {
                    graphics.setCell(x, r, body);
                }
            }
        }
    }

    @Override
    /**
     * Reallocates internal buffers for the new terminal size.
     * @param newSize the new size
     */
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        rebuildWaves(newSize);
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
     * Visible for tests.
     *
     * @return a snapshot of the current wave parameters
     */
    public List<Wave> getWaves() {
        synchronized (waveLock) {
            return List.copyOf(waves);
        }
    }

    /**
     * Visible for tests.
     *
     * @return the accumulated animation time in seconds
     */
    public double getTime() {
        return time;
    }

    /**
     * Package-visible wave parameters.
     *
     * @param baseY     mean row of the wave's surface line, in rows from the top
     * @param amplitude wave height in rows (peak deviation from {@code baseY})
     * @param frequency horizontal wave frequency in radians per column
     * @param speed     phase advance in radians per second of animation time
     * @param color     surface color (bright variant at the crest)
     * @param dimColor  color of the water body below the surface
     */
    public record Wave(
            int baseY,
            double amplitude,
            double frequency,
            double speed,
            AnsiColor color,
            AnsiColor dimColor
    ) {
    }
}
