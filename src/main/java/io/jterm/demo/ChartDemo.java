package io.jterm.demo;

import io.jterm.core.AnsiTerminal;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.screen.DefaultScreen;
import io.jterm.style.AnsiColor;
import io.jterm.widget.Label;
import io.jterm.widget.Panel;
import io.jterm.widget.chart.Chart;
import io.jterm.widget.chart.ChartAxisConfig;
import io.jterm.widget.chart.ChartSeries;
import io.jterm.widget.chart.ChartType;
import io.jterm.layout.BorderLayout;
import io.jterm.layout.LinearLayout;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowImpl;
import io.jterm.window.WindowHint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Chart demo: displays synthetic stock-price data using line, bar, and scatter
 * charts. Simulates a mini trading dashboard.
 *
 * <p><b>Keyboard</b></p>
 * <ul>
 *   <li><b>1</b> — Line chart view (price over time)</li>
 *   <li><b>2</b> — Bar chart view (volume)</li>
 *   <li><b>3</b> — Scatter chart view (tick data)</li>
 *   <li><b>4</b> — Multi-series view (price vs MA)</li>
 *   <li><b>r</b> — Regenerate data</li>
 *   <li><b>q</b> — quit</li>
 * </ul>
 */
public class ChartDemo {
    private static final int NUM_POINTS = 30;
    private static final Random rng = new Random(42);

    public static void main(String[] args) throws IOException {
        var terminal = new AnsiTerminal();
        var screen = new DefaultScreen(terminal);
        var gui = new DefaultTextGUI(screen);
        gui.getScreen().startScreen();

        var window = new WindowImpl("Chart Demo — Stock Data");
        window.setHints(List.of(WindowHint.FULLSCREEN));
        var content = window.getContents();
        content.setLayoutManager(new BorderLayout());

        // Header
        var header = new Label(" ── Chart Demo — Press 1/2/3/4 to switch views, r to regenerate, q to quit ── ",
                AnsiColor.BRIGHT_CYAN, AnsiColor.DEFAULT);

        // Main chart (center)
        var chart = new Chart("AAPL — 30 Day Price");
        chart.setYAxisConfig(ChartAxisConfig.fixed(130, 170, "$%.0f"));

        // Footer info
        var infoLabel = new Label(" View: Line  |  Points: " + NUM_POINTS,
                AnsiColor.BRIGHT_YELLOW, AnsiColor.DEFAULT);

        content.addComponent(header, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));
        content.addComponent(chart, new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));
        content.addComponent(infoLabel, new BorderLayout.BorderLayoutData(BorderLayout.Region.SOUTH));

        // Generate initial data BEFORE first render
        var prices = generatePrices(150.0, NUM_POINTS);
        var volumes = generateVolumes(NUM_POINTS);
        var ticks = generateTicks(150.0, NUM_POINTS * 3);
        var ma = calculateMovingAverage(prices, 5);

        // Start with line chart
        showLineChart(chart, prices);
        chart.invalidate();

        gui.addWindow(window);
        gui.updateScreen();

        var running = true;
        try {
            while (running) {
                while (true) {
                    var ks = screen instanceof DefaultScreen ds
                            ? ds.getTerminal().pollInput().orElse(null)
                            : null;
                    if (ks == null) break;

                    if (ks.type() == KeyType.CHARACTER) {
                        char ch = ks.character();
                        if (ch == 'q' || ch == 'Q') {
                            running = false;
                            break;
                        }
                        switch (ch) {
                            case '1' -> {
                                chart.setTitle("AAPL — 30 Day Price (Line)");
                                chart.setYAxisConfig(ChartAxisConfig.fixed(130, 170, "$%.0f"));
                                showLineChart(chart, prices);
                                infoLabel.setText(" View: Line  |  Points: " + NUM_POINTS + " ");
                                gui.requestRefresh();
                            }
                            case '2' -> {
                                chart.setTitle("AAPL — 30 Day Volume (Bar)");
                                chart.setYAxisConfig(ChartAxisConfig.auto());
                                showBarChart(chart, volumes);
                                infoLabel.setText(" View: Bar (Volume)  |  Points: " + NUM_POINTS + " ");
                                gui.requestRefresh();
                            }
                            case '3' -> {
                                chart.setTitle("AAPL — Tick Data (Scatter)");
                                chart.setYAxisConfig(ChartAxisConfig.fixed(140, 160, "$%.1f"));
                                showScatterChart(chart, ticks);
                                infoLabel.setText(" View: Scatter (Ticks)  |  Points: " + ticks.size() + " ");
                                gui.requestRefresh();
                            }
                            case '4' -> {
                                chart.setTitle("AAPL — Price vs 5-day MA");
                                chart.setYAxisConfig(ChartAxisConfig.fixed(130, 170, "$%.0f"));
                                showMultiSeriesChart(chart, prices, ma);
                                infoLabel.setText(" View: Multi-series (Price + MA)  |  Points: " + NUM_POINTS + " ");
                                gui.requestRefresh();
                            }
                            case 'r' -> {
                                prices = generatePrices(150.0, NUM_POINTS);
                                volumes = generateVolumes(NUM_POINTS);
                                ticks = generateTicks(150.0, NUM_POINTS * 3);
                                ma = calculateMovingAverage(prices, 5);
                                infoLabel.setText(" Data regenerated!  |  Press 1/2/3/4 to view ");
                                gui.requestRefresh();
                            }
                        }
                    }
                }
                gui.updateScreen();
                Thread.sleep(16);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            gui.close();
        }
    }

    // ── View switchers ────────────────────────────────────────

    private static void showLineChart(Chart chart, List<Double> prices) {
        chart.removeSeries("Price");
        chart.removeSeries("MA");
        chart.removeSeries("Volume");
        chart.removeSeries("Ticks");
        chart.addSeries(new ChartSeries("Price", prices, ChartType.LINE, AnsiColor.BRIGHT_GREEN));
    }

    private static void showBarChart(Chart chart, List<Double> volumes) {
        chart.removeSeries("Price");
        chart.removeSeries("MA");
        chart.removeSeries("Volume");
        chart.removeSeries("Ticks");
        chart.addSeries(new ChartSeries("Volume", volumes, ChartType.BAR, AnsiColor.BRIGHT_CYAN));
    }

    private static void showScatterChart(Chart chart, List<Double> ticks) {
        chart.removeSeries("Price");
        chart.removeSeries("MA");
        chart.removeSeries("Volume");
        chart.removeSeries("Ticks");
        chart.addSeries(new ChartSeries("Ticks", ticks, ChartType.SCATTER, AnsiColor.BRIGHT_YELLOW));
    }

    private static void showMultiSeriesChart(Chart chart, List<Double> prices, List<Double> ma) {
        chart.removeSeries("Price");
        chart.removeSeries("MA");
        chart.removeSeries("Volume");
        chart.removeSeries("Ticks");
        chart.addSeries(new ChartSeries("Price", prices, ChartType.LINE, AnsiColor.BRIGHT_GREEN));
        chart.addSeries(new ChartSeries("MA", ma, ChartType.LINE, AnsiColor.BRIGHT_MAGENTA));
    }

    // ── Data generation ───────────────────────────────────────

    /**
     * Generate a random walk price series starting at {@code startPrice}.
     */
    private static List<Double> generatePrices(double startPrice, int count) {
        var prices = new ArrayList<Double>(count);
        double price = startPrice;
        for (int i = 0; i < count; i++) {
            price += rng.nextGaussian() * 2.5;
            price = Math.max(130, Math.min(170, price));
            prices.add(price);
        }
        return prices;
    }

    /**
     * Generate random volume data (in millions).
     */
    private static List<Double> generateVolumes(int count) {
        var volumes = new ArrayList<Double>(count);
        for (int i = 0; i < count; i++) {
            volumes.add(20 + rng.nextDouble() * 80);
        }
        return volumes;
    }

    /**
     * Generate tick-level price data (tighter range, more points).
     */
    private static List<Double> generateTicks(double centerPrice, int count) {
        var ticks = new ArrayList<Double>(count);
        for (int i = 0; i < count; i++) {
            ticks.add(centerPrice + rng.nextGaussian() * 5);
        }
        return ticks;
    }

    /**
     * Simple moving average of {@code window} periods.
     * Returns a list shorter than the input by {@code window-1} elements,
     * padded with NaN at the start so indices align.
     */
    private static List<Double> calculateMovingAverage(List<Double> data, int window) {
        var ma = new ArrayList<Double>(data.size());
        for (int i = 0; i < data.size(); i++) {
            if (i < window - 1) {
                ma.add(Double.NaN);
            } else {
                double sum = 0;
                for (int j = 0; j < window; j++) sum += data.get(i - j);
                ma.add(sum / window);
            }
        }
        // Replace NaN with the first valid MA value so the line doesn't gap
        if (!ma.isEmpty()) {
            double firstValid = ma.stream().filter(d -> !Double.isNaN(d)).findFirst().orElse(data.get(0));
            for (int i = 0; i < ma.size(); i++) {
                if (Double.isNaN(ma.get(i))) ma.set(i, firstValid);
            }
        }
        return ma;
    }
}