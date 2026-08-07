package io.jterm.sprite;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ParticleEffect} — simple terminal particle system.
 */
class ParticleEffectTest {

    @Test
    @DisplayName("explosion spawns N particles")
    void explosionSpawns() {
        var fx = ParticleEffect.explosion(40, 12, 20);
        assertEquals(20, fx.particleCount());
        fx.tick(0);
        fx.tick(50);
        // some particles may die but should still be > 0 at start
        assertTrue(fx.particleCount() > 0);
    }

    @Test
    @DisplayName("sparkle spawns N twinkling particles")
    void sparkleSpawns() {
        var fx = ParticleEffect.sparkle(40, 12, 15);
        assertEquals(15, fx.particleCount());
    }

    @Test
    @DisplayName("particles die after their lifetime expires")
    void particlesDieAfterLifetime() {
        var fx = ParticleEffect.explosion(40, 12, 10);
        // explosion particles default to 500ms lifetime, tick well past
        fx.tick(0);
        fx.tick(2000);
        assertEquals(0, fx.particleCount());
    }

    @Test
    @DisplayName("isAlive returns false when no particles left")
    void isAliveWhenParticlesLeft() {
        var fx = ParticleEffect.explosion(40, 12, 5);
        fx.tick(0);
        assertTrue(fx.isAlive());
        fx.tick(5000);
        assertFalse(fx.isAlive());
    }

    @Test
    @DisplayName("tick advances particle positions")
    void tickAdvancesPositions() {
        var fx = ParticleEffect.explosion(40, 12, 10);
        fx.tick(0);
        // Capture first particle's position
        var before = fx.snapshot();
        fx.tick(100);
        var after = fx.snapshot();
        // At least one particle should have moved
        boolean moved = false;
        for (int i = 0; i < before.length && i < after.length; i++) {
            if (!before[i].equals(after[i])) {
                moved = true;
                break;
            }
        }
        assertTrue(moved, "at least one particle should have moved");
    }

    @Test
    @DisplayName("gravity pulls particles down over time")
    void gravityPullsDown() {
        var fx = new ParticleEffect();
        fx.setGravity(1.0);
        // Spawn a particle with no initial velocity; gravity should pull it down
        fx.spawn(40, 12, 0, 0, '*', AnsiColor.RED, 1000);
        fx.tick(0);
        double y0 = fx.snapshot()[0].y();
        fx.tick(100);
        double y1 = fx.snapshot()[0].y();
        // y should be higher (towards bottom of terminal) due to gravity
        assertTrue(y1 > y0, "particle should fall due to gravity (y0=" + y0 + ", y1=" + y1 + ")");
    }

    @Test
    @DisplayName("friction reduces velocity")
    void frictionReducesVelocity() {
        var fx = new ParticleEffect();
        fx.setFriction(0.5);
        fx.spawn(40, 12, 2.0, 0.0, '#', AnsiColor.WHITE, 1000);
        fx.tick(0);
        var p0 = fx.snapshot()[0];
        fx.tick(100);
        var p1 = fx.snapshot()[0];
        // after 100ms with 50% friction, the velocity should have decreased
        double dx = p1.x() - p0.x();
        // move per tick should be less than initial velocity * dt
        assertTrue(Math.abs(dx) < Math.abs(2.0 * 0.1) + 0.001,
                "friction should slow particle, dx=" + dx);
    }

