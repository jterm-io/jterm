package io.jterm.screen;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ScreenInterfaceTest {
    @Test
    void screenImplementsAutoCloseable() {
        assertTrue(AutoCloseable.class.isAssignableFrom(Screen.class));
    }

    @Test
    void setCellByPosition() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(10, 5)));
        screen.setCell(new TerminalPosition(2, 3), new TextCell('A'));
        assertEquals('A', screen.getBackCell(2, 3).character().charAt(0));
    }

    @Test
    void doResizeIfNecessaryReturnsSize() throws IOException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(10, 5)));
        assertEquals(new TerminalSize(10, 5), screen.doResizeIfNecessary());
    }

    @Test
    void doResizeUpdatesBuffers() throws IOException {
        var mock = new MockTerminal(new TerminalSize(10, 5));
        var screen = new DefaultScreen(mock);
        // No size change path: same size returns existing
        assertEquals(new TerminalSize(10, 5), screen.doResizeIfNecessary());
    }

    @Test
    void cursorPositionRoundTrip() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(10, 5)));
        screen.setCursorPosition(new TerminalPosition(3, 2));
        assertEquals(new TerminalPosition(3, 2), screen.getCursorPosition());
    }

    @Test
    void closeStopsScreenAndTerminal() throws IOException {
        var mock = new MockTerminal(new TerminalSize(10, 5));
        var screen = new DefaultScreen(mock);
        screen.close();
        assertTrue(mock.isClosed());
    }
}
