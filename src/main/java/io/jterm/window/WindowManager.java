package io.jterm.window;

import java.util.Collection;

/** Manages window z-order, focus activation, and lifecycle callbacks. */
public interface WindowManager {
    void addWindow(Window window);
    void removeWindow(Window window);
    void setActiveWindow(Window window);
    Window getActiveWindow();
    Collection<Window> getWindows();
    boolean containsWindow(Window window);
}
