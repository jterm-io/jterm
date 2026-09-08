package io.jterm.core;

/**
 * Zero-indexed (column, row) position on the terminal. (0,0) = top-left.
 *
 * @param column zero-indexed column, growing rightward
 * @param row    zero-indexed row, growing downward
 */
public record TerminalPosition(int column, int row) {
    /**
     * Shared constant for the top-left corner of the terminal.
     */
    public static final TerminalPosition TOP_LEFT = new TerminalPosition(0, 0);

    /**
     * Returns a copy of this position with the given column.
     *
     * @param col the new column
     * @return a new position with this row and the given column
     */
    public TerminalPosition withColumn(int col) { return new TerminalPosition(col, row); }

    /**
     * Returns a copy of this position with the given row.
     *
     * @param row the new row
     * @return a new position with this column and the given row
     */
    public TerminalPosition withRow(int row) { return new TerminalPosition(column, row); }

    /**
     * Returns a new position offset by the given deltas.
     *
     * @param deltaCol the column delta
     * @param deltaRow the row delta
     * @return a new position offset by (deltaCol, deltaRow)
     */
    public TerminalPosition offset(int deltaCol, int deltaRow) {
        return new TerminalPosition(column + deltaCol, row + deltaRow);
    }

    /**
     * Returns whether this position is within the given terminal size.
     *
     * @param size the bounding size
     * @return true if the position is valid (0 &le; column &lt; size.columns()
     *         and 0 &le; row &lt; size.rows())
     */
    public boolean isValid(TerminalSize size) {
        return column >= 0 && column < size.columns() && row >= 0 && row < size.rows();
    }
}
