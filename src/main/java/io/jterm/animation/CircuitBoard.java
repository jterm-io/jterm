package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.widget.AbstractComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated circuit-board (PCB) background. Generates a grid of orthogonal
 * copper traces, pads, and vias, then sends pulses of electricity flowing
 * along random routes. Uses ANSI colors only for broad terminal compatibility.
 *
 * <p>This is an {@link AnimatedBackground} and a regular Component, so it can
 * be drawn into a background window or added to a panel like the other
 * animated backgrounds.</p>
 */
public class CircuitBoard extends AbstractComponent implements AnimatedBackground {
    private static final int MIN_FPS = 1;
    private static final int MAX_FPS = 60;
    private static final long NS_PER_MS = 1_000_000L;

    private static final double TRACE_DENSITY = 0.55;
    private static final int PAD_SPACING = 4;
    private static final int MIN_TRACE_LENGTH = 6;
    private static final int MAX_TRACE_LENGTH = 22;
    private static final double PULSE_SPAWN_CHANCE = 0.12;
    private static final int MAX_PULSES = 8;

    private final Random random = new Random();
    private TerminalSize preferredSize;

    private volatile int targetFps = 8;
    private volatile boolean paused = false;
    private volatile boolean running = false;

    private volatile long lastTickNanos = -1;
    private volatile long accumulatedNs;
    private volatile long frameElapsedMs;
    private volatile int frame;

    private int gridCols;
    private int gridRows;
    private final List<Trace> traces = new ArrayList<>();
    private final List<Pulse> pulses = new ArrayList<>();

    public CircuitBoard(TerminalSize preferredSize) {
        this.preferredSize = preferredSize;
    }

    public void setTargetFps(int fps) {
        this.targetFps = Math.max(MIN_FPS, Math.min(MAX_FPS, fps));
    }

