package io.jterm.core;

/** Zero-indexed (column, row) position on the terminal. (0,0) = top-left. */
public record TerminalPosition(int column, int row) {
    public static final TerminalPosition TOP_LEFT = new TerminalPosition(0, 0);

    public TerminalPosition withColumn(int col) { return new TerminalPosition(col, row); }
    public TerminalPosition withRow(int row) { return new TerminalPosition(column, row); }

    public TerminalPosition offset(int deltaCol, int deltaRow) {
        return new TerminalPosition(column + deltaCol, row + deltaRow);
    }

    public boolean isValid(TerminalSize size) {
        return column >= 0 && column < size.columns() && row >= 0 && row < size.rows();
    }
}
