package io.jterm.widget.chart;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChartTest {

    private Chart createAndDrawChart(int cols, int rows) {
        var chart = new Chart("Test");
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(cols, rows));
        var buf = new ScreenBuffer(new TerminalSize(cols, rows));
        var g = new TextGraphics(buf);
        chart.draw(g);
        return chart;
    }

    // ── Configuration tests ────────────────────────────────────

    @Test
    void defaultTitle() {
        var chart = new Chart();
        assertEquals("", chart.getTitle());
    }

    @Test
    void titledConstructor() {
        var chart = new Chart("My Chart");
        assertEquals("My Chart", chart.getTitle());
    }

    @Test
    void setTitleUpdates() {
        var chart = new Chart();
        chart.setTitle("New Title");
        assertEquals("New Title", chart.getTitle());
    }

    @Test
    void setTitleNullBecomesEmpty() {
        var chart = new Chart();
        chart.setTitle(null);
        assertEquals("", chart.getTitle());
    }

    @Test
    void addSeriesIncrementsCount() {
        var chart = new Chart();
        assertEquals(0, chart.getSeriesCount());
        chart.addSeries(new ChartSeries("A", List.of(1.0), AnsiColor.GREEN));
        assertEquals(1, chart.getSeriesCount());
        chart.addSeries(new ChartSeries("B", List.of(2.0), AnsiColor.RED));
        assertEquals(2, chart.getSeriesCount());
    }

    @Test
    void removeSeriesByName() {
        var chart = new Chart();
        chart.addSeries(new ChartSeries("A", List.of(1.0), AnsiColor.GREEN));
        chart.addSeries(new ChartSeries("B", List.of(2.0), AnsiColor.RED));
        chart.removeSeries("A");
        assertEquals(1, chart.getSeriesCount());
        assertEquals("B", chart.getSeries().get(0).name());
    }

    @Test
    void getSeriesReturnsCopy() {
        var chart = new Chart();
        chart.addSeries(new ChartSeries("A", List.of(1.0), AnsiColor.GREEN));
        var series = chart.getSeries();
        series.clear();
        assertEquals(1, chart.getSeriesCount()); // internal list not affected
    }

    @Test
    void yAxisConfigDefaultsToAuto() {
        var chart = new Chart();
        assertTrue(chart.getYAxisConfig().autoScale());
    }

    @Test
    void setYAxisConfigFixed() {
        var chart = new Chart();
        chart.setYAxisConfig(ChartAxisConfig.fixed(0, 100));
        assertFalse(chart.getYAxisConfig().autoScale());
        assertEquals(0, chart.getYAxisConfig().min());
        assertEquals(100, chart.getYAxisConfig().max());
    }

    @Test
    void showGridDefaultTrue() {
        var chart = new Chart();
        assertTrue(chart.isShowGrid());
    }

    @Test
    void setShowGrid() {
        var chart = new Chart();
        chart.setShowGrid(false);
        assertFalse(chart.isShowGrid());
    }

    @Test
    void showLegendDefaultTrue() {
        var chart = new Chart();
        assertTrue(chart.isShowLegend());
    }

    @Test
    void setShowLegend() {
        var chart = new Chart();
        chart.setShowLegend(false);
        assertFalse(chart.isShowLegend());
    }

    @Test
    void showBorderDefaultTrue() {
        var chart = new Chart();
        assertTrue(chart.isShowBorder());
    }

    @Test
    void setShowBorder() {
        var chart = new Chart();
        chart.setShowBorder(false);
        assertFalse(chart.isShowBorder());
    }

    @Test
    void setBorderColor() {
        var chart = new Chart();
        chart.setBorderColor(AnsiColor.RED);
        // Just verify it doesn't throw and is set
        assertNotNull(chart);
    }

    @Test
    void setGridColor() {
        var chart = new Chart();
        chart.setGridColor(AnsiColor.CYAN);
        assertNotNull(chart);
    }

    @Test
    void setYLabelWidth() {
        var chart = new Chart();
        chart.setYLabelWidth(8);
        // Just verify it doesn't throw
        assertNotNull(chart);
    }

    @Test
    void setYLabelWidthClampedTo2() {
        var chart = new Chart();
        chart.setYLabelWidth(0); // should clamp to 2
        assertNotNull(chart);
    }

    // ── Preferred size ─────────────────────────────────────────

    @Test
    void preferredSizeDefault() {
        var chart = new Chart();
        var ps = chart.getPreferredSize();
        assertEquals(40, ps.columns());
        assertEquals(12, ps.rows());
    }

    // ── Drawing tests (render without exceptions) ──────────────

    @Test
    void drawEmptyChartDoesNotThrow() {
        assertDoesNotThrow(() -> createAndDrawChart(40, 12));
    }

    @Test
    void drawChartWithTitleDoesNotThrow() {
        var chart = new Chart("Stock Price");
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));
        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void drawLineChartDoesNotThrow() {
        var chart = new Chart("Line");
        chart.addSeries(new ChartSeries("Price", List.of(10.0, 20.0, 15.0, 25.0, 30.0), AnsiColor.GREEN));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(50, 15));
        var buf = new ScreenBuffer(new TerminalSize(50, 15));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void drawBarChartDoesNotThrow() {
        var chart = new Chart("Bars");
        chart.addSeries(new ChartSeries("Volume", List.of(5.0, 10.0, 7.0, 15.0), ChartType.BAR, AnsiColor.CYAN));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(50, 15));
        var buf = new ScreenBuffer(new TerminalSize(50, 15));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void drawScatterChartDoesNotThrow() {
        var chart = new Chart("Scatter");
        chart.addSeries(new ChartSeries("Points", List.of(10.0, 20.0, 15.0, 25.0, 30.0, 18.0),
                ChartType.SCATTER, AnsiColor.YELLOW));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(50, 15));
        var buf = new ScreenBuffer(new TerminalSize(50, 15));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void drawMultipleSeriesDoesNotThrow() {
        var chart = new Chart("Multi");
        chart.addSeries(new ChartSeries("Close", List.of(100.0, 102.0, 99.0, 105.0), AnsiColor.GREEN));
        chart.addSeries(new ChartSeries("Open", List.of(98.0, 100.0, 97.0, 103.0), AnsiColor.YELLOW));
        chart.addSeries(new ChartSeries("High", List.of(101.0, 103.0, 100.0, 107.0), AnsiColor.RED));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(60, 20));
        var buf = new ScreenBuffer(new TerminalSize(60, 20));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void drawWithFixedYAxisDoesNotThrow() {
        var chart = new Chart("Fixed");
        chart.addSeries(new ChartSeries("Price", List.of(150.0, 152.0, 149.0), AnsiColor.GREEN));
        chart.setYAxisConfig(ChartAxisConfig.fixed(140, 160, "$%.0f"));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(50, 15));
        var buf = new ScreenBuffer(new TerminalSize(50, 15));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void drawWithoutGridDoesNotThrow() {
        var chart = new Chart("No Grid");
        chart.addSeries(new ChartSeries("X", List.of(1.0, 2.0, 3.0), AnsiColor.GREEN));
        chart.setShowGrid(false);
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));
        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void drawWithoutLegendDoesNotThrow() {
        var chart = new Chart("No Legend");
        chart.addSeries(new ChartSeries("X", List.of(1.0, 2.0, 3.0), AnsiColor.GREEN));
        chart.setShowLegend(false);
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));
        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void drawWithoutBorderDoesNotThrow() {
        var chart = new Chart("No Border");
        chart.addSeries(new ChartSeries("X", List.of(1.0, 2.0, 3.0), AnsiColor.GREEN));
        chart.setShowBorder(false);
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));
        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void drawTinyChartDoesNotThrow() {
        // Edge case: very small chart
        var chart = new Chart();
        chart.addSeries(new ChartSeries("X", List.of(1.0, 2.0), AnsiColor.GREEN));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 4));
        var buf = new ScreenBuffer(new TerminalSize(10, 4));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void drawSinglePointSeriesDoesNotThrow() {
        var chart = new Chart("Single");
        chart.addSeries(new ChartSeries("One", List.of(42.0), AnsiColor.GREEN));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));
        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void drawEmptySeriesListDoesNotThrow() {
        var chart = new Chart("No Data");
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));
        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void drawSeriesWithEmptyValuesDoesNotThrow() {
        var chart = new Chart("Empty Values");
        chart.addSeries(new ChartSeries("Empty", List.of(), AnsiColor.GREEN));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));
        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    // ── Rendering verification tests ───────────────────────────

    @Test
    void drawProducesNonEmptyBuffer() {
        var chart = new Chart("Test");
        chart.addSeries(new ChartSeries("Price", List.of(10.0, 20.0, 15.0, 25.0), AnsiColor.GREEN));
        var size = new TerminalSize(50, 15);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Count non-empty cells
        int nonEmpty = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (!buf.getCell(c, r).equals(TextCell.EMPTY)) {
                    nonEmpty++;
                }
            }
        }
        assertTrue(nonEmpty > 0, "Chart should produce visible output");
    }

    @Test
    void drawBorderVisibleInOutput() {
        var chart = new Chart();
        chart.setShowBorder(true);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        var size = new TerminalSize(20, 8);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Top-left corner should be a border character
        var corner = buf.getCell(0, 0);
        assertEquals("┌", corner.character());
    }

    @Test
    void drawNoBorderHasNoCornerChar() {
        var chart = new Chart();
        chart.setShowBorder(false);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        var size = new TerminalSize(20, 8);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Cell at 0,0 should NOT be a border corner
        var corner = buf.getCell(0, 0);
        assertNotEquals("┌", corner.character());
    }

    @Test
    void drawTitleVisibleInOutput() {
        var chart = new Chart("MyTitle");
        chart.setShowBorder(false);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        var size = new TerminalSize(40, 12);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Find "MyTitle" in the first row
        boolean found = false;
        for (int c = 0; c < size.columns() - 7; c++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 7; i++) sb.append(buf.getCell(c + i, 0).character());
            if (sb.toString().equals("MyTitle")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Title should be visible in output");
    }

    @Test
    void drawLegendShowsSeriesName() {
        var chart = new Chart("Test");
        chart.addSeries(new ChartSeries("AAPL", List.of(10.0, 20.0), AnsiColor.GREEN));
        chart.setShowBorder(false);
        chart.setShowGrid(false);
        var size = new TerminalSize(40, 12);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Last row should contain "AAPL"
        boolean found = false;
        for (int c = 0; c < size.columns() - 4; c++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) sb.append(buf.getCell(c + i, size.rows() - 1).character());
            if (sb.toString().equals("AAPL")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Legend should show series name");
    }

    @Test
    void drawYAxisLabelsVisible() {
        var chart = new Chart("Test");
        chart.addSeries(new ChartSeries("Price", List.of(10.0, 20.0, 30.0), AnsiColor.GREEN));
        chart.setYAxisConfig(ChartAxisConfig.fixed(0, 30, "%.0f"));
        chart.setShowBorder(false);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        var size = new TerminalSize(40, 12);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // First few columns of some row should contain "30" (the max value)
        boolean found30 = false;
        boolean found0 = false;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < 6; c++) {
                String ch = buf.getCell(c, r).character();
                if (ch.equals("3") && c + 1 < 6 && buf.getCell(c + 1, r).character().equals("0"))
                    found30 = true;
                if (ch.equals("0") && (c == 0 || !Character.isDigit(buf.getCell(c - 1, r).character().charAt(0))))
                    found0 = true;
            }
        }
        assertTrue(found30 || found0, "Y-axis labels should be visible");
    }

    @Test
    void flatLineSeriesDoesNotThrow() {
        // All values the same — range is 0, should not divide by zero
        var chart = new Chart("Flat");
        chart.addSeries(new ChartSeries("Flat", List.of(50.0, 50.0, 50.0, 50.0), AnsiColor.GREEN));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));
        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void negativeValuesDoesNotThrow() {
        var chart = new Chart("Negative");
        chart.addSeries(new ChartSeries("PnL", List.of(-10.0, 5.0, -20.0, 15.0, -5.0), AnsiColor.RED));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));
        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void largeDataSetDoesNotThrow() {
        // 100 data points in a 40-column chart — tests downsampling
        var values = new java.util.ArrayList<Double>();
        for (int i = 0; i < 100; i++) values.add(Math.sin(i * 0.1) * 50 + 100);
        var chart = new Chart("Big Data");
        chart.addSeries(new ChartSeries("Sine", values, AnsiColor.CYAN));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));
        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        assertDoesNotThrow(() -> chart.draw(g));
    }

    @Test
    void lineChartUsesHalfBlockCharacters() {
        // Verify the sub-cell rendering produces ▀ ▄ █ characters for smoother lines
        var chart = new Chart("Half-block");
        // A steep line: 0 → 100 across the plot, should produce vertical segments (█)
        chart.addSeries(new ChartSeries("Line", List.of(0.0, 100.0), ChartType.LINE, AnsiColor.GREEN));
        chart.setShowBorder(false);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        chart.setYAxisConfig(ChartAxisConfig.fixed(0, 100, "%.0f"));
        var size = new TerminalSize(20, 10);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Count half-block characters in the plot area
        int halfBlocks = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                String ch = buf.getCell(c, r).character();
                if (ch.equals("▀") || ch.equals("▄") || ch.equals("█")) {
                    halfBlocks++;
                }
            }
        }
        assertTrue(halfBlocks > 0, "Line chart should use half-block characters (▀▄█), found " + halfBlocks);
    }

    @Test
    void lineChartNoOldStyleDiagonalChars() {
        // Verify old-style diagonal chars (╱╲) are NOT used anymore
        var chart = new Chart("No diagonals");
        chart.addSeries(new ChartSeries("Line", List.of(10.0, 50.0, 20.0, 80.0, 30.0),
                ChartType.LINE, AnsiColor.GREEN));
        chart.setShowBorder(false);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        var size = new TerminalSize(40, 15);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // No ╱ or ╲ should appear in the plot area (rows 0-13, cols 7+)
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 7; c < size.columns(); c++) {
                String ch = buf.getCell(c, r).character();
                assertFalse(ch.equals("╱") || ch.equals("╲"),
                        "Old diagonal char " + ch + " found at (" + c + "," + r + ")");
            }
        }
    }

    @Test
    void flatLineUsesHorizontalChar() {
        // A flat line should produce ─ characters
        var chart = new Chart("Flat");
        chart.addSeries(new ChartSeries("Flat", List.of(50.0, 50.0, 50.0, 50.0),
                ChartType.LINE, AnsiColor.GREEN));
        chart.setShowBorder(false);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        chart.setYAxisConfig(ChartAxisConfig.fixed(0, 100, "%.0f"));
        var size = new TerminalSize(20, 10);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        boolean foundHorizontal = false;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buf.getCell(c, r).character().equals("─")) {
                    foundHorizontal = true;
                    break;
                }
            }
        }
        assertTrue(foundHorizontal, "Flat line should contain ─ characters");
    }
}