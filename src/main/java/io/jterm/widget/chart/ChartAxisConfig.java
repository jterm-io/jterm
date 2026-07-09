package io.jterm.widget.chart;

/**
 * Axis configuration for a {@link Chart}. Controls the value range, label
 * formatting, tick count, and scale type.
 *
 * <p>When {@code autoScale} is true (default), the chart recomputes min/max
 * from the data on each render. Set explicit min/max via {@link #fixed} for
 * a locked scale (e.g. $0–$200 for a stock chart).
 *
 * <p>When {@code logarithmic} is true, the Y-axis uses a logarithmic scale:
 * ticks are placed at "nice" log intervals (1, 2, 5, 10, 20, 50, 100, ...)
 * and value-to-pixel mapping uses log scale. The data range must be
 * strictly positive. Linear is the default.
 */
public record ChartAxisConfig(double min, double max, boolean autoScale, String labelFormat, boolean logarithmic) {

    /** Default: auto-scale, "%.0f" format, linear. */
    public static ChartAxisConfig auto() {
        return new ChartAxisConfig(0, 0, true, "%.0f", false);
    }

    /** Fixed range with custom format, linear. */
    public static ChartAxisConfig fixed(double min, double max, String labelFormat) {
        return new ChartAxisConfig(min, max, false, labelFormat, false);
    }

    /** Fixed range with default format, linear. */
    public static ChartAxisConfig fixed(double min, double max) {
        return new ChartAxisConfig(min, max, false, "%.0f", false);
    }

    /** Auto-scale with logarithmic Y-axis. Data must be strictly positive. */
    public static ChartAxisConfig logAuto() {
        return new ChartAxisConfig(0, 0, true, "%.0f", true);
    }

    /** Auto-scale with logarithmic Y-axis and custom label format. */
    public static ChartAxisConfig logAuto(String labelFormat) {
        return new ChartAxisConfig(0, 0, true, labelFormat, true);
    }

    /** Fixed range with logarithmic Y-axis and custom label format. */
    public static ChartAxisConfig logFixed(double min, double max, String labelFormat) {
        return new ChartAxisConfig(min, max, false, labelFormat, true);
    }

    /** Fixed range with logarithmic Y-axis and default format. */
    public static ChartAxisConfig logFixed(double min, double max) {
        return new ChartAxisConfig(min, max, false, "%.0f", true);
    }

    /** Format a value for axis display. */
    public String format(double value) {
        return labelFormat.formatted(value);
    }
}