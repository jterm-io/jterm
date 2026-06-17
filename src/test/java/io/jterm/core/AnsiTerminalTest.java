package io.jterm.core;

import io.jterm.style.AnsiColor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AnsiTerminalTest {
    @Test
    void cursorPositionEmitsEscape() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new AnsiTerminal(out, new ByteArrayInputStream(new byte[0]), new TerminalSize(80, 24));
        term.setCursorPosition(5, 10);
        term.flush();
        assertEquals("\033[11;6H", out.toString());
    }

    @Test
    void clearScreenEmitsEscape() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new AnsiTerminal(out, new ByteArrayInputStream(new byte[0]), new TerminalSize(80, 24));
        term.clearScreen();
        term.flush();
        assertTrue(out.toString().contains("\033[2J"));
    }

    @Test
    void foregroundColorEmitsEscape() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new AnsiTerminal(out, new ByteArrayInputStream(new byte[0]), new TerminalSize(80, 24));
        term.setForegroundColor(AnsiColor.RED);
        term.flush();
        assertTrue(out.toString().contains("\033[31m"));
    }
}
