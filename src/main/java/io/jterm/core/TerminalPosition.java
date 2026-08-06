package io.jterm.core;

/** Zero-indexed (column, row) position on the terminal. (0,0) = top-left. */
public record TerminalPosition(int column, int row) {
    public static final TerminalPosition TOP_LEFT = new TerminalPosition(0, 0);

    /** Returns a copy of this position with the given column. @param col the new column @return the new position */
    public TerminalPosition withColumn(int col) { return new TerminalPosition(col, row); }
    /** Returns a copy of this position with the given row. @param row the new row @return the new position */
    public TerminalPosition withRow(int row) { return new TerminalPosition(column, row); }

    /** Returns a new position offset by (deltaCol, deltaRow). @param deltaCol the column delta @param deltaRow the row delta @return the offset position */
    public TerminalPosition offset(int deltaCol, int deltaRow) {
        return new TerminalPosition(column + deltaCol, row + deltaRow);
    }

    /** Returns true if this position is within the given terminal size. @param size the bounding size @return true if valid */
    public boolean isValid(TerminalSize size) {
        return column >= 0 && column < size.columns() && row >= 0 && row < size.rows();
    }
}
