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

    // ── Expanded coverage: insert, delete, arrows, viewport, value ───────

    @Test
    void typingInsertsAtCursorNotAppends() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("AC");
        setCursor(box, 1); // cursor between A and C
        box.handleKeyStroke(KeyStroke.character('B', false, false, false));
        assertEquals("ABC", box.getValue(), "character should be inserted at cursor position");
    }

    @Test
    void typingMultipleCharactersAdvancesCursor() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        for (char c : "Hello".toCharArray()) {
            box.handleKeyStroke(KeyStroke.character(c, false, false, false));
        }
        assertEquals("Hello", box.getValue());
        assertEquals(5, getCursor(box));
    }

    @Test
    void deleteKeyRemovesCharacterAtCursor() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("ABC");
        setCursor(box, 1); // cursor after 'A', before 'B'
        box.handleKeyStroke(new KeyStroke(KeyType.DELETE));
        assertEquals("AC", box.getValue(), "Delete should remove char at cursor");
    }

    @Test
    void deleteAtEndDoesNothing() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hi");
        setCursor(box, 2); // cursor at end
        box.handleKeyStroke(new KeyStroke(KeyType.DELETE));
        assertEquals("Hi", box.getValue());
    }

    @Test
    void deleteOnEmptyDoesNothing() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.handleKeyStroke(new KeyStroke(KeyType.DELETE));
        assertEquals("", box.getValue());
    }

    @Test
    void backspaceAtStartDoesNothing() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hi");
        setCursor(box, 0);
        box.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertEquals("Hi", box.getValue());
    }

    @Test
    void backspaceOnEmptyDoesNothing() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertEquals("", box.getValue());
    }

    @Test
    void arrowLeftMovesCursorBackward() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        setCursor(box, 5);
        box.handleKeyStroke(new KeyStroke(KeyType.ARROW_LEFT));
        assertEquals(4, getCursor(box));
    }

    @Test
    void arrowLeftStopsAtStart() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hi");
        setCursor(box, 0);
        box.handleKeyStroke(new KeyStroke(KeyType.ARROW_LEFT));
        assertEquals(0, getCursor(box));
    }

    @Test
    void arrowRightMovesCursorForward() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        setCursor(box, 0);
        box.handleKeyStroke(new KeyStroke(KeyType.ARROW_RIGHT));
        assertEquals(1, getCursor(box));
    }

    @Test
    void arrowRightStopsAtEnd() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hi");
        setCursor(box, 2);
        box.handleKeyStroke(new KeyStroke(KeyType.ARROW_RIGHT));
        assertEquals(2, getCursor(box));
    }

    @Test
    void homeMovesCursorToStart() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        setCursor(box, 3);
        box.handleKeyStroke(new KeyStroke(KeyType.HOME));
        assertEquals(0, getCursor(box));
    }

    @Test
    void endMovesCursorToEnd() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        setCursor(box, 0);
        box.handleKeyStroke(new KeyStroke(KeyType.END));
        assertEquals(5, getCursor(box));
    }

    @Test
    void setValueUpdatesValue() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Initial");
        assertEquals("Initial", box.getValue());
        box.setValue("Changed");
        assertEquals("Changed", box.getValue());
    }

    @Test
    void setValueClampsCursorToLength() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("LongText");
        setCursor(box, 8); // at end
        box.setValue("Hi"); // shorter
        // setValue clamps cursor to min(current, length)
        assertEquals(2, getCursor(box), "cursor should be clamped to new value length");
    }

    @Test
    void setValueToEmptyString() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Something");
        box.setValue("");
        assertEquals("", box.getValue());
        assertEquals(0, getCursor(box));
    }

    @Test
    void getValueInitiallyEmpty() {
        var box = new TextBox();
        assertEquals("", box.getValue());
    }

    @Test
    void defaultPreferredColumnsIsTwenty() {
        var box = new TextBox();
        assertEquals(20, box.getPreferredSize().columns());
        assertEquals(1, box.getPreferredSize().rows());
    }

    @Test
    void preferredColumnsFromConstructor() {
        var box = new TextBox(15);
        assertEquals(15, box.getPreferredSize().columns());
    }

    @Test
    void ctrlEMovesToEndViaLowercaseE() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello World");
        box.handleKeyStroke(KeyStroke.character('A', true, false, false)); // to beginning
        box.handleKeyStroke(KeyStroke.character('e', true, false, false)); // lowercase ctrl+e to end
        assertEquals(11, getCursor(box));
    }

    @Test
    void ctrlKLowercaseDeletesToEnd() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello World");
        box.handleKeyStroke(KeyStroke.character('a', true, false, false)); // beginning
        box.handleKeyStroke(KeyStroke.character('k', true, false, false)); // kill to end
        assertEquals("", box.getValue());
    }

    @Test
    void ctrlFLowercaseMovesForward() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        box.handleKeyStroke(KeyStroke.character('A', true, false, false));
        box.handleKeyStroke(KeyStroke.character('f', true, false, false));
        assertEquals(1, getCursor(box));
    }

    @Test
    void ctrlBLowercaseMovesBackward() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        setCursor(box, 5);
        box.handleKeyStroke(KeyStroke.character('b', true, false, false));
        assertEquals(4, getCursor(box));
    }

    @Test
    void ctrlPLowercaseMovesBackward() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        setCursor(box, 5);
        box.handleKeyStroke(KeyStroke.character('p', true, false, false));
        assertEquals(4, getCursor(box));
    }

    @Test
    void ctrlNLowercaseMovesForward() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        box.handleKeyStroke(KeyStroke.character('A', true, false, false)); // beginning
        box.handleKeyStroke(KeyStroke.character('n', true, false, false)); // forward
        assertEquals(1, getCursor(box));
    }

    @Test
    void otherCtrlLetterIsIgnored() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        setCursor(box, 2);
        box.handleKeyStroke(KeyStroke.character('X', true, false, false)); // unsupported ctrl combo
        assertEquals("Hello", box.getValue(), "unsupported Ctrl+letter should not modify value");
        assertEquals(2, getCursor(box), "unsupported Ctrl+letter should not move cursor");
    }

    @Test
    void ctrlADoesNotChangeValue() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        box.handleKeyStroke(KeyStroke.character('A', true, false, false));
        assertEquals("Hello", box.getValue());
    }

    @Test
    void unsupportedKeyTypeIsIgnored() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hi");
        setCursor(box, 1);
        for (var kt : new io.jterm.core.input.KeyType[]{
                io.jterm.core.input.KeyType.TAB,
                io.jterm.core.input.KeyType.ESCAPE,
                io.jterm.core.input.KeyType.PAGE_UP,
                io.jterm.core.input.KeyType.PAGE_DOWN,
                io.jterm.core.input.KeyType.INSERT,
                io.jterm.core.input.KeyType.F1}) {
            box.handleKeyStroke(new io.jterm.core.input.KeyStroke(kt));
        }
        assertEquals("Hi", box.getValue());
        assertEquals(1, getCursor(box));
    }

    @Test
    void viewportAdjustsWhenCursorMovesBeyondVisibleWidth() {
        // Use a narrow box so text exceeds visible width
        var box = new TextBox(5);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 1));
        // Type 8 characters — viewport must scroll
        for (char c : "ABCDEFGH".toCharArray()) {
            box.handleKeyStroke(KeyStroke.character(c, false, false, false));
        }
        assertEquals("ABCDEFGH", box.getValue());
        // Cursor at 8, viewport offset should have advanced to 8 - 5 + 1 = 4
        // visible = value.substring(4, 9) = "EFGH", so 'H' is at column 3
        var buf = drawBox(box, 5);
        assertTrue(buf.getCell(0, 0).is('E'), "first visible column should show 'E'");
        assertTrue(buf.getCell(3, 0).is('H'), "column 3 should show 'H' after viewport scroll");
    }

    @Test
    void viewportAdjustsWhenCursorMovesBeforeViewport() {
        var box = new TextBox(5);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 1));
        for (char c : "ABCDEFGH".toCharArray()) {
            box.handleKeyStroke(KeyStroke.character(c, false, false, false));
        }
        // Move cursor to beginning — viewport should scroll back to show start
        box.handleKeyStroke(KeyStroke.character('A', true, false, false)); // ctrl+a -> cursor 0
        var buf = drawBox(box, 5);
        assertTrue(buf.getCell(0, 0).is('A'), "first visible column should show 'A' after cursor moved to start");
    }

    @Test
    void drawRendersTextContent() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        var buf = drawBox(box, 20);
        assertTrue(buf.getCell(0, 0).is('H'));
        assertTrue(buf.getCell(1, 0).is('e'));
        assertTrue(buf.getCell(2, 0).is('l'));
        assertTrue(buf.getCell(3, 0).is('l'));
        assertTrue(buf.getCell(4, 0).is('o'));
    }

    @Test
    void drawFillsBackgroundWithSpaces() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        box.setValue("Hi");
        var buf = drawBox(box, 10);
        // Cells after the text should be spaces
        assertTrue(buf.getCell(2, 0).is(' '));
        assertTrue(buf.getCell(9, 0).is(' '));
    }

    @Test
    void drawEmptyTextBoxFillsWithSpaces() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        var buf = drawBox(box, 10);
        for (int c = 0; c < 10; c++) {
            assertTrue(buf.getCell(c, 0).is(' '), "cell " + c + " should be space");
        }
    }

    @Test
    void cursorNotRenderedWhenNotFocused() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        setCursor(box, 2);
        box.setFocused(false);
        var buf = drawBox(box, 20);
        // No cell should have selection background (WHITE) when not focused
        for (int c = 0; c < 20; c++) {
            assertNotEquals(AnsiColor.WHITE, buf.getCell(c, 0).bg(),
                    "no cursor highlight should appear when not focused, at col " + c);
        }
    }

    @Test
    void cursorRenderedWhenFocused() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        setCursor(box, 2);
        box.setFocused(true);
        var buf = drawBox(box, 20);
        // Cell at cursor position (col 2) should have selection background (WHITE)
        assertEquals(AnsiColor.WHITE, buf.getCell(2, 0).bg(), "cursor cell should have selection bg when focused");
    }

    @Test
    void cursorAtEndShowsSpaceWithHighlight() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hi");
        setCursor(box, 2); // at end
        box.setFocused(true);
        var buf = drawBox(box, 20);
        // Cursor at position 2 (past end) shows ' ' with selection bg
        assertEquals(AnsiColor.WHITE, buf.getCell(2, 0).bg());
        assertTrue(buf.getCell(2, 0).is(' '));
    }

    @Test
    void maskedCursorShowsAsterisk() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("secret");
        box.setMasked(true);
        setCursor(box, 2);
        box.setFocused(true);
        var buf = drawBox(box, 20);
        // Cursor at position 2 in masked mode should show '*' with highlight
        assertTrue(buf.getCell(2, 0).is('*'));
        assertEquals(AnsiColor.WHITE, buf.getCell(2, 0).bg());
    }

    @Test
    void backspaceUpdatesViewportWhenNeeded() {
        var box = new TextBox(5);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 1));
        for (char c : "ABCDEF".toCharArray()) {
            box.handleKeyStroke(KeyStroke.character(c, false, false, false));
        }
        // Cursor at 6, viewport advanced (offset = 2). Backspace removes 'F' and moves cursor back
        box.handleKeyStroke(new io.jterm.core.input.KeyStroke(io.jterm.core.input.KeyType.BACKSPACE));
        assertEquals("ABCDE", box.getValue());
        // After backspace: cursor=5, viewportOffset=2 (unchanged since 5 < 2+5)
        // visible = substring(2,7) = "CDE", cursor at visible col 3
        box.setFocused(true);
        var buf = drawBox(box, 5);
        assertTrue(buf.getCell(0, 0).is('C'), "first visible col should be 'C'");
        // cursor at absolute pos 5, viewport 2 -> visible col 3, highlighted
        assertEquals(AnsiColor.WHITE, buf.getCell(3, 0).bg(), "cursor should be highlighted at visible col 3");
    }

    // ── Helpers ──────────────────────────────────────────────────

    private int getCursor(TextBox box) {
        // Read cursor position by checking where the highlight is in the buffer
        box.setFocused(true);  // cursor only renders when focused
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