    @Test
    @DisplayName("render draws particles onto graphics buffer")
    void renderDrawsParticles() {
        var fx = ParticleEffect.explosion(40, 12, 5);
        fx.tick(0);
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);
        fx.render(g);
        // Should find at least one non-blank cell
        boolean anyNonBlank = false;
        for (int r = 0; r < 24 && !anyNonBlank; r++) {
            for (int c = 0; c < 80; c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') {
                    anyNonBlank = true;
                    break;
                }
            }
        }
        assertTrue(anyNonBlank, "render should draw at least one particle");
    }

    @Test
    @DisplayName("reset clears all particles")
    void resetClears() {
        var fx = ParticleEffect.explosion(40, 12, 10);
        fx.tick(0);
        fx.reset();
        assertEquals(0, fx.particleCount());
        assertFalse(fx.isAlive());
    }

    @Test
    @DisplayName("spawn single particle with explicit params")
    void spawnSingle() {
        var fx = new ParticleEffect();
        fx.spawn(10, 5, 1.0, 0.5, '+', AnsiColor.CYAN, 500);
        assertEquals(1, fx.particleCount());
        fx.tick(0);
        fx.tick(600);
        assertEquals(0, fx.particleCount()); // dies after 500ms
    }

    // ── radialExplosion tests ────────────────────────────────────────────────

    @Test
    @DisplayName("radialExplosion has no gravity")
    void radialExplosionHasNoGravity() {
        var fx = ParticleEffect.radialExplosion(40, 12, 10, AnsiColor.BRIGHT_RED);
        assertEquals(0.0, fx.getGravity(), "radial explosion should have no gravity");
    }

    @Test
    @DisplayName("radialExplosion has fade enabled")
    void radialExplosionHasFadeEnabled() {
        var fx = ParticleEffect.radialExplosion(40, 12, 10, AnsiColor.BRIGHT_RED);
        assertTrue(fx.isFade(), "radial explosion should have fade enabled");
    }

    @Test
    @DisplayName("radialExplosion particles all use the given color")
    void radialExplosionParticlesUseGivenColor() {
        var color = AnsiColor.BRIGHT_YELLOW;
        var fx = ParticleEffect.radialExplosion(40, 12, 10, color);
        assertEquals(10, fx.particleCount());
        for (var p : fx.getParticles()) {
            assertEquals(color, p.color(), "all particles should use the given color");
        }
    }

    @Test
    @DisplayName("radialExplosion particles radiate outward after one tick")
    void radialExplosionParticlesRadiateOutward() {
        int cx = 40, cy = 12;
        var fx = ParticleEffect.radialExplosion(cx, cy, 20, AnsiColor.BRIGHT_RED);
        // At spawn, all particles are at the center
        for (var p : fx.getParticles()) {
            assertEquals(cx, p.x(), 0.001, "particle should start at center x");
            assertEquals(cy, p.y(), 0.001, "particle should start at center y");
        }
        fx.tick(100);
        // After one tick, particles should be farther from center than at spawn
        boolean moved = false;
        for (var p : fx.getParticles()) {
            double dist = Math.hypot(p.x() - cx, p.y() - cy);
            if (dist > 0.01) {
                moved = true;
                break;
            }
        }
        assertTrue(moved, "at least one particle should have moved away from center");
    }

    @Test
    @DisplayName("fade dims particle color after half lifetime")
    void fadeDimsParticleColor() {
        var fx = new ParticleEffect();
        fx.setFade(true);
        // Spawn a particle with 1000ms lifetime
        fx.spawn(40, 12, 0, 0, '*', AnsiColor.BRIGHT_WHITE, 1000);
        fx.tick(0);
        // At birth, fadedColor should match original color
        var p0 = fx.getParticles().get(0);
        assertEquals(AnsiColor.BRIGHT_WHITE, p0.fadedColor(),
                "at birth fadedColor should be full brightness");
        // Tick past half lifetime (600ms)
        fx.tick(600);
        var p1 = fx.getParticles().get(0);
        // fadedColor should be different (dimmed) from original
        assertNotEquals(AnsiColor.BRIGHT_WHITE, p1.fadedColor(),
                "after 60% lifetime, fadedColor should be dimmed (different from original)");
    }

    @Test
    @DisplayName("fade reaches black at end of life (particle is removed)")
    void fadeReachesBlackAtEndOfLife() {
        var fx = new ParticleEffect();
        fx.setFade(true);
        fx.spawn(40, 12, 0, 0, '*', AnsiColor.BRIGHT_RED, 500);
        fx.tick(0);
        assertEquals(1, fx.particleCount());
        // Tick well past lifetime — particle should be removed (dead)
        fx.tick(2000);
        assertEquals(0, fx.particleCount(), "particle should be removed after lifetime expires");
    }
}