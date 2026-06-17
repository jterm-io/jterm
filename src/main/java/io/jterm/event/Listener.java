package io.jterm.event;

/** Functional listener interface. */
@FunctionalInterface
public interface Listener<T> {
    void onEvent(T event);
}
