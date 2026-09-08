package io.jterm.event;

/**
 * Functional listener interface.
 *
 * @param <T> the event type this listener receives
 */
@FunctionalInterface
public interface Listener<T> {
    /**
     * Invoked when an event is fired to this listener.
     *
     * @param event the event that occurred
     */
    void onEvent(T event);
}