    public int getTargetFps() {
        return targetFps;
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

    public void resetFrame() {
        frame = 0;
        lastTickNanos = -1;
        accumulatedNs = 0;
        traces.clear();
        pulses.clear();
    }

    public List<Trace> getTraces() {
        return List.copyOf(traces);
    }

    public List<Pulse> getPulses() {
        return List.copyOf(pulses);
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
        int newGridCols = Math.max(1, newSize.columns() / PAD_SPACING);
        int newGridRows = Math.max(1, newSize.rows() / PAD_SPACING);
        if (newGridCols != gridCols || newGridRows != gridRows) {
            gridCols = newGridCols;
            gridRows = newGridRows;
            traces.clear();
            pulses.clear();
            generateTraces();
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
        Color bg = theme.background();
        Color traceColor = AnsiColor.GREEN;
        Color padColor = AnsiColor.BRIGHT_GREEN;
        Color viaColor = AnsiColor.GREEN;

        graphics.fillRectangle(0, 0, size.columns(), size.rows(),
                new TextCell(' ', theme.foreground(), bg));

        if (gridCols == 0 || gridRows == 0) {
            onResize(size);
        }
        if (traces.isEmpty()) {
            generateTraces();
        }

        // Draw the copper grid first.
        for (Trace trace : traces) {
            drawTrace(graphics, trace, traceColor, bg);
        }

        // Draw static pads/vias.
        for (int gy = 0; gy < gridRows; gy++) {
            for (int gx = 0; gx < gridCols; gx++) {
                int x = gx * PAD_SPACING + PAD_SPACING / 2;
                int y = gy * PAD_SPACING + PAD_SPACING / 2;
                if (x < size.columns() && y < size.rows()) {
                    boolean hasTrace = false;
                    for (Trace trace : traces) {
                        if (trace.gx0 == gx && trace.gy0 == gy
                                || trace.gx1 == gx && trace.gy1 == gy) {
                            hasTrace = true;
                            break;
                        }
                    }
                    if (hasTrace) {
                        graphics.setCell(x, y, new TextCell('+', padColor, bg, SGR.BOLD));
                    } else if (random.nextDouble() < 0.08) {
                        graphics.setCell(x, y, new TextCell('·', viaColor, bg));
                    }
                }
            }
        }

        advancePulses();
        spawnPulses();

        // Draw electricity pulses over the traces.
        for (Pulse pulse : pulses) {
            drawPulse(graphics, pulse, size);
        }
    }

    private void generateTraces() {
        traces.clear();
        if (gridCols <= 1 || gridRows <= 1) return;

        for (int gy = 0; gy < gridRows; gy++) {
            for (int gx = 0; gx < gridCols; gx++) {
                if (random.nextDouble() >= TRACE_DENSITY) continue;

                int gx1;
                int gy1;
                if (random.nextBoolean()) {
                    gx1 = gx + random.nextInt(Math.min(gridCols - gx, MAX_TRACE_LENGTH / PAD_SPACING) + 1);
                    gy1 = gy;
                    if (gx1 == gx) continue;
                } else {
                    gx1 = gx;
                    gy1 = gy + random.nextInt(Math.min(gridRows - gy, MAX_TRACE_LENGTH / PAD_SPACING) + 1);
                    if (gy1 == gy) continue;
                }

                int length = (Math.abs(gx1 - gx) + Math.abs(gy1 - gy)) * PAD_SPACING;
                if (length < MIN_TRACE_LENGTH) continue;
                if (length > MAX_TRACE_LENGTH) continue;

                traces.add(new Trace(gx, gy, gx1, gy1, random.nextBoolean()));
            }
        }
    }

    private void drawTrace(TextGraphics graphics, Trace trace, Color traceColor, Color bg) {
        int x0 = trace.gx0 * PAD_SPACING + PAD_SPACING / 2;
        int y0 = trace.gy0 * PAD_SPACING + PAD_SPACING / 2;
        int x1 = trace.gx1 * PAD_SPACING + PAD_SPACING / 2;
        int y1 = trace.gy1 * PAD_SPACING + PAD_SPACING / 2;

        var cell = new TextCell('\u2500', traceColor, bg);
        var vert = new TextCell('\u2502', traceColor, bg);
        var cornerHtoV = new TextCell('\u2510', traceColor, bg);
        var cornerVtoH = new TextCell('\u2518', traceColor, bg);

        if (trace.horizontalFirst) {
            drawLineRange(graphics, x0, y0, x1, y0, cell);
            if (y1 != y0) {
                int cornerX = x1;
                int cornerY = y0;
                graphics.setCell(cornerX, cornerY,
                        (y1 > y0) ? cornerHtoV : new TextCell('\u2518', traceColor, bg));
                drawLineRange(graphics, cornerX, cornerY + (y1 > y0 ? 1 : -1), x1, y1, vert);
            }
        } else {
            drawLineRange(graphics, x0, y0, x0, y1, vert);
            if (x1 != x0) {
                int cornerX = x0;
                int cornerY = y1;
                graphics.setCell(cornerX, cornerY,
                        (x1 > x0) ? new TextCell('\u2514', traceColor, bg) : new TextCell('\u2518', traceColor, bg));
                drawLineRange(graphics, cornerX + (x1 > x0 ? 1 : -1), cornerY, x1, y1, cell);
            }
        }
    }

    private void drawLineRange(TextGraphics graphics, int x0, int y0, int x1, int y1, TextCell cell) {
        int stepX = Integer.compare(x1, x0);
        int stepY = Integer.compare(y1, y0);
        int x = x0;
        int y = y0;
        while (true) {
            if (x >= 0 && x < graphics.getSize().columns() && y >= 0 && y < graphics.getSize().rows()) {
                graphics.setCell(x, y, cell);
            }
            if (x == x1 && y == y1) break;
            if (x != x1) x += stepX;
            if (y != y1) y += stepY;
        }
    }

    private void spawnPulses() {
        if (pulses.size() >= MAX_PULSES) return;
        if (traces.isEmpty()) return;
        if (random.nextDouble() >= PULSE_SPAWN_CHANCE) return;

        Trace trace = traces.get(random.nextInt(traces.size()));
        pulses.add(new Pulse(trace, random.nextBoolean()));
    }

    private void advancePulses() {
        pulses.removeIf(pulse -> {
            pulse.progress++;
            return pulse.progress >= pulse.length();
        });
    }

    private void drawPulse(TextGraphics graphics, Pulse pulse, TerminalSize size) {
        int x0 = pulse.trace.gx0 * PAD_SPACING + PAD_SPACING / 2;
        int y0 = pulse.trace.gy0 * PAD_SPACING + PAD_SPACING / 2;
        int x1 = pulse.trace.gx1 * PAD_SPACING + PAD_SPACING / 2;
        int y1 = pulse.trace.gy1 * PAD_SPACING + PAD_SPACING / 2;

        int total = pulse.length();
        int pos = pulse.forward ? pulse.progress : total - 1 - pulse.progress;
        if (pos < 0 || pos >= total) return;

        int px;
        int py;
        if (pulse.trace.horizontalFirst) {
            int horizontalSteps = Math.abs(x1 - x0);
            if (pos <= horizontalSteps) {
                px = Math.min(x0, x1) + pos;
                py = y0;
            } else {
                px = x1;
                py = Math.min(y0, y1) + (pos - horizontalSteps);
            }
        } else {
            int verticalSteps = Math.abs(y1 - y0);
            if (pos <= verticalSteps) {
                px = x0;
                py = Math.min(y0, y1) + pos;
            } else {
                px = Math.min(x0, x1) + (pos - verticalSteps);
                py = y1;
            }
        }

        if (px < 0 || px >= size.columns() || py < 0 || py >= size.rows()) return;

        Color bright = AnsiColor.BRIGHT_GREEN;
        Color medium = AnsiColor.GREEN;
        Color bg = ThemeManager.active().background();

        // Draw bright head and a short fading tail.
        graphics.setCell(px, py, new TextCell('\u2588', bright, bg, SGR.BOLD));
        int tailLen = 3;
        for (int i = 1; i <= tailLen; i++) {
            int tailPos = pulse.forward ? pos - i : pos + i;
            if (tailPos < 0 || tailPos >= total) continue;
            int tx, ty;
            if (pulse.trace.horizontalFirst) {
                int horizontalSteps = Math.abs(x1 - x0);
                if (tailPos <= horizontalSteps) {
                    tx = Math.min(x0, x1) + tailPos;
                    ty = y0;
                } else {
                    tx = x1;
                    ty = Math.min(y0, y1) + (tailPos - horizontalSteps);
                }
            } else {
                int verticalSteps = Math.abs(y1 - y0);
                if (tailPos <= verticalSteps) {
                    tx = x0;
                    ty = Math.min(y0, y1) + tailPos;
                } else {
                    tx = Math.min(x0, x1) + (tailPos - verticalSteps);
                    ty = y1;
                }
            }
            if (tx < 0 || tx >= size.columns() || ty < 0 || ty >= size.rows()) continue;
            Color color = i == 1 ? medium : dimColor(medium, 0.5);
            graphics.setCell(tx, ty, new TextCell('\u2591', color, bg));
        }
    }

    private Color dimColor(Color color, double weight) {
        if (color == AnsiColor.BRIGHT_GREEN) {
            return weight < 0.5 ? AnsiColor.GREEN : AnsiColor.BRIGHT_BLACK;
        }
        if (color == AnsiColor.GREEN) {
            return weight < 0.5 ? AnsiColor.GREEN : AnsiColor.BRIGHT_BLACK;
        }
        if (color == AnsiColor.BRIGHT_BLACK) {
            return AnsiColor.BRIGHT_BLACK;
        }
        return color;
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

    @Override
    public int targetFps() {
        return targetFps;
    }

    /** A single copper trace between two grid pads. */
    public record Trace(int gx0, int gy0, int gx1, int gy1, boolean horizontalFirst) {
        int length() {
            return Math.abs(gx1 - gx0) * PAD_SPACING + Math.abs(gy1 - gy0) * PAD_SPACING;
        }
    }

    /** A packet of electricity moving along a trace. */
    public static final class Pulse {
        final Trace trace;
        final boolean forward;
        int progress;

        Pulse(Trace trace, boolean forward) {
            this.trace = trace;
            this.forward = forward;
        }

        int length() {
            return trace.length();
        }
    }
}
