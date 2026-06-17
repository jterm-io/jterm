package io.jterm.widget.chart;

/**
 * Axis configuration for a {@link Chart}. Controls the value range, label
 * formatting, and tick count.
 *
 * <p>When {@code autoScale} is true (default), the chart recomputes min/max
 * from the data on each render. Set explicit min/max via {@link #fixed} for
 * a locked scale (e.g. $0–$200 for a stock chart).
 */
public record ChartAxisConfig(double min, double max, boolean autoScale, String labelFormat) {

    /** Default: auto-scale, "%.0f" format. */
    public static ChartAxisConfig auto() {
        return new ChartAxisConfig(0, 0, true, "%.0f");
    }

    /** Fixed range with custom format. */
    public static ChartAxisConfig fixed(double min, double max, String labelFormat) {
        return new ChartAxisConfig(min, max, false, labelFormat);
    }

    /** Fixed range with default format. */
    public static ChartAxisConfig fixed(double min, double max) {
        return new ChartAxisConfig(min, max, false, "%.0f");
    }

    /** Format a value for axis display. */
    public String format(double value) {
        return labelFormat.formatted(value);
    }
}