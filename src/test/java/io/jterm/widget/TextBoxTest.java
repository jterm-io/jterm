package io.jterm.widget;

import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextBoxTest {
    @Test
    void typingAppendsCharacters() {
        var box = new TextBox(10);
        box.setBounds(new io.jterm.core.TerminalPosition(0, 0), new io.jterm.core.TerminalSize(10, 1));
        box.handleKeyStroke(KeyStroke.character('H', false, false, false));
        box.handleKeyStroke(KeyStroke.character('i', false, false, false));
        assertEquals("Hi", box.getValue());
    }

    @Test
    void backspaceRemovesCharacter() {
        var box = new TextBox(10);
        box.setBounds(new io.jterm.core.TerminalPosition(0, 0), new io.jterm.core.TerminalSize(10, 1));
        box.handleKeyStroke(KeyStroke.character('A', false, false, false));
        box.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertEquals("", box.getValue());
    }
}
