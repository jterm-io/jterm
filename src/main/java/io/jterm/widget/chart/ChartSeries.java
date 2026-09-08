package io.jterm.widget.chart;

import io.jterm.style.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single data series for {@link Chart}. Immutable after construction —
 * mutate by replacing the series in the chart.
 *
 * <p><b>Usage</b></p>
 * <pre>{@code
 * var series = new ChartSeries("AAPL", List.of(150.0, 152.0, 149.0, 155.0),
 *         ChartType.LINE, AnsiColor.GREEN);
 * }</pre>
 *
 * @param name   series name shown in the chart legend (not {@code null})
 * @param values data values in plot order (stored unmodifiable; {@code null} becomes empty)
 * @param type   chart type controlling how the values are rendered (not {@code null})
 * @param color  color used for the series line/markers and legend swatch (not {@code null})
 */
public record ChartSeries(String name, List<Double> values, ChartType type, Color color) {

    /**
     * Compact constructor: validates required fields and stores {@code values}
     * as an unmodifiable copy ({@code null} becomes empty).
     *
     * @throws IllegalArgumentException if {@code name}, {@code type}, or {@code color} is {@code null}
     */
    public ChartSeries {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        values = values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        if (color == null) throw new IllegalArgumentException("color cannot be null");
    }

    /**
     * Convenience constructor — defaults to LINE type.
     *
     * @param name   series name shown in the chart legend
     * @param values data values in plot order
     * @param color  color used for the series line/markers and legend swatch
     */
    public ChartSeries(String name, List<Double> values, Color color) {
        this(name, values, ChartType.LINE, color);
    }

    /**
     * Number of data points in this series.
     *
     * @return the number of data points in this series
     */
    public int size() { return values.size(); }

    /**
     * Whether the series has at least one point.
     *
     * @return {@code true} if the series has no data points
     */
    public boolean isEmpty() { return values.isEmpty(); }

    /**
     * Minimum value in the series, or {@code Double.NaN} if empty.
     *
     * @return the minimum value, or {@code Double.NaN} if empty
     */
    public double min() {
        if (values.isEmpty()) return Double.NaN;
        return values.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
    }

    /**
     * Maximum value in the series, or {@code Double.NaN} if empty.
     *
     * @return the maximum value, or {@code Double.NaN} if empty
     */
    public double max() {
        if (values.isEmpty()) return Double.NaN;
        return values.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
    }
}