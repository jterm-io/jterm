package io.jterm.screen;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.style.TextCell;
import java.util.ArrayList;
import java.util.List;

/** 2D grid of TextCells. Backed by a flat array for cache efficiency.
 *  All public methods are synchronized for thread safety. */
public class ScreenBuffer {
    private final int columns;
    private final int rows;
    private final TextCell[] cells;

    /** Creates a screen buffer filled with empty cells. @param size the buffer dimensions */
    public ScreenBuffer(TerminalSize size) {
        this(size, TextCell.EMPTY);
    }

    /** Creates a screen buffer filled with the given cell. @param size the buffer dimensions @param fill the fill cell */
    public ScreenBuffer(TerminalSize size, TextCell fill) {
        this.columns = size.columns();
        this.rows = size.rows();
        this.cells = new TextCell[columns * rows];
        java.util.Arrays.fill(cells, fill);
    }

    /** Returns the dimensions of this buffer. @return the size */
    public TerminalSize size() { return new TerminalSize(columns, rows); }

    /** Returns the cell at the given coordinates, or EMPTY if out of bounds. @param col the column @param row the row @return the cell */
    public synchronized TextCell getCell(int col, int row) {
        if (col < 0 || col >= columns || row < 0 || row >= rows)
            return TextCell.EMPTY;
        return cells[row * columns + col];
    }

    /** Sets the cell at the given coordinates; out-of-bounds writes are ignored. @param col the column @param row the row @param cell the cell to set */
    public synchronized void setCell(int col, int row, TextCell cell) {
        if (col < 0 || col >= columns || row < 0 || row >= rows) return;
        cells[row * columns + col] = cell;
    }

    /** Sets the cell at the given position. @param pos the position @param cell the cell to set */
    public void setCell(TerminalPosition pos, TextCell cell) {
        setCell(pos.column(), pos.row(), cell);
    }

    /** Fills the entire buffer with the given cell. @param cell the fill cell */
    public synchronized void fill(TextCell cell) {
        java.util.Arrays.fill(cells, cell);
    }

    /** Fills a rectangular region with the given cell. @param col the starting column @param row the starting row @param width the region width @param height the region height @param cell the fill cell */
    public synchronized void fillRect(int col, int row, int width, int height, TextCell cell) {
        for (int r = row; r < row + height && r < rows; r++) {
            for (int c = col; c < col + width && c < columns; c++) {
                setCell(c, r, cell);
            }
        }
    }

    /** Returns list of (position, cell) pairs where this buffer differs from other. */
    public synchronized List<CellDiff> diffFrom(ScreenBuffer other) {
        List<CellDiff> diffs = new ArrayList<>();
        int maxRows = Math.min(rows, other.rows);
        int maxCols = Math.min(columns, other.columns);
        for (int r = 0; r < maxRows; r++) {
            for (int c = 0; c < maxCols; c++) {
                TextCell a = getCell(c, r);
                TextCell b = other.getCell(c, r);
                if (!a.equals(b)) {
                    diffs.add(new CellDiff(c, r, a));
                }
            }
        }
        return diffs;
    }

    /** Copies cell data from the source buffer into this one (up to the smaller of both dimensions). @param source the buffer to copy from */
    public synchronized void copyFrom(ScreenBuffer source) {
        int maxRows = Math.min(rows, source.rows);
        int maxCols = Math.min(columns, source.columns);
        for (int r = 0; r < maxRows; r++) {
            System.arraycopy(source.cells, r * source.columns, cells, r * columns, maxCols);
        }
    }

    public record CellDiff(int column, int row, TextCell cell) {}
}
