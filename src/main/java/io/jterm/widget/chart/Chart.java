package io.jterm.widget.chart;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.util.Symbols;
import io.jterm.widget.AbstractComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * A terminal-based chart widget that renders one or more {@link ChartSeries}
 * to a character grid. Supports line, bar, and scatter plots.
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>Automatic or fixed Y-axis scaling</li>
 *   <li>Y-axis labels with configurable formatting</li>
 *   <li>Horizontal grid lines (dashed)</li>
 *   <li>Legend showing series name and color</li>
 *   <li>Title display</li>
 *   <li>Box border around plot area</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * var chart = new Chart("AAPL — 30 Day");
 * chart.addSeries(new ChartSeries("Close", prices, ChartType.LINE, AnsiColor.GREEN));
 * chart.setYAxisConfig(ChartAxisConfig.fixed(140, 160, "$%.0f"));
 * panel.addComponent(chart);
 * }</pre>
 *
 * <h3>Layout</h3>
 * <pre>
 *  ┌─ Title ──────────────────────┐
 *  │ 160 ┤                        │
 *  │     │        ╭─╮             │
 *  │ 150 ┤    ╭───╯ ╰──╮          │
 *  │     │  ──╯        ╰──        │
 *  │ 140 ┤                        │
 *  └─────┴────────────────────────┘
 * </pre>
 */
public class Chart extends AbstractComponent {

    private String title = "";
    private final List<ChartSeries> seriesList = new ArrayList<>();
    private ChartAxisConfig yAxisConfig = ChartAxisConfig.auto();
    private boolean showGrid = true;
    private boolean showLegend = true;
    private boolean showBorder = true;
    private int yLabelWidth = 6;
    private Color borderColor = AnsiColor.BRIGHT_BLACK;
    private Color gridColor = AnsiColor.BRIGHT_BLACK;
    private Color axisLabelColor = AnsiColor.WHITE;

    // Layout constants
    private static final int Y_AXIS_WIDTH = 1;   // the ┤ column
    private static final int X_AXIS_HEIGHT = 1;  // the ┴ row

    public Chart() {}

    public Chart(String title) {
        this.title = title;
    }

    // ── Configuration ──────────────────────────────────────────

    public Chart setTitle(String title) {
        this.title = title == null ? "" : title;
        invalidate();
        return this;
    }

    public String getTitle() { return title; }

    public Chart addSeries(ChartSeries series) {
        seriesList.add(series);
        invalidate();
        return this;
    }

    public Chart removeSeries(String name) {
        seriesList.removeIf(s -> s.name().equals(name));
        invalidate();
        return this;
    }

    public List<ChartSeries> getSeries() { return new ArrayList<>(seriesList); }

    public int getSeriesCount() { return seriesList.size(); }

    public Chart setYAxisConfig(ChartAxisConfig config) {
        this.yAxisConfig = config;
        invalidate();
        return this;
    }

    public ChartAxisConfig getYAxisConfig() { return yAxisConfig; }

    public Chart setShowGrid(boolean show) {
        this.showGrid = show;
        invalidate();
        return this;
    }

    public boolean isShowGrid() { return showGrid; }

    public Chart setShowLegend(boolean show) {
        this.showLegend = show;
        invalidate();
        return this;
    }

    public boolean isShowLegend() { return showLegend; }

    public Chart setShowBorder(boolean show) {
        this.showBorder = show;
        invalidate();
        return this;
    }

    public boolean isShowBorder() { return showBorder; }

    public Chart setYLabelWidth(int width) {
        this.yLabelWidth = Math.max(2, width);
        invalidate();
        return this;
    }

    public Chart setBorderColor(Color color) {
        this.borderColor = color;
        invalidate();
        return this;
    }

    public Chart setGridColor(Color color) {
        this.gridColor = color;
        invalidate();
        return this;
    }

    // ── Layout calculations ────────────────────────────────────

