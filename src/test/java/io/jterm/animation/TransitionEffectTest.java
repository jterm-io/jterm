package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TransitionEffect} interface contract.
 */
class TransitionEffectTest {

    @Test
    @DisplayName("default targetFps is 30")
    void defaultTargetFpsIs30() {
        TransitionEffect effect = new DummyTransition(500);
        assertEquals(30, effect.targetFps());
    }

    @Test
    @DisplayName("durationMs returns configured duration")
    void durationMsReturnsConfigured() {
        TransitionEffect effect = new DummyTransition(750);
        assertEquals(750, effect.durationMs());
    }

    @Test
    @DisplayName("renderFrame is called and can draw to graphics")
    void renderFrameDrawsToGraphics() {
        var effect = new DummyTransition(500);
        var size = new TerminalSize(10, 5);
        var buffer = new ScreenBuffer(size);
        effect.renderFrame(new TextGraphics(buffer), size, 0.5);
        // DummyTransition fills with 'X'
        assertEquals('X', buffer.getCell(0, 0).character().charAt(0));
    }

    /** Simple test double that fills the screen with 'X'. */
    static class DummyTransition implements TransitionEffect {
        private final long duration;
        DummyTransition(long durationMs) { this.duration = durationMs; }
        @Override public long durationMs() { return duration; }
        @Override public void renderFrame(TextGraphics graphics, TerminalSize size, double progress) {
            for (int r = 0; r < size.rows(); r++)
                for (int c = 0; c < size.columns(); c++)
                    graphics.setCell(c, r, new io.jterm.style.TextCell('X'));
        }
    }
}