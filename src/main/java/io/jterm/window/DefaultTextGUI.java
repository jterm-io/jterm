package io.jterm.window;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.event.FocusManager;
import io.jterm.event.Listener;
import io.jterm.graphics.TextGraphics;
import io.jterm.graphics.TextGraphicsExtensions;
import io.jterm.screen.Screen;
import io.jterm.widget.Button;
import io.jterm.widget.CheckBox;
import io.jterm.widget.ListBox;
import io.jterm.widget.Table;
import io.jterm.widget.TextBox;
import io.jterm.widget.Container;
import io.jterm.widget.Component;
import io.jterm.style.ThemeManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Default window manager, input dispatch, and event loop. */
public class DefaultTextGUI implements TextGUI, WindowManager {
    private final Screen screen;
    private final List<Window> windows = new CopyOnWriteArrayList<>();
    private final List<Window> windowsToRemove = new CopyOnWriteArrayList<>();
    private volatile Window activeWindow;
    private final FocusManager focusManager = new FocusManager();
    private volatile boolean running = true;
    private volatile boolean needsRefresh = true;
    private final Object screenLock = new Object();

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
        if (activeWindow == window) {
            var remaining = new ArrayList<>(windows);
            remaining.removeAll(windowsToRemove);
            activeWindow = remaining.isEmpty() ? null : remaining.get(remaining.size() - 1);
            focusManager.clearFocus();
            if (activeWindow != null) focusFirst(activeWindow.getContents());
        }
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
    public boolean containsWindow(Window window) {
        return windows.contains(window);
    }

    public FocusManager getFocusManager() {
        return focusManager;
    }

    public boolean isRunning() {
        return running;
    }

    /** Marks the screen as needing a refresh on the next {@link #updateScreen()} call. */
    public void requestRefresh() {
        needsRefresh = true;
    }

    public void stopRunning() {
        running = false;
    }

    @Override
    public boolean processInput() throws IOException {
        return processInput(null);
    }

    public boolean processInput(KeyStroke injected) throws IOException {
        synchronized (screenLock) {
            var ks = injected != null ? injected : getInput();
            if (ks == null) return false; // no input available
            if (ks.type() == KeyType.CHARACTER) {
                char ch = ks.character();
                if (ks.ctrl() && (ch == 'C' || ch == 'c')) {
                    running = false;
                    return false;
                }
            }
            if (ks.type() == KeyType.ESCAPE) {
                // Don't quit on Escape in BBS mode — let screens handle it
            }
            if (ks.type() == KeyType.TAB) {
                advanceFocus();
                needsRefresh = true;
                return true;
            }
            var modal = modalWindow();
            if (modal != null && activeWindow != null && !modal.equals(activeWindow)) {
                return true;
            }
            var focused = activeWindow != null ? activeWindow.getFocusedComponent() : null;
            if (focused != null) {
                focused.handleKeyStroke(ks);
            }
            // Always let the window handle keys too (e.g. MainMenu hotkeys)
            if (activeWindow != null) {
                activeWindow.handleKeyStroke(ks);
            }
            needsRefresh = true;
            return true;
        }
    }

    private KeyStroke getInput() throws IOException {
        return screen instanceof io.jterm.screen.DefaultScreen ds
                ? ds.getTerminal().pollInput().orElse(null)
                : null;
    }

    @Override
    public void waitForInput() throws IOException {
        if (screen instanceof io.jterm.screen.DefaultScreen ds) {
            ds.getTerminal().readInput();
        }
    }

    @Override
    public void updateScreen() throws IOException {
        synchronized (screenLock) {
            if (!needsRefresh) return;
            screen.doResizeIfNecessary();
            screen.clear();
            // Initialize the rendering buffer with theme colors so areas not
            // covered by any window get the theme background instead of black.
            var theme = ThemeManager.active();
            var fillCell = new io.jterm.style.TextCell(' ', theme.foreground(), theme.background());
            var buf = new io.jterm.screen.ScreenBuffer(screen.getTerminalSize(), fillCell);
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
    }

    private io.jterm.screen.ScreenBuffer gBuffer;

    public void runEventLoop() throws IOException {
        needsRefresh = true;
        while (running) {
            boolean hadInput = processInput();
            updateScreen();
            if (!hadInput) {
                try { Thread.sleep(16); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        screen.close();
    }

    private Window modalWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            var w = windows.get(i);
            if (!windowsToRemove.contains(w) && w.getHints().contains(WindowHint.MODAL)) return w;
        }
        return null;
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
        var first = findFirstFocusable(component);
        var target = first != null ? first : component;
        if (activeWindow != null) activeWindow.setFocusedComponent(target);
        focusManager.setFocusedComponent(target);
    }

    private Component findFirstFocusable(Component component) {
        if (isFocusable(component)) return component;
        if (component instanceof Container cont) {
            for (var child : cont.getChildren()) {
                if (!child.isVisible()) continue;
                var found = findFirstFocusable(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void advanceFocus() {
        if (activeWindow == null) return;
        var root = activeWindow.getContents();
        var order = collectFocusable(root);
        if (order.isEmpty()) return;
        var current = activeWindow.getFocusedComponent();
        if (current == null) current = focusManager.getFocusedComponent();
        int idx = current != null ? order.indexOf(current) : -1;
        int next = (idx + 1) % order.size();
        var nextComponent = order.get(next);
        activeWindow.setFocusedComponent(nextComponent);
        focusManager.setFocusedComponent(nextComponent);
    }

    private List<Component> collectFocusable(Component component) {
        List<Component> list = new ArrayList<>();
        if (component.isVisible() && isFocusable(component)) list.add(component);
        if (component instanceof Container cont) {
            for (var child : cont.getChildren()) {
                list.addAll(collectFocusable(child));
            }
        }
        return list;
    }

    private boolean isFocusable(Component component) {
        return component instanceof Button
                || component instanceof CheckBox
                || component instanceof ListBox
                || component instanceof Table
                || component instanceof TextBox;
    }
}
