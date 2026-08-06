package io.jterm.core;

/** Terminal dimensions in columns × rows. */
public record TerminalSize(int columns, int rows) {
    public static final TerminalSize ZERO = new TerminalSize(0, 0);

    /** Returns a copy of this size with the given column count. @param cols the new column count @return the new size */
    public TerminalSize withColumns(int cols) { return new TerminalSize(cols, rows); }
    /** Returns a copy of this size with the given row count. @param rows the new row count @return the new size */
    public TerminalSize withRows(int rows) { return new TerminalSize(columns, rows); }

    /** Returns the total cell area (columns × rows). @return the cell count */
    public int area() { return columns * rows; }

    /** Returns true if the given position lies within this size. @param pos the position to test @return true if contained */
    public boolean contains(TerminalPosition pos) {
        return pos.column() >= 0 && pos.column() < columns
            && pos.row() >= 0 && pos.row() < rows;
    }
}
