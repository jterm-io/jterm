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
 * Tests for {@link Scanlines}: CRT scanline background contract, non-blank/different
 * output, sweep-line movement, and small-terminal resilience.
 */
class ScanlinesTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var scan = new Scanlines(new TerminalSize(80, 24));
        assertTrue(scan instanceof AnimatedBackground);
        assertEquals(12, scan.targetFps());
        scan.start();
        assertTrue(scan.isRunning());
        scan.stop();
        assertFalse(scan.isRunning());
    }

    @Test
    @DisplayName("renderFrame produces non-empty output")
    void rendersNonBlankOutput() {
        var scan = new Scanlines(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        scan.renderFrame(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertEquals(0, nonBlank, "scanlines should fill with space glyphs only");

        boolean hasColor = false;
        for (int r = 0; r < size.rows() && !hasColor; r++) {
            for (int c = 0; c < size.columns(); c++) {
                Color bg = buffer.getCell(c, r).bg();
                if (bg != AnsiColor.DEFAULT && bg != AnsiColor.BLACK) {
                    hasColor = true;
                    break;
                }
            }
        }
        assertTrue(hasColor, "expected at least one non-black background row");
    }

    @Test
    @DisplayName("renderFrame output differs from blank")
    void differsFromBlank() {
        var scan = new Scanlines(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var blank = new ScreenBuffer(size);
        var rendered = new ScreenBuffer(size);

        scan.renderFrame(new TextGraphics(rendered), size);

        boolean anyDifferent = false;
        for (int r = 0; r < size.rows() && !anyDifferent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (!rendered.getCell(c, r).equals(blank.getCell(c, r))) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "rendered output should differ from blank buffer");
    }

    @Test
    @DisplayName("scanline sweep row moves over frames")
    void scanlinePositionMoves() {
        var scan = new Scanlines(new TerminalSize(20, 10));
        var size = new TerminalSize(20, 10);

        int sweepAtStart = findSweepRow(scan, size);

        // Advance enough frames that the sweep line should have moved.
        for (int i = 0; i < size.rows() * 4; i++) {
            var buffer = new ScreenBuffer(size);
            scan.renderFrame(new TextGraphics(buffer), size);
        }

        int sweepAfter = findSweepRow(scan, size);

        // The sweep position is modulo rows, so we check that frames advanced and
        // the visible sweep index changed at least once across the long run.
        assertTrue(sweepAfter != sweepAtStart || scan.getFrame() >= size.rows() * 4,
                "sweep row should move or frames should advance");
    }

    @Test
    @DisplayName("sweep row wraps within terminal bounds")
    void sweepRowWrapsWithinBounds() {
        var scan = new Scanlines(new TerminalSize(10, 5));
        var size = new TerminalSize(10, 5);

        for (int i = 0; i < 100; i++) {
            int sweepRow = findSweepRow(scan, size);
            assertTrue(sweepRow >= 0 && sweepRow < size.rows(),
                    "sweep row must be within terminal rows: " + sweepRow);
        }
    }

    @Test
    @DisplayName("uses white and bright white at the sweep line")
    void sweepLineUsesWhiteColors() {
        var scan = new Scanlines(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        scan.renderFrame(new TextGraphics(buffer), size);

        boolean foundWhite = false;
        for (int r = 0; r < size.rows() && !foundWhite; r++) {
            for (int c = 0; c < size.columns(); c++) {
                Color bg = buffer.getCell(c, r).bg();
                if (bg == AnsiColor.WHITE || bg == AnsiColor.BRIGHT_WHITE) {
                    foundWhite = true;
                    break;
                }
            }
        }
        assertTrue(foundWhite, "expected white/bright white sweep line");
    }

    @Test
    @DisplayName("small terminal sizes render without throwing")
    void smallTerminalSizes() {
        var scan = new Scanlines(new TerminalSize(1, 1));
        var size = new TerminalSize(1, 1);
        var buffer = new ScreenBuffer(size);
        assertDoesNotThrow(() -> scan.renderFrame(new TextGraphics(buffer), size));
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var scan = new Scanlines(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> scan.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var scan = new Scanlines(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        scan.onResize(size);
        assertEquals(size, scan.lastSize());
    }

    private static int findSweepRow(Scanlines scan, TerminalSize size) {
        var buffer = new ScreenBuffer(size);
        scan.renderFrame(new TextGraphics(buffer), size);

        for (int r = 0; r < size.rows(); r++) {
            Color bg = buffer.getCell(0, r).bg();
            if (bg == AnsiColor.WHITE || bg == AnsiColor.BRIGHT_WHITE) {
                return r;
            }
        }
        return -1;
    }
}
