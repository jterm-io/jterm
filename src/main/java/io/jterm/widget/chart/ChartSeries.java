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
 */
public record ChartSeries(String name, List<Double> values, ChartType type, Color color) {

    public ChartSeries {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        values = values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        if (color == null) throw new IllegalArgumentException("color cannot be null");
    }

    /** Convenience constructor — defaults to LINE type. */
    public ChartSeries(String name, List<Double> values, Color color) {
        this(name, values, ChartType.LINE, color);
    }

    /** Number of data points in this series. */
    public int size() { return values.size(); }

    /** Whether the series has at least one point. */
    public boolean isEmpty() { return values.isEmpty(); }

    /** Minimum value in the series, or {@code Double.NaN} if empty. */
    public double min() {
        if (values.isEmpty()) return Double.NaN;
        return values.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
    }

    /** Maximum value in the series, or {@code Double.NaN} if empty. */
    public double max() {
        if (values.isEmpty()) return Double.NaN;
        return values.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
    }
}