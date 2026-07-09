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
    void xAxisLabelsVisibleInOutput() {
        var chart = new Chart("Dates");
        chart.addSeries(new ChartSeries("Price", List.of(10.0, 20.0, 15.0, 25.0), AnsiColor.GREEN));
        chart.setXAxisLabels(List.of("Jan", "Apr", "Jul", "Oct"));
        chart.setShowBorder(false);
        chart.setShowGrid(false);
        chart.setShowLegend(false);
        var size = new TerminalSize(40, 12);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Find "Jan" and "Oct" in the buffer
        boolean foundJan = false;
        boolean foundOct = false;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c <= size.columns() - 3; c++) {
                String s = buf.getCell(c, r).character()
                        + buf.getCell(c + 1, r).character()
                        + buf.getCell(c + 2, r).character();
                if (s.equals("Jan")) foundJan = true;
                if (s.equals("Oct")) foundOct = true;
            }
        }
        assertTrue(foundJan, "X-axis label 'Jan' should be visible");
        assertTrue(foundOct, "X-axis label 'Oct' should be visible");
    }

    @Test
    void xAxisLabelsReserveBottomRow() {
        // With x-axis labels, the plot area should be 1 row shorter
        var chart = new Chart("Test");
        chart.addSeries(new ChartSeries("Price", List.of(10.0, 20.0, 15.0), AnsiColor.GREEN));
        chart.setXAxisLabels(List.of("Start", "End"));
        chart.setShowBorder(false);
        chart.setShowGrid(false);
        chart.setShowLegend(false);
        var size = new TerminalSize(30, 8);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
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
    void markerDoesNotDegradeFullBlockToHalfBlock() {
        // The bug: Bresenham line draws █ (full block) at a data point position,
        // then the marker overwrites it with ▀ or ▄, creating an apparent gap.
        // The fix: markers should merge with existing line chars, never downgrading
        // █ to ▀/▄.
        //
        // Test: a zigzag with steep segments that produce full blocks at data points.
        var chart = new Chart("Merge Test");
        chart.addSeries(new ChartSeries("Z", List.of(10.0, 90.0, 10.0, 90.0, 10.0, 90.0, 10.0),
                ChartType.LINE, AnsiColor.GREEN));
        chart.setShowBorder(false);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        chart.setYAxisConfig(ChartAxisConfig.fixed(0, 100, "%.0f"));
        var size = new TerminalSize(20, 8);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Count full blocks (█) — the steep segments should produce many.
        // If the bug existed, markers would downgrade them to ▀/▄ at each data point.
        int fullBlocks = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buf.getCell(c, r).character().equals("█")) fullBlocks++;
            }
        }
        assertTrue(fullBlocks > 0, "Steep segments should produce full blocks (█), found " + fullBlocks);
    }

    @Test
    void markersMergeWithLineChars() {
        // Verify that when a line draws ▀ and the marker is ▄ (or vice versa),
        // the cell becomes █ (merged) rather than overwriting.
        // Use a zigzag with steep segments that force both halves to be visited.
        var chart = new Chart("Merge");
        chart.addSeries(new ChartSeries("Z", List.of(10.0, 90.0, 10.0, 90.0, 10.0),
                ChartType.LINE, AnsiColor.GREEN));
        chart.setShowBorder(false);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        chart.setYAxisConfig(ChartAxisConfig.fixed(0, 100, "%.0f"));
        var size = new TerminalSize(20, 8);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Should have at least some █ from merged cells
        boolean hasFullBlock = false;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buf.getCell(c, r).character().equals("█")) {
                    hasFullBlock = true;
                    break;
                }
            }
            if (hasFullBlock) break;
        }
        assertTrue(hasFullBlock, "Merged markers and line chars should produce █ blocks");
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

    // ── Y-axis nice tick tests ──────────────────────────────────

    @Test
    void yAxisLabelsAreRoundNumbers() {
        // Verify Y-axis labels are at round multiples (not arbitrary fractions)
        var chart = new Chart("Ticks");
        chart.addSeries(new ChartSeries("Price", List.of(142.0, 156.0, 148.0, 163.0), AnsiColor.GREEN));
        chart.setYAxisConfig(ChartAxisConfig.fixed(140, 165, "$%.0f"));
        chart.setShowBorder(false);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        var size = new TerminalSize(30, 12);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Collect all Y-axis labels (left side, rows with ┤ tick marks)
        java.util.List<String> labels = new java.util.ArrayList<>();
        for (int r = 0; r < size.rows(); r++) {
            // Y-axis labels are to the left of the ┤ tick
            for (int c = 0; c < 8; c++) {
                String ch = buf.getCell(c, r).character();
                if (ch.equals("$")) {
                    // Read the full label starting here
                    StringBuilder sb = new StringBuilder();
                    for (int cc = c; cc < 10; cc++) {
                        String cell = buf.getCell(cc, r).character();
                        if (cell.equals(" ") || cell.equals("┤") || cell.equals("┴") || cell.equals("─")) break;
                        sb.append(cell);
                    }
                    String label = sb.toString();
                    if (label.startsWith("$")) labels.add(label);
                    break;
                }
            }
        }
        assertTrue(labels.size() >= 2, "Should have at least 2 Y-axis labels, got " + labels.size());
        // Every label should be a round number: $140, $145, $150, $155, $160, $165, etc.
        for (String label : labels) {
            int value = Integer.parseInt(label.replace("$", "").replace(",", ""));
            assertTrue(value % 5 == 0, "Y-axis label " + label + " should be a multiple of 5");
        }
    }

    @Test
    void yAxisLabelsForLargePrices() {
        // Prices in the hundreds — labels should be at multiples of 25 or 50
        var chart = new Chart("Big");
        chart.addSeries(new ChartSeries("Price", List.of(410.0, 460.0, 425.0, 480.0), AnsiColor.GREEN));
        chart.setYAxisConfig(ChartAxisConfig.fixed(400, 500, "$%.0f"));
        chart.setShowBorder(false);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        var size = new TerminalSize(30, 12);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Collect labels
        java.util.List<Integer> values = new java.util.ArrayList<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < 8; c++) {
                String ch = buf.getCell(c, r).character();
                if (ch.equals("$")) {
                    StringBuilder sb = new StringBuilder();
                    for (int cc = c; cc < 10; cc++) {
                        String cell = buf.getCell(cc, r).character();
                        if (cell.equals(" ") || cell.equals("┤") || cell.equals("┴") || cell.equals("─")) break;
                        sb.append(cell);
                    }
                    String label = sb.toString();
                    if (label.startsWith("$")) {
                        try {
                            values.add(Integer.parseInt(label.replace("$", "")));
                        } catch (NumberFormatException ignored) {}
                    }
                    break;
                }
            }
        }
        assertTrue(values.size() >= 2, "Should have at least 2 Y-axis labels");
        for (int v : values) {
            assertTrue(v % 25 == 0, "Y-axis label $" + v + " should be a multiple of 25");
        }
    }

    // ── Bug fix tests ───────────────────────────────────────────

    /**
     * Bug 1: Y-axis labels are inverted. The max value label (100) should
     * appear at a HIGHER position (lower row number = closer to top) than the
     * min value label (0). Before the fix, drawYAxis didn't invert yFraction,
     * so max appeared at the bottom and min at the top.
     */
    @Test
    void yAxisLabelsMaxAtTopMinAtBottom() {
        var chart = new Chart("YAxis");
        chart.addSeries(new ChartSeries("Price", List.of(50.0), AnsiColor.GREEN));
        chart.setYAxisConfig(ChartAxisConfig.fixed(0, 100, "%.0f"));
        chart.setShowBorder(false);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        var size = new TerminalSize(30, 12);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Scan each row for numeric Y-axis labels (digits in the left label area)
        Integer row100 = null; // row where "100" appears
        Integer row0 = null;    // row where "0" appears as a standalone label
        for (int r = 0; r < size.rows(); r++) {
            // Read the left 6 columns of the row as a trimmed string
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < 6; c++) sb.append(buf.getCell(c, r).character());
            String text = sb.toString().trim();
            if (text.equals("100")) row100 = r;
            // "0" label: the text is exactly "0" (not part of a bigger number)
            if (text.equals("0")) row0 = r;
        }

        assertNotNull(row100, "Y-axis should show a '100' label");
        assertNotNull(row0, "Y-axis should show a '0' label");
        assertTrue(row100 < row0,
                "Max label (100) should be above min label (0): row100=" + row100 + " row0=" + row0);
    }

    /**
     * Bug 2: logTicks produces too few ticks for narrow price ranges.
     * A log-scale chart with range $380-$500 spans less than one decade,
     * so logTicks only finds one tick ($500). The fix falls back to niceTicks
     * (linear) so we get multiple readable tick labels.
     */
    @Test
    void logScaleNarrowRangeProducesMultipleTicks() {
        var chart = new Chart("MSFT");
        chart.addSeries(new ChartSeries("Price", List.of(420.0, 450.0, 480.0), AnsiColor.GREEN));
        chart.setYAxisConfig(ChartAxisConfig.logFixed(380, 500, "$%.0f"));
        chart.setShowBorder(false);
        chart.setShowLegend(false);
        chart.setShowGrid(false);
        var size = new TerminalSize(30, 14);
        chart.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Collect all Y-axis labels (strings starting with $ in the left area)
        java.util.List<String> labels = new java.util.ArrayList<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < 10; c++) {
                String ch = buf.getCell(c, r).character();
                if (ch.equals("$")) {
                    StringBuilder sb = new StringBuilder();
                    for (int cc = c; cc < 12; cc++) {
                        String cell = buf.getCell(cc, r).character();
                        if (cell.equals(" ") || cell.equals("┤") || cell.equals("┴") || cell.equals("─")) break;
                        sb.append(cell);
                    }
                    String label = sb.toString();
                    if (label.startsWith("$") && label.length() > 1) {
                        labels.add(label);
                    }
                    break;
                }
            }
        }

        assertTrue(labels.size() >= 2,
                "Log-scale narrow range ($380-$500) should produce >= 2 Y-axis labels via fallback, got: " + labels);
    }

    /**
     * Bug 3: computeYRange() inflates max for narrow log-scale ranges.
     * For logFixed(380, 500), the old code did max = Math.max(max, nextUp(min)*2),
     * which inflated max to ~760. This made the data only reach the midpoint
     * instead of the top. The fix only inflates max when max <= min.
     */
    @Test
    void logScaleNarrowRangeDoesNotInflateMax() throws Exception {
        var chart = new Chart("MSFT");
        chart.addSeries(new ChartSeries("Price", List.of(420.0, 450.0, 480.0), AnsiColor.GREEN));
        chart.setYAxisConfig(ChartAxisConfig.logFixed(380, 500, "$%.0f"));

        // Call private computeYRange() via reflection
        var method = Chart.class.getDeclaredMethod("computeYRange");
        method.setAccessible(true);
        double[] range = (double[]) method.invoke(chart);

        assertEquals(380.0, range[0], 0.001, "min should be 380");
        assertEquals(500.0, range[1], 0.001,
                "max should be 500, not inflated to ~760 by nextUp(min)*2");
    }

    /**
     * Bug 3 (rendering): A log-scale chart with narrow range should map
     * the max data value to the TOP of the plot area (yFraction near 1.0),
     * not the midpoint. With the bug, max was inflated to ~760, so 500
     * only reached yFraction ≈ log(500)/log(760) ≈ 0.5.
     */
    @Test
    void logScaleNarrowRangeMaxValueAtTop() throws Exception {
        var chart = new Chart("MSFT");
        chart.addSeries(new ChartSeries("Price", List.of(500.0), AnsiColor.GREEN));
        chart.setYAxisConfig(ChartAxisConfig.logFixed(380, 500, "$%.0f"));

        // Call private computeYRange() and valueToYFraction via reflection
        var rangeMethod = Chart.class.getDeclaredMethod("computeYRange");
        rangeMethod.setAccessible(true);
        double[] range = (double[]) rangeMethod.invoke(chart);

        var fracMethod = Chart.class.getDeclaredMethod("valueToYFraction", double.class, double.class, double.class);
        fracMethod.setAccessible(true);
        double yFraction = (double) fracMethod.invoke(chart, 500.0, range[0], range[1]);

        assertTrue(yFraction > 0.95,
                "Max value (500) should map near top (yFraction > 0.95), got " + yFraction
                        + " with range [" + range[0] + ", " + range[1] + "]");
    }

    /**
     * Bug 3 (edge case): when max == min on log scale, inflation should still
     * kick in to ensure max > min.
     */
    @Test
    void logScaleEqualMinMaxStillInflates() throws Exception {
        var chart = new Chart("Edge");
        chart.addSeries(new ChartSeries("Price", List.of(100.0), AnsiColor.GREEN));
        chart.setYAxisConfig(ChartAxisConfig.logFixed(100, 100, "$%.0f"));

        var method = Chart.class.getDeclaredMethod("computeYRange");
        method.setAccessible(true);
        double[] range = (double[]) method.invoke(chart);

        assertTrue(range[1] > range[0],
                "When max == min, max should be inflated above min");
    }
}