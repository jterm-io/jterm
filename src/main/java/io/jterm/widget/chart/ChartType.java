package io.jterm.widget.chart;

/**
 * Plotting style for a {@link ChartSeries}.
 *
 * <ul>
 *   <li>{@link #LINE} — connected line segments using box-drawing characters</li>
 *   <li>{@link #BAR} — vertical bar chart using block characters</li>
 *   <li>{@link #SCATTER} — individual point markers, no connecting lines</li>
 * </ul>
 */
public enum ChartType {
    LINE,
    BAR,
    SCATTER
}