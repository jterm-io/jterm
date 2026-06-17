package io.jterm.screen;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.style.TextCell;
import java.util.ArrayList;
import java.util.List;

/** 2D grid of TextCells. Backed by a flat array for cache efficiency. */
public class ScreenBuffer {
    private final int columns;
    private final int rows;
    private final TextCell[] cells;

    public ScreenBuffer(TerminalSize size) {
        this(size, TextCell.EMPTY);
    }

    public ScreenBuffer(TerminalSize size, TextCell fill) {
        this.columns = size.columns();
        this.rows = size.rows();
        this.cells = new TextCell[columns * rows];
        java.util.Arrays.fill(cells, fill);
    }

    public TerminalSize size() { return new TerminalSize(columns, rows); }

    public TextCell getCell(int col, int row) {
        if (col < 0 || col >= columns || row < 0 || row >= rows)
            return TextCell.EMPTY;
        return cells[row * columns + col];
    }

    public void setCell(int col, int row, TextCell cell) {
        if (col < 0 || col >= columns || row < 0 || row >= rows) return;
        cells[row * columns + col] = cell;
    }

    public void setCell(TerminalPosition pos, TextCell cell) {
        setCell(pos.column(), pos.row(), cell);
    }

    public void fill(TextCell cell) {
        java.util.Arrays.fill(cells, cell);
    }

    public void fillRect(int col, int row, int width, int height, TextCell cell) {
        for (int r = row; r < row + height && r < rows; r++) {
            for (int c = col; c < col + width && c < columns; c++) {
                setCell(c, r, cell);
            }
        }
    }

    /** Returns list of (position, cell) pairs where this buffer differs from other. */
    public List<CellDiff> diffFrom(ScreenBuffer other) {
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

    public void copyFrom(ScreenBuffer source) {
        int maxRows = Math.min(rows, source.rows);
        int maxCols = Math.min(columns, source.columns);
        for (int r = 0; r < maxRows; r++) {
            System.arraycopy(source.cells, r * source.columns, cells, r * columns, maxCols);
        }
    }

    public record CellDiff(int column, int row, TextCell cell) {}
}
