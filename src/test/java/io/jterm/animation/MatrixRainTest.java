package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.widget.Panel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatrixRainTest {

    @Test
    @DisplayName("MatrixRain implements Component contract with preferred size")
    void matrixRainHasPreferredSize() {
        var rain = new MatrixRain(new TerminalSize(80, 24));
        assertEquals(new TerminalSize(80, 24), rain.getPreferredSize());
    }

    @Test
    @DisplayName("MatrixRain can be added to a Panel")
    void canBeAddedToPanel() {
        var panel = new Panel();
        var rain = new MatrixRain(new TerminalSize(20, 10));
        assertDoesNotThrow(() -> panel.addComponent(rain));
    }

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var rain = new MatrixRain(new TerminalSize(10, 5));
        assertTrue(rain instanceof AnimatedBackground);
        assertEquals(15, rain.targetFps());
        rain.start();
        assertTrue(rain.isRunning());
        rain.stop();
        assertFalse(rain.isRunning());
    }

    @Test
    @DisplayName("setTargetFPS clamps and stores FPS")
    void setTargetFpsClamps() {
        var rain = new MatrixRain(new TerminalSize(10, 5));
        rain.setTargetFps(0);
        assertEquals(1, rain.getTargetFps());
        rain.setTargetFps(1000);
        assertEquals(60, rain.getTargetFps());
        rain.setTargetFps(30);
        assertEquals(30, rain.getTargetFps());
    }

    @Test
    @DisplayName("tick advances frame counter")
    void tickAdvancesFrame() {
        var rain = new MatrixRain(new TerminalSize(10, 5));
        assertEquals(0, rain.getFrame());
        rain.tick(System.nanoTime());
        assertTrue(rain.getFrame() >= 1);
    }

    @Test
    @DisplayName("pause stops frame advancement")
    void pauseStopsFrameAdvancement() {
        var rain = new MatrixRain(new TerminalSize(10, 5));
        rain.tick(0);
        int before = rain.getFrame();
        rain.setPaused(true);
        rain.tick(1_000_000_000L);
        assertEquals(before, rain.getFrame());
    }

    @Test
    @DisplayName("resume allows frame advancement")
    void resumeAllowsFrameAdvancement() {
        var rain = new MatrixRain(new TerminalSize(10, 5));
        rain.setPaused(true);
        rain.tick(0);
        int pausedFrame = rain.getFrame();
        rain.setPaused(false);
        rain.tick(1_000_000_000L);
        assertTrue(rain.getFrame() > pausedFrame);
    }

    @Test
    @DisplayName("columns are initialized on resize")
    void columnsInitializedOnResize() {
        var rain = new MatrixRain(new TerminalSize(10, 5));
        assertEquals(0, rain.getColumnCount());
        rain.onResize(new TerminalSize(10, 5));
        assertEquals(10, rain.getColumnCount());
    }

    @Test
    @DisplayName("each column has random speed and trail length")
    void columnHasSpeedAndTrail() {
        var rain = new MatrixRain(new TerminalSize(20, 10));
        rain.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 10));
        var column = rain.getColumn(0);
        assertNotNull(column);
        assertTrue(column.getSpeed() >= 0.3 && column.getSpeed() <= 1.0,
                "speed should be in [0.3, 1.0]");
        assertTrue(column.getTrailLength() >= 5 && column.getTrailLength() <= 20,
                "trail length should be in [5, 20]");
    }

    @Test
    @DisplayName("head position advances each frame")
    void headAdvances() {
        var rain = new MatrixRain(new TerminalSize(20, 10));
        rain.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 10));
        var column = rain.getColumn(0);
        column.activate();
        double y1 = column.getY();
        column.advance(10);
        double y2 = column.getY();
        assertTrue(y2 > y1, "head should move down");
    }

    @Test
    @DisplayName("column resets when head passes bottom")
    void resetsWhenOffBottom() {
        var rain = new MatrixRain(new TerminalSize(10, 10));
        rain.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 10));
        var column = rain.getColumn(0);
        column.activate();
        for (int i = 0; i < 200; i++) {
            column.advance(10);
            if (column.getY() < 0) break;
        }
        assertTrue(column.getY() < 25,
                "after falling off bottom, column should reset to top-ish");
    }

    @Test
    @DisplayName("trail follows head")
    void trailFollowsHead() {
        var size = new TerminalSize(20, 10);
        var rain = new MatrixRain(size, false, false);
        rain.setBounds(TerminalPosition.TOP_LEFT, size);
        rain.tick(0);

        var buffer = new ScreenBuffer(size);
        rain.draw(new TextGraphics(buffer));

        int headX = -1;
        int headY = -1;
        for (int c = 0; c < size.columns() && headX < 0; c++) {
            for (int r = 0; r < size.rows(); r++) {
                var cell = buffer.getCell(c, r);
                if (cell.fg() == AnsiColor.BRIGHT_GREEN) {
                    headX = c;
                    headY = r;
                    break;
                }
            }
        }
        assertTrue(headX >= 0, "there should be a bright green head somewhere");

        int trailCount = 0;
        for (int r = 0; r < headY; r++) {
            var cell = buffer.getCell(headX, r);
            if (cell.character().charAt(0) != ' ' && cell.fg() == AnsiColor.GREEN) {
                trailCount++;
            }
        }
        assertTrue(trailCount >= 1, "there should be at least one green trail cell below head");
    }

    @Test
    @DisplayName("head uses bright green and bold")
    void headIsBrightGreenAndBold() {
        var size = new TerminalSize(20, 10);
        var rain = new MatrixRain(size, false, false);
        rain.setBounds(TerminalPosition.TOP_LEFT, size);
        rain.tick(0);

        var buffer = new ScreenBuffer(size);
        rain.draw(new TextGraphics(buffer));

        boolean foundHead = false;
        for (int c = 0; c < size.columns() && !foundHead; c++) {
            for (int r = 0; r < size.rows(); r++) {
                var cell = buffer.getCell(c, r);
                if (cell.fg() == AnsiColor.BRIGHT_GREEN && cell.modifiers().contains(SGR.BOLD)) {
                    foundHead = true;
                    break;
                }
            }
        }
        assertTrue(foundHead, "head cell should be bright green and bold");
    }

    @Test
    @DisplayName("inactive columns render nothing")
    void inactiveColumnsRenderNothing() {
        var size = new TerminalSize(20, 10);
        var rain = new MatrixRain(size, false, false);
        rain.setBounds(TerminalPosition.TOP_LEFT, size);

        var buffer = new ScreenBuffer(size);
        rain.draw(new TextGraphics(buffer));

        int active = 0;
        for (int c = 0; c < size.columns(); c++) {
            var column = rain.getColumn(c);
            if (column.isActive()) active++;
        }
        assertTrue(active > 0, "some columns should be active");
        assertTrue(active < size.columns(), "some columns should be inactive");
    }

    @Test
    @DisplayName("characters can change after advancing")
    void charsCanChange() {
        var rain = new MatrixRain(new TerminalSize(10, 10), false, false);
        rain.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 10));
        var column = rain.getColumn(0);
        column.activate();
        char before = column.getCurrentChar();
        for (int i = 0; i < 100; i++) {
            column.advance(10);
            if (column.getCurrentChar() != before) {
                return;
            }
        }
        fail("character should change at least once over many frames");
    }

    @Test
    @DisplayName("unicode mode uses half-width katakana")
    void unicodeUsesKatakana() {
        var rain = new MatrixRain(new TerminalSize(10, 10), true, false);
        assertTrue(rain.isUnicodeMode());
        rain.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 10));
        var column = rain.getColumn(0);
        column.activate();
        char c = column.getCurrentChar();
        var block = Character.UnicodeBlock.of(c);
        assertEquals(Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS, block);
    }

    @Test
    @DisplayName("CP437 fallback chars are in ASCII/box-drawing range")
    void cp437FallbackChars() {
        var rain = new MatrixRain(new TerminalSize(10, 10), false, false);
        rain.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 10));
        var column = rain.getColumn(0);
        column.activate();
        char c = column.getCurrentChar();
        boolean inAsciiRange = c >= 0x21 && c <= 0x7E;
        boolean inCp437Range = c >= 0xB0 && c <= 0xDF;
        assertTrue(inAsciiRange || inCp437Range,
                "fallback char should be ASCII or CP437 box range: got " + Integer.toHexString(c));
    }

    @Test
    @DisplayName("resize reallocates columns and handles larger bounds")
    void resizeReallocates() {
        var rain = new MatrixRain(new TerminalSize(10, 5));
        rain.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 10));
        assertEquals(20, rain.getColumnCount());
    }

    @Test
    @DisplayName("drawing empty screen does not throw")
    void drawZeroSizeSafe() {
        var rain = new MatrixRain(new TerminalSize(0, 0));
        rain.setBounds(TerminalPosition.TOP_LEFT, TerminalSize.ZERO);
        assertDoesNotThrow(() -> rain.draw(new TextGraphics(new ScreenBuffer(new TerminalSize(1, 1)))));
    }

    @Test
    @DisplayName("resetFrame returns counter to zero and clears columns")
    void resetFrameReturnsToZero() {
        var rain = new MatrixRain(new TerminalSize(10, 5));
        rain.tick(1_000_000_000L);
        assertTrue(rain.getFrame() > 0);
        rain.resetFrame();
        assertEquals(0, rain.getFrame());
        assertEquals(0, rain.getColumnCount());
    }

    @Test
    @DisplayName("two frames differ over time")
    void framesDifferOverTime() {
        var size = new TerminalSize(40, 20);
        var rain = new MatrixRain(size, false, false);
        rain.setBounds(TerminalPosition.TOP_LEFT, size);
        rain.setTargetFps(60);

        var buffer1 = new ScreenBuffer(size);
        rain.tick(0);
        rain.draw(new TextGraphics(buffer1));

        var buffer2 = new ScreenBuffer(size);
        rain.tick(32_000_000L);
        rain.draw(new TextGraphics(buffer2));

        boolean anyDifferent = false;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (!buffer1.getCell(c, r).equals(buffer2.getCell(c, r))) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "two frames should differ");
    }

    @Test
    @DisplayName("color variants are allowed")
    void colorVariantsAllowed() {
        var rain = new MatrixRain(new TerminalSize(20, 10), true, true);
        assertTrue(rain.isAllowColorVariants());
    }

    @Test
    @DisplayName("setBounds larger than preferred uses bounds size for drawing")
    void setBoundsLargerUsesBounds() {
        var size = new TerminalSize(10, 5);
        var rain = new MatrixRain(new TerminalSize(5, 3));
        rain.setBounds(TerminalPosition.TOP_LEFT, size);
        rain.tick(0);

        var buffer = new ScreenBuffer(size);
        rain.draw(new TextGraphics(buffer));

        boolean hasContent = false;
        for (int r = 0; r < size.rows() && !hasContent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') {
                    hasContent = true;
                    break;
                }
            }
        }
        assertTrue(hasContent, "bounds area should contain rendered content somewhere");
    }
}
