package io.jterm.widget.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Convenience base class for {@link ListModel} implementations.
 *
 * @param <T> the type of element held in this model
 */
public abstract class AbstractListModel<T> implements ListModel<T> {

    private final List<ListDataListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    /** Registers a listener to be notified of list data changes.
     * @param listener the listener to add */
    public void addListDataListener(ListDataListener listener) {
        listeners.add(listener);
    }

    @Override
    /** Removes a previously registered list data listener.
     * @param listener the listener to remove */
    public void removeListDataListener(ListDataListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies listeners that the entire contents have changed.
     *
     * @param index0 first affected index, inclusive
     * @param index1 last affected index, inclusive
     */
    protected void fireContentsChanged(int index0, int index1) {
        fireEvent(new ListDataEvent(ListDataEventType.CONTENTS_CHANGED, index0, index1));
    }

    /**
     * Notifies listeners that items have been added in the specified interval.
     *
     * @param index0 first added index, inclusive
     * @param index1 last added index, inclusive
     */
    protected void fireIntervalAdded(int index0, int index1) {
        fireEvent(new ListDataEvent(ListDataEventType.INTERVAL_ADDED, index0, index1));
    }

    /**
     * Notifies listeners that items have been removed from the specified interval.
     *
     * @param index0 first removed index, inclusive
     * @param index1 last removed index, inclusive
     */
    protected void fireIntervalRemoved(int index0, int index1) {
        fireEvent(new ListDataEvent(ListDataEventType.INTERVAL_REMOVED, index0, index1));
    }

    private void fireEvent(ListDataEvent event) {
        for (ListDataListener listener : listeners) {
            switch (event.type()) {
                case CONTENTS_CHANGED -> listener.contentsChanged(event);
                case INTERVAL_ADDED -> listener.intervalAdded(event);
                case INTERVAL_REMOVED -> listener.intervalRemoved(event);
                default -> throw new IllegalStateException("Unexpected event type: " + event.type());
            }
        }
    }
}