    @Override
    protected TerminalSize calculatePreferredSize() {
        // Default preferred size: 40×12 (enough for a useful chart)
        return new TerminalSize(40, 12);
    }

    /**
     * Computes the effective plot area bounds within the widget.
     * Returns [x0, y0, width, height] for the inner plotting region.
     */
    private int[] computePlotArea(int totalCols, int totalRows) {
        int topMargin = 0;
        int bottomMargin = 0;
        int leftMargin = yLabelWidth + Y_AXIS_WIDTH;
        int rightMargin = 0;

        if (!title.isEmpty()) topMargin += 1;
        if (showLegend) bottomMargin += 1;

        int plotX = leftMargin;
        int plotY = topMargin;
        int plotW = totalCols - leftMargin - rightMargin;
        int plotH = totalRows - topMargin - bottomMargin;

        if (showBorder) {
            plotX += 1;
            plotY += 1;
            plotW -= 2;
            plotH -= 2;
        }

        // Clamp
        plotW = Math.max(1, plotW);
        plotH = Math.max(1, plotH);

        return new int[]{plotX, plotY, plotW, plotH};
    }

    /**
     * Computes the effective Y-axis range from config or data.
     */
    private double[] computeYRange() {
        if (!yAxisConfig.autoScale()) {
            return new double[]{yAxisConfig.min(), yAxisConfig.max()};
        }
        double dataMin = Double.MAX_VALUE;
        double dataMax = Double.MIN_VALUE;
        for (var s : seriesList) {
            if (s.isEmpty()) continue;
            dataMin = Math.min(dataMin, s.min());
            dataMax = Math.max(dataMax, s.max());
        }
        if (dataMin == Double.MAX_VALUE) {
            dataMin = 0;
            dataMax = 1;
        }
        // Add 5% padding
        double range = dataMax - dataMin;
        if (range == 0) range = 1;
        double pad = range * 0.05;
        return new double[]{dataMin - pad, dataMax + pad};
    }

    // ── Rendering ──────────────────────────────────────────────

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        int cols = size.columns();
        int rows = size.rows();

        var plotArea = computePlotArea(cols, rows);
        int px = plotArea[0], py = plotArea[1], pw = plotArea[2], ph = plotArea[3];

        var yRange = computeYRange();
        double yMin = yRange[0], yMax = yRange[1];

        // Draw border if enabled
        if (showBorder) {
            drawChartBorder(graphics, cols, rows);
        }

        // Draw title
        if (!title.isEmpty()) {
            int titleY = showBorder ? 0 : 0;
            int titleX = showBorder ? 2 : yLabelWidth;
            graphics.drawString(titleX, titleY, truncate(title, cols - titleX - 1),
                    new TextCell(' ', AnsiColor.BRIGHT_WHITE, AnsiColor.DEFAULT, SGR.BOLD));
        }

        // Draw Y-axis labels and grid
        drawYAxis(graphics, px, py, pw, ph, yMin, yMax);

        // Draw X-axis baseline
        drawXAxis(graphics, px, py + ph - 1, pw);

        // Draw each series
        for (var series : seriesList) {
            if (series.isEmpty()) continue;
            drawSeries(graphics, series, px, py, pw, ph, yMin, yMax);
        }

        // Draw legend
        if (showLegend && !seriesList.isEmpty()) {
            drawLegend(graphics, cols, rows);
        }
    }

    private void drawChartBorder(TextGraphics g, int cols, int rows) {
        var borderCell = new TextCell(' ', borderColor, AnsiColor.DEFAULT);

        // Top
        for (int c = 0; c < cols; c++) g.setCell(c, 0, borderCell.withCharacter(getBorderChar(c, 0, cols, rows, 'h')));
        // Bottom
        for (int c = 0; c < cols; c++) g.setCell(c, rows - 1, borderCell.withCharacter(getBorderChar(c, rows - 1, cols, rows, 'h')));
        // Left
        for (int r = 0; r < rows; r++) g.setCell(0, r, borderCell.withCharacter(getBorderChar(0, r, cols, rows, 'v')));
        // Right
        for (int r = 0; r < rows; r++) g.setCell(cols - 1, r, borderCell.withCharacter(getBorderChar(cols - 1, r, cols, rows, 'v')));

        // Corners
        g.setCell(0, 0, borderCell.withCharacter('┌'));
        g.setCell(cols - 1, 0, borderCell.withCharacter('┐'));
        g.setCell(0, rows - 1, borderCell.withCharacter('└'));
        g.setCell(cols - 1, rows - 1, borderCell.withCharacter('┘'));
    }

    private char getBorderChar(int c, int r, int cols, int rows, char orient) {
        if (c == 0 || c == cols - 1) return '│';
        if (r == 0 || r == rows - 1) return '─';
        return ' ';
    }

    private void drawYAxis(TextGraphics g, int px, int py, int pw, int ph, double yMin, double yMax) {
        var labelCell = new TextCell(' ', axisLabelColor, AnsiColor.DEFAULT);
        var gridCell = new TextCell(' ', gridColor, AnsiColor.DEFAULT);
        var axisCell = new TextCell(' ', borderColor, AnsiColor.DEFAULT);

        int yTickCount = Math.max(2, Math.min(5, ph / 2));

        for (int i = 0; i < yTickCount; i++) {
            double fraction = (double) i / (yTickCount - 1);
            // Top = yMax, bottom = yMin (inverted because terminal y grows down)
            double value = yMax - fraction * (yMax - yMin);
            int y = py + (int) (fraction * (ph - 1));

            // Draw label (right-aligned in label area)
            String label = yAxisConfig.format(value);
            int labelStart = px - Y_AXIS_WIDTH - label.length();
            if (labelStart < 0) labelStart = 0;
            g.drawString(labelStart, y, label, labelCell);

            // Draw axis tick
            g.setCell(px - 1, y, axisCell.withCharacter('┤'));

            // Draw grid line (dashed)
            if (showGrid && i > 0 && i < yTickCount - 1) {
                for (int c = px; c < px + pw; c++) {
                    if ((c - px) % 2 == 0) {
                        g.setCell(c, y, gridCell.withCharacter('·'));
                    }
                }
            }
        }
    }

    private void drawXAxis(TextGraphics g, int px, int pyBottom, int pw) {
        var axisCell = new TextCell(' ', borderColor, AnsiColor.DEFAULT);
        for (int c = px; c < px + pw; c++) {
            g.setCell(c, pyBottom, axisCell.withCharacter('─'));
        }
        g.setCell(px - 1, pyBottom, axisCell.withCharacter('┴'));
    }

    private void drawSeries(TextGraphics g, ChartSeries series, int px, int py, int pw, int ph, double yMin, double yMax) {
        int n = series.size();
        double yRange = yMax - yMin;
        if (yRange == 0) yRange = 1;

        // Map data index → column
        // If we have more data points than columns, we sample; if fewer, we spread
        var valueCell = new TextCell(' ', series.color(), AnsiColor.DEFAULT, SGR.BOLD);

        switch (series.type()) {
            case LINE -> drawLineSeries(g, series, px, py, pw, ph, yMin, yMax, yRange, valueCell);
            case BAR -> drawBarSeries(g, series, px, py, pw, ph, yMin, yMax, yRange, valueCell);
            case SCATTER -> drawScatterSeries(g, series, px, py, pw, ph, yMin, yMax, yRange, valueCell);
        }
    }

    private void drawLineSeries(TextGraphics g, ChartSeries series, int px, int py, int pw, int ph, double yMin, double yMax, double yRange, TextCell cell) {
        int n = series.size();
        int[] screenX = new int[n];
        int[] screenY = new int[n];

        for (int i = 0; i < n; i++) {
            double xFraction = n == 1 ? 0.5 : (double) i / (n - 1);
            screenX[i] = px + (int) (xFraction * (pw - 1));

            double val = series.values().get(i);
            double yFraction = (val - yMin) / yRange;
            // Invert: y=0 is top (yMax), y=ph-1 is bottom (yMin)
            screenY[i] = py + (int) ((1.0 - yFraction) * (ph - 1));
            screenY[i] = Math.max(py, Math.min(py + ph - 1, screenY[i]));
        }

        // Draw line segments using Bresenham
        for (int i = 0; i < n - 1; i++) {
            drawPlotLine(g, screenX[i], screenY[i], screenX[i + 1], screenY[i + 1], cell);
        }

        // Draw markers at data points
        for (int i = 0; i < n; i++) {
            g.setCell(screenX[i], screenY[i], cell.withCharacter('●'));
        }
    }

    private void drawBarSeries(TextGraphics g, ChartSeries series, int px, int py, int pw, int ph, double yMin, double yMax, double yRange, TextCell cell) {
        int n = series.size();
        int barWidth = Math.max(1, pw / n - 1);
        int baselineY = py + ph - 1;

        for (int i = 0; i < n; i++) {
            double xFraction = n == 1 ? 0.5 : (double) i / (n - 1);
            int x = px + (int) (xFraction * (pw - 1));

            double val = series.values().get(i);
            double yFraction = (val - yMin) / yRange;
            int barHeight = (int) (yFraction * (ph - 1));
            barHeight = Math.max(0, Math.min(ph - 1, barHeight));

            for (int dy = 0; dy <= barHeight; dy++) {
                for (int dx = 0; dx < barWidth && x + dx < px + pw; dx++) {
                    char ch = dy == barHeight ? '▀' : '█';
                    g.setCell(x + dx, baselineY - dy, cell.withCharacter(ch));
                }
            }
        }
    }

    private void drawScatterSeries(TextGraphics g, ChartSeries series, int px, int py, int pw, int ph, double yMin, double yMax, double yRange, TextCell cell) {
        int n = series.size();
        for (int i = 0; i < n; i++) {
            double xFraction = n == 1 ? 0.5 : (double) i / (n - 1);
            int x = px + (int) (xFraction * (pw - 1));

            double val = series.values().get(i);
            double yFraction = (val - yMin) / yRange;
            int y = py + (int) ((1.0 - yFraction) * (ph - 1));
            y = Math.max(py, Math.min(py + ph - 1, y));

            g.setCell(x, y, cell.withCharacter('●'));
        }
    }

    private void drawPlotLine(TextGraphics g, int x0, int y0, int x1, int y1, TextCell cell) {
        // Bresenham line with slope-aware character selection
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            char ch;
            if (x0 == x1) {
                ch = '│';
            } else if (y0 == y1) {
                ch = '─';
            } else {
                ch = (sy > 0) ? '╲' : '╱';
            }
            g.setCell(x0, y0, cell.withCharacter(ch));

            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                int prevY = y0;
                x0 += sx;
                // Draw corner when changing direction
                if (x0 != x1 || y0 != y1) {
                    int nextErr = err + dx; // simulate next step's err
                }
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private void drawLegend(TextGraphics g, int cols, int rows) {
        int legendY = rows - 1;
        int x = showBorder ? 2 : 0;

        var labelCell = new TextCell(' ', AnsiColor.WHITE, AnsiColor.DEFAULT);

        for (int i = 0; i < seriesList.size(); i++) {
            var s = seriesList.get(i);
            String entry = " " + s.name() + " ";
            // Draw color marker
            var markerCell = new TextCell(' ', s.color(), s.color());
            g.setCell(x, legendY, markerCell.withCharacter('█'));
            g.drawString(x + 1, legendY, entry, labelCell);
            x += 1 + entry.length() + 1;

            if (x >= cols - 2) break; // truncate if too many series
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, Math.max(0, maxLen - 1)) + "…";
    }
}