package io.jterm.widget.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default mutable implementation of {@link TableModel}.
 */
public class DefaultTableModel extends AbstractTableModel {

    private final List<String> headers;
    private final List<List<String>> rows = new CopyOnWriteArrayList<>();

    /**
     * Creates a model with the given column headers.
     *
     * @param headers the column headers
     */
    public DefaultTableModel(String... headers) {
        this.headers = new CopyOnWriteArrayList<>(Arrays.asList(headers));
    }

    /** {@inheritDoc} — returns the current row count. */
    @Override
    public int getRowCount() {
        return rows.size();
    }

    /** {@inheritDoc} — returns the column count from the headers. */
    @Override
    public int getColumnCount() {
        return headers.size();
    }

    /**
     * {@inheritDoc} — returns the header at the given index, or empty string if out of range.
     */
    @Override
    public String getColumnName(int col) {
        if (col < 0 || col >= headers.size()) return "";
        return headers.get(col);
    }

    /**
     * {@inheritDoc} — returns the cell value, or empty string if row/column is out of range.
     */
    @Override
    public String getValueAt(int row, int col) {
        if (row < 0 || row >= rows.size()) return "";
        List<String> cells = rows.get(row);
        if (col < 0 || col >= cells.size()) return "";
        return cells.get(col);
    }

    /**
     * Adds a row to the end of this model.
     *
     * @param cells the cell values
     */
    public void addRow(String... cells) {
        int index = rows.size();
        rows.add(new ArrayList<>(Arrays.asList(cells)));
        fireRowsAdded(index, index);
    }

    /**
     * Inserts a row at the specified index.
     *
     * @param index the index at which to insert
     * @param cells the cell values
     */
    public void insertRow(int index, String... cells) {
        rows.add(index, new ArrayList<>(Arrays.asList(cells)));
        fireRowsAdded(index, index);
    }

    /**
     * Replaces the value of a single cell.
     *
     * @param row the row index
     * @param col the column index
     * @param value the new value
     */
    public void setValueAt(int row, int col, String value) {
        rows.get(row).set(col, value);
        fireCellsChanged(row, row, col);
    }

    /**
     * Removes the row at the specified index.
     *
     * @param index the row index
     */
    public void removeRow(int index) {
        rows.remove(index);
        fireRowsRemoved(index, index);
    }

    /**
     * Removes all rows from this model.
     */
    public void clear() {
        int size = rows.size();
        rows.clear();
        fireRowsChanged(0, Math.max(0, size - 1));
    }
}
