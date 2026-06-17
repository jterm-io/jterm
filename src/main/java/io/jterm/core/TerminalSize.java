package io.jterm.core;

/** Terminal dimensions in columns × rows. */
public record TerminalSize(int columns, int rows) {
    public static final TerminalSize ZERO = new TerminalSize(0, 0);

    public TerminalSize withColumns(int cols) { return new TerminalSize(cols, rows); }
    public TerminalSize withRows(int rows) { return new TerminalSize(columns, rows); }

    public int area() { return columns * rows; }

    public boolean contains(TerminalPosition pos) {
        return pos.column() >= 0 && pos.column() < columns
            && pos.row() >= 0 && pos.row() < rows;
    }
}
