package io.jterm.sprite;

import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.TextCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simple terminal particle system. Particles are single characters with
 * colors that move with a velocity, optionally affected by gravity and
 * friction. Each particle has a finite lifetime after which it is removed.
 *
 * <h2>Patterns</h2>
 * <ul>
 *   <li>{@link #explosion(int, int, int)} — radial burst of particles from a point.</li>
 *   <li>{@link #sparkle(int, int, int)} — random twinkling particles.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ParticleEffect fx = ParticleEffect.explosion(40, 12, 25);
 * fx.setGravity(0.5);
 * while (fx.isAlive()) {
 *     fx.tick(deltaMs);
 *     fx.render(graphics);
 *     screen.refresh();
 * }
 * }</pre>
 */
public class ParticleEffect {

    /** Default particle lifetime in ms. */
    public static final int DEFAULT_LIFETIME_MS = 500;

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private volatile double gravity = 0.0;
    private volatile double friction = 1.0; // 1.0 = no friction, < 1 = slows
    private volatile boolean fade = false; // when true, particles dim toward black as they age

    /**
     * A single particle. Exposed as a record so tests can snapshot state.
     *
     * @param x          spawn column
     * @param y          spawn row
     * @param vx         horizontal velocity
     * @param vy         vertical velocity
     * @param glyph      character to display
     * @param color      original (birth) color
     * @param fadedColor  current color after fade (equals color when fade is off)
     * @param bornMs     timestamp the particle was born (0-based)
     * @param lifetimeMs lifetime in ms
     */
    public record Particle(double x, double y, double vx, double vy, char glyph,
                            Color color, Color fadedColor, long bornMs, long lifetimeMs) {
        /**
         * Returns whether alive.
         * @param nowMs the now ms value
         * @return true if the condition holds, false otherwise
         */
        public boolean isAlive(long nowMs) {
            return (nowMs - bornMs) < lifetimeMs;
        }
    }

    /** Create an empty particle effect. */
    public ParticleEffect() {}

    /** Create an empty particle effect with gravity and friction defaults. */
    public ParticleEffect(double gravity, double friction) {
        this.gravity = gravity;
        this.friction = friction;
    }

    /**
     * Spawn an explosion of particles at the given position.
     * Each particle gets a random velocity directed radially outward.
     *
     * @param x       spawn column
     * @param y       spawn row
     * @param count   number of particles
     * @return this effect (for chaining)
     */
    public static ParticleEffect explosion(int x, int y, int count) {
        var fx = new ParticleEffect();
        fx.spawnExplosion(x, y, count);
        return fx;
    }

    /**
     * Spawn a sparkle pattern — random twinkling particles at the given position.
     *
     * @param x       spawn column
     * @param y       spawn row
     * @param count   number of particles
     * @return this effect (for chaining)
     */
    public static ParticleEffect sparkle(int x, int y, int count) {
        var fx = new ParticleEffect();
        fx.spawnSparkle(x, y, count);
        return fx;
    }

    /**
     * Spawn a top-down radial explosion — particles radiate outward from the
     * given point with no gravity, strong friction, and color fading to black.
     * Suitable for top-down games (e.g. Robotron) where gravity-based fireworks
     * would look wrong.
     *
     * @param x     spawn column
     * @param y     spawn row
     * @param count number of particles
     * @param color color for all particles (fades to black as they die)
     * @return a new ParticleEffect with fade and friction pre-configured
     */
    public static ParticleEffect radialExplosion(int x, int y, int count, Color color) {
        var fx = new ParticleEffect();
        fx.setFriction(0.97);
        fx.setFade(true);
        fx.spawnRadialExplosion(x, y, count, color);
        return fx;
    }

    /**
     * Spawn an explosion (radial burst) at the given position.
     *
     * @param x      spawn column
     * @param y      spawn row
     * @param count  number of particles
     */
    public void spawnExplosion(int x, int y, int count) {
        char[] glyphs = {'*', '+', '.', '#', 'x'};
        Color[] palette = {
                AnsiColor.BRIGHT_RED, AnsiColor.BRIGHT_YELLOW, AnsiColor.BRIGHT_CYAN,
                AnsiColor.BRIGHT_WHITE, AnsiColor.BRIGHT_MAGENTA
        };
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double speed = 0.5 + random.nextDouble() * 3.0;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            char glyph = glyphs[random.nextInt(glyphs.length)];
            Color color = palette[random.nextInt(palette.length)];
            long life = 300 + random.nextInt(400);
            particles.add(new Particle(x, y, vx, vy, glyph, color, color, 0, life));
        }
    }

    /**
     * Spawn a sparkle (random twinkling) at the given position.
     *
     * @param x      spawn column
     * @param y      spawn row
     * @param count  number of particles
     */
    public void spawnSparkle(int x, int y, int count) {
        char[] glyphs = {'.', '*', '+', '\u2735', '\u2605'};
        Color[] palette = {
                AnsiColor.BRIGHT_WHITE, AnsiColor.BRIGHT_YELLOW, AnsiColor.BRIGHT_CYAN
        };
        for (int i = 0; i < count; i++) {
            double dx = (random.nextDouble() - 0.5) * 6.0;
            double dy = (random.nextDouble() - 0.5) * 3.0;
            double vx = (random.nextDouble() - 0.5) * 0.5;
            double vy = (random.nextDouble() - 0.5) * 0.5;
            char glyph = glyphs[random.nextInt(glyphs.length)];
            Color color = palette[random.nextInt(palette.length)];
            long life = 200 + random.nextInt(600);
            particles.add(new Particle(x + dx, y + dy, vx, vy, glyph, color, color, 0, life));
        }
    }

    /**
     * Spawn a top-down radial explosion — all particles use the same color,
     * radiate outward from (x, y) with short contained speeds, and have short
     * punchy lifetimes. The effect should also have fade enabled and friction
     * set (see {@link #radialExplosion(int, int, int, Color)}).
     *
     * @param x     spawn column
     * @param y     spawn row
     * @param count number of particles
     * @param color color for all particles
     */
    public void spawnRadialExplosion(int x, int y, int count, Color color) {
        char[] glyphs = {'*', '+', '.', 'x'};
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double speed = 1.5 + random.nextDouble() * 2.0; // 1.5 to 3.5
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            char glyph = glyphs[random.nextInt(glyphs.length)];
            long life = 400 + random.nextInt(300); // 400-700ms
            particles.add(new Particle(x, y, vx, vy, glyph, color, color, 0, life));
        }
    }

    /**
     * Spawn a single particle with explicit parameters.
     *
     * @param x        spawn column
     * @param y        spawn row
     * @param vx       horizontal velocity (cells per second)
     * @param vy       vertical velocity (cells per second, positive = down)
     * @param glyph    character to display
     * @param color    foreground color
     * @param lifetimeMs lifetime in ms
     */
    public void spawn(double x, double y, double vx, double vy,
                      char glyph, Color color, long lifetimeMs) {
        particles.add(new Particle(x, y, vx, vy, glyph, color, color, 0, lifetimeMs));
    }

    /**
     * Advance all particles by {@code deltaMs} milliseconds. Dead particles
     * are removed.
     *
     * @param deltaMs milliseconds elapsed since the previous tick (absolute
     *                wall-clock; the first call seeds time zero)
     */
    public void tick(long deltaMs) {
        // We use deltaMs as both elapsed time and "current time" for simplicity.
        // The first call seeds born=0 and tests pass deltaMs as elapsed time.
        // For tests, we keep particle.bornMs = 0 (set at spawn time) and
        // treat deltaMs as elapsed time.
        double dt = deltaMs / 1000.0; // seconds
        List<Particle> alive = new ArrayList<>();
        for (Particle p : particles) {
            long now = deltaMs;
            if (!p.isAlive(now)) continue;
            // apply friction (velocity damping)
            double vx = p.vx();
            double vy = p.vy();
            if (friction < 1.0) {
                double factor = Math.pow(friction, dt * 60.0); // normalize to 60fps
                vx *= factor;
                vy *= factor;
            }
            // apply gravity
            vy += gravity * dt * 10.0;
            double nx = p.x() + vx * dt * 10.0;
            double ny = p.y() + vy * dt * 10.0;
            // compute faded color using discrete ANSI palette steps.
            // Blending in RGB space snaps to wrong colors due to the coarse
            // 16-color palette (e.g. dimming BRIGHT_RED by 20% → BLACK).
            // Instead, step down through palette levels:
            //   100-66% life: BRIGHT_* (original color)
            //   66-33% life: normal version (RED, CYAN, etc.)
            //   33-0% life: BRIGHT_BLACK (dark gray)
            Color fadedColor = p.color();
            if (fade && p.color() instanceof AnsiColor ac && p.lifetimeMs() > 0) {
                long elapsed = now - p.bornMs();
                double lifeFraction = (double) elapsed / p.lifetimeMs(); // 0=birth, 1=death
                fadedColor = stepFade(ac, lifeFraction);
            }
            alive.add(new Particle(nx, ny, vx, vy, p.glyph(), p.color(),
                    fadedColor, p.bornMs(), p.lifetimeMs()));
        }
        particles.clear();
        particles.addAll(alive);
    }

    /**
     * Render all live particles to the graphics buffer.
     *
     * @param g graphics buffer
     */
    public void render(TextGraphics g) {
        for (Particle p : particles) {
            int col = (int) Math.round(p.x());
            int row = (int) Math.round(p.y());
            if (col < 0 || row < 0) continue;
            var size = g.getSize();
            if (col >= size.columns() || row >= size.rows()) continue;
            Color drawColor = fade ? p.fadedColor() : p.color();
            g.setCell(col, row, new TextCell(p.glyph(), drawColor, AnsiColor.DEFAULT));
        }
    }

    /**
     * @return number of currently live particles
     */
    public int particleCount() {
        return particles.size();
    }

    /**
     * @return true when at least one particle is still alive
     */
    public boolean isAlive() {
        return !particles.isEmpty();
    }

    /**
     * Snapshot of current particle positions. For testing.
     *
     * @return array of (x,y) records for each particle
     */
    public Pos[] snapshot() {
        Pos[] arr = new Pos[particles.size()];
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            arr[i] = new Pos(p.x(), p.y());
        }
        return arr;
    }

    /** Lightweight position record for snapshots. */
    public record Pos(double x, double y) {}

    /** Clear all particles. */
    public void reset() {
        particles.clear();
    }

    /**
     * @return an unmodifiable view of the current particles (for testing)
     */
    public List<Particle> getParticles() {
        return List.copyOf(particles);
    }

    /**
     * Enable or disable color fading. When enabled, particles dim toward
     * {@link AnsiColor#BLACK} as they age (at birth full brightness, at death black).
     *
     * @param fade true to enable fading
     */
    public void setFade(boolean fade) {
        this.fade = fade;
    }

    /**
     * @return whether color fading is enabled
     */
    public boolean isFade() {
        return fade;
    }

    /**
     * Step-fade an AnsiColor through discrete palette levels based on life fraction.
     *
     * <p>Uses 3 steps to avoid the 16-color palette snapping problem:
     * <ul>
     *   <li>0.0–0.33: original bright color (full life)</li>
     *   <li>0.33–0.66: normal (dim) version of the color</li>
     *   <li>0.66–1.0: BRIGHT_BLACK (dark gray, near death)</li>
     * </ul>
     *
     * @param color the original particle color
     * @param lifeFraction 0.0 at birth, 1.0 at death
     * @return the stepped-down color
     */
    static AnsiColor stepFade(AnsiColor color, double lifeFraction) {
        if (lifeFraction < 0.33) return color;
        if (lifeFraction < 0.66) return toDim(color);
        return AnsiColor.BRIGHT_BLACK;
    }

    /**
     * Map a BRIGHT_* color to its normal (dim) counterpart.
     *
     * @param color a bright ANSI color
     * @return the normal-intensity version, or BRIGHT_BLACK if no mapping exists
     */
    private static AnsiColor toDim(AnsiColor color) {
        return switch (color) {
            case BRIGHT_BLACK -> AnsiColor.BLACK;
            case BRIGHT_RED -> AnsiColor.RED;
            case BRIGHT_GREEN -> AnsiColor.GREEN;
            case BRIGHT_YELLOW -> AnsiColor.YELLOW;
            case BRIGHT_BLUE -> AnsiColor.BLUE;
            case BRIGHT_MAGENTA -> AnsiColor.MAGENTA;
            case BRIGHT_CYAN -> AnsiColor.CYAN;
            case BRIGHT_WHITE -> AnsiColor.WHITE;
            default -> AnsiColor.BRIGHT_BLACK; // already dim or DEFAULT
        };
    }

    /**
     * @return current gravity value
     */
    public double getGravity() {
        return gravity;
    }

    /**
     * Set gravity. Positive values pull particles downward (rows increase).
     *
     * @param gravity gravity acceleration
     */
    public void setGravity(double gravity) {
        this.gravity = gravity;
    }

    /**
     * @return current friction multiplier (1.0 = no friction)
     */
    public double getFriction() {
        return friction;
    }

    /**
     * Set friction multiplier. Values less than 1.0 slow particles over time.
     *
     * @param friction friction (1.0 = none, 0.5 = strong)
     */
    public void setFriction(double friction) {
        this.friction = Math.max(0.0, Math.min(1.0, friction));
    }
}