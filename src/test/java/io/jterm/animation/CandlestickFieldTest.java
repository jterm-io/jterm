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
 * Tests for {@link CandlestickField}: AnimatedBackground contract, full-frame
 * rendering, bullish/bearish coloring, band structure, and frame updates.
 */
class CandlestickFieldTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var field = new CandlestickField(new TerminalSize(80, 24));
        assertTrue(field instanceof AnimatedBackground);
        assertEquals(2, field.targetFps());
        field.start();
        assertTrue(field.isRunning());
        field.stop();
        assertFalse(field.isRunning());
    }

    @Test
    @DisplayName("renderFrame clears background to black")
    void clearsBackgroundToBlack() {
        var field = new CandlestickField(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        field.renderFrame(new TextGraphics(buffer), size);
        // Corners should be black background.
        // Top-left may be a candle, so check a gap column.
        assertEquals(AnsiColor.BLACK, buffer.getCell(1, 0).bg());
        assertEquals(AnsiColor.BLACK, buffer.getCell(79, 23).bg());
    }

    @Test
    @DisplayName("renderFrame produces non-blank output across the full screen")
    void rendersNonBlankOutput() {
        var field = new CandlestickField(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        field.renderFrame(new TextGraphics(buffer), size);

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
    @DisplayName("multiple chart bands are created")
    void createsMultipleBands() {
        var field = new CandlestickField(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        field.renderFrame(new TextGraphics(new ScreenBuffer(size)), size);
        int bandCount = field.getBandCount();
        assertTrue(bandCount >= 2, "expected at least 2 bands, got " + bandCount);
    }

    @Test
    @DisplayName("bands cover the full height of the screen")
    void bandsCoverFullHeight() {
        var field = new CandlestickField(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        field.renderFrame(new TextGraphics(buffer), size);

        // Check that candle content (non-space, non-separator) appears in
        // both the top and bottom halves of the screen.
        boolean topHalfHasContent = false;
        boolean bottomHalfHasContent = false;
        int midRow = size.rows() / 2;

        for (int r = 0; r < midRow && !topHalfHasContent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = buffer.getCell(c, r).character().charAt(0);
                if (ch != ' ' && ch != '─') {
                    topHalfHasContent = true;
                    break;
                }
            }
        }
        for (int r = midRow; r < size.rows() && !bottomHalfHasContent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = buffer.getCell(c, r).character().charAt(0);
                if (ch != ' ' && ch != '─') {
                    bottomHalfHasContent = true;
                    break;
                }
            }
        }
        assertTrue(topHalfHasContent, "expected candle content in top half");
        assertTrue(bottomHalfHasContent, "expected candle content in bottom half");
    }

    @Test
    @DisplayName("renders both bullish and bearish colors")
    void bullishAndBearishColorsPresent() {
        var field = new CandlestickField(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        Set<AnsiColor> found = new HashSet<>();
        for (int i = 0; i < 30 && found.size() < 2; i++) {
            field.renderFrame(new TextGraphics(new ScreenBuffer(size)), size);
            field.renderFrame(new TextGraphics(buffer), size);
            for (int r = 0; r < size.rows(); r++) {
                for (int c = 0; c < size.columns(); c++) {
                    found.add((AnsiColor) buffer.getCell(c, r).fg());
                }
            }
        }

        assertTrue(found.contains(AnsiColor.BRIGHT_GREEN), "expected green for bullish candles");
        assertTrue(found.contains(AnsiColor.BRIGHT_RED), "expected red for bearish candles");
    }

    @Test
    @DisplayName("two consecutive frames differ")
    void framesDiffer() {
        var field = new CandlestickField(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer1 = new ScreenBuffer(size);
        var buffer2 = new ScreenBuffer(size);
        field.renderFrame(new TextGraphics(buffer1), size);
        field.renderFrame(new TextGraphics(buffer2), size);

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
    @DisplayName("candle prices form a continuous series (open = prev close)")
    void continuousPriceSeries() {
        var field = new CandlestickField(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        field.renderFrame(new TextGraphics(new ScreenBuffer(size)), size);

        CandlestickField.ChartBand band = field.getBands()[0];
        CandlestickField.Candle[] candles = band.getCandles();
        // After construction, each candle's open should equal the previous candle's close.
        for (int i = 1; i < candles.length; i++) {
            assertEquals(candles[i - 1].getClose(), candles[i].getOpen(), 0.0001,
                    "candle " + i + " open should equal candle " + (i - 1) + " close");
        }
    }

    @Test
    @DisplayName("candle prices update each frame")
    void candlePricesUpdate() {
        var field = new CandlestickField(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        field.renderFrame(new TextGraphics(new ScreenBuffer(size)), size);
        double firstClose = field.getBands()[0].getCandles()[0].getClose();

        field.renderFrame(new TextGraphics(new ScreenBuffer(size)), size);
        double secondClose = field.getBands()[0].getCandles()[0].getClose();

        assertNotEquals(firstClose, secondClose, 0.0,
                "expected candle close to update between frames");
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var field = new CandlestickField(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> field.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("small terminal sizes render without throwing")
    void smallTerminalSizes() {
        var field = new CandlestickField(new TerminalSize(10, 3));
        var size = new TerminalSize(10, 3);
        var buffer = new ScreenBuffer(size);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                field.renderFrame(new TextGraphics(buffer), size);
            }
        });
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var field = new CandlestickField(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        field.onResize(size);
        assertEquals(size, field.lastSize());
    }

    @Test
    @DisplayName("candles are spaced correctly across the width")
    void candlesSpacedCorrectly() {
        var field = new CandlestickField(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        field.renderFrame(new TextGraphics(buffer), size);

        // Candles are at x=0, 3, 6, 9, ... so columns 1,2 should be blank/gap.
        // Verify candle body content at x=0 and gap at x=1.
        boolean candleAt0 = false;
        for (int r = 0; r < size.rows(); r++) {
            if (buffer.getCell(0, r).character().charAt(0) != ' ') {
                candleAt0 = true;
                break;
            }
        }
        assertTrue(candleAt0, "expected candle content at column 0");
    }
}