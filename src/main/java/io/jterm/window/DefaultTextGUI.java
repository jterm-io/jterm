package io.jterm.window;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.event.Listener;
import io.jterm.graphics.TextGraphics;
import io.jterm.graphics.TextGraphicsExtensions;
import io.jterm.screen.Screen;
import io.jterm.widget.Button;
import io.jterm.widget.ListBox;
import io.jterm.widget.Table;
import io.jterm.widget.TextBox;
import io.jterm.widget.Container;
import io.jterm.widget.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Default window manager, input dispatch, and event loop. */
public class DefaultTextGUI implements TextGUI {
    private final Screen screen;
    private final List<Window> windows = new ArrayList<>();
    private final List<Window> windowsToRemove = new ArrayList<>();
    private Window activeWindow;
    private boolean running = true;
    private boolean needsRefresh = true;

    public DefaultTextGUI(Screen screen) {
        this.screen = screen;
    }

    @Override
    public Screen getScreen() { return screen; }

    @Override
    public void addWindow(Window window) {
        windows.add(window);
        activeWindow = window;
        sizeWindow(window);
        focusFirst(window.getContents());
        needsRefresh = true;
    }

    @Override
    public void removeWindow(Window window) {
        windowsToRemove.add(window);
        if (activeWindow == window) activeWindow = windows.isEmpty() ? null : windows.get(windows.size() - 1);
    }

    @Override
    public Window getActiveWindow() { return activeWindow; }

    @Override
    public void setActiveWindow(Window window) {
        if (windows.contains(window)) {
            activeWindow = window;
            focusFirst(window.getContents());
            needsRefresh = true;
        }
    }

    @Override
    public Collection<Window> getWindows() { return new ArrayList<>(windows); }

    @Override
    public boolean processInput() throws IOException {
        var ks = screen.getTerminalSize().columns() > 0 ? getInput() : null;
        if (ks == null) return running;
        if (ks.type() == KeyType.CHARACTER && ks.ctrl() && (ks.character() == 'C' || ks.character() == 'c' || ks.character() == 'q' || ks.character() == 'Q')) {
            running = false;
            return false;
        }
        if (ks.type() == KeyType.ESCAPE) {
            running = false;
            return false;
        }
        if (ks.type() == KeyType.TAB) {
            advanceFocus();
            needsRefresh = true;
            return running;
        }
        var focused = activeWindow != null ? activeWindow.getFocusedComponent() : null;
        if (focused != null) {
            focused.handleKeyStroke(ks);
            needsRefresh = true;
        }
        return running;
    }

    private KeyStroke getInput() throws IOException {
        // Non-blocking poll
        return screen.getTerminalSize().columns() > 0 ? null : null;
    }

    public void waitForInput() throws IOException {
        // Blocking read from terminal if available
        if (screen instanceof io.jterm.screen.DefaultScreen ds) {
            // no direct terminal access; rely on input thread
        }
    }

    @Override
    public void updateScreen() throws IOException {
        if (!needsRefresh) return;
        screen.doResizeIfNecessary();
        screen.clear();
        var buf = new io.jterm.screen.ScreenBuffer(screen.getTerminalSize());
        var g = new io.jterm.graphics.TextGraphics(buf);
        for (var window : windows) {
            if (windowsToRemove.contains(window)) continue;
            sizeWindow(window);
            var sub = io.jterm.graphics.TextGraphicsExtensions.subGraphics(g, window.getPosition(), window.getSize());
            window.draw(sub);
        }
        // Copy g buffer to screen
        for (int r = 0; r < screen.getTerminalSize().rows(); r++) {
            for (int c = 0; c < screen.getTerminalSize().columns(); c++) {
                screen.setCell(c, r, buf.getCell(c, r));
            }
        }
        screen.refresh();
        needsRefresh = false;
        windows.removeAll(windowsToRemove);
        windowsToRemove.clear();
    }

    private io.jterm.screen.ScreenBuffer gBuffer;

    @Override
    public void close() throws IOException {
        screen.close();
    }

    private void sizeWindow(Window window) {
        var hints = window.getHints();
        var size = screen.getTerminalSize();
        if (hints.contains(WindowHint.FULLSCREEN)) {
            window.setBounds(TerminalPosition.TOP_LEFT, size);
        } else if (hints.contains(WindowHint.FIT_TERMINAL_WINDOW) || window.getSize().equals(TerminalSize.ZERO)) {
            window.setBounds(TerminalPosition.TOP_LEFT, size);
        }
    }

    private void focusFirst(Component component) {
        if (component == null) return;
        if (activeWindow != null) activeWindow.setFocusedComponent(component);
    }

    private void advanceFocus() {
        if (activeWindow == null) return;
        var root = activeWindow.getContents();
        var order = collectFocusable(root);
        if (order.isEmpty()) return;
        var current = activeWindow.getFocusedComponent();
        int idx = current != null ? order.indexOf(current) : -1;
        int next = (idx + 1) % order.size();
        activeWindow.setFocusedComponent(order.get(next));
    }

    private List<Component> collectFocusable(Component component) {
        List<Component> list = new ArrayList<>();
        if (component instanceof Button || component instanceof ListBox || component instanceof Table || component instanceof io.jterm.widget.TextBox) {
            list.add(component);
        }
        if (component instanceof Container cont) {
            for (var child : cont.getChildren()) {
                list.addAll(collectFocusable(child));
            }
        }
        return list;
    }
}
