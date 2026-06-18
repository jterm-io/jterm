package io.jterm.widget.chart;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.Theme;
import io.jterm.style.ThemeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the Chart widget uses theme background colors instead of
 * AnsiColor.DEFAULT for all rendered cells (grid lines, axis labels,
 * borders, series data, legend).
 *
 * Bug: Chart used AnsiColor.DEFAULT as background for all TextCells, so
 * grid lines and other elements showed terminal default (black) instead
 * of the theme background.
 */
class ChartThemeTest {

    @AfterEach
    void resetTheme() {
        ThemeManager.setActive(Theme.DARK);
    }

    @Test
    void gridLinesUseThemeBackground() {
        ThemeManager.setActive(Theme.YELLOW_ON_BLUE);
        var chart = new Chart("Test");
        chart.addSeries(new ChartSeries("S", java.util.List.of(1.0, 5.0, 3.0), ChartType.LINE, AnsiColor.GREEN));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));

        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Scan for grid cells (the '·' character) and verify they use theme bg
        boolean foundGridCell = false;
        for (int r = 0; r < 12; r++) {
            for (int c = 0; c < 40; c++) {
                var cell = buf.getCell(c, r);
                if (cell.character().equals("·")) {
                    foundGridCell = true;
                    assertEquals(Theme.YELLOW_ON_BLUE.background(), cell.bg(),
                            "Grid cell at (" + c + "," + r + ") should use theme background, not DEFAULT");
                }
            }
        }
        assertTrue(foundGridCell, "Should have found at least one grid cell");
    }

    @Test
    void borderUsesThemeBackground() {
        ThemeManager.setActive(Theme.GREEN_ON_BLACK);
        var chart = new Chart("Test");
        chart.addSeries(new ChartSeries("S", java.util.List.of(1.0, 5.0), ChartType.LINE, AnsiColor.GREEN));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));

        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Find border characters and check their bg
        boolean foundBorder = false;
        for (int c = 0; c < 40; c++) {
            var cell = buf.getCell(c, 0);
            if (cell.character().equals("┌") || cell.character().equals("─") || cell.character().equals("┐")) {
                foundBorder = true;
                assertEquals(Theme.GREEN_ON_BLACK.background(), cell.bg(),
                        "Border cell at (" + c + ",0) should use theme background");
            }
        }
        assertTrue(foundBorder, "Should have found border cells");
    }

    @Test
    void axisLabelsUseThemeBackground() {
        ThemeManager.setActive(Theme.WHITE_ON_GREEN);
        var chart = new Chart("Test");
        chart.addSeries(new ChartSeries("S", java.util.List.of(1.0, 5.0, 3.0), ChartType.LINE, AnsiColor.GREEN));
        chart.setYAxisConfig(ChartAxisConfig.fixed(0, 10, "%.0f"));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));

        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Find axis label characters (digits) in the y-label area and verify bg
        boolean foundLabel = false;
        for (int r = 0; r < 12; r++) {
            for (int c = 0; c < 8; c++) {  // y-label area is leftmost ~8 cols
                var cell = buf.getCell(c, r);
                if (Character.isDigit(cell.character().charAt(0))) {
                    foundLabel = true;
                    assertEquals(Theme.WHITE_ON_GREEN.background(), cell.bg(),
                            "Axis label at (" + c + "," + r + ") should use theme background");
                }
            }
        }
        assertTrue(foundLabel, "Should have found axis label digits");
    }

    @Test
    void seriesDataUsesThemeBackground() {
        ThemeManager.setActive(Theme.YELLOW_ON_BLUE);
        var chart = new Chart("Test");
        chart.addSeries(new ChartSeries("S", java.util.List.of(1.0, 5.0, 3.0), ChartType.LINE, AnsiColor.GREEN));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));

        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Find series data cells (▀, ▄, █) and verify bg — exclude legend row
        boolean foundSeriesCell = false;
        int legendRow = 11; // last row = legend
        for (int r = 0; r < 12; r++) {
            for (int c = 0; c < 40; c++) {
                var cell = buf.getCell(c, r);
                char ch = cell.character().charAt(0);
                if ((ch == '▀' || ch == '▄' || ch == '█') && r != legendRow) {
                    foundSeriesCell = true;
                    assertEquals(Theme.YELLOW_ON_BLUE.background(), cell.bg(),
                            "Series cell at (" + c + "," + r + ") should use theme background");
                }
            }
        }
        assertTrue(foundSeriesCell, "Should have found series data cells");
    }

    @Test
    void legendUsesThemeBackground() {
        ThemeManager.setActive(Theme.GREEN_ON_BLACK);
        var chart = new Chart("Test");
        chart.addSeries(new ChartSeries("Prices", java.util.List.of(1.0, 5.0, 3.0), ChartType.LINE, AnsiColor.GREEN));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));

        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        chart.draw(g);

        // Legend is at the bottom row; find legend text and check bg
        boolean foundLegendText = false;
        for (int c = 0; c < 40; c++) {
            var cell = buf.getCell(c, 11);  // bottom row
            if (!cell.character().equals(" ") && !cell.character().equals("─") && !cell.character().equals("┘")) {
                // This should be legend text or marker
                char ch = cell.character().charAt(0);
                // Skip the color marker (█) — it intentionally uses the series color as bg
                if (ch == '█' && cell.bg() != AnsiColor.WHITE) continue;
                if (ch == 'P' || ch == 'r' || ch == 'i' || ch == 'c' || ch == 'e' || ch == 's') {
                    foundLegendText = true;
                    assertEquals(Theme.GREEN_ON_BLACK.background(), cell.bg(),
                            "Legend cell at (" + c + ",11) should use theme background, got bg=" + cell.bg());
                }
            }
        }
        assertTrue(foundLegendText, "Should have found legend text");
    }

    @Test
    void noDefaultBackgroundInAnyChartCell() {
        // Comprehensive: with a non-DARK theme, NO cell in the chart should
        // have AnsiColor.DEFAULT as its background.
        ThemeManager.setActive(Theme.YELLOW_ON_BLUE);
        var chart = new Chart("Test Chart");
        chart.addSeries(new ChartSeries("S", java.util.List.of(1.0, 5.0, 3.0, 7.0, 2.0), ChartType.LINE, AnsiColor.GREEN));
        chart.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 12));

        var buf = new ScreenBuffer(new TerminalSize(40, 12));
        var g = new TextGraphics(buf);
        chart.draw(g);

        for (int r = 0; r < 12; r++) {
            for (int c = 0; c < 40; c++) {
                var cell = buf.getCell(c, r);
                // Legend color markers intentionally use the series color as bg
                if (cell.character().equals("█") && r == 11 && cell.fg() == cell.bg()) continue;
                assertNotEquals(AnsiColor.DEFAULT, cell.bg(),
                        "Cell at (" + c + "," + r + ") char='" + cell.character() + "' should not have DEFAULT bg");
            }
        }
    }
}