package io.jterm.widget.model;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default mutable implementation of {@link GridModel}, backed by a
 * {@link CopyOnWriteArrayList} for thread-safe reads and writes.
 *
 * <p>All mutating methods fire {@link GridListener#gridChanged()} exactly
 * once per call (not once per element).
 *
 * @param <T> the row type
 */
public class DefaultGridModel<T> implements GridModel<T> {

    private final List<T> rows = new CopyOnWriteArrayList<>();
    private final List<GridListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a single row to the end of this model and fires {@code gridChanged}.
     *
     * @param row the row to add
     */
    public void addRow(T row) {
        rows.add(row);
        fireGridChanged();
    }

    /**
     * Adds all rows from the given collection to the end of this model and
     * fires {@code gridChanged} exactly once.
     *
     * @param newRows the rows to add
     */
    public void addRows(Collection<T> newRows) {
        rows.addAll(newRows);
        fireGridChanged();
    }

    /**
     * Replaces all rows in this model with the given collection, firing
     * {@code gridChanged} exactly once.
     *
     * @param newRows the new set of rows
     */
    public void setRows(Collection<T> newRows) {
        rows.clear();
        rows.addAll(newRows);
        fireGridChanged();
    }

    /**
     * Removes all rows from this model and fires {@code gridChanged}.
     */
    public void clear() {
        rows.clear();
        fireGridChanged();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public T getRow(int index) {
        return rows.get(index);  // CopyOnWriteArrayList.get throws IndexOutOfBoundsException for invalid index
    }

    @Override
    public void addGridListener(GridListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeGridListener(GridListener listener) {
        listeners.remove(listener);
    }

    private void fireGridChanged() {
        for (GridListener listener : listeners) {
            listener.gridChanged();
        }
    }
}