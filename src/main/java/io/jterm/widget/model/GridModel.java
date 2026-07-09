package io.jterm.widget.model;

/**
 * Generic interface for row data used by the {@code DataGrid} widget.
 *
 * @param <T> the row type
 */
public interface GridModel<T> {

    /**
     * Returns the number of rows currently held by this model.
     *
     * @return the row count
     */
    int getRowCount();

    /**
     * Returns the row at the specified index.
     *
     * @param index the row index
     * @return the row at that index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    T getRow(int index);

    /**
     * Registers a listener to be notified of changes to this model.
     *
     * @param listener the listener to add
     */
    void addGridListener(GridListener listener);

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    void removeGridListener(GridListener listener);
}