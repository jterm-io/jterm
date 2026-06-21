package io.jterm.widget.model;

/**
 * Model providing indexed access to a list of elements.
 *
 * @param <T> the type of element held in this model
 */
public interface ListModel<T> {

    /**
     * Returns the number of elements in this model.
     *
     * @return the number of elements
     */
    int getSize();

    /**
     * Returns the element at the specified index.
     *
     * @param index the index of the element to retrieve
     * @return the element at {@code index}
     */
    T getElementAt(int index);

    /**
     * Registers a listener to be notified of changes to this model.
     *
     * @param listener the listener to add
     */
    void addListDataListener(ListDataListener listener);

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    void removeListDataListener(ListDataListener listener);
}
