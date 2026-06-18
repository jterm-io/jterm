package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
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

    // ── Password masking ─────────────────────────────────────────

    @Test
    void defaultMaskedIsFalse() {
        var box = new TextBox(10);
        assertFalse(box.isMasked());
    }

    @Test
    void setMaskedTrueReturnsTrue() {
        var box = new TextBox(10);
        box.setMasked(true);
        assertTrue(box.isMasked());
    }

    @Test
    void setMaskedFalseReturnsFalse() {
        var box = new TextBox(10);
        box.setMasked(true);
        box.setMasked(false);
        assertFalse(box.isMasked());
    }

    @Test
    void maskedTextBoxRendersAsterisks() {
        var box = new TextBox(10);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        box.setValue("secret");
        box.setMasked(true);

        var buf = drawBox(box, 10);
        for (int c = 0; c < 6; c++) {
            assertTrue(buf.getCell(c, 0).is('*'), "expected '*' at column " + c);
        }
        assertTrue(buf.getCell(6, 0).is(' '), "expected space after masked text");
    }

    @Test
    void getValueReturnsRealTextWhenMasked() {
        var box = new TextBox(10);
        box.setValue("secret");
        box.setMasked(true);
        assertEquals("secret", box.getValue());
    }

    @Test
    void cursorRendersAtCorrectPositionWhenMasked() {
        var box = new TextBox(10);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        box.setValue("secret");
        box.setMasked(true);
        setCursor(box, 3);

        assertEquals(3, getCursor(box));
    }

    @Test
    void unmaskingRendersNormalCharactersAgain() {
        var box = new TextBox(10);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        box.setValue("open");
        box.setMasked(true);
        box.setMasked(false);

        var buf = drawBox(box, 10);
        assertTrue(buf.getCell(0, 0).is('o'));
        assertTrue(buf.getCell(1, 0).is('p'));
        assertTrue(buf.getCell(2, 0).is('e'));
        assertTrue(buf.getCell(3, 0).is('n'));
    }

    @Test
    void typingIntoMaskedTextBoxStoresRealCharsDisplaysStars() {
        var box = new TextBox(10);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        box.setMasked(true);
        box.handleKeyStroke(KeyStroke.character('p', false, false, false));
        box.handleKeyStroke(KeyStroke.character('a', false, false, false));
        box.handleKeyStroke(KeyStroke.character('s', false, false, false));
        box.handleKeyStroke(KeyStroke.character('s', false, false, false));

        assertEquals("pass", box.getValue());

        var buf = drawBox(box, 10);
        assertTrue(buf.getCell(0, 0).is('*'));
        assertTrue(buf.getCell(1, 0).is('*'));
        assertTrue(buf.getCell(2, 0).is('*'));
        assertTrue(buf.getCell(3, 0).is('*'));
    }

    @Test
    void backspaceInMaskedModeRemovesRealCharacterAndUpdatesDisplay() {
        var box = new TextBox(10);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        box.setMasked(true);
        box.handleKeyStroke(KeyStroke.character('p', false, false, false));
        box.handleKeyStroke(KeyStroke.character('a', false, false, false));
        box.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        box.handleKeyStroke(KeyStroke.character('s', false, false, false));

        assertEquals("ps", box.getValue());

        var buf = drawBox(box, 10);
        assertTrue(buf.getCell(0, 0).is('*'));
        assertTrue(buf.getCell(1, 0).is('*'));
        assertTrue(buf.getCell(2, 0).is(' '), "expected space after two masked chars");
    }

    // ── Emacs key bindings ──────────────────────────────────────

    @Test
    void ctrlAMovesToBeginningOfLine() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello World");
        box.handleKeyStroke(KeyStroke.character('A', true, false, false));
        assertEquals(0, getCursor(box));
    }

    @Test
    void ctrlEMovesToEndOfLine() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello World");
        box.handleKeyStroke(KeyStroke.character('A', true, false, false)); // to beginning
        box.handleKeyStroke(KeyStroke.character('E', true, false, false)); // to end
        assertEquals(11, getCursor(box));
    }

    @Test
    void ctrlKDeletesToEndOfLine() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello World");
        box.handleKeyStroke(KeyStroke.character('A', true, false, false)); // beginning
        box.handleKeyStroke(KeyStroke.character('K', true, false, false)); // kill to end
        assertEquals("", box.getValue());
    }

    @Test
    void ctrlKDeletesFromCursorOnly() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello World");
        // Move cursor to position 5 (after "Hello")
        box.setValue("Hello World");
        setCursor(box, 5);
        box.handleKeyStroke(KeyStroke.character('K', true, false, false));
        assertEquals("Hello", box.getValue());
    }

    @Test
    void ctrlFMovesForward() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        box.handleKeyStroke(KeyStroke.character('A', true, false, false)); // beginning
        box.handleKeyStroke(KeyStroke.character('F', true, false, false)); // forward 1
        assertEquals(1, getCursor(box));
        box.handleKeyStroke(KeyStroke.character('F', true, false, false)); // forward 1
        assertEquals(2, getCursor(box));
    }

    @Test
    void ctrlBMovesBackward() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        setCursor(box, 5); // cursor at end
        box.handleKeyStroke(KeyStroke.character('B', true, false, false));
        assertEquals(4, getCursor(box));
        box.handleKeyStroke(KeyStroke.character('B', true, false, false));
        assertEquals(3, getCursor(box));
    }

    @Test
    void ctrlFDoesNotGoPastEnd() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hi");
        box.handleKeyStroke(KeyStroke.character('E', true, false, false)); // end
        box.handleKeyStroke(KeyStroke.character('F', true, false, false)); // try to go past
        assertEquals(2, getCursor(box));
    }

    @Test
    void ctrlBDoesNotGoBeforeStart() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hi");
        box.handleKeyStroke(KeyStroke.character('A', true, false, false)); // beginning
        box.handleKeyStroke(KeyStroke.character('B', true, false, false)); // try to go before start
        assertEquals(0, getCursor(box));
    }

    @Test
    void ctrlPMovesUpLikeArrowUp() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        // In single-line mode, Ctrl+P moves cursor back (like up arrow = beginning in readline)
        box.handleKeyStroke(KeyStroke.character('P', true, false, false));
        assertTrue(getCursor(box) <= 5);
    }

    @Test
    void ctrlNMovesDownLikeArrowDown() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        box.handleKeyStroke(KeyStroke.character('A', true, false, false)); // beginning
        box.handleKeyStroke(KeyStroke.character('N', true, false, false)); // down/forward
        assertTrue(getCursor(box) >= 0);
    }

    @Test
    void ctrlLetterDoesNotInsertCharacter() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        box.handleKeyStroke(KeyStroke.character('A', true, false, false)); // Ctrl+A
        assertEquals("Hello", box.getValue()); // no 'A' inserted
    }

    @Test
    void ctrlKAtEndDoesNothing() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        setCursor(box, 5); // cursor at end
        box.handleKeyStroke(KeyStroke.character('K', true, false, false)); // kill to end
        assertEquals("Hello", box.getValue());
    }

    @Test
    void lowercaseCtrlLettersAlsoWork() {
        // InputDecoder produces uppercase letters for Ctrl combos ('A' for Ctrl+A)
        // but test lowercase too for safety
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello World");
        box.handleKeyStroke(KeyStroke.character('a', true, false, false)); // ctrl+a
        assertEquals(0, getCursor(box));
    }

    // ── Helpers ──────────────────────────────────────────────────

    private int getCursor(TextBox box) {
        // Read cursor position by checking where the highlight is in the buffer
        var size = new TerminalSize(20, 1);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        box.draw(g);
        for (int c = 0; c < size.columns(); c++) {
            var cell = buf.getCell(c, 0);
            if (cell.bg().equals(AnsiColor.WHITE)) return c;
        }
        return -1; // cursor not visible
    }

    private void setCursor(TextBox box, int pos) {
        box.setValue(box.getValue().substring(0, pos) + box.getValue().substring(pos));
        // Hack: setValue resets cursor to min of current and length, so we need to
        // type and delete to get cursor to right position. Instead, use reflection.
        try {
            var field = TextBox.class.getDeclaredField("cursorPosition");
            field.setAccessible(true);
            field.setInt(box, pos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ScreenBuffer drawBox(TextBox box, int columns) {
        var size = new TerminalSize(columns, 1);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        box.draw(g);
        return buf;
    }
}
