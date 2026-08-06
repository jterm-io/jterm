package io.jterm.gui;

import io.jterm.widget.Component;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.Window;

import java.util.ArrayDeque;
import java.util.Deque;

/** Manages a stack of screens (windows) in a DefaultTextGUI for push/pop navigation. */
public class ScreenManager {
    private final DefaultTextGUI gui;
    private final Deque<Window> stack = new ArrayDeque<>();
    private final Deque<Component> previousFocus = new ArrayDeque<>();
    private final Object lock = new Object();

    /**
     * Create a screen manager attached to the given GUI.
     *
     * @param gui the GUI to manage screens for
     */
    public ScreenManager(DefaultTextGUI gui) {
        this.gui = gui;
    }

    /**
     * Push a new window onto the stack and make it active.
     *
     * @param window the window to push
     */
    public void push(Window window) {
        synchronized (lock) {
            var current = stack.peek();
            if (current != null) {
                previousFocus.push(current.getFocusedComponent());
                // Keep previous window registered but inactive; GUI will draw it as inactive.
            }
            stack.push(window);
            gui.addWindow(window);
            window.open(gui);
        }
    }

    /**
     * Pop the top window off the stack and restore the previous one.
     *
     * @return the removed window, or null if the stack was empty
     */
    public Window pop() {
        synchronized (lock) {
            var window = stack.poll();
            if (window == null) {
                return null;
            }
            gui.removeWindow(window);
            var previous = stack.peek();
            if (previous != null) {
                previousFocus.poll();
                gui.setActiveWindow(previous);
            }
            return window;
        }
    }

    /**
     * Return the top window without removing it.
     *
     * @return the current top-of-stack window, or null if empty
     */
    public Window peek() {
        synchronized (lock) {
            return stack.peek();
        }
    }

    /**
     * Return the number of windows currently on the stack.
     *
     * @return the stack depth
     */
    public int size() {
        synchronized (lock) {
            return stack.size();
        }
    }

    /**
     * Remove all windows from the stack.
     */
    public void clear() {
        synchronized (lock) {
            while (!stack.isEmpty()) {
                gui.removeWindow(stack.pop());
            }
            previousFocus.clear();
        }
    }
}
