package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated fireworks background. Rockets launch from the bottom, arc upward,
 * and explode into colorful particle bursts that expand and fade.
 */
public class Fireworks implements AnimatedBackground {

    private static final int TARGET_FPS = 10;
    private static final int MAX_ROCKETS = 3;
    private static final int MIN_PARTICLES = 8;
    private static final int MAX_PARTICLES = 15;

    private final Random random = new Random();

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private volatile double time;

    private List<Rocket> rockets;
    private List<Particle> particles;

    private int launchCooldown;

    public Fireworks(TerminalSize preferredSize) {
        onResize(preferredSize);
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        lastSize = size;
        if (size.columns() <= 0 || size.rows() <= 0) return;

        renderAtTime(graphics, size);
        time += 0.1;
    }

    /** Visible for tests. */
    public void renderAtTime(TextGraphics graphics, TerminalSize size) {
        if (size.columns() <= 0 || size.rows() <= 0) return;

        // Black background.
        TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bg);

        if (rockets == null) rockets = new ArrayList<>();
        if (particles == null) particles = new ArrayList<>();

        // Launch new rockets if we have room.
        if (rockets.size() < MAX_ROCKETS) {
            if (launchCooldown <= 0) {
                rockets.add(new Rocket(size, random));
                launchCooldown = 5 + random.nextInt(10);
            } else {
                launchCooldown--;
            }
        }

        // Update and draw rockets.
        var rocketIter = rockets.iterator();
        while (rocketIter.hasNext()) {
            Rocket rocket = rocketIter.next();
            rocket.advance();
            int rx = (int) Math.round(rocket.x);
            int ry = (int) Math.round(rocket.y);
            if (ry < 0 || ry >= size.rows() || rx < 0 || rx >= size.columns()) {
                rocketIter.remove();
                continue;
            }
            if (rocket.vy >= 0) {
                // Explode!
                int particleCount = MIN_PARTICLES + random.nextInt(MAX_PARTICLES - MIN_PARTICLES + 1);
                AnsiColor[] colors = {
                    AnsiColor.BRIGHT_RED, AnsiColor.BRIGHT_YELLOW, AnsiColor.BRIGHT_CYAN,
                    AnsiColor.BRIGHT_GREEN, AnsiColor.BRIGHT_MAGENTA
                };
                for (int i = 0; i < particleCount; i++) {
                    AnsiColor color = colors[random.nextInt(colors.length)];
                    particles.add(new Particle(rocket.x, rocket.y, color, random));
                }
                rocketIter.remove();
            } else {
                TextCell trail = new TextCell('|', AnsiColor.BRIGHT_WHITE, AnsiColor.BLACK);
                graphics.setCell(rx, ry, trail);
                // Trail behind.
                if (ry + 1 < size.rows()) {
                    TextCell trailDim = new TextCell('.', AnsiColor.BRIGHT_BLACK, AnsiColor.BLACK);
                    graphics.setCell(rx, ry + 1, trailDim);
                }
            }
        }

        // Update and draw particles.
        var partIter = particles.iterator();
        while (partIter.hasNext()) {
            Particle p = partIter.next();
            p.advance();
            int px = (int) Math.round(p.x);
            int py = (int) Math.round(p.y);
            if (p.life <= 0 || px < 0 || px >= size.columns() || py < 0 || py >= size.rows()) {
                partIter.remove();
                continue;
            }
            char glyph;
            double lifeRatio = (double) p.life / p.maxLife;
            if (lifeRatio > 0.66) {
                glyph = '*';
            } else if (lifeRatio > 0.33) {
                glyph = '+';
            } else if (lifeRatio > 0.10) {
                glyph = '.';
            } else {
                glyph = ' ';
            }
            TextCell cell = new TextCell(glyph, p.color, AnsiColor.BLACK);
            graphics.setCell(px, py, cell);
        }
    }

    @Override
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        this.rockets = new ArrayList<>();
        this.particles = new ArrayList<>();
        this.launchCooldown = 0;
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
        return TARGET_FPS;
    }

    @Override
    public TerminalSize lastSize() {
        return lastSize;
    }

    /** Visible for tests. */
    public double getTime() {
        return time;
    }

    /** Visible for tests. */
    public List<Rocket> getRockets() {
        return rockets;
    }

    /** Visible for tests. */
    public List<Particle> getParticles() {
        return particles;
    }

    /** Visible for tests: forces a launch immediately. */
    public void launchRocket(TerminalSize size) {
        if (rockets == null) rockets = new ArrayList<>();
        rockets.add(new Rocket(size, random));
    }

    /** Rocket state. */
    public static final class Rocket {
        private double x;
        private double y;
        private double vx;
        private double vy;

        Rocket(TerminalSize size, Random random) {
            this.x = random.nextInt(Math.max(1, size.columns()));
            this.y = size.rows() - 1;
            this.vx = (random.nextDouble() - 0.5) * 0.5;
            this.vy = -1.0 - random.nextDouble(); // -1 to -2
        }

        void advance() {
            x += vx;
            y += vy;
            vy += 0.15; // gravity — peaks within 7-13 frames
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getVx() { return vx; }
        public double getVy() { return vy; }
    }

    /** Particle state. */
    public static final class Particle {
        private double x;
        private double y;
        private double vx;
        private double vy;
        private final AnsiColor color;
        private int life;
        private final int maxLife;

        Particle(double startX, double startY, AnsiColor color, Random random) {
            this.x = startX;
            this.y = startY;
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 0.5 + random.nextDouble() * 2.0;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
            this.color = color;
            this.maxLife = 8 + random.nextInt(8);
            this.life = maxLife;
        }

        void advance() {
            x += vx;
            y += vy;
            vy += 0.05; // slight gravity
            vx *= 0.97;  // air resistance
            vy *= 0.97;
            life--;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public AnsiColor getColor() { return color; }
        public int getLife() { return life; }
        public int getMaxLife() { return maxLife; }
    }
}
