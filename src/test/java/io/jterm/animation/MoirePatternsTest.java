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
 * Tests for {@link MoirePatterns}: AnimatedBackground contract, non-empty render,
 * animation changes over frames, and small-terminal resilience.
 */
class MoirePatternsTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var moire = new MoirePatterns(new TerminalSize(80, 24));
        assertTrue(moire instanceof AnimatedBackground);
        assertEquals(9, moire.targetFps());
        moire.start();
        assertTrue(moire.isRunning());
        moire.stop();
        assertFalse(moire.isRunning());
    }

    @Test
    @DisplayName("renderFrame produces non-empty output")
    void rendersNonBlankOutput() {
        var moire = new MoirePatterns(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        moire.renderFrame(new TextGraphics(buffer), size);

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
    @DisplayName("renderFrame output differs from blank buffer")
    void differsFromBlank() {
        var moire = new MoirePatterns(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var blank = new ScreenBuffer(size);
        var rendered = new ScreenBuffer(size);

        moire.renderFrame(new TextGraphics(rendered), size);

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
    @DisplayName("pattern changes over frames as angles rotate")
    void patternChangesOverFrames() {
        var moire = new MoirePatterns(new TerminalSize(40, 20));
        var size = new TerminalSize(40, 20);

        var buffer1 = new ScreenBuffer(size);
        moire.renderFrame(new TextGraphics(buffer1), size);

        double angleABefore = moire.getAngleA();
        double angleBBefore = moire.getAngleB();

        // Render enough frames that the rotation should visibly shift the pattern.
        var buffer2 = new ScreenBuffer(size);
        for (int i = 0; i < 20; i++) {
            moire.renderFrame(new TextGraphics(buffer2), size);
        }

        assertNotEquals(angleABefore, moire.getAngleA(), "angleA should rotate");
        assertNotEquals(angleBBefore, moire.getAngleB(), "angleB should rotate");

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
    @DisplayName("uses cyan/blue/green-ish palette for grid colors")
    void usesExpectedColors() {
        var moire = new MoirePatterns(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        moire.renderFrame(new TextGraphics(buffer), size);

        boolean foundExpected = false;
        for (int r = 0; r < size.rows() && !foundExpected; r++) {
            for (int c = 0; c < size.columns(); c++) {
                Color fg = buffer.getCell(c, r).fg();
                if (fg == AnsiColor.CYAN || fg == AnsiColor.BLUE || fg == AnsiColor.BRIGHT_CYAN) {
                    foundExpected = true;
                    break;
                }
            }
        }
        assertTrue(foundExpected, "expected cyan/blue colors in output");
    }

    @Test
    @DisplayName("overlapValue is bounded in [0, 1]")
    void overlapValueBounded() {
        var moire = new MoirePatterns(new TerminalSize(20, 10));
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 20; x++) {
                double v = moire.overlapValue(x, y, 10.0, 5.0);
                assertTrue(v >= 0.0 && v <= 1.0,
                        "overlapValue at (" + x + "," + y + ") out of bounds: " + v);
            }
        }
    }

    @Test
    @DisplayName("small terminal sizes render without throwing")
    void smallTerminalSizes() {
        var moire = new MoirePatterns(new TerminalSize(1, 1));
        var size = new TerminalSize(1, 1);
        var buffer = new ScreenBuffer(size);
        assertDoesNotThrow(() -> moire.renderFrame(new TextGraphics(buffer), size));
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var moire = new MoirePatterns(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> moire.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var moire = new MoirePatterns(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        moire.onResize(size);
        assertEquals(size, moire.lastSize());
    }
}
