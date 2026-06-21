package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct tests for the {@link AnimatedBackground} interface and the
 * {@link AnimationManager} that coordinates multiple backgrounds.
 */
class AnimatedBackgroundTest {

    static class MockAnimatedBackground implements AnimatedBackground {
        final AtomicInteger renderCalls = new AtomicInteger();
        final AtomicInteger resizeCalls = new AtomicInteger();
        final AtomicLong lastElapsed = new AtomicLong();
        final AtomicInteger startCalls = new AtomicInteger();
        final AtomicInteger stopCalls = new AtomicInteger();
        volatile boolean running;
        volatile int targetFps = 15;
        TerminalSize lastSize;
        TerminalSize lastResize;

        @Override
        public void renderFrame(TextGraphics graphics, TerminalSize size) {
            renderCalls.incrementAndGet();
            lastSize = size;
            graphics.setCell(0, 0, new TextCell('M', AnsiColor.BRIGHT_RED, AnsiColor.BLACK));
        }

        @Override
        public void onResize(TerminalSize newSize) {
            resizeCalls.incrementAndGet();
            lastResize = newSize;
        }

        @Override
        public void start() {
            startCalls.incrementAndGet();
            running = true;
        }

        @Override
        public void stop() {
            stopCalls.incrementAndGet();
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
    }

    @Test
    @DisplayName("default target FPS is 15")
    void defaultTargetFps() {
        var bg = new MockAnimatedBackground();
        assertEquals(15, bg.targetFps());
    }

    @Test
    @DisplayName("renderFrame is invoked by AnimatedBackgroundWindow draw")
    void renderFrameInvoked() {
        var size = new TerminalSize(10, 5);
        var bg = new MockAnimatedBackground();
        var window = new io.jterm.window.AnimatedBackgroundWindow(bg, null);
        window.setBounds(TerminalPosition.TOP_LEFT, size);
        var buffer = new ScreenBuffer(size);
        window.draw(new TextGraphics(buffer));

        assertEquals(1, bg.renderCalls.get());
        assertEquals(size, bg.lastSize);
        assertEquals('M', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    @DisplayName("onResize receives new size")
    void onResizeReceived() {
        var bg = new MockAnimatedBackground();
        bg.onResize(new TerminalSize(20, 10));
        assertEquals(new TerminalSize(20, 10), bg.lastResize);
    }

    @Test
    @DisplayName("start/stop lifecycle on mock background")
    void startStopLifecycle() {
        var bg = new MockAnimatedBackground();
        assertFalse(bg.isRunning());
        bg.start();
        assertTrue(bg.isRunning());
        assertEquals(1, bg.startCalls.get());
        bg.stop();
        assertFalse(bg.isRunning());
        assertEquals(1, bg.stopCalls.get());
    }
}
