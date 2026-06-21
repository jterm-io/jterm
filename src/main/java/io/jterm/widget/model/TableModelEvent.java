package io.jterm.widget.model;

/**
 * Event describing a change to a {@link TableModel}.
 *
 * @param type the kind of change
 * @param firstRow first affected row, inclusive
 * @param lastRow last affected row, inclusive
 * @param column affected column; a negative value indicates all columns
 */
public record TableModelEvent(TableModelEventType type, int firstRow, int lastRow, int column) {
}
