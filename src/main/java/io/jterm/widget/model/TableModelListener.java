package io.jterm.widget.model;

/**
 * Listener for changes to a {@link TableModel}.
 */
public interface TableModelListener {

    /**
     * Called when the table model has changed.
     *
     * @param e the event describing the change
     */
    void tableChanged(TableModelEvent e);
}
