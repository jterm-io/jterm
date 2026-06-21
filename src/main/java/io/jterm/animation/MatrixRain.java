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

import java.util.Random;

/**
 * Classic Matrix digital rain background. Columns of falling characters leave a
 * fading green trail behind a bright green head. Can be used as a full-frame
 * background layer or as a regular component.
 */
public class MatrixRain extends AbstractComponent implements AnimatedBackground {
    private static final int MIN_FPS = 1;
    private static final int MAX_FPS = 60;
    private static final long NS_PER_MS = 1_000_000L;
    private static final double MIN_SPEED = 0.3;
    private static final double MAX_SPEED = 1.0;
    private static final int MIN_TRAIL = 5;
    private static final int MAX_TRAIL = 20;
    private static final double CHAR_CHANGE_CHANCE = 0.30;
    private static final double COLUMN_INACTIVE_CHANCE = 0.20;
    private static final double COLOR_VARIANT_CHANCE = 0.05;

    private static final char[] HALFWIDTH_KATAKANA = {
        '\uff71', '\uff72', '\uff73', '\uff74', '\uff75', '\uff76', '\uff77', '\uff78', '\uff79',
        '\uff7a', '\uff7b', '\uff7c', '\uff7d', '\uff7e', '\uff7f', '\uff80', '\uff81', '\uff82',
        '\uff83', '\uff84', '\uff85', '\uff86', '\uff87', '\uff88'
    };

    private final Random random = new Random();
    private final boolean unicodeMode;
    private final boolean allowColorVariants;
    private TerminalSize preferredSize;

    private volatile int targetFps = 15;
    private volatile boolean paused = false;
    private volatile boolean running = false;
    private volatile long lastTickNanos = -1;
    private volatile long accumulatedNs;
    private volatile long frameElapsedMs;
    private volatile int frame;

    private Column[] columns;
    private Color backgroundColor;

    public MatrixRain(TerminalSize preferredSize) {
        this(preferredSize, true, true);
    }

    public MatrixRain(TerminalSize preferredSize, boolean unicodeMode, boolean allowColorVariants) {
        this.preferredSize = preferredSize;
        this.unicodeMode = unicodeMode;
        this.allowColorVariants = allowColorVariants;
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
        columns = null;
    }

    public boolean isUnicodeMode() {
        return unicodeMode;
    }

    public boolean isAllowColorVariants() {
        return allowColorVariants;
    }

    /**
     * Advances the animation by the given absolute wall-clock time in nanoseconds.
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
        if (columns == null || columns.length != newSize.columns()) {
            columns = new Column[newSize.columns()];
            for (int i = 0; i < columns.length; i++) {
                columns[i] = new Column(newSize.rows(), random, unicodeMode, allowColorVariants);
            }
        } else {
            for (Column column : columns) {
                column.setRows(newSize.rows());
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
        backgroundColor = theme.background();
        graphics.fillRectangle(0, 0, size.columns(), size.rows(),
                new TextCell(' ', theme.foreground(), backgroundColor));

        if (columns == null || columns.length != size.columns()) {
            onResize(size);
        }

        for (int x = 0; x < size.columns(); x++) {
            Column column = columns[x];
            if (column == null) continue;
            column.advance(size.rows());
            column.draw(graphics, x, size.rows(), backgroundColor);
        }
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

    /** Package-private for tests: access a column directly. */
    Column getColumn(int index) {
        if (columns == null) return null;
        return columns[index];
    }

    int getColumnCount() {
        return columns == null ? 0 : columns.length;
    }

    static final class Column {
        private final Random random;
        private final boolean unicodeMode;
        private final boolean allowColorVariants;
        private final Color accent;
        private final Color[] trailPalette;
        private final Color darkColor;

        private double y;
        private double speed;
        private int trailLength;
        private boolean active;
        private int rows;
        private char currentChar;

        Column(int rows, Random random, boolean unicodeMode, boolean allowColorVariants) {
            this.rows = rows;
            this.random = random;
            this.unicodeMode = unicodeMode;
            this.allowColorVariants = allowColorVariants;
            this.accent = pickAccent();
            this.trailPalette = buildTrailPalette(accent);
            this.darkColor = blendToBlack(accent, 0.15);
            reset(true);
        }

        void setRows(int rows) {
            this.rows = rows;
            if (y >= rows) {
                reset(false);
            }
        }

        void activate() {
            active = true;
        }

        void advance(int availableRows) {
            if (!active) {
                if (random.nextDouble() < 0.02) {
                    active = true;
                    reset(false);
                }
                return;
            }

            y += speed;
            if (random.nextDouble() < CHAR_CHANGE_CHANCE) {
                currentChar = randomChar();
            }
            if (y - trailLength > availableRows) {
                reset(false);
            }
        }

        void draw(TextGraphics graphics, int x, int availableRows, Color background) {
            if (!active) return;

            int headY = (int) Math.floor(y);
            if (headY >= 0 && headY < availableRows) {
                graphics.setCell(x, headY, new TextCell(currentChar, accent, background, SGR.BOLD));
            }

            for (int offset = 1; offset <= trailLength; offset++) {
                int trailY = headY - offset;
                if (trailY < 0 || trailY >= availableRows) continue;
                Color color = trailColor(offset);
                graphics.setCell(x, trailY, new TextCell(randomChar(), color, background));
            }
        }

        double getY() {
            return y;
        }

        double getSpeed() {
            return speed;
        }

        int getTrailLength() {
            return trailLength;
        }

        boolean isActive() {
            return active;
        }

        char getCurrentChar() {
            return currentChar;
        }

        Color getAccent() {
            return accent;
        }

        void reset(boolean initial) {
            this.speed = MIN_SPEED + random.nextDouble() * (MAX_SPEED - MIN_SPEED);
            this.trailLength = MIN_TRAIL + random.nextInt(MAX_TRAIL - MIN_TRAIL + 1);
            this.currentChar = randomChar();
            if (initial) {
                this.y = random.nextInt(Math.max(1, rows));
                this.active = random.nextDouble() >= COLUMN_INACTIVE_CHANCE;
            } else {
                this.y = -trailLength + random.nextInt(Math.max(1, trailLength));
                this.active = true;
            }
        }

        private Color pickAccent() {
            if (!allowColorVariants || random.nextDouble() >= COLOR_VARIANT_CHANCE) {
                return AnsiColor.BRIGHT_GREEN;
            }
            double roll = random.nextDouble();
            if (roll < 0.6) return AnsiColor.BRIGHT_CYAN;
            return AnsiColor.BRIGHT_WHITE;
        }

        private Color[] buildTrailPalette(Color accent) {
            if (accent == AnsiColor.BRIGHT_GREEN) {
                return new Color[] {
                        AnsiColor.GREEN,
                        AnsiColor.GREEN,
                        blendToBlack(AnsiColor.GREEN, 0.5),
                        blendToBlack(AnsiColor.GREEN, 0.3),
                        blendToBlack(AnsiColor.GREEN, 0.15)
                };
            }
            if (accent == AnsiColor.BRIGHT_CYAN) {
                return new Color[] {
                        AnsiColor.CYAN,
                        AnsiColor.CYAN,
                        blendToBlack(AnsiColor.CYAN, 0.5),
                        blendToBlack(AnsiColor.CYAN, 0.3),
                        blendToBlack(AnsiColor.CYAN, 0.15)
                };
            }
            return new Color[] {
                    AnsiColor.WHITE,
                    AnsiColor.WHITE,
                    blendToBlack(AnsiColor.WHITE, 0.5),
                    blendToBlack(AnsiColor.WHITE, 0.3),
                    blendToBlack(AnsiColor.WHITE, 0.15)
            };
        }

        private Color trailColor(int offset) {
            if (offset == 1) return trailPalette[0];
            if (offset == 2) return trailPalette[1];
            if (offset == 3) return trailPalette[2];
            if (offset == 4) return trailPalette[3];
            return darkColor;
        }

        private char randomChar() {
            if (unicodeMode) {
                return HALFWIDTH_KATAKANA[random.nextInt(HALFWIDTH_KATAKANA.length)];
            }
            int roll = random.nextInt(2);
            if (roll == 0) {
                return (char) (0x21 + random.nextInt(0x7E - 0x21 + 1));
            }
            return (char) (0xB0 + random.nextInt(0xDF - 0xB0 + 1));
        }

        private static Color blendToBlack(Color color, double weight) {
            if (color instanceof AnsiColor ansi) {
                // Snap to nearest ANSI color for terminal compatibility (no 24-bit RGB).
                return switch (ansi) {
                    case GREEN, BRIGHT_GREEN -> AnsiColor.blendAnsi(AnsiColor.BLACK, AnsiColor.GREEN, weight);
                    case CYAN, BRIGHT_CYAN -> AnsiColor.blendAnsi(AnsiColor.BLACK, AnsiColor.CYAN, weight);
                    case WHITE, BRIGHT_WHITE -> AnsiColor.blendAnsi(AnsiColor.BLACK, AnsiColor.WHITE, weight);
                    default -> AnsiColor.BLACK;
                };
            }
            return AnsiColor.BLACK;
        }
    }
}
