package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DvdLogo}: animated background contract, bouncing behavior,
 * motion over frames, color changes on corner hits, small terminal resilience.
 */
class DvdLogoTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var dvd = new DvdLogo(new TerminalSize(80, 24));
        assertTrue(dvd instanceof AnimatedBackground);
        assertEquals(15, dvd.targetFps());
        dvd.start();
        assertTrue(dvd.isRunning());
        dvd.stop();
        assertFalse(dvd.isRunning());
    }

    @Test
    @DisplayName("renderFrame produces non-empty output")
    void rendersNonBlankOutput() {
        var dvd = new DvdLogo(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        dvd.renderFrame(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') {
                    nonBlank++;
                }
            }
        }
        assertTrue(nonBlank > 0, "expected non-blank rendered output");
    }

    @Test
    @DisplayName("logo position moves over frames")
    void positionMovesOverFrames() {
        var dvd = new DvdLogo(new TerminalSize(40, 20));
        var size = new TerminalSize(40, 20);
        var buffer = new ScreenBuffer(size);

        int[] start = dvd.getPosition();
        dvd.renderFrame(new TextGraphics(buffer), size);
        int[] afterOne = dvd.getPosition();

        assertNotEquals(start[0], afterOne[0], "logo x should change after one frame");
        assertNotEquals(start[1], afterOne[1], "logo y should change after one frame");
    }

    @Test
    @DisplayName("logo reverses horizontal direction at left edge")
    void bouncesOffLeftEdge() {
        var dvd = new DvdLogo(new TerminalSize(40, 20));
        var size = new TerminalSize(40, 20);
        var buffer = new ScreenBuffer(size);

        dvd.setPosition(0, 5);
        dvd.setDirection(-1, 1);
        dvd.renderFrame(new TextGraphics(buffer), size);

        int[] dir = dvd.getDirection();
        assertEquals(1, dir[0], "horizontal direction should reverse at left edge");
    }

    @Test
    @DisplayName("logo reverses horizontal direction at right edge")
    void bouncesOffRightEdge() {
        var dvd = new DvdLogo(new TerminalSize(40, 20));
        var size = new TerminalSize(40, 20);
        var buffer = new ScreenBuffer(size);

        dvd.setPosition(37, 5);
        dvd.setDirection(1, 1);
        dvd.renderFrame(new TextGraphics(buffer), size);

        int[] dir = dvd.getDirection();
        assertEquals(-1, dir[0], "horizontal direction should reverse at right edge");
    }

    @Test
    @DisplayName("logo reverses vertical direction at top edge")
    void bouncesOffTopEdge() {
        var dvd = new DvdLogo(new TerminalSize(40, 20));
        var size = new TerminalSize(40, 20);
        var buffer = new ScreenBuffer(size);

        dvd.setPosition(5, 0);
        dvd.setDirection(1, -1);
        dvd.renderFrame(new TextGraphics(buffer), size);

        int[] dir = dvd.getDirection();
        assertEquals(1, dir[1], "vertical direction should reverse at top edge");
    }

    @Test
    @DisplayName("logo reverses vertical direction at bottom edge")
    void bouncesOffBottomEdge() {
        var dvd = new DvdLogo(new TerminalSize(40, 20));
        var size = new TerminalSize(40, 20);
        var buffer = new ScreenBuffer(size);

        dvd.setPosition(5, 19);
        dvd.setDirection(1, 1);
        dvd.renderFrame(new TextGraphics(buffer), size);

        int[] dir = dvd.getDirection();
        assertEquals(-1, dir[1], "vertical direction should reverse at bottom edge");
    }

    @Test
    @DisplayName("logo changes color on corner hit")
    void changesColorOnCornerHit() {
        var dvd = new DvdLogo(new TerminalSize(40, 20));
        var size = new TerminalSize(40, 20);
        var buffer = new ScreenBuffer(size);

        dvd.setPosition(0, 0);
        dvd.setDirection(-1, -1);
        AnsiColor before = dvd.getColor();
        dvd.renderFrame(new TextGraphics(buffer), size);
        AnsiColor after = dvd.getColor();

        assertNotEquals(before, after, "color should change when logo hits a corner");
    }

    @Test
    @DisplayName("small terminal sizes render without throwing")
    void smallTerminalSizes() {
        var dvd = new DvdLogo(new TerminalSize(2, 2));
        var size = new TerminalSize(2, 2);
        var buffer = new ScreenBuffer(size);

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                dvd.renderFrame(new TextGraphics(buffer), size);
            }
        });
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var dvd = new DvdLogo(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> dvd.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var dvd = new DvdLogo(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        dvd.onResize(size);
        assertEquals(size, dvd.lastSize());
    }
}
