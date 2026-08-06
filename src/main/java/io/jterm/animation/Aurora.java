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
 * Animated aurora borealis background. Renders flowing sine-wave bands across
 * the top two-thirds of the screen in green, cyan, magenta and blue. Each band
 * has its own base height, amplitude, frequency and speed. Overlapping bands
 * blend by choosing the brighter color, and a subtle vertical gradient makes
 * cells above the band line dimmer than the bright band center.
 */
public class Aurora implements AnimatedBackground {

    private static final char BAND_EDGE = '~';
    private static final char BAND_CENTER = '#';
    private static final char BAND_GLOW = '.';
    private static final char BACKGROUND = ' ';

    private static final int TARGET_FPS = 10;

    private final List<Band> bands = new ArrayList<>();
    private final Object bandLock = new Object();

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private volatile double time;

    /**
     * Constructs a new Aurora instance.
     * @param preferredSize the preferred size
     */
    public Aurora(TerminalSize preferredSize) {
        onResize(preferredSize);
    }

    private void rebuildBands(TerminalSize size) {
        if (size == null) return;
        int rows = size.rows();
        int columns = size.columns();
        if (rows <= 0 || columns <= 0) return;

        synchronized (bandLock) {
            bands.clear();
            // Aurora spans the top ~65% of the screen, back (high/dim) to front (low/bright).
            bands.add(new Band(
                    baseY(rows, 0.20), amplitude(rows, 0.06), frequency(columns, 0.25),
                    speed(0.18), AnsiColor.BRIGHT_BLUE));
            bands.add(new Band(
                    baseY(rows, 0.32), amplitude(rows, 0.08), frequency(columns, 0.40),
                    speed(0.28), AnsiColor.BRIGHT_GREEN));
            bands.add(new Band(
                    baseY(rows, 0.45), amplitude(rows, 0.10), frequency(columns, 0.55),
                    speed(0.38), AnsiColor.BRIGHT_CYAN));
            bands.add(new Band(
                    baseY(rows, 0.58), amplitude(rows, 0.11), frequency(columns, 0.70),
                    speed(0.48), AnsiColor.BRIGHT_MAGENTA));
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

    /** Visible for tests to avoid wall-clock time. */
    public void renderAtTime(TextGraphics graphics, TerminalSize size, double timeSeconds) {
        if (size.columns() <= 0 || size.rows() <= 0) return;

        // Black background.
        TextCell bg = new TextCell(BACKGROUND, AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bg);

        List<Band> snapshot;
        synchronized (bandLock) {
            snapshot = List.copyOf(bands);
        }

        // Render from back to front so nearer bands can overwrite farther ones.
        for (Band band : snapshot) {
            renderBand(graphics, size, band, timeSeconds);
        }
    }

    private void renderBand(TextGraphics graphics, TerminalSize size, Band band, double t) {
        int rows = size.rows();
        int cols = size.columns();

        int[] bandY = new int[cols];
        for (int x = 0; x < cols; x++) {
            double y = band.baseY + band.amplitude * Math.sin(x * band.frequency + t * band.speed);
            bandY[x] = Math.max(0, Math.min(rows - 1, (int) Math.round(y)));
        }

        for (int x = 0; x < cols; x++) {
            int y = bandY[x];

            // Vertical gradient: bright at center, dim glow above, background far above.
            int glowStart = Math.max(0, y - 2);
            for (int r = glowStart; r <= y && r < rows; r++) {
                int distance = y - r;
                TextCell cell = cellForDistance(band.color, distance);
                Color existing = graphics.getCell(x, r).fg();
                Color chosen = brighter(existing, cell.fg());
                if (chosen != cell.fg()) {
                    // Keep glyph of brighter existing color.
                    TextCell existingCell = graphics.getCell(x, r);
                    graphics.setCell(x, r, existingCell.withForeground(chosen));
                } else {
                    graphics.setCell(x, r, cell);
                }
            }
        }
    }

    private static TextCell cellForDistance(AnsiColor color, int distance) {
        return switch (distance) {
            case 0 -> new TextCell(BAND_CENTER, color, AnsiColor.BLACK, SGR.BOLD);
            case 1 -> new TextCell(BAND_EDGE, color, AnsiColor.BLACK);
            default -> new TextCell(BAND_GLOW, dim(color), AnsiColor.BLACK);
        };
    }

    private static AnsiColor dim(AnsiColor color) {
        return switch (color) {
            case BRIGHT_GREEN -> AnsiColor.GREEN;
            case BRIGHT_CYAN -> AnsiColor.CYAN;
            case BRIGHT_MAGENTA -> AnsiColor.MAGENTA;
            case BRIGHT_BLUE -> AnsiColor.BLUE;
            default -> AnsiColor.BLACK;
        };
    }

    /** Returns the brighter of two colors by perceived luminance. */
    private static Color brighter(Color a, Color b) {
        if (!(a instanceof AnsiColor ac) || !(b instanceof AnsiColor bc)) {
            return b;
        }
        double la = luminance(ac);
        double lb = luminance(bc);
        return lb > la ? b : a;
    }

    private static double luminance(AnsiColor color) {
        var rgb = color.toRgb();
        return 0.299 * rgb.r() + 0.587 * rgb.g() + 0.114 * rgb.b();
    }

    @Override
    /**
     * Reallocates internal buffers for the new terminal size.
     * @param newSize the new size
     */
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        rebuildBands(newSize);
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
    public List<Band> getBands() {
        synchronized (bandLock) {
            return List.copyOf(bands);
        }
    }

    /** Visible for tests. */
    public double getTime() {
        return time;
    }

    /** Package-visible band parameters. */
    public record Band(
            int baseY,
            double amplitude,
            double frequency,
            double speed,
            AnsiColor color
    ) {
    }
}
