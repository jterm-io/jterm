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
 * Tests for {@link MandelbrotZoom}: animated background contract, non-empty
 * render output, frame-to-frame zoom evolution, and small-terminal resilience.
 */
class MandelbrotZoomTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var mandel = new MandelbrotZoom(new TerminalSize(80, 24));
        assertTrue(mandel instanceof AnimatedBackground);
        assertEquals(10, mandel.targetFps());
        mandel.start();
        assertTrue(mandel.isRunning());
        mandel.stop();
        assertFalse(mandel.isRunning());
    }

    @Test
    @DisplayName("renderFrame produces non-empty output")
    void rendersNonBlankOutput() {
        var mandel = new MandelbrotZoom(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        mandel.renderFrame(new TextGraphics(buffer), size);

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
    @DisplayName("renderFrame uses expected Mandelbrot palette")
    void usesExpectedColors() {
        var mandel = new MandelbrotZoom(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        mandel.renderFrame(new TextGraphics(buffer), size);

        boolean foundExpected = false;
        for (int r = 0; r < size.rows() && !foundExpected; r++) {
            for (int c = 0; c < size.columns(); c++) {
                Color fg = buffer.getCell(c, r).fg();
                if (fg != AnsiColor.DEFAULT && fg != AnsiColor.BLACK) {
                    foundExpected = true;
                    break;
                }
            }
        }
        assertTrue(foundExpected, "expected colored output from Mandelbrot palette");
    }

    @Test
    @DisplayName("zoom changes the pattern over frames")
    void zoomChangesOverFrames() {
        var mandel = new MandelbrotZoom(new TerminalSize(40, 40));
        var size = new TerminalSize(40, 40);

        var buffer1 = new ScreenBuffer(size);
        mandel.renderFrame(new TextGraphics(buffer1), size);
        double rangeBefore = mandel.getRange();

        var buffer2 = new ScreenBuffer(size);
        for (int i = 0; i < 10; i++) {
            mandel.renderFrame(new TextGraphics(buffer2), size);
        }
        double rangeAfter = mandel.getRange();

        assertTrue(rangeAfter < rangeBefore,
                "viewport range should shrink each frame (" + rangeBefore + " -> " + rangeAfter + ")");

        boolean anyDifferent = false;
        for (int r = 0; r < size.rows() && !anyDifferent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (!buffer1.getCell(c, r).equals(buffer2.getCell(c, r))) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "zoomed pattern should differ from the first frame");
    }

    @Test
    @DisplayName("viewport resets after zooming in deeply")
    void viewportResetsAfterDeepZoom() {
        var mandel = new MandelbrotZoom(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        double tinyRange = 0.0004;
        mandel.setRange(tinyRange);

        double beforeReset = mandel.getRange();
        mandel.renderFrame(new TextGraphics(new ScreenBuffer(size)), size);
        double afterReset = mandel.getRange();

        assertTrue(afterReset > beforeReset,
                "range should reset after deep zoom (" + beforeReset + " -> " + afterReset + ")");
        assertEquals(2.5, afterReset, 1e-10,
                "range should reset to the initial value");
    }

    @Test
    @DisplayName("small terminal sizes render without throwing")
    void smallTerminalSizes() {
        var mandel = new MandelbrotZoom(new TerminalSize(1, 1));
        var size = new TerminalSize(1, 1);
        var buffer = new ScreenBuffer(size);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                mandel.renderFrame(new TextGraphics(buffer), size);
            }
        });
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var mandel = new MandelbrotZoom(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> mandel.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var mandel = new MandelbrotZoom(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        mandel.onResize(size);
        assertEquals(size, mandel.lastSize());
    }
}
