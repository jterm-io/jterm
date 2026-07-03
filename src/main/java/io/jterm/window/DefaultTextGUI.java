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
import io.jterm.widget.TextArea;
import io.jterm.widget.Container;
import io.jterm.widget.Component;
import io.jterm.style.ThemeManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
    private volatile boolean forceComplete = true;  // start with a complete refresh
    private final Object screenLock = new Object();

    public DefaultTextGUI(Screen screen) {
        this.screen = screen;
    }

    @Override
    public Screen getScreen() { return screen; }

    @Override
    public void addWindow(Window window) {
        windows.add(window);
        if (!window.getHints().contains(WindowHint.BACKGROUND)) {
            activeWindow = window;
            focusFirst(window.getContents());
        }
        sizeWindow(window);
        needsRefresh = true;
        forceComplete = true;
    }

    @Override
    public void removeWindow(Window window) {
        windowsToRemove.add(window);
        if (activeWindow == window) {
            var remaining = new ArrayList<>(windows);
            remaining.removeAll(windowsToRemove);
            activeWindow = remaining.isEmpty() ? null : findTopmostNonBackground(remaining);
            focusManager.clearFocus();
            if (activeWindow != null) focusFirst(activeWindow.getContents());
        }
        needsRefresh = true;
        forceComplete = true;
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
    public Collection<Window> getWindows() {
        if (windowsToRemove.isEmpty()) return new ArrayList<>(windows);
        var visible = new ArrayList<>(windows);
        visible.removeAll(windowsToRemove);
        return visible;
    }

    @Override
    public boolean containsWindow(Window window) {
        return windows.contains(window) && !windowsToRemove.contains(window);
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
            if (activeWindow != null && activeWindow.getHints().contains(WindowHint.BACKGROUND)) {
                activeWindow = findTopmostNonBackground(new ArrayList<>(windows));
            }
            var focused = activeWindow != null ? activeWindow.getFocusedComponent() : null;
            var windowBefore = activeWindow;
            if (focused != null) {
                focused.handleKeyStroke(ks);
            }
            // Only forward to the window if the focused component didn't consume it
            // by changing the active window (e.g. popping a sub-screen on ESC).
            if (activeWindow != null && activeWindow == windowBefore) {
                activeWindow.handleKeyStroke(ks);
            }
            needsRefresh = true;
            return true;
        }
    }

    private KeyStroke getInput() throws IOException {
        // Use pollInput with a 5ms timeout for near-instant input delivery.
        // This replaces the old pattern of pollInput() + Thread.sleep(16),
        // which added up to 16ms latency to every keystroke.
        // With a BlockingQueue-backed terminal, this call blocks efficiently
        // and returns immediately when input arrives.
        // 5ms timeout = 200 wake-ups/sec idle, still <1ms average input latency.
        return screen instanceof io.jterm.screen.DefaultScreen ds
                ? ds.getTerminal().pollInput(5).orElse(null)
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
            for (var window : sortedWindows()) {
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
            if (forceComplete) {
                screen.refresh(io.jterm.screen.RefreshType.COMPLETE);
                forceComplete = false;
            } else {
                screen.refresh();
            }
            needsRefresh = false;
            // Call close() on windows being removed so they can clean up listeners/resources
            for (var w : windowsToRemove) {
                w.close();
            }
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
            // No more Thread.sleep(16) — getInput() now uses pollInput(1ms)
            // which blocks efficiently and returns as soon as input arrives.
            // The 1ms timeout ensures needsRefresh is checked promptly even
            // when no input is available.
            if (!hadInput) {
                // Yield to other threads (e.g. background CompletableFuture workers)
                Thread.yield();
            }
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        screen.close();
    }

    private Window findTopmostNonBackground(List<Window> list) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (!list.get(i).getHints().contains(WindowHint.BACKGROUND)) return list.get(i);
        }
        return null;
    }

    private Window modalWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            var w = windows.get(i);
            if (!windowsToRemove.contains(w) && w.getHints().contains(WindowHint.MODAL)) return w;
        }
        return null;
    }

    private List<Window> sortedWindows() {
        return windows.stream()
                .sorted(Comparator.comparingInt((Window w) -> w.getHints().contains(WindowHint.BACKGROUND) ? 0 : 1)
                        .thenComparingInt(windows::indexOf))
                .toList();
    }

    private void sizeWindow(Window window) {
        var hints = window.getHints();
        var size = screen.getTerminalSize();
        if (hints.contains(WindowHint.FULLSCREEN)) {
            window.setBounds(TerminalPosition.TOP_LEFT, size);
        } else if (hints.contains(WindowHint.FIT_TERMINAL_WINDOW) || window.getSize().equals(TerminalSize.ZERO)) {
            window.setBounds(TerminalPosition.TOP_LEFT, size);
        } else if (hints.contains(WindowHint.CENTERED)) {
            var preferred = window.getPreferredSize();
            int width = Math.min(preferred.columns(), size.columns());
            int height = Math.min(preferred.rows(), size.rows());
            int col = Math.max(0, (size.columns() - width) / 2);
            int row = Math.max(0, (size.rows() - height) / 2);
            window.setBounds(new TerminalPosition(col, row), new TerminalSize(width, height));
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
        if (!component.isFocusable()) return false;
        return component instanceof Button
                || component instanceof CheckBox
                || component instanceof ListBox
                || component instanceof Table
                || component instanceof TextBox
                || component instanceof TextArea;
    }
}
