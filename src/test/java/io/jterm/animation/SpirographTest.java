package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Spirograph}: AnimatedBackground contract, non-empty render,
 * frame-to-frame evolution, and small-terminal resilience.
 */
class SpirographTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var spiro = new Spirograph(new TerminalSize(80, 24));
        assertTrue(spiro instanceof AnimatedBackground);
        assertEquals(12, spiro.targetFps());
        spiro.start();
        assertTrue(spiro.isRunning());
        spiro.stop();
        assertFalse(spiro.isRunning());
    }

    @Test
    @DisplayName("renderFrame produces non-empty output")
    void rendersNonBlankOutput() {
        var spiro = new Spirograph(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        // Render enough frames to build up a visible trail.
        for (int i = 0; i < 50; i++) {
            spiro.renderFrame(new TextGraphics(buffer), size);
        }

        boolean hasContent = false;
        for (int r = 0; r < size.rows() && !hasContent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') {
                    hasContent = true;
                    break;
                }
            }
        }
        assertTrue(hasContent, "rendered output should contain non-space characters");
    }

    @Test
    @DisplayName("pattern changes over frames")
    void patternChangesOverFrames() {
        var spiro = new Spirograph(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer1 = new ScreenBuffer(size);

        for (int i = 0; i < 30; i++) {
            spiro.renderFrame(new TextGraphics(buffer1), size);
        }

        double phaseXBefore = spiro.getPhaseX();
        double phaseYBefore = spiro.getPhaseY();

        var buffer2 = new ScreenBuffer(size);
        for (int i = 0; i < 30; i++) {
            spiro.renderFrame(new TextGraphics(buffer2), size);
        }

        assertNotEquals(phaseXBefore, spiro.getPhaseX(), "phaseX should advance");
        assertNotEquals(phaseYBefore, spiro.getPhaseY(), "phaseY should advance");

        boolean anyDifferent = false;
        for (int r = 0; r < size.rows() && !anyDifferent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (!buffer1.getCell(c, r).equals(buffer2.getCell(c, r))) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "pattern should change across frames");
    }

    @Test
    @DisplayName("uses magenta/cyan palette for the curve")
    void usesExpectedColors() {
        var spiro = new Spirograph(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        for (int i = 0; i < 50; i++) {
            spiro.renderFrame(new TextGraphics(buffer), size);
        }

        boolean foundExpected = false;
        for (int r = 0; r < size.rows() && !foundExpected; r++) {
            for (int c = 0; c < size.columns(); c++) {
                Color fg = buffer.getCell(c, r).fg();
                if (fg == AnsiColor.MAGENTA || fg == AnsiColor.BRIGHT_MAGENTA
                        || fg == AnsiColor.BRIGHT_CYAN || fg == AnsiColor.BRIGHT_WHITE) {
                    foundExpected = true;
                    break;
                }
            }
        }
        assertTrue(foundExpected, "expected magenta/cyan/white colors in output");
    }

    @Test
    @DisplayName("small terminal sizes render without throwing")
    void smallTerminalSizes() {
        var spiro = new Spirograph(new TerminalSize(1, 1));
        var size = new TerminalSize(1, 1);
        var buffer = new ScreenBuffer(size);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                spiro.renderFrame(new TextGraphics(buffer), size);
            }
        });
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var spiro = new Spirograph(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> spiro.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize clears trail and stores new size")
    void onResizeClearsTrailAndStoresSize() {
        var spiro = new Spirograph(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        for (int i = 0; i < 20; i++) {
            spiro.renderFrame(new TextGraphics(buffer), size);
        }
        assertFalse(spiro.getTrail().isEmpty(), "trail should have points after rendering");

        var newSize = new TerminalSize(100, 40);
        spiro.onResize(newSize);
        assertEquals(newSize, spiro.lastSize());
        assertTrue(spiro.getTrail().isEmpty(), "trail should be cleared on resize");
    }

    @Test
    @DisplayName("trail length stays bounded")
    void trailLengthBounded() {
        var spiro = new Spirograph(new TerminalSize(40, 20));
        var size = new TerminalSize(40, 20);
        var buffer = new ScreenBuffer(size);

        for (int i = 0; i < 500; i++) {
            spiro.renderFrame(new TextGraphics(buffer), size);
        }

        assertTrue(spiro.getTrail().size() <= 300, "trail should not exceed buffer capacity");
    }
}
