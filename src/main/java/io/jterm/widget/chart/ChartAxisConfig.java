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
 *
 * @param min         fixed minimum value when {@code autoScale} is false (ignored otherwise)
 * @param max         fixed maximum value when {@code autoScale} is false (ignored otherwise)
 * @param autoScale   true to recompute min/max from the data on each render
 * @param labelFormat a {@link String#format(String, Object...)} pattern such as {@code "%.0f"} for tick labels
 * @param logarithmic true for a logarithmic Y-axis (data must be strictly positive)
 */
public record ChartAxisConfig(double min, double max, boolean autoScale, String labelFormat, boolean logarithmic) {

    /**
     * Default: auto-scale, "%.0f" format, linear.
     *
     * @return an auto-scaling, linear axis configuration
     */
    public static ChartAxisConfig auto() {
        return new ChartAxisConfig(0, 0, true, "%.0f", false);
    }

    /**
     * Fixed range with custom format, linear.
     *
     * @param min         fixed minimum value of the axis
     * @param max         fixed maximum value of the axis
     * @param labelFormat {@link String#format(String, Object...)} pattern for tick labels
     * @return a fixed-range, linear axis configuration
     */
    public static ChartAxisConfig fixed(double min, double max, String labelFormat) {
        return new ChartAxisConfig(min, max, false, labelFormat, false);
    }

    /**
     * Fixed range with default format, linear.
     *
     * @param min fixed minimum value of the axis
     * @param max fixed maximum value of the axis
     * @return a fixed-range, linear axis configuration using the "%.0f" format
     */
    public static ChartAxisConfig fixed(double min, double max) {
        return new ChartAxisConfig(min, max, false, "%.0f", false);
    }

    /**
     * Auto-scale with logarithmic Y-axis. Data must be strictly positive.
     *
     * @return an auto-scaling, logarithmic axis configuration
     */
    public static ChartAxisConfig logAuto() {
        return new ChartAxisConfig(0, 0, true, "%.0f", true);
    }

    /**
     * Auto-scale with logarithmic Y-axis and custom label format.
     *
     * @param labelFormat {@link String#format(String, Object...)} pattern for tick labels
     * @return an auto-scaling, logarithmic axis configuration
     */
    public static ChartAxisConfig logAuto(String labelFormat) {
        return new ChartAxisConfig(0, 0, true, labelFormat, true);
    }

    /**
     * Fixed range with logarithmic Y-axis and custom label format.
     *
     * @param min         fixed minimum value of the axis (must be positive)
     * @param max         fixed maximum value of the axis
     * @param labelFormat {@link String#format(String, Object...)} pattern for tick labels
     * @return a fixed-range, logarithmic axis configuration
     */
    public static ChartAxisConfig logFixed(double min, double max, String labelFormat) {
        return new ChartAxisConfig(min, max, false, labelFormat, true);
    }

    /**
     * Fixed range with logarithmic Y-axis and default format.
     *
     * @param min fixed minimum value of the axis (must be positive)
     * @param max fixed maximum value of the axis
     * @return a fixed-range, logarithmic axis configuration using the "%.0f" format
     */
    public static ChartAxisConfig logFixed(double min, double max) {
        return new ChartAxisConfig(min, max, false, "%.0f", true);
    }

    /**
     * Format a value for axis display.
     *
     * @param value the data value to format
     * @return the formatted label produced by this config's label format
     */
    public String format(double value) {
        return labelFormat.formatted(value);
    }
}