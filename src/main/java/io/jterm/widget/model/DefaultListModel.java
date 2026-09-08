package io.jterm.widget.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default mutable implementation of {@link ListModel}.
 *
 * @param <T> the type of element held in this model
 */
public class DefaultListModel<T> extends AbstractListModel<T> {

    /** Creates an empty list model. */
    public DefaultListModel() {}

    private final List<T> items = new CopyOnWriteArrayList<>();

    /**
     * Returns the number of elements in this list model.
     *
     * @return the list size
     */
    @Override
    public int getSize() {
        return items.size();
    }

    /**
     * Returns the element at the specified index.
     *
     * @param index the zero-based index
     * @return the element at that index
     */
    @Override
    public T getElementAt(int index) {
        return items.get(index);
    }

    /**
     * Adds an element to the end of this model.
     *
     * @param item the element to add
     */
    public void addElement(T item) {
        int index = items.size();
        items.add(item);
        fireIntervalAdded(index, index);
    }

    /**
     * Inserts an element at the specified index.
     *
     * @param index the index at which to insert
     * @param item the element to insert
     */
    public void addElementAt(int index, T item) {
        items.add(index, item);
        fireIntervalAdded(index, index);
    }

    /**
     * Replaces the element at the specified index.
     *
     * @param index the index of the element to replace
     * @param item the new element
     */
    public void setElementAt(int index, T item) {
        items.set(index, item);
        fireContentsChanged(index, index);
    }

    /**
     * Removes the element at the specified index.
     *
     * @param index the index of the element to remove
     */
    public void removeElementAt(int index) {
        items.remove(index);
        fireIntervalRemoved(index, index);
    }

    /**
     * Removes all elements from this model.
     */
    public void clear() {
        int size = items.size();
        items.clear();
        fireContentsChanged(0, Math.max(0, size - 1));
    }
}
