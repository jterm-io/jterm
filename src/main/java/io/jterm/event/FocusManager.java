package io.jterm.event;

import io.jterm.widget.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the focused component across windows and notifies listeners when focus changes.
 *
 * One FocusManager is owned per {@link io.jterm.window.TextGUI}.
 */
public class FocusManager {
    private volatile Component focusedComponent;
    private final List<Listener<Component>> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * Return the component that currently has focus.
     *
     * @return the focused component, or null if none has focus
     */
    public Component getFocusedComponent() {
        return focusedComponent;
    }

    /**
     * Set the focused component for this window.
     *
     * @param component the component
     */
    public void setFocusedComponent(Component component) {
        if (focusedComponent == component) return;
        Component old = focusedComponent;
        focusedComponent = component;
        if (old != null) old.setFocused(false);
        if (component != null) component.setFocused(true);
        for (var listener : listeners) {
            listener.onEvent(component);
        }
    }

    /**
     * Remove focus from the currently focused component.
     */
    public void clearFocus() {
        if (focusedComponent != null) {
            Component old = focusedComponent;
            focusedComponent = null;
            old.setFocused(false);
            for (var listener : listeners) {
                listener.onEvent(null);
            }
        }
    }

    /**
     * Register a listener to be notified when focus changes.
     *
     * @param listener the listener to register
     */
    public void addListener(Listener<Component> listener) {
        listeners.add(listener);
    }

    /**
     * Remove a previously registered focus-change listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(Listener<Component> listener) {
        listeners.remove(listener);
    }
}
