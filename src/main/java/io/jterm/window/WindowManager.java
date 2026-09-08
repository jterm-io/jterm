package io.jterm.window;

import java.util.Collection;

/**
 * Manages window z-order, focus activation, and lifecycle callbacks.
 */
public interface WindowManager {
    /**
     * Adds a window to the top of the z-order and gives it focus.
     *
     * @param window the window to add
     */
    void addWindow(Window window);

    /**
     * Removes a window and activates the next one below it.
     *
     * @param window the window to remove
     */
    void removeWindow(Window window);

    /**
     * Makes the given window the active (focused) window.
     *
     * @param window the window to activate
     */
    void setActiveWindow(Window window);

    /**
     * Returns the currently active (focused) window.
     *
     * @return the active window, or {@code null} if none
     */
    Window getActiveWindow();

    /**
     * Returns the windows in z-order (bottom first).
     *
     * @return an unmodifiable view of the managed windows
     */
    Collection<Window> getWindows();

    /**
     * Returns true if the given window is managed by this manager.
     *
     * @param window the window to test
     * @return true if the window is managed
     */
    boolean containsWindow(Window window);
}