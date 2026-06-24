package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MarketDepth}: AnimatedBackground contract, bid/ask ladder
 * rendering, mid-price display, market movement, and edge cases.
 */
class MarketDepthTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var depth = new MarketDepth(new TerminalSize(80, 24));
        assertTrue(depth instanceof AnimatedBackground);
        assertEquals(5, depth.targetFps());
        depth.start();
        assertTrue(depth.isRunning());
        depth.stop();
        assertFalse(depth.isRunning());
    }

    @Test
    @DisplayName("renderFrame clears background to black")
    void clearsBackgroundToBlack() {
        var depth = new MarketDepth(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        depth.renderFrame(new TextGraphics(buffer), size);
        assertEquals(AnsiColor.BLACK, buffer.getCell(0, 0).bg());
        assertEquals(AnsiColor.BLACK, buffer.getCell(79, 23).bg());
    }

    @Test
    @DisplayName("renderFrame produces non-blank output")
    void rendersNonBlankOutput() {
        var depth = new MarketDepth(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        depth.renderFrame(new TextGraphics(buffer), size);

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
    @DisplayName("bid side is green and ask side is red")
    void bidGreenAskRed() {
        var depth = new MarketDepth(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        depth.setMidPrice(100.0);
        var buffer = new ScreenBuffer(size);
        depth.renderFrame(new TextGraphics(buffer), size);

        Set<AnsiColor> leftColors = new HashSet<>();
        Set<AnsiColor> rightColors = new HashSet<>();
        int mid = size.columns() / 2;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < mid; c++) {
                leftColors.add((AnsiColor) buffer.getCell(c, r).fg());
            }
            for (int c = mid; c < size.columns(); c++) {
                rightColors.add((AnsiColor) buffer.getCell(c, r).fg());
            }
        }

        assertTrue(leftColors.contains(AnsiColor.BRIGHT_GREEN), "expected green on bid side");
        assertTrue(rightColors.contains(AnsiColor.BRIGHT_RED), "expected red on ask side");
    }

    @Test
    @DisplayName("mid price is shown in the center")
    void midPriceInCenter() {
        var depth = new MarketDepth(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        depth.setMidPrice(100.0);
        var buffer = new ScreenBuffer(size);
        depth.renderFrame(new TextGraphics(buffer), size);

        int centerRow = size.rows() / 2;
        StringBuilder centerText = new StringBuilder();
        for (int c = 0; c < size.columns(); c++) {
            centerText.append(buffer.getCell(c, centerRow).character());
        }
        assertTrue(centerText.toString().contains("100.0"),
                "expected mid price 100.0 in center row");
    }

    @Test
    @DisplayName("mid price changes each frame")
    void midPriceChanges() {
        var depth = new MarketDepth(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        depth.renderFrame(new TextGraphics(new ScreenBuffer(size)), size);
        double first = depth.getMidPrice();

        depth.renderFrame(new TextGraphics(new ScreenBuffer(size)), size);
        double second = depth.getMidPrice();

        assertNotEquals(first, second, 0.0, "expected mid price to move between frames");
    }

    @Test
    @DisplayName("two consecutive frames differ")
    void framesDiffer() {
        var depth = new MarketDepth(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer1 = new ScreenBuffer(size);
        var buffer2 = new ScreenBuffer(size);
        depth.renderFrame(new TextGraphics(buffer1), size);
        depth.renderFrame(new TextGraphics(buffer2), size);

        boolean anyDifferent = false;
        for (int r = 0; r < size.rows() && !anyDifferent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (!buffer1.getCell(c, r).equals(buffer2.getCell(c, r))) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "expected consecutive frames to differ");
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var depth = new MarketDepth(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> depth.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("small terminal sizes render without throwing")
    void smallTerminalSizes() {
        var depth = new MarketDepth(new TerminalSize(10, 3));
        var size = new TerminalSize(10, 3);
        var buffer = new ScreenBuffer(size);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                depth.renderFrame(new TextGraphics(buffer), size);
            }
        });
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var depth = new MarketDepth(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        depth.onResize(size);
        assertEquals(size, depth.lastSize());
    }

    @Test
    @DisplayName("size arrays are resized on construction and resize")
    void sizeArraysResized() {
        var depth = new MarketDepth(new TerminalSize(80, 24));
        assertEquals(12, depth.getBidSizes().length);
        assertEquals(12, depth.getAskSizes().length);

        depth.onResize(new TerminalSize(80, 10));
        assertEquals(5, depth.getBidSizes().length);
        assertEquals(5, depth.getAskSizes().length);
    }
}
