package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AnimatedBackground} interface default methods:
 * {@code targetFps()}, {@code lastSize()}, and {@code tick(long)}.
 * These defaults are only covered when called on an implementation that
 * does NOT override them, so we use a minimal stub.
 */
class AnimatedBackgroundTest {

    /** Minimal stub that inherits all AnimatedBackground defaults. */
    static class StubBackground implements AnimatedBackground {
        boolean running;
        TerminalSize lastSize;

        @Override
        public void renderFrame(TextGraphics graphics, TerminalSize size) {}

        @Override
        public void onResize(TerminalSize newSize) {
            this.lastSize = newSize;
        }

        @Override
        public void start() { running = true; }

        @Override
        public void stop() { running = false; }

        @Override
        public boolean isRunning() { return running; }

        // Intentionally NOT overriding targetFps(), lastSize(), tick()
        // so the interface defaults get exercised.
    }

    @Test
    @DisplayName("default targetFps() returns 15")
    void defaultTargetFps() {
        var bg = new StubBackground();
        assertEquals(15, bg.targetFps());
    }

    @Test
    @DisplayName("default lastSize() returns null when never resized")
    void defaultLastSizeNull() {
        var bg = new StubBackground();
        assertNull(bg.lastSize());
    }

    @Test
    @DisplayName("default lastSize() still returns null after onResize since stub overrides onResize")
    void defaultLastSizeIndependentOfOnResize() {
        var bg = new StubBackground();
        // The default lastSize() always returns null; our stub stores
        // resize events in its own field, not via the default method.
        bg.onResize(new TerminalSize(40, 12));
        assertNull(bg.lastSize()); // default impl, not the stub field
    }

    @Test
    @DisplayName("default tick() does nothing — no exception, no side effects")
    void defaultTickNoOp() {
        var bg = new StubBackground();
        // Should not throw; there is no state to observe
        assertDoesNotThrow(() -> bg.tick(System.nanoTime()));
        assertDoesNotThrow(() -> bg.tick(0L));
        assertDoesNotThrow(() -> bg.tick(-1L));
    }

    @Test
    @DisplayName("stub implements AnimatedBackground")
    void stubIsAnimatedBackground() {
        var bg = new StubBackground();
        assertInstanceOf(AnimatedBackground.class, bg);
    }

    @Test
    @DisplayName("default targetFps() is stable across calls")
    void defaultTargetFpsStable() {
        var bg = new StubBackground();
        assertEquals(15, bg.targetFps());
        assertEquals(15, bg.targetFps());
    }
}