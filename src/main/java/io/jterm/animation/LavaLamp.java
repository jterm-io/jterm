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
 * Metaball-style lava lamp background. Warm-colored blobs drift slowly
 * upward and downward, merging and splitting as they pass each other.
 * Uses only ANSI colors and CP437-safe glyphs for broad terminal compatibility.
 *
 * <p>This is an {@link AnimatedBackground} and a regular Component.</p>
 */
public class LavaLamp extends AbstractComponent implements AnimatedBackground {
    private static final int MIN_FPS = 1;
    private static final int MAX_FPS = 60;
    private static final long NS_PER_MS = 1_000_000L;

    private static final int BALL_COUNT = 5;
    private static final double THRESHOLD = 1.0;
    private static final double EDGE_THRESHOLD = 0.7;
    private static final double CENTER_GLYPH_THRESHOLD = 1.5;

    private final Random random = new Random();
    private final TerminalSize preferredSize;

    private volatile int targetFps = 8;
    private volatile boolean paused = false;
    private volatile boolean running = false;

    private volatile long lastTickNanos = -1;
    private volatile long accumulatedNs;
    private volatile long frameElapsedMs;
    private volatile int frame;
    private volatile long totalMs;

    private Metaball[] balls;
    private TerminalSize lastSize;

    public LavaLamp(TerminalSize preferredSize) {
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

    public long getTotalMs() {
        return totalMs;
    }

    public void resetFrame() {
        frame = 0;
        lastTickNanos = -1;
        accumulatedNs = 0;
        totalMs = 0;
        balls = null;
    }

    /** Package-private for tests. */
    Metaball[] getBalls() {
        return balls;
    }

    @Override
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
        totalMs = nowNanos / NS_PER_MS;
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
        this.lastSize = newSize;
        if (balls == null) {
            balls = new Metaball[BALL_COUNT];
            for (int i = 0; i < balls.length; i++) {
                balls[i] = new Metaball(newSize, random, i);
            }
        } else {
            for (Metaball ball : balls) {
                ball.setSize(newSize);
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

        Color black = AnsiColor.BLACK;
        graphics.fillRectangle(0, 0, size.columns(), size.rows(),
                new TextCell(' ', AnsiColor.DEFAULT, black));

        if (balls == null || balls.length != BALL_COUNT) {
            onResize(size);
        }

        for (Metaball ball : balls) {
            ball.advance(frameElapsedMs);
        }

        int rows = size.rows();
        int cols = size.columns();
        double[][] field = new double[rows][cols];

        for (Metaball ball : balls) {
            double r = ball.radius;
            double rSq = r * r;
            int range = (int) Math.ceil(r + 3.0);
            int minX = Math.max(0, (int) Math.floor(ball.getX()) - range);
            int maxX = Math.min(cols - 1, (int) Math.ceil(ball.getX()) + range);
            int minY = Math.max(0, (int) Math.floor(ball.getY()) - range);
            int maxY = Math.min(rows - 1, (int) Math.ceil(ball.getY()) + range);
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    double dx = x - ball.getX();
                    double dy = y - ball.getY();
                    double distSq = dx * dx + dy * dy;
                    if (distSq < 0.5) distSq = 0.5;
                    field[y][x] += rSq / distSq;
                }
            }
        }

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                double f = field[y][x];
                if (f > THRESHOLD) {
                    Color fg;
                    Color bg = black;
                    SGR mod = null;
                    char ch;

                    if (f >= CENTER_GLYPH_THRESHOLD) {
                        ch = (char) 0xDB; // full block
                        if (f >= 2.5) {
                            fg = AnsiColor.BRIGHT_WHITE;
                        } else if (f >= 2.0) {
                            fg = AnsiColor.BRIGHT_YELLOW;
                            mod = SGR.BOLD;
                        } else {
                            fg = AnsiColor.BRIGHT_RED;
                            mod = SGR.BOLD;
                        }
                    } else if (f >= EDGE_THRESHOLD) {
                        ch = (char) 0xB0; // light shade
                        fg = AnsiColor.YELLOW;
                    } else {
                        ch = (char) 0xB0;
                        fg = AnsiColor.RED;
                    }

                    var cell = mod == null
                            ? new TextCell(ch, fg, bg)
                            : new TextCell(ch, fg, bg, mod);
                    graphics.setCell(x, y, cell);
                }
            }
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

    @Override
    public TerminalSize lastSize() {
        return lastSize;
    }

    static final class Metaball {
        private static final double MIN_RADIUS = 3.0;
        private static final double MAX_RADIUS = 6.0;
        private static final double MIN_SPEED = 0.15;
        private static final double MAX_SPEED = 0.45;
        private static final double X_SWING = 1.2;
        private static final double PHASE_SPEED = 0.0012;
        private static final double WRAP_PAD = 2.0;

        private final Random random;
        private final int index;

        private double radius;
        private double vy;
        private double phase;
        private double baseX;
        private double x;
        private double y;
        private int cols;
        private int rows;

        Metaball(TerminalSize size, Random random, int index) {
            this.random = random;
            this.index = index;
            this.cols = size.columns();
            this.rows = size.rows();
            reset(true);
        }

        void setSize(TerminalSize size) {
            this.cols = size.columns();
            this.rows = size.rows();
            if (x >= cols) x = cols - 1;
            if (y >= rows + WRAP_PAD) reset(false);
        }

        void reset(boolean initial) {
            this.radius = MIN_RADIUS + random.nextDouble() * (MAX_RADIUS - MIN_RADIUS);
            this.vy = MIN_SPEED + random.nextDouble() * (MAX_SPEED - MIN_SPEED);
            if (index % 2 == 0) this.vy = -this.vy;
            this.phase = random.nextDouble() * Math.PI * 2;
            this.baseX = radius + random.nextDouble() * Math.max(1, cols - 2 * radius - 1);
            if (initial) {
                this.y = radius + random.nextDouble() * Math.max(1, rows - 2 * radius - 1);
            } else {
                this.y = vy > 0 ? -radius - WRAP_PAD : rows + radius + WRAP_PAD;
            }
            updateX();
        }

        void advance(long elapsedMs) {
            y += vy * (1.0 + elapsedMs / 80.0);
            phase += PHASE_SPEED * (1.0 + elapsedMs / 80.0);
            updateX();

            if (vy > 0 && y > rows + radius + WRAP_PAD) {
                reset(false);
            } else if (vy < 0 && y < -radius - WRAP_PAD) {
                reset(false);
            }
        }

        private void updateX() {
            x = baseX + Math.sin(phase + index) * X_SWING;
            if (x < radius) x = radius;
            if (x >= cols - radius && cols > 2 * radius) x = cols - radius - 1;
        }

        double getX() {
            return x;
        }

        double getY() {
            return y;
        }

        double getRadius() {
            return radius;
        }

        double getVy() {
            return vy;
        }

        double getPhase() {
            return phase;
        }
    }
}
