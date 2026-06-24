package io.jterm.window;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.screen.DefaultScreen;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTextGUIExtraTest {
    @Test
    void addAndRemoveWindowUpdatesActiveWindow() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var window = new AbstractWindow("W") {
            @Override public void close() {}
        };
        gui.addWindow(window);
        assertSame(window, gui.getActiveWindow());
        gui.removeWindow(window);
        assertNull(gui.getActiveWindow());
    }

    @Test
    void containsWindowReportsMembership() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var window = new AbstractWindow("W") { @Override public void close() {} };
        assertFalse(gui.containsWindow(window));
        gui.addWindow(window);
        assertTrue(gui.containsWindow(window));
    }

    @Test
    void setActiveWindowOnlyWorksForMember() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var w1 = new AbstractWindow("W1") { @Override public void close() {} };
        var w2 = new AbstractWindow("W2") { @Override public void close() {} };
        gui.addWindow(w1);
        gui.setActiveWindow(w2);
        assertSame(w1, gui.getActiveWindow());
    }

    @Test
    void stopRunningIsReflected() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        assertTrue(gui.isRunning());
        gui.stopRunning();
        assertFalse(gui.isRunning());
    }

    @Test
    void requestRefreshDoesNotThrow() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        gui.requestRefresh();
        assertDoesNotThrow(() -> gui.updateScreen());
    }

    @Test
    void backgroundWindowDoesNotBecomeActive() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var window = new AbstractWindow("Bg") {
            @Override public java.util.List<WindowHint> getHints() { return List.of(WindowHint.BACKGROUND); }
            @Override public void close() {}
        };
        gui.addWindow(window);
        assertNull(gui.getActiveWindow());
    }
}
