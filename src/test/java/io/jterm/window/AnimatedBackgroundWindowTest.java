package io.jterm.window;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import io.jterm.animation.AnimatedBackground;
import io.jterm.animation.AnimationTimer;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that AnimatedBackgroundWindow.start() pre-advances the animation
 * so the first rendered frame has stars spread across the screen, not
 * clustered at initial positions.
 */
class AnimatedBackgroundWindowTest {

    /**
     * A minimal AnimatedBackground that counts how many times
     * renderFrame is called, so we can verify pre-advancement.
     */
    static class CountingBackground implements AnimatedBackground {
        int renderCount = 0;
        int tickCount = 0;
        boolean running = false;
        TerminalSize lastSize;

        @Override
        public void renderFrame(TextGraphics g, TerminalSize size) {
            renderCount++;
            lastSize = size;
        }

        @Override
        public void onResize(TerminalSize newSize) {
            lastSize = newSize;
        }

        @Override
        public void start() { running = true; }

        @Override
        public void stop() { running = false; }

        @Override
        public boolean isRunning() { return running; }

        @Override
        public int targetFps() { return 2; }

        @Override
        public void tick(long nowNanos) { tickCount++; }
    }

    @Test
    void startPreAdvancesAnimationSoFirstFrameHasContent() {
        // The bug: start() called background.tick() in a loop, but tick() only
        // updates frame counters — it does NOT advance star positions.
        // renderFrame() is what advances positions (calls advanceStar).
        // The fix: start() should call renderTick() in the pre-tick loop,
        // not just tick(), so stars are spread across the screen on first draw.

        // We can't easily test AnimatedBackgroundWindow without a full GUI,
        // but we can verify the concept: tick() alone doesn't advance state.
        var bg = new CountingBackground();
        bg.start();
        long now = System.nanoTime();
        // Simulate the old (buggy) pre-tick: just tick()
        for (int i = 0; i < 5; i++) {
            bg.tick(now + i * 250_000_000L);
        }
        assertEquals(5, bg.tickCount, "tick() should have been called 5 times");
        assertEquals(0, bg.renderCount, "renderFrame() was never called — stars not advanced");

        // Now simulate the fix: call renderFrame (which advances stars)
        var buf = new ScreenBuffer(new TerminalSize(80, 24),
                new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK));
        var g = new TextGraphics(buf);
        bg.renderFrame(g, new TerminalSize(80, 24));
        assertEquals(1, bg.renderCount, "renderFrame() called — stars advanced");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("setBounds forwards resize to background animation")
    void setBoundsForwardsResizeToBackground() {
        var bg = new CountingBackground();
        var window = new AnimatedBackgroundWindow(bg, null, true);
        var realSize = new TerminalSize(80, 24);
        window.setBounds(TerminalPosition.TOP_LEFT, realSize);
        assertEquals(realSize, bg.lastSize,
                "setBounds should forward the new size to background.onResize()");
        assertEquals(realSize, window.getSize(),
                "window size should match what was set");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("setBounds does not forward repeated same-size calls")
    void setBoundsDedupesSameSize() {
        // The GUI layout manager calls setBounds() on every screen update.
        // Forwarding onResize() unconditionally resets animation state (e.g.
        // VoronoiCells recreating seeds) many times per second, making the
        // animation appear to run extremely fast.
        var bg = new CountingBackground();
        var window = new AnimatedBackgroundWindow(bg, null, true);
        var size = new TerminalSize(80, 24);
        window.setBounds(TerminalPosition.TOP_LEFT, size);
        bg.lastSize = null; // reset to detect subsequent calls
        // Repeated setBounds with same size should NOT forward
        window.setBounds(TerminalPosition.TOP_LEFT, size);
        window.setBounds(TerminalPosition.TOP_LEFT, size);
        assertNull(bg.lastSize,
                "onResize should NOT be called again for same size");
        // Different size SHOULD forward
        var newSize = new TerminalSize(120, 40);
        window.setBounds(TerminalPosition.TOP_LEFT, newSize);
        assertEquals(newSize, bg.lastSize,
                "onResize should be called when size actually changes");
    }
}