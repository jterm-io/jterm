package io.jterm.widget.model;

/**
 * Listener for changes to a {@link ListModel}.
 */
public interface ListDataListener {

    /**
     * Called when the contents of the list have changed.
     *
     * @param e the event describing the change
     */
    void contentsChanged(ListDataEvent e);

    /**
     * Called when one or more items have been added.
     *
     * @param e the event describing the added interval
     */
    void intervalAdded(ListDataEvent e);

    /**
     * Called when one or more items have been removed.
     *
     * @param e the event describing the removed interval
     */
    void intervalRemoved(ListDataEvent e);
}
