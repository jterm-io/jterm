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

    public ScreenManager(DefaultTextGUI gui) {
        this.gui = gui;
    }

    public void push(Window window) {
        synchronized (lock) {
            var current = stack.peek();
            if (current != null) {
                previousFocus.push(current.getFocusedComponent());
                // Keep previous window registered but inactive; GUI will draw it as inactive.
            }
            stack.push(window);
            gui.addWindow(window);
        }
    }

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

    public Window peek() {
        synchronized (lock) {
            return stack.peek();
        }
    }

    public int size() {
        synchronized (lock) {
            return stack.size();
        }
    }

    public void clear() {
        synchronized (lock) {
            while (!stack.isEmpty()) {
                gui.removeWindow(stack.pop());
            }
            previousFocus.clear();
        }
    }
}
