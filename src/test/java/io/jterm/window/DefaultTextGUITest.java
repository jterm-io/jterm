package io.jterm.window;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.screen.DefaultScreen;
import io.jterm.widget.Button;
import io.jterm.widget.Label;
import io.jterm.widget.Panel;
import io.jterm.layout.BorderLayout;
import io.jterm.layout.LinearLayout;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTextGUITest {
    @Test
    void addWindowMakesActive() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var window = new WindowImpl("Test");
        gui.addWindow(window);
        assertEquals(window, gui.getActiveWindow());
        assertTrue(gui.containsWindow(window));
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
        assertNull(gui.getActiveWindow());
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

    @Test
    void focusManagerNotifiesListeners() throws IOException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var window = new WindowImpl("Test");
        var button = new Button("Click");
        window.getContents().setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        window.getContents().addComponent(button);
        gui.addWindow(window);

        AtomicReference<io.jterm.widget.Component> focused = new AtomicReference<>();
        gui.getFocusManager().clearFocus();
        gui.getFocusManager().addListener(focused::set);
        gui.getFocusManager().setFocusedComponent(button);

        assertEquals(button, focused.get());
        assertTrue(button.isFocused());
    }

    @Test
    void tabAdvancesFocus() throws IOException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var window = new WindowImpl("Test");
        var button1 = new Button("One");
        var button2 = new Button("Two");
        window.getContents().setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        window.getContents().addComponent(button1);
        window.getContents().addComponent(button2);
        gui.addWindow(window);

        assertTrue(button1.isFocused());
        gui.processInput(new KeyStroke(KeyType.TAB));
        assertTrue(button2.isFocused());
        gui.processInput(new KeyStroke(KeyType.TAB));
        assertTrue(button1.isFocused());
    }

    @Test
    void inputDispatchFiresButtonClick() throws IOException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var window = new WindowImpl("Test");
        var button = new Button("Click");
        var fired = new boolean[1];
        button.addListener(() -> fired[0] = true);
        window.getContents().setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        window.getContents().addComponent(button);
        gui.addWindow(window);

        gui.processInput(new KeyStroke(KeyType.ENTER));
        assertTrue(fired[0]);
    }

    @Test
    void modalBlocksInputToOtherWindows() throws IOException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);

        var backWindow = new WindowImpl("Back");
        var backButton = new Button("Back");
        var backFired = new boolean[1];
        backButton.addListener(() -> backFired[0] = true);
        backWindow.getContents().setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        backWindow.getContents().addComponent(backButton);
        gui.addWindow(backWindow);

        var modal = new WindowImpl("Modal");
        modal.setHints(List.of(WindowHint.MODAL));
        gui.addWindow(modal);

        gui.processInput(new KeyStroke(KeyType.ENTER));
        assertFalse(backFired[0], "modal should block input to back window");
    }

    @Test
    void removeModalRestoresFocus() throws IOException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);

        var backWindow = new WindowImpl("Back");
        var backButton = new Button("Back");
        backWindow.getContents().setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        backWindow.getContents().addComponent(backButton);
        gui.addWindow(backWindow);

        var modal = new WindowImpl("Modal");
        modal.setHints(List.of(WindowHint.MODAL));
        gui.addWindow(modal);

        gui.removeWindow(modal);
        gui.updateScreen();

        assertEquals(backWindow, gui.getActiveWindow());
        assertTrue(backButton.isFocused());
    }

    @Test
    void escapeStopsEventLoop() throws IOException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        gui.addWindow(new WindowImpl("Test"));
        boolean stillRunning = gui.processInput(new KeyStroke(KeyType.ESCAPE));
        assertFalse(stillRunning);
    }

    @Test
    void updateScreenDrawsWindow() throws IOException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var window = new WindowImpl("Test");
        window.getContents().setLayoutManager(new BorderLayout());
        window.getContents().addComponent(new Label("Hello"), new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));
        gui.addWindow(window);
        gui.updateScreen();
        assertNotNull(screen.getBackCell(1, 1));
    }

    @Test
    void processInputReadsFromTerminal() throws IOException {
        var input = new ByteArrayInputStream("\033".getBytes());
        var terminal = new MockTerminal(new TerminalSize(80, 24), new java.io.ByteArrayOutputStream(), input);
        var screen = new DefaultScreen(terminal);
        var gui = new DefaultTextGUI(screen);
        gui.addWindow(new WindowImpl("Test"));
        boolean stillRunning = gui.processInput();
        assertFalse(stillRunning, "escape should stop the GUI");
    }

    private static DefaultTextGUI createGui() {
        return new DefaultTextGUI(new DefaultScreen(new MockTerminal(new TerminalSize(80, 24))));
    }
}
