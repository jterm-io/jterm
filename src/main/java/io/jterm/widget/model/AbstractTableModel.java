package io.jterm.widget.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Convenience base class for {@link TableModel} implementations.
 */
public abstract class AbstractTableModel implements TableModel {

    private final List<TableModelListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    /** Registers a listener to be notified of table model changes.
     * @param listener the listener to add */
    public void addTableModelListener(TableModelListener listener) {
        listeners.add(listener);
    }

    @Override
    /** Removes a previously registered table model listener.
     * @param listener the listener to remove */
    public void removeTableModelListener(TableModelListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies listeners that rows have been added.
     *
     * @param firstRow first added row, inclusive
     * @param lastRow last added row, inclusive
     */
    protected void fireRowsAdded(int firstRow, int lastRow) {
        fireEvent(new TableModelEvent(TableModelEventType.ROWS_ADDED, firstRow, lastRow, -1));
    }

    /**
     * Notifies listeners that rows have been removed.
     *
     * @param firstRow first removed row, inclusive
     * @param lastRow last removed row, inclusive
     */
    protected void fireRowsRemoved(int firstRow, int lastRow) {
        fireEvent(new TableModelEvent(TableModelEventType.ROWS_REMOVED, firstRow, lastRow, -1));
    }

    /**
     * Notifies listeners that rows have changed.
     *
     * @param firstRow first changed row, inclusive
     * @param lastRow last changed row, inclusive
     */
    protected void fireRowsChanged(int firstRow, int lastRow) {
        fireEvent(new TableModelEvent(TableModelEventType.ROWS_CHANGED, firstRow, lastRow, -1));
    }

    /**
     * Notifies listeners that cells in a single column have changed.
     *
     * @param firstRow first changed row, inclusive
     * @param lastRow last changed row, inclusive
     * @param column the affected column
     */
    protected void fireCellsChanged(int firstRow, int lastRow, int column) {
        fireEvent(new TableModelEvent(TableModelEventType.CELLS_CHANGED, firstRow, lastRow, column));
    }

    /**
     * Notifies listeners that the structure of the table has changed
     * (for example, columns were added or removed).
     */
    protected void fireStructureChanged() {
        fireEvent(new TableModelEvent(TableModelEventType.STRUCTURE_CHANGED, -1, -1, -1));
    }

    private void fireEvent(TableModelEvent event) {
        for (TableModelListener listener : listeners) {
            listener.tableChanged(event);
        }
    }
}
