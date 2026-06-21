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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Animated circuit-board (PCB) background. Generates a grid of orthogonal
 * copper traces, pads, vias, and PCB components (chips, resistors, capacitors,
 * crystals), then sends pulses of electricity flowing along random routes.
 * Uses ANSI colors only for broad terminal compatibility.
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
    private static final int TAIL_LEN = 3;
    private static final double COMPONENT_CHANCE = 0.25;

    private final Random random = new Random();
    private TerminalSize preferredSize;

    private volatile int targetFps = 2;
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
    private final Set<Integer> viaPositions = new HashSet<>();
    private final List<PcbComponent> components = new ArrayList<>();
    private final Set<Integer> componentCells = new HashSet<>();

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
        viaPositions.clear();
        components.clear();
        componentCells.clear();
    }

    public List<Trace> getTraces() {
        return List.copyOf(traces);
    }

    public List<Pulse> getPulses() {
        return List.copyOf(pulses);
    }

    public List<PcbComponent> getComponents() {
        return List.copyOf(components);
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
            viaPositions.clear();
            components.clear();
            componentCells.clear();
            generateComponents();
            generateTraces();
            generateVias();
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
        Color viaColor = AnsiColor.BRIGHT_CYAN;

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
                    } else if (viaPositions.contains(gy * gridCols + gx)) {
                        graphics.setCell(x, y, new TextCell('\u00B0', viaColor, bg));
                    }
                }
            }
        }

        // Draw PCB components (chips, resistors, etc.) over the traces.
        for (PcbComponent comp : components) {
            drawComponent(graphics, comp, size, bg);
        }

        advancePulses();
        spawnPulses();

        // Draw electricity pulses over everything.
        for (Pulse pulse : pulses) {
            drawPulse(graphics, pulse, size);
        }
    }

    // ── Component generation ──────────────────────────────────────────────

    private void generateComponents() {
        components.clear();
        componentCells.clear();
        if (gridCols < 4 || gridRows < 3) return;

        for (int gy = 0; gy <= gridRows - 3; gy++) {
            for (int gx = 0; gx <= gridCols - 4; gx++) {
                if (random.nextDouble() >= COMPONENT_CHANCE) continue;
                // Check this area is free
                int w = 4;  // max footprint for overlap check
                int h = 3;
                if (isAreaOccupied(gx, gy, w, h)) continue;

                PcbComponent.Type type = pickComponentType();
                int tw = type.width;
                int th = type.height;
                if (gx + tw > gridCols || gy + th > gridRows) continue;

                // Mark cells as occupied
                for (int dy = 0; dy < th; dy++) {
                    for (int dx = 0; dx < tw; dx++) {
                        componentCells.add((gy + dy) * gridCols + (gx + dx));
                    }
                }
                components.add(new PcbComponent(type, gx, gy));
            }
        }
    }

    private boolean isAreaOccupied(int gx, int gy, int w, int h) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                if (componentCells.contains((gy + dy) * gridCols + (gx + dx))) return true;
            }
        }
        return false;
    }

    private PcbComponent.Type pickComponentType() {
        var types = PcbComponent.Type.values();
        return types[random.nextInt(types.length)];
    }

    // ── Trace generation ──────────────────────────────────────────────────

    private void generateTraces() {
        traces.clear();
        if (gridCols <= 1 || gridRows <= 1) return;

        for (int gy = 0; gy < gridRows; gy++) {
            for (int gx = 0; gx < gridCols; gx++) {
                // Skip trace origins that are inside a component
                if (componentCells.contains(gy * gridCols + gx)) continue;

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

                // Skip if endpoint is inside a component
                if (componentCells.contains(gy1 * gridCols + gx1)) continue;

                traces.add(new Trace(gx, gy, gx1, gy1, random.nextBoolean()));
            }
        }
    }

    private void generateVias() {
        for (int gy = 0; gy < gridRows; gy++) {
            for (int gx = 0; gx < gridCols; gx++) {
                if (componentCells.contains(gy * gridCols + gx)) continue;
                boolean hasTrace = false;
                for (Trace trace : traces) {
                    if (trace.gx0 == gx && trace.gy0 == gy
                            || trace.gx1 == gx && trace.gy1 == gy) {
                        hasTrace = true;
                        break;
                    }
                }
                if (!hasTrace && random.nextDouble() < 0.15) {
                    viaPositions.add(gy * gridCols + gx);
                }
            }
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────

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

    private void drawComponent(TextGraphics graphics, PcbComponent comp, TerminalSize size, Color bg) {
        int ox = comp.gx * PAD_SPACING + PAD_SPACING / 2;
        int oy = comp.gy * PAD_SPACING + PAD_SPACING / 2;
        int w = comp.type.width * PAD_SPACING;
        int h = comp.type.height * PAD_SPACING;

        switch (comp.type) {
            case CHIP -> drawChip(graphics, ox, oy, w, h, size, bg);
            case RESISTOR -> drawResistor(graphics, ox, oy, w, h, size, bg);
            case CAPACITOR -> drawCapacitor(graphics, ox, oy, w, h, size, bg);
            case CRYSTAL -> drawCrystal(graphics, ox, oy, w, h, size, bg);
            case DIODE -> drawDiode(graphics, ox, oy, w, h, size, bg);
            case LED -> drawLed(graphics, ox, oy, w, h, size, bg);
            case TRANSISTOR -> drawTransistor(graphics, ox, oy, w, h, size, bg);
            case INDUCTOR -> drawInductor(graphics, ox, oy, w, h, size, bg);
        }
    }

    /**
     * Draws an IC/chip as a rectangle with pins. Example (5×3 grid → 20×12 chars):
     * <pre>
     *  ┌──────────────┐
     *  │  ▓▓▓▓▓▓▓▓▓▓  │
     *  │  ▓▓ IC  ▓▓  │
     *  │  ▓▓▓▓▓▓▓▓▓▓  │
     *  └──────────────┘
     * ─┤├────────────┤├─
     * </pre>
     */
    private void drawChip(TextGraphics graphics, int ox, int oy, int w, int h, TerminalSize size, Color bg) {
        Color bodyColor = AnsiColor.BRIGHT_YELLOW;
        Color pinColor = AnsiColor.GREEN;
        Color labelColor = AnsiColor.BRIGHT_WHITE;
        int cols = size.columns();
        int rows = size.rows();

        // Outline (single-line box)
        var hLine = new TextCell('\u2500', bodyColor, bg);
        var vLine = new TextCell('\u2502', bodyColor, bg);
        var tl = new TextCell('\u250C', bodyColor, bg);
        var tr = new TextCell('\u2510', bodyColor, bg);
        var bl = new TextCell('\u2514', bodyColor, bg);
        var br = new TextCell('\u2518', bodyColor, bg);
        var fill = new TextCell('\u2592', bodyColor, bg); // medium shade fill

        // Top edge
        for (int x = ox + 1; x < ox + w; x++) {
            if (inBounds(x, oy, cols, rows)) graphics.setCell(x, oy, hLine);
        }
        // Bottom edge
        int botY = oy + h;
        for (int x = ox + 1; x < ox + w; x++) {
            if (inBounds(x, botY, cols, rows)) graphics.setCell(x, botY, hLine);
        }
        // Left and right edges, plus fill
        for (int y = oy + 1; y < botY; y++) {
            if (inBounds(ox, y, cols, rows)) graphics.setCell(ox, y, vLine);
            if (inBounds(ox + w, y, cols, rows)) graphics.setCell(ox + w, y, vLine);
            // Fill interior with shade
            for (int x = ox + 1; x < ox + w; x++) {
                if (inBounds(x, y, cols, rows)) graphics.setCell(x, y, fill);
            }
        }
        // Corners
        if (inBounds(ox, oy, cols, rows)) graphics.setCell(ox, oy, tl);
        if (inBounds(ox + w, oy, cols, rows)) graphics.setCell(ox + w, oy, tr);
        if (inBounds(ox, botY, cols, rows)) graphics.setCell(ox, botY, bl);
        if (inBounds(ox + w, botY, cols, rows)) graphics.setCell(ox + w, botY, br);

        // Label "IC" centered
        int labelX = ox + w / 2 - 1;
        int labelY = oy + h / 2;
        if (inBounds(labelX, labelY, cols, rows))
            graphics.setCell(labelX, labelY, new TextCell('I', labelColor, bg, SGR.BOLD));
        if (inBounds(labelX + 1, labelY, cols, rows))
            graphics.setCell(labelX + 1, labelY, new TextCell('C', labelColor, bg, SGR.BOLD));

        // Pin notch (small dot on top-left to indicate pin 1)
        if (inBounds(ox + 1, oy + 1, cols, rows))
            graphics.setCell(ox + 1, oy + 1, new TextCell('\u00B0', pinColor, bg));

        // Pins extending left and right from mid-row
        int pinY = oy + h / 2;
        if (inBounds(ox - 2, pinY, cols, rows))
            graphics.setCell(ox - 2, pinY, new TextCell('\u2500', pinColor, bg));
        if (inBounds(ox - 1, pinY, cols, rows))
            graphics.setCell(ox - 1, pinY, new TextCell('\u2500', pinColor, bg));
        if (inBounds(ox + w + 1, pinY, cols, rows))
            graphics.setCell(ox + w + 1, pinY, new TextCell('\u2500', pinColor, bg));
        if (inBounds(ox + w + 2, pinY, cols, rows))
            graphics.setCell(ox + w + 2, pinY, new TextCell('\u2500', pinColor, bg));
    }

    /**
     * Draws a resistor as a small filled rectangle with leads.
     * <pre>
     * ───╞═══╡───
     * </pre>
     */
    private void drawResistor(TextGraphics graphics, int ox, int oy, int w, int h, TerminalSize size, Color bg) {
        Color bodyColor = AnsiColor.BRIGHT_YELLOW;
        Color leadColor = AnsiColor.GREEN;
        int cols = size.columns();
        int rows = size.rows();
        int midY = oy + h / 2;

        // Body: shaded block
        var body = new TextCell('\u2593', bodyColor, bg); // dark shade
        int bodyStart = ox + 2;
        int bodyEnd = ox + w - 2;
        for (int x = bodyStart; x < bodyEnd; x++) {
            if (inBounds(x, midY, cols, rows)) graphics.setCell(x, midY, body);
        }

        // End caps
        if (inBounds(bodyStart - 1, midY, cols, rows))
            graphics.setCell(bodyStart - 1, midY, new TextCell('\u251C', bodyColor, bg)); // ├
        if (inBounds(bodyEnd, midY, cols, rows))
            graphics.setCell(bodyEnd, midY, new TextCell('\u2524', bodyColor, bg)); // ┤

        // Leads
        for (int x = ox; x < bodyStart - 1; x++) {
            if (inBounds(x, midY, cols, rows))
                graphics.setCell(x, midY, new TextCell('\u2500', leadColor, bg));
        }
        for (int x = bodyEnd + 1; x <= ox + w; x++) {
            if (inBounds(x, midY, cols, rows))
                graphics.setCell(x, midY, new TextCell('\u2500', leadColor, bg));
        }
    }

    /**
     * Draws a capacitor as two parallel plates with leads.
     * <pre>
     * ───│▓▓│───
     * </pre>
     */
    private void drawCapacitor(TextGraphics graphics, int ox, int oy, int w, int h, TerminalSize size, Color bg) {
        Color plateColor = AnsiColor.BRIGHT_CYAN;
        Color leadColor = AnsiColor.GREEN;
        Color fillColor = AnsiColor.CYAN;
        int cols = size.columns();
        int rows = size.rows();
        int midY = oy + h / 2;

        int plate1 = ox + w / 2 - 1;
        int plate2 = ox + w / 2;

        // Plates (vertical bars)
        if (inBounds(plate1, midY, cols, rows))
            graphics.setCell(plate1, midY, new TextCell('\u2502', plateColor, bg, SGR.BOLD));
        if (inBounds(plate2, midY, cols, rows))
            graphics.setCell(plate2, midY, new TextCell('\u2502', plateColor, bg, SGR.BOLD));

        // Fill between plates
        // (no fill — plates are adjacent, representing a non-polarized cap)

        // Leads
        for (int x = ox; x < plate1; x++) {
            if (inBounds(x, midY, cols, rows))
                graphics.setCell(x, midY, new TextCell('\u2500', leadColor, bg));
        }
        for (int x = plate2 + 1; x <= ox + w; x++) {
            if (inBounds(x, midY, cols, rows))
                graphics.setCell(x, midY, new TextCell('\u2500', leadColor, bg));
        }
    }

    /**
     * Draws a crystal oscillator as a small rectangle with "XT" label.
     * <pre>
     *  ┌────────┐
     *  │ ▓▓XT▓▓ │
     *  └────────┘
     * </pre>
     */
    private void drawCrystal(TextGraphics graphics, int ox, int oy, int w, int h, TerminalSize size, Color bg) {
        Color bodyColor = AnsiColor.BRIGHT_CYAN;
        Color labelColor = AnsiColor.BRIGHT_WHITE;
        int cols = size.columns();
        int rows = size.rows();

        var hLine = new TextCell('\u2500', bodyColor, bg);
        var vLine = new TextCell('\u2502', bodyColor, bg);
        var tl = new TextCell('\u250C', bodyColor, bg);
        var tr = new TextCell('\u2510', bodyColor, bg);
        var bl = new TextCell('\u2514', bodyColor, bg);
        var br = new TextCell('\u2518', bodyColor, bg);
        var fill = new TextCell('\u2591', bodyColor, bg); // light shade

        int botY = oy + h;

        // Top and bottom edges
        for (int x = ox + 1; x < ox + w; x++) {
            if (inBounds(x, oy, cols, rows)) graphics.setCell(x, oy, hLine);
            if (inBounds(x, botY, cols, rows)) graphics.setCell(x, botY, hLine);
        }
        // Sides and fill
        for (int y = oy + 1; y < botY; y++) {
            if (inBounds(ox, y, cols, rows)) graphics.setCell(ox, y, vLine);
            if (inBounds(ox + w, y, cols, rows)) graphics.setCell(ox + w, y, vLine);
            for (int x = ox + 1; x < ox + w; x++) {
                if (inBounds(x, y, cols, rows)) graphics.setCell(x, y, fill);
            }
        }
        // Corners
        if (inBounds(ox, oy, cols, rows)) graphics.setCell(ox, oy, tl);
        if (inBounds(ox + w, oy, cols, rows)) graphics.setCell(ox + w, oy, tr);
        if (inBounds(ox, botY, cols, rows)) graphics.setCell(ox, botY, bl);
        if (inBounds(ox + w, botY, cols, rows)) graphics.setCell(ox + w, botY, br);

        // Label "XT"
        int labelX = ox + w / 2 - 1;
        int labelY = oy + h / 2;
        if (inBounds(labelX, labelY, cols, rows))
            graphics.setCell(labelX, labelY, new TextCell('X', labelColor, bg, SGR.BOLD));
        if (inBounds(labelX + 1, labelY, cols, rows))
            graphics.setCell(labelX + 1, labelY, new TextCell('T', labelColor, bg, SGR.BOLD));
    }

    /**
     * Draws a diode as a triangle pointing into a cathode bar.
     * <pre>
     * ───►│───
     * </pre>
     */
    private void drawDiode(TextGraphics graphics, int ox, int oy, int w, int h, TerminalSize size, Color bg) {
        Color bodyColor = AnsiColor.BRIGHT_YELLOW;
        Color leadColor = AnsiColor.GREEN;
        int cols = size.columns();
        int rows = size.rows();
        int midY = oy + h / 2;

        // Triangle (anode) → bar (cathode)
        int triX = ox + w / 3;
        int barX = triX + 2;
        if (inBounds(triX, midY, cols, rows))
            graphics.setCell(triX, midY, new TextCell('\u25BA', bodyColor, bg, SGR.BOLD)); // ►
        if (inBounds(barX, midY, cols, rows))
            graphics.setCell(barX, midY, new TextCell('\u2502', bodyColor, bg, SGR.BOLD)); // │

        // Leads
        for (int x = ox; x < triX; x++) {
            if (inBounds(x, midY, cols, rows))
                graphics.setCell(x, midY, new TextCell('\u2500', leadColor, bg));
        }
        for (int x = barX + 1; x <= ox + w; x++) {
            if (inBounds(x, midY, cols, rows))
                graphics.setCell(x, midY, new TextCell('\u2500', leadColor, bg));
        }
    }

    /**
     * Draws an LED as a diode with a small circle around it.
     * <pre>
     * ───►│───
     *  °
     * </pre>
     */
    private void drawLed(TextGraphics graphics, int ox, int oy, int w, int h, TerminalSize size, Color bg) {
        Color bodyColor = AnsiColor.BRIGHT_RED;
        Color leadColor = AnsiColor.GREEN;
        int cols = size.columns();
        int rows = size.rows();
        int midY = oy + h / 2;

        int triX = ox + w / 3;
        int barX = triX + 2;
        if (inBounds(triX, midY, cols, rows))
            graphics.setCell(triX, midY, new TextCell('\u25BA', bodyColor, bg, SGR.BOLD)); // ►
        if (inBounds(barX, midY, cols, rows))
            graphics.setCell(barX, midY, new TextCell('\u2502', bodyColor, bg, SGR.BOLD)); // │

        // LED indicator dot below
        if (inBounds(triX, midY + 1, cols, rows))
            graphics.setCell(triX, midY + 1, new TextCell('\u00B0', bodyColor, bg));

        // Leads
        for (int x = ox; x < triX; x++) {
            if (inBounds(x, midY, cols, rows))
                graphics.setCell(x, midY, new TextCell('\u2500', leadColor, bg));
        }
        for (int x = barX + 1; x <= ox + w; x++) {
            if (inBounds(x, midY, cols, rows))
                graphics.setCell(x, midY, new TextCell('\u2500', leadColor, bg));
        }
    }

    /**
     * Draws a transistor: three leads (base, collector, emitter) converging
     * to a vertical body line.
     * <pre>
     *      ┌──
     *  ────┤
     *      └──
     * </pre>
     */
    private void drawTransistor(TextGraphics graphics, int ox, int oy, int w, int h, TerminalSize size, Color bg) {
        Color bodyColor = AnsiColor.BRIGHT_YELLOW;
        Color leadColor = AnsiColor.GREEN;
        int cols = size.columns();
        int rows = size.rows();

        int bodyX = ox + w / 2;
        int topY = oy + h / 4;
        int midY = oy + h / 2;
        int botY = oy + 3 * h / 4;

        // Vertical body line
        for (int y = topY; y <= botY; y++) {
            if (inBounds(bodyX, y, cols, rows))
                graphics.setCell(bodyX, y, new TextCell('\u2502', bodyColor, bg, SGR.BOLD));
        }

        // Base lead (left → body center)
        for (int x = ox; x < bodyX; x++) {
            if (inBounds(x, midY, cols, rows))
                graphics.setCell(x, midY, new TextCell('\u2500', leadColor, bg));
        }

        // Collector lead (body top → right)
        for (int x = bodyX; x <= ox + w; x++) {
            if (inBounds(x, topY, cols, rows))
                graphics.setCell(x, topY, new TextCell('\u2500', leadColor, bg));
        }

        // Emitter lead (body bottom → right)
        for (int x = bodyX; x <= ox + w; x++) {
            if (inBounds(x, botY, cols, rows))
                graphics.setCell(x, botY, new TextCell('\u2500', leadColor, bg));
        }
    }

    /**
     * Draws an inductor as a series of bumps (coils).
     * <pre>
     * ───∩∩∩───
     * </pre>
     */
    private void drawInductor(TextGraphics graphics, int ox, int oy, int w, int h, TerminalSize size, Color bg) {
        Color bodyColor = AnsiColor.BRIGHT_CYAN;
        Color leadColor = AnsiColor.GREEN;
        int cols = size.columns();
        int rows = size.rows();
        int midY = oy + h / 2;

        int coilStart = ox + 2;
        int coilEnd = ox + w - 2;
        var coil = new TextCell('\u2229', bodyColor, bg, SGR.BOLD); // ∩

        for (int x = coilStart; x < coilEnd; x++) {
            if (inBounds(x, midY, cols, rows))
                graphics.setCell(x, midY, coil);
        }

        // Leads
        for (int x = ox; x < coilStart; x++) {
            if (inBounds(x, midY, cols, rows))
                graphics.setCell(x, midY, new TextCell('\u2500', leadColor, bg));
        }
        for (int x = coilEnd; x <= ox + w; x++) {
            if (inBounds(x, midY, cols, rows))
                graphics.setCell(x, midY, new TextCell('\u2500', leadColor, bg));
        }
    }

    // ── Pulses ────────────────────────────────────────────────────────────

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
            // Keep the pulse alive past the trace end so the tail can fade out
            // gradually instead of vanishing abruptly.
            return pulse.progress >= pulse.length() + TAIL_LEN;
        });
    }

    private void drawPulse(TextGraphics graphics, Pulse pulse, TerminalSize size) {
        int x0 = pulse.trace.gx0 * PAD_SPACING + PAD_SPACING / 2;
        int y0 = pulse.trace.gy0 * PAD_SPACING + PAD_SPACING / 2;
        int x1 = pulse.trace.gx1 * PAD_SPACING + PAD_SPACING / 2;
        int y1 = pulse.trace.gy1 * PAD_SPACING + PAD_SPACING / 2;

        int total = pulse.length();
        int pos = pulse.forward ? pulse.progress : total - 1 - pulse.progress;

        Color bright = AnsiColor.BRIGHT_GREEN;
        Color medium = AnsiColor.GREEN;
        Color bg = ThemeManager.active().background();

        // Draw bright head only while it's still on the trace.
        // Once progress >= total (head passed the end), only the tail remains
        // and fades out over TAIL_LEN frames.
        if (pos >= 0 && pos < total) {
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
            if (px >= 0 && px < size.columns() && py >= 0 && py < size.rows()) {
                graphics.setCell(px, py, new TextCell('\u2588', bright, bg, SGR.BOLD));
            }
        }

        // Draw fading tail behind the head.
        int tailLen = TAIL_LEN;
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
            // Tail fades: first cell medium green, rest dim.
            // During fade-out phase (pos >= total), the head is gone so
            // the tail shortens naturally as cells fall off the end.
            Color color = i == 1 ? medium : AnsiColor.BRIGHT_BLACK;
            graphics.setCell(tx, ty, new TextCell('\u2591', color, bg));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static boolean inBounds(int x, int y, int cols, int rows) {
        return x >= 0 && x < cols && y >= 0 && y < rows;
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

    /** A PCB component placed on the grid. */
    public record PcbComponent(Type type, int gx, int gy) {

        /** Component types with their grid-cell dimensions. */
        public enum Type {
            /** IC chip: rectangle with pins, "IC" label. 3×2 grid (12×8 chars). */
            CHIP(3, 2),
                /** Resistor: shaded body with leads. 5×1 grid. */
                RESISTOR(5, 1),
                /** Capacitor: two parallel plates with leads. 4×1 grid. */
                CAPACITOR(4, 1),
                /** Crystal oscillator: rectangle with "XT" label. 5×2 grid. */
                CRYSTAL(5, 2),
                /** Diode: triangle pointing into a bar. 3×1 grid. */
                DIODE(3, 1),
                /** LED: diode with a circle indicator. 3×1 grid. */
                LED(3, 1),
                /** Transistor: three leads converging to a body. 3×3 grid. */
                TRANSISTOR(3, 3),
                /** Inductor: coiled line. 5×1 grid. */
                INDUCTOR(5, 1);

                public final int width;
                public final int height;

                Type(int width, int height) {
                    this.width = width;
                    this.height = height;
                }
            }
    }
}
