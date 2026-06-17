package io.jterm.window;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.screen.DefaultScreen;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WindowManagerTest {
    @Test
    void implementsWindowManager() {
        var gui = new DefaultTextGUI(new DefaultScreen(new MockTerminal(new TerminalSize(80, 24))));
        assertTrue(gui instanceof WindowManager);
    }

    @Test
    void windowsReturnedInZOrder() {
        var gui = new DefaultTextGUI(new DefaultScreen(new MockTerminal(new TerminalSize(80, 24))));
        var a = new WindowImpl("A");
        var b = new WindowImpl("B");
        gui.addWindow(a);
        gui.addWindow(b);
        var windows = gui.getWindows();
        assertEquals(List.of(a, b), windows);
    }

    @Test
    void setActiveWindowReordersFocus() {
        var gui = new DefaultTextGUI(new DefaultScreen(new MockTerminal(new TerminalSize(80, 24))));
        var a = new WindowImpl("A");
        var b = new WindowImpl("B");
        gui.addWindow(a);
        gui.addWindow(b);
        assertEquals(b, gui.getActiveWindow());
        gui.setActiveWindow(a);
        assertEquals(a, gui.getActiveWindow());
    }

    @Test
    void containsWindow() throws IOException {
        var gui = new DefaultTextGUI(new DefaultScreen(new MockTerminal(new TerminalSize(80, 24))));
        var window = new WindowImpl("Test");
        gui.addWindow(window);
        assertTrue(gui.containsWindow(window));
        gui.removeWindow(window);
        gui.updateScreen();
        assertFalse(gui.containsWindow(window));
    }
}
