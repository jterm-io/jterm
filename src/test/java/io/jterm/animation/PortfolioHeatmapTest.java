package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PortfolioHeatmap}: AnimatedBackground contract, fallback
 * data rendering, heatmap coloring by P&amp;L, ticker sizing by weight, and edge
 * cases.
 */
class PortfolioHeatmapTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var heatmap = new PortfolioHeatmap(new TerminalSize(80, 24));
        assertTrue(heatmap instanceof AnimatedBackground);
        assertEquals(4, heatmap.targetFps());
        heatmap.start();
        assertTrue(heatmap.isRunning());
        heatmap.stop();
        assertFalse(heatmap.isRunning());
    }

    @Test
    @DisplayName("default supplier provides fallback positions")
    void defaultSupplierProvidesFallback() {
        var heatmap = new PortfolioHeatmap(new TerminalSize(80, 24));
        assertFalse(heatmap.getPositions().isEmpty(), "expected fallback positions");
    }

    @Test
    @DisplayName("custom supplier overrides fallback data")
    void customSupplierOverridesFallback() {
        var pos = new PortfolioHeatmap.Position("TEST", 10, 100.0, 110.0);
        var heatmap = new PortfolioHeatmap(new TerminalSize(80, 24), () -> List.of(pos));
        assertEquals(List.of(pos), heatmap.getPositions());
    }

    @Test
    @DisplayName("renderFrame clears background to black")
    void clearsBackgroundToBlack() {
        var heatmap = new PortfolioHeatmap(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        heatmap.renderFrame(new TextGraphics(buffer), size);
        assertEquals(AnsiColor.BLACK, buffer.getCell(0, 0).bg());
        assertEquals(AnsiColor.BLACK, buffer.getCell(79, 23).bg());
    }

    @Test
    @DisplayName("renderFrame produces non-blank output")
    void rendersNonBlankOutput() {
        var heatmap = new PortfolioHeatmap(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        heatmap.renderFrame(new TextGraphics(buffer), size);

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
    @DisplayName("ticker symbols from supplier are rendered")
    void rendersTickerSymbols() {
        var pos = new PortfolioHeatmap.Position("ZOOM", 10, 100.0, 110.0);
        var heatmap = new PortfolioHeatmap(new TerminalSize(80, 24), () -> List.of(pos));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        heatmap.renderFrame(new TextGraphics(buffer), size);

        boolean found = false;
        for (int r = 0; r < size.rows() && !found; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().equals("Z")
                        || buffer.getCell(c, r).character().equals("O")) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found, "expected ticker symbol to appear in render output");
    }

    @Test
    @DisplayName("gains are green and losses are red")
    void gainsGreenLossesRed() {
        var positions = List.of(
                new PortfolioHeatmap.Position("UP", 10, 100.0, 115.0),
                new PortfolioHeatmap.Position("DOWN", 10, 100.0, 85.0)
        );
        var heatmap = new PortfolioHeatmap(new TerminalSize(80, 24), () -> positions);
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        heatmap.renderFrame(new TextGraphics(buffer), size);

        Set<AnsiColor> backgrounds = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                backgrounds.add((AnsiColor) buffer.getCell(c, r).bg());
            }
        }
        assertTrue(backgrounds.contains(AnsiColor.BRIGHT_GREEN), "expected green background for gain");
        assertTrue(backgrounds.contains(AnsiColor.BRIGHT_RED), "expected red background for loss");
    }

    @Test
    @DisplayName("larger positions occupy more screen area than smaller ones")
    void largerPositionsUseMoreArea() {
        var small = new PortfolioHeatmap.Position("S", 1, 1.0, 1.0);
        var big = new PortfolioHeatmap.Position("B", 1000, 1.0, 1.0);
        var heatmap = new PortfolioHeatmap(new TerminalSize(80, 24), () -> List.of(big, small));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        heatmap.renderFrame(new TextGraphics(buffer), size);

        int bigArea = countBackgroundCells(buffer, size, "B");
        int smallArea = countBackgroundCells(buffer, size, "S");
        assertTrue(bigArea > smallArea,
                "expected larger position to occupy more cells: " + bigArea + " vs " + smallArea);
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var heatmap = new PortfolioHeatmap(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> heatmap.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("small terminal sizes render without throwing")
    void smallTerminalSizes() {
        var heatmap = new PortfolioHeatmap(new TerminalSize(10, 3));
        var size = new TerminalSize(10, 3);
        var buffer = new ScreenBuffer(size);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                heatmap.renderFrame(new TextGraphics(buffer), size);
            }
        });
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var heatmap = new PortfolioHeatmap(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        heatmap.onResize(size);
        assertEquals(size, heatmap.lastSize());
    }

    @Test
    @DisplayName("position daily P&amp;L percent is computed correctly")
    void dailyPnlPercent() {
        var up = new PortfolioHeatmap.Position("UP", 10, 100.0, 110.0);
        assertEquals(10.0, up.dailyPnlPct(), 0.001);

        var down = new PortfolioHeatmap.Position("DOWN", 10, 100.0, 90.0);
        assertEquals(-10.0, down.dailyPnlPct(), 0.001);

        var flat = new PortfolioHeatmap.Position("FLAT", 10, 100.0, 100.0);
        assertEquals(0.0, flat.dailyPnlPct(), 0.001);
    }

    @Test
    @DisplayName("position market value is computed correctly")
    void marketValue() {
        var pos = new PortfolioHeatmap.Position("SPY", 50, 400.0, 500.0);
        assertEquals(25000.0, pos.marketValue(), 0.001);
    }

    private static int countBackgroundCells(ScreenBuffer buffer, TerminalSize size, String ticker) {
        int count = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().equals(ticker)) {
                    // Count all non-black background cells in the same column up to this row
                    // as a rough proxy for the cell's area (works for single-column ticker).
                    for (int rr = 0; rr < size.rows(); rr++) {
                        if (!buffer.getCell(c, rr).bg().equals(AnsiColor.BLACK)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }
}
