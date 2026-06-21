package io.jterm.widget.model;

/**
 * Model providing tabular data.
 */
public interface TableModel {

    /**
     * Returns the number of rows in this model.
     *
     * @return the row count
     */
    int getRowCount();

    /**
     * Returns the number of columns in this model.
     *
     * @return the column count
     */
    int getColumnCount();

    /**
     * Returns the name of the specified column.
     *
     * @param col the column index
     * @return the column name
     */
    String getColumnName(int col);

    /**
     * Returns the value at the specified row and column.
     *
     * @param row the row index
     * @param col the column index
     * @return the value as a string
     */
    String getValueAt(int row, int col);

    /**
     * Registers a listener to be notified of changes to this model.
     *
     * @param listener the listener to add
     */
    void addTableModelListener(TableModelListener listener);

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    void removeTableModelListener(TableModelListener listener);
}
