package io.jterm.window;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.screen.DefaultScreen;
import io.jterm.widget.Button;
import io.jterm.widget.Label;
import io.jterm.widget.Panel;
import io.jterm.layout.BorderLayout;
import io.jterm.layout.LinearLayout;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTextGUITest {
    @Test
    void addWindowMakesActive() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var window = new WindowImpl("Test");
        gui.addWindow(window);
        assertEquals(window, gui.getActiveWindow());
    }

    @Test
    void removeWindowClearsActive() throws Exception {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var window = new WindowImpl("Test");
        gui.addWindow(window);
        gui.removeWindow(window);
        gui.updateScreen();
        assertTrue(gui.getWindows().isEmpty());
    }

    @Test
    void layoutSizesWindow() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var window = new WindowImpl("Test");
        window.getContents().setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        window.getContents().addComponent(new Label("Hello"));
        gui.addWindow(window);
        assertTrue(window.getSize().columns() > 0);
        assertTrue(window.getSize().rows() > 0);
    }
}
