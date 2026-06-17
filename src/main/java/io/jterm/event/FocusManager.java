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
    private Component focusedComponent;
    private final List<Listener<Component>> listeners = new ArrayList<>();

    public Component getFocusedComponent() {
        return focusedComponent;
    }

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

    public void addListener(Listener<Component> listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener<Component> listener) {
        listeners.remove(listener);
    }
}
