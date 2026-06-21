package io.jterm.widget;

import io.jterm.core.AnsiTerminal;
import io.jterm.core.MockTerminal;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextAreaTest {

    // ── Basic construction & state ─────────────────────────────

    @Test
    void emptyByDefault() {
        var ta = new TextArea();
        assertEquals("", ta.getText());
        assertEquals(1, ta.getLineCount()); // one empty line
    }

    @Test
    void initWithText() {
        var ta = new TextArea("Hello\nWorld");
        assertEquals("Hello\nWorld", ta.getText());
        assertEquals(2, ta.getLineCount());
    }

    @Test
    void setTextReplacesContent() {
        var ta = new TextArea("Old");
        ta.setText("Line 1\nLine 2\nLine 3");
        assertEquals(3, ta.getLineCount());
        assertEquals("Line 1\nLine 2\nLine 3", ta.getText());
    }

    @Test
    void preferredSizeDefaultsToColumns20Rows5() {
        var ta = new TextArea();
        var ps = ta.getPreferredSize();
        assertEquals(20, ps.columns());
        assertEquals(5, ps.rows());
    }

    @Test
    void preferredSizeFromConstructor() {
        var ta = new TextArea("", 40, 10);
        var ps = ta.getPreferredSize();
        assertEquals(40, ps.columns());
        assertEquals(10, ps.rows());
    }

    // ── Line management ────────────────────────────────────────

    @Test
    void getLineByIndex() {
        var ta = new TextArea("AAA\nBBB\nCCC");
        assertEquals("AAA", ta.getLine(0));
        assertEquals("BBB", ta.getLine(1));
        assertEquals("CCC", ta.getLine(2));
    }

    @Test
    void emptyTextHasOneLine() {
        var ta = new TextArea();
        assertEquals(1, ta.getLineCount());
    }

    @Test
    void singleLineNoNewline() {
        var ta = new TextArea("Hello");
        assertEquals(1, ta.getLineCount());
        assertEquals("Hello", ta.getLine(0));
    }

    @Test
    void trailingNewlineCreatesEmptyLine() {
        var ta = new TextArea("Hello\n");
        assertEquals(2, ta.getLineCount());
        assertEquals("Hello", ta.getLine(0));
        assertEquals("", ta.getLine(1));
    }

    // ── Typing characters ──────────────────────────────────────

    @Test
    void typeCharacter() {
        var ta = new TextArea();
        ta.handleKeyStroke(ks('H'));
        ta.handleKeyStroke(ks('i'));
        assertEquals("Hi", ta.getText());
    }

    @Test
    void typeMultipleCharacters() {
        var ta = new TextArea();
        type(ta, "Hello");
        assertEquals("Hello", ta.getText());
    }

    @Test
    void typeAtSpecificPosition() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 2); // after "He"
        ta.handleKeyStroke(ks('X'));
        assertEquals("HeXllo", ta.getText());
    }

    // ── Enter / newline ────────────────────────────────────────

    @Test
    void enterCreatesNewLine() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 2); // after "He"
        ta.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertEquals(2, ta.getLineCount());
        assertEquals("He", ta.getLine(0));
        assertEquals("llo", ta.getLine(1));
    }

    @Test
    void enterAtEndOfLine() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 5); // end
        ta.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertEquals(2, ta.getLineCount());
        assertEquals("Hello", ta.getLine(0));
        assertEquals("", ta.getLine(1));
    }

    @Test
    void enterOnEmptyArea() {
        var ta = new TextArea();
        ta.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertEquals(2, ta.getLineCount());
        assertEquals("", ta.getLine(0));
        assertEquals("", ta.getLine(1));
    }

    // ── Backspace ──────────────────────────────────────────────

    @Test
    void backspaceRemovesChar() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 5);
        ta.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertEquals("Hell", ta.getText());
    }

    @Test
    void backspaceAtLineStartJoinsLines() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 0); // start of line 2
        ta.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertEquals(1, ta.getLineCount());
        assertEquals("HelloWorld", ta.getLine(0));
    }

    @Test
    void backspaceAtVeryStartDoesNothing() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 0);
        ta.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertEquals("Hello", ta.getText());
    }

    // ── Arrow keys ─────────────────────────────────────────────

    @Test
    void arrowLeftMovesCursorLeft() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 3);
        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_LEFT));
        assertEquals(0, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void arrowRightMovesCursorRight() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 1);
        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_RIGHT));
        assertEquals(0, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void arrowRightAtEndOfLineMovesToNextLine() {
        var ta = new TextArea("Hi\nWorld");
        setCursor(ta, 0, 2); // end of line 1
        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_RIGHT));
        assertEquals(1, getCursorRow(ta));
        assertEquals(0, getCursorCol(ta));
    }

    @Test
    void arrowLeftAtStartOfLineMovesToPrevLineEnd() {
        var ta = new TextArea("Hi\nWorld");
        setCursor(ta, 1, 0); // start of line 2
        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_LEFT));
        assertEquals(0, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void arrowDownMovesToNextLine() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 0, 2);
        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        assertEquals(1, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void arrowDownAtLastLineDoesNothing() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 2);
        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        assertEquals(1, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void arrowUpMovesToPrevLine() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 2);
        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_UP));
        assertEquals(0, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void arrowUpAtFirstLineDoesNothing() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 0, 2);
        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_UP));
        assertEquals(0, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void arrowDownClampsColumnToShorterLine() {
        var ta = new TextArea("Hello\nHi");
        setCursor(ta, 0, 4); // col 4 in "Hello" (5 chars)
        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        assertEquals(1, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta)); // clamped to end of "Hi"
    }

    @Test
    void arrowUpClampsColumnToShorterLine() {
        var ta = new TextArea("Hi\nHello");
        setCursor(ta, 1, 4); // col 4 in "Hello"
        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_UP));
        assertEquals(0, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta)); // clamped to end of "Hi"
    }

    // ── Home / End ──────────────────────────────────────────────

    @Test
    void homeMovesToLineStart() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 3);
        ta.handleKeyStroke(new KeyStroke(KeyType.HOME));
        assertEquals(1, getCursorRow(ta));
        assertEquals(0, getCursorCol(ta));
    }

    @Test
    void endMovesToLineEnd() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 1);
        ta.handleKeyStroke(new KeyStroke(KeyType.END));
        assertEquals(1, getCursorRow(ta));
        assertEquals(5, getCursorCol(ta));
    }

    // ── Page Up / Page Down ────────────────────────────────────

    @Test
    void pageDownMovesDownByViewportHeight() {
        var ta = new TextArea("", 20, 5);
        ta.setText("L1\nL2\nL3\nL4\nL5\nL6\nL7\nL8\nL9\nL10");
        setCursor(ta, 0, 0);
        ta.handleKeyStroke(new KeyStroke(KeyType.PAGE_DOWN));
        assertEquals(5, getCursorRow(ta));
        assertEquals(0, getCursorCol(ta));
    }

    @Test
    void pageUpMovesUpByViewportHeight() {
        var ta = new TextArea("", 20, 5);
        ta.setText("L1\nL2\nL3\nL4\nL5\nL6\nL7\nL8\nL9\nL10");
        setCursor(ta, 8, 0);
        ta.handleKeyStroke(new KeyStroke(KeyType.PAGE_UP));
        assertEquals(3, getCursorRow(ta));
        assertEquals(0, getCursorCol(ta));
    }

    // ── Delete ──────────────────────────────────────────────────

    @Test
    void deleteRemovesCharAfterCursor() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 0);
        ta.handleKeyStroke(new KeyStroke(KeyType.DELETE));
        assertEquals("ello", ta.getText());
    }

    @Test
    void deleteAtEndOfLineJoinsNextLine() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 0, 5); // end of line 1
        ta.handleKeyStroke(new KeyStroke(KeyType.DELETE));
        assertEquals(1, ta.getLineCount());
        assertEquals("HelloWorld", ta.getLine(0));
    }

    // ── Emacs key bindings ────────────────────────────────────

    @Test
    void ctrlAMovesToLineStart() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 3);
        ta.handleKeyStroke(ctrl('A'));
        assertEquals(1, getCursorRow(ta));
        assertEquals(0, getCursorCol(ta));
    }

    @Test
    void ctrlEMovesToLineEnd() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 0, 1);
        ta.handleKeyStroke(ctrl('E'));
        assertEquals(0, getCursorRow(ta));
        assertEquals(5, getCursorCol(ta));
    }

    @Test
    void ctrlKDeletesToEndOfLine() {
        var ta = new TextArea("Hello World");
        setCursor(ta, 0, 5);
        ta.handleKeyStroke(ctrl('K'));
        assertEquals("Hello", ta.getText());
    }

    @Test
    void ctrlKAtLineEndDeletesNewlineAndJoins() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 0, 5);
        ta.handleKeyStroke(ctrl('K'));
        // Ctrl+K at end of non-last line: deletes the newline, joining lines
        assertEquals("HelloWorld", ta.getText());
    }

    @Test
    void ctrlFMovesForward() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 0);
        ta.handleKeyStroke(ctrl('F'));
        assertEquals(0, getCursorRow(ta));
        assertEquals(1, getCursorCol(ta));
    }

    @Test
    void ctrlBMovesBackward() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 3);
        ta.handleKeyStroke(ctrl('B'));
        assertEquals(0, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void ctrlNMovesDown() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 0, 2);
        ta.handleKeyStroke(ctrl('N'));
        assertEquals(1, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void ctrlPMovesUp() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 2);
        ta.handleKeyStroke(ctrl('P'));
        assertEquals(0, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    // ── Rendering ──────────────────────────────────────────────

    @Test
    void rendersTextOnScreenBuffer() {
        var ta = new TextArea("", 10, 3);
        ta.setText("Hello\nWorld\nFoo");
        var buf = new ScreenBuffer(new io.jterm.core.TerminalSize(10, 3));
        var g = new TextGraphics(buf);
        ta.setBounds(new io.jterm.core.TerminalPosition(0, 0),
                      new io.jterm.core.TerminalSize(10, 3));
        ta.draw(g);
        // First line "Hello" should appear on row 0
        assertEquals('H', buf.getCell(0, 0).character().charAt(0));
        assertEquals('e', buf.getCell(1, 0).character().charAt(0));
        assertEquals('l', buf.getCell(2, 0).character().charAt(0));
        // Second line "World" on row 1
        assertEquals('W', buf.getCell(0, 1).character().charAt(0));
        // Third line "Foo" on row 2
        assertEquals('F', buf.getCell(0, 2).character().charAt(0));
    }

    @Test
    void cursorRendersAsHighlightedCell() {
        var ta = new TextArea("", 10, 3);
        ta.setText("Hello");
        setCursor(ta, 0, 1); // cursor on 'e'
        var buf = new ScreenBuffer(new io.jterm.core.TerminalSize(10, 3));
        var g = new TextGraphics(buf);
        ta.setBounds(new io.jterm.core.TerminalPosition(0, 0),
                      new io.jterm.core.TerminalSize(10, 3));
        ta.draw(g);
        // Cell at (1, 0) should have WHITE bg (cursor highlight)
        var cursorCell = buf.getCell(1, 0);
        assertEquals(io.jterm.style.AnsiColor.WHITE, cursorCell.bg());
    }

    @Test
    void rendersBlankCellsForEmptyArea() {
        var ta = new TextArea("", 10, 3);
        var buf = new ScreenBuffer(new io.jterm.core.TerminalSize(10, 3));
        var g = new TextGraphics(buf);
        ta.setBounds(new io.jterm.core.TerminalPosition(0, 0),
                      new io.jterm.core.TerminalSize(10, 3));
        ta.draw(g);
        // All cells should be spaces
        assertEquals(' ', buf.getCell(0, 0).character().charAt(0));
        assertEquals(' ', buf.getCell(9, 2).character().charAt(0));
    }

    // ── Horizontal scrolling ───────────────────────────────────

    @Test
    void longLineScrollsHorizontally() {
        var ta = new TextArea("", 5, 2);
        ta.setText("1234567890");
        setCursor(ta, 0, 9);
        var buf = new ScreenBuffer(new io.jterm.core.TerminalSize(5, 2));
        var g = new TextGraphics(buf);
        ta.setBounds(new io.jterm.core.TerminalPosition(0, 0),
                      new io.jterm.core.TerminalSize(5, 2));
        ta.draw(g);
        // With cursor at position 9 and viewport 5 wide, viewport offset should be 5
        // So columns 5-9 should show "67890"
        assertEquals('6', buf.getCell(0, 0).character().charAt(0));
    }

    // ── Vertical scrolling ─────────────────────────────────────

    @Test
    void verticalScrollFollowsCursor() {
        var ta = new TextArea("", 10, 2);
        ta.setText("L1\nL2\nL3\nL4\nL5");
        setCursor(ta, 4, 0); // cursor on line 5 (row 4), viewport is 2 rows
        var buf = new ScreenBuffer(new io.jterm.core.TerminalSize(10, 2));
        var g = new TextGraphics(buf);
        ta.setBounds(new io.jterm.core.TerminalPosition(0, 0),
                      new io.jterm.core.TerminalSize(10, 2));
        ta.draw(g);
        // Should show lines 4 and 5 (rows 3 and 4 in the text)
        assertEquals('L', buf.getCell(0, 0).character().charAt(0));
        assertEquals('4', buf.getCell(1, 0).character().charAt(0));
    }

    // ── Boundary: cursor at extremes ─────────────────────────

    @Test
    void arrowRightAtEndOfLastLineDoesNothing() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 5); // end of last line
        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_RIGHT));
        assertEquals(1, getCursorRow(ta));
        assertEquals(5, getCursorCol(ta));
    }

    @Test
    void arrowLeftAtStartOfFirstLineDoesNothing() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 0, 0);
        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_LEFT));
        assertEquals(0, getCursorRow(ta));
        assertEquals(0, getCursorCol(ta));
    }

    @Test
    void deleteAtEndOfLastLineDoesNothing() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 5); // end of last line
        ta.handleKeyStroke(new KeyStroke(KeyType.DELETE));
        assertEquals(2, ta.getLineCount());
        assertEquals("Hello\nWorld", ta.getText());
    }

    @Test
    void deleteInMiddleOfLine() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 2); // after "He"
        ta.handleKeyStroke(new KeyStroke(KeyType.DELETE));
        assertEquals("Helo", ta.getText());
    }

    @Test
    void backspaceInMiddleOfLine() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 3); // after "Hel"
        ta.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertEquals("Helo", ta.getText());
        assertEquals(2, getCursorCol(ta));
    }

    // ── Ctrl+K edge cases ─────────────────────────────────────

    @Test
    void ctrlKAtEndOfLastLineDoesNothing() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 5); // end of last line
        ta.handleKeyStroke(ctrl('K'));
        assertEquals("Hello\nWorld", ta.getText());
        assertEquals(2, ta.getLineCount());
    }

    @Test
    void ctrlKAtStartOfLineDeletesEntireLine() {
        var ta = new TextArea("Hello World");
        setCursor(ta, 0, 0);
        ta.handleKeyStroke(ctrl('K'));
        assertEquals("", ta.getText());
    }

    @Test
    void ctrlKMiddleOfLine() {
        var ta = new TextArea("Hello World");
        setCursor(ta, 0, 5); // after "Hello"
        ta.handleKeyStroke(ctrl('K'));
        assertEquals("Hello", ta.getText());
    }

    // ── Emacs movement edge cases ─────────────────────────────

    @Test
    void ctrlFAtEndOfLineMovesToNextLine() {
        var ta = new TextArea("Hi\nWorld");
        setCursor(ta, 0, 2); // end of line 1
        ta.handleKeyStroke(ctrl('F'));
        assertEquals(1, getCursorRow(ta));
        assertEquals(0, getCursorCol(ta));
    }

    @Test
    void ctrlBAtStartOfLineMovesToPrevLineEnd() {
        var ta = new TextArea("Hi\nWorld");
        setCursor(ta, 1, 0); // start of line 2
        ta.handleKeyStroke(ctrl('B'));
        assertEquals(0, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void ctrlFAtEndOfLastLineDoesNothing() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 5);
        ta.handleKeyStroke(ctrl('F'));
        assertEquals(0, getCursorRow(ta));
        assertEquals(5, getCursorCol(ta));
    }

    @Test
    void ctrlBAtStartOfFirstLineDoesNothing() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 0);
        ta.handleKeyStroke(ctrl('B'));
        assertEquals(0, getCursorRow(ta));
        assertEquals(0, getCursorCol(ta));
    }

    @Test
    void ctrlNAtLastLineDoesNothing() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 2);
        ta.handleKeyStroke(ctrl('N'));
        assertEquals(1, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void ctrlPAtFirstLineDoesNothing() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 0, 2);
        ta.handleKeyStroke(ctrl('P'));
        assertEquals(0, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void unknownCtrlComboIgnored() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 2);
        ta.handleKeyStroke(ctrl('X'));
        assertEquals("Hello", ta.getText());
        assertEquals(0, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void ctrlCombosAreCaseInsensitive() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 3);
        // lowercase 'a' should work same as uppercase 'A'
        ta.handleKeyStroke(ctrl('a'));
        assertEquals(1, getCursorRow(ta));
        assertEquals(0, getCursorCol(ta));

        setCursor(ta, 0, 1);
        ta.handleKeyStroke(ctrl('e'));
        assertEquals(0, getCursorRow(ta));
        assertEquals(5, getCursorCol(ta));
    }

    // ── Ctrl+D (delete char forward) ────────────────────────────

    @Test
    void ctrlDDeletesCharAtCursor() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 1); // between H and e
        ta.handleKeyStroke(ctrl('D'));
        assertEquals("Hllo", ta.getText());
        assertEquals(0, getCursorRow(ta));
        assertEquals(1, getCursorCol(ta), "cursor should not move after Ctrl+D");
    }

    @Test
    void ctrlDAtEndOfLineJoinsWithNextLine() {
        var ta = new TextArea("Hi\nWorld");
        setCursor(ta, 0, 2); // end of first line
        ta.handleKeyStroke(ctrl('D'));
        assertEquals("HiWorld", ta.getText());
        assertEquals(1, ta.getLineCount());
        assertEquals(0, getCursorRow(ta));
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void ctrlDAtEndOfLastLineDoesNothing() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 5); // end of last line
        ta.handleKeyStroke(ctrl('D'));
        assertEquals("Hello\nWorld", ta.getText());
        assertEquals(2, ta.getLineCount());
        assertEquals(1, getCursorRow(ta));
        assertEquals(5, getCursorCol(ta));
    }

    @Test
    void lowercaseCtrlDAlsoWorks() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 0);
        ta.handleKeyStroke(ctrl('d'));
        assertEquals("ello", ta.getText());
        assertEquals(0, getCursorCol(ta));
    }

    // ── Constructor variants ──────────────────────────────────

    @Test
    void constructorWithTextAndDimensions() {
        var ta = new TextArea("Line1\nLine2", 60, 15);
        assertEquals("Line1\nLine2", ta.getText());
        assertEquals(2, ta.getLineCount());
        var ps = ta.getPreferredSize();
        assertEquals(60, ps.columns());
        assertEquals(15, ps.rows());
    }

    @Test
    void emptyStringConstructor() {
        var ta = new TextArea("");
        assertEquals("", ta.getText());
        assertEquals(1, ta.getLineCount());
    }

    // ── setText edge cases ────────────────────────────────────

    @Test
    void setTextResetsCursorAndViewport() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 3);
        ta.setText("New\nText\nHere");
        assertEquals(0, getCursorRow(ta));
        assertEquals(0, getCursorCol(ta));
    }

    @Test
    void setTextToEmptyString() {
        var ta = new TextArea("Hello\nWorld");
        ta.setText("");
        assertEquals("", ta.getText());
        assertEquals(1, ta.getLineCount());
        assertEquals(0, getCursorRow(ta));
        assertEquals(0, getCursorCol(ta));
    }

    @Test
    void setTextWithMultipleNewlines() {
        var ta = new TextArea();
        ta.setText("A\n\n\nB");
        assertEquals(4, ta.getLineCount());
        assertEquals("A", ta.getLine(0));
        assertEquals("", ta.getLine(1));
        assertEquals("", ta.getLine(2));
        assertEquals("B", ta.getLine(3));
    }

    @Test
    void setTextWithOnlyNewlines() {
        var ta = new TextArea();
        ta.setText("\n\n\n");
        assertEquals(4, ta.getLineCount());
        assertEquals("", ta.getLine(0));
        assertEquals("", ta.getLine(3));
    }

    // ── Multi-line editing sequences ──────────────────────────

    @Test
    void typeThenEnterThenType() {
        var ta = new TextArea();
        type(ta, "Hello");
        ta.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        type(ta, "World");
        assertEquals("Hello\nWorld", ta.getText());
        assertEquals(2, ta.getLineCount());
    }

    @Test
    void multipleEntersCreateMultipleLines() {
        var ta = new TextArea("Start");
        setCursor(ta, 0, 5);
        ta.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        ta.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        ta.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertEquals(4, ta.getLineCount());
        assertEquals("Start", ta.getLine(0));
        assertEquals("", ta.getLine(1));
        assertEquals("", ta.getLine(2));
        assertEquals("", ta.getLine(3));
    }

    @Test
    void backspaceJoinsMultipleTimes() {
        var ta = new TextArea("AAA\nBBB\nCCC");
        setCursor(ta, 2, 0);
        ta.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertEquals(2, ta.getLineCount());
        assertEquals("AAA", ta.getLine(0));
        assertEquals("BBBCCC", ta.getLine(1));
        // After join, cursor is at col 3 (end of "BBB"). Move to start of line 1 to join again.
        setCursor(ta, 1, 0);
        ta.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertEquals(1, ta.getLineCount());
        assertEquals("AAABBBCCC", ta.getLine(0));
    }

    @Test
    void deleteJoinsMultipleTimes() {
        var ta = new TextArea("AAA\nBBB\nCCC");
        setCursor(ta, 0, 3); // end of line 0
        ta.handleKeyStroke(new KeyStroke(KeyType.DELETE));
        assertEquals(2, ta.getLineCount());
        assertEquals("AAABBB", ta.getLine(0));
        assertEquals("CCC", ta.getLine(1));
        // After join, cursor is at col 3. Line is now 6 chars, so delete removes char not join.
        // Move to end of line to trigger join.
        setCursor(ta, 0, 6);
        ta.handleKeyStroke(new KeyStroke(KeyType.DELETE));
        assertEquals(1, ta.getLineCount());
        assertEquals("AAABBBCCC", ta.getLine(0));
    }

    // ── Ignored key types ─────────────────────────────────────

    @Test
    void tabKeyIgnored() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 2);
        ta.handleKeyStroke(new KeyStroke(KeyType.TAB));
        assertEquals("Hello", ta.getText());
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void escapeKeyIgnored() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 2);
        ta.handleKeyStroke(new KeyStroke(KeyType.ESCAPE));
        assertEquals("Hello", ta.getText());
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void insertKeyIgnored() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 2);
        ta.handleKeyStroke(new KeyStroke(KeyType.INSERT));
        assertEquals("Hello", ta.getText());
        assertEquals(2, getCursorCol(ta));
    }

    @Test
    void functionKeysIgnored() {
        var ta = new TextArea("Hello");
        setCursor(ta, 0, 2);
        ta.handleKeyStroke(new KeyStroke(KeyType.F1));
        ta.handleKeyStroke(new KeyStroke(KeyType.F5));
        ta.handleKeyStroke(new KeyStroke(KeyType.F12));
        assertEquals("Hello", ta.getText());
        assertEquals(2, getCursorCol(ta));
    }

    // ── Rendering edge cases ─────────────────────────────────

    @Test
    void rendersCursorAtEndOfLine() {
        var ta = new TextArea("", 10, 3);
        ta.setText("Hello");
        setCursor(ta, 0, 5); // cursor past end of text
        var buf = new ScreenBuffer(new io.jterm.core.TerminalSize(10, 3));
        var g = new TextGraphics(buf);
        ta.setBounds(new io.jterm.core.TerminalPosition(0, 0),
                      new io.jterm.core.TerminalSize(10, 3));
        ta.draw(g);
        // Cursor at col 5 should be a space with white bg
        var cell = buf.getCell(5, 0);
        assertEquals(' ', cell.character().charAt(0));
        assertEquals(io.jterm.style.AnsiColor.WHITE, cell.bg());
    }

    @Test
    void rendersCursorOnEmptyLine() {
        var ta = new TextArea("", 10, 3);
        var buf = new ScreenBuffer(new io.jterm.core.TerminalSize(10, 3));
        var g = new TextGraphics(buf);
        ta.setBounds(new io.jterm.core.TerminalPosition(0, 0),
                      new io.jterm.core.TerminalSize(10, 3));
        ta.draw(g);
        // Cursor at (0,0) should be space with white bg
        var cell = buf.getCell(0, 0);
        assertEquals(' ', cell.character().charAt(0));
        assertEquals(io.jterm.style.AnsiColor.WHITE, cell.bg());
    }

    @Test
    void rendersPartialLineWhenShorterThanViewport() {
        var ta = new TextArea("", 10, 3);
        ta.setText("Hi");
        var buf = new ScreenBuffer(new io.jterm.core.TerminalSize(10, 3));
        var g = new TextGraphics(buf);
        ta.setBounds(new io.jterm.core.TerminalPosition(0, 0),
                      new io.jterm.core.TerminalSize(10, 3));
        ta.draw(g);
        assertEquals('H', buf.getCell(0, 0).character().charAt(0));
        assertEquals('i', buf.getCell(1, 0).character().charAt(0));
        // Rest of row should be blank
        assertEquals(' ', buf.getCell(2, 0).character().charAt(0));
        assertEquals(' ', buf.getCell(9, 0).character().charAt(0));
    }

    @Test
    void rendersMultipleLinesWithBlanksBetween() {
        var ta = new TextArea("", 10, 5);
        ta.setText("A\n\nB");
        var buf = new ScreenBuffer(new io.jterm.core.TerminalSize(10, 5));
        var g = new TextGraphics(buf);
        ta.setBounds(new io.jterm.core.TerminalPosition(0, 0),
                      new io.jterm.core.TerminalSize(10, 5));
        ta.draw(g);
        assertEquals('A', buf.getCell(0, 0).character().charAt(0));
        // Row 1 should be blank (empty line)
        assertEquals(' ', buf.getCell(0, 1).character().charAt(0));
        assertEquals('B', buf.getCell(0, 2).character().charAt(0));
    }

    // ── Page Up/Down clamping ─────────────────────────────────

    @Test
    void pageDownClampsToLastLine() {
        var ta = new TextArea("", 20, 5);
        ta.setText("L1\nL2\nL3");
        setCursor(ta, 0, 0);
        ta.handleKeyStroke(new KeyStroke(KeyType.PAGE_DOWN));
        // Only 3 lines, page is 5 — should clamp to last line
        assertEquals(2, getCursorRow(ta));
    }

    @Test
    void pageUpClampsToFirstLine() {
        var ta = new TextArea("", 20, 5);
        ta.setText("L1\nL2\nL3");
        setCursor(ta, 1, 0);
        ta.handleKeyStroke(new KeyStroke(KeyType.PAGE_UP));
        assertEquals(0, getCursorRow(ta));
    }

    // ── getLineCount and getLine ──────────────────────────────

    @Test
    void getLineCountAfterSplit() {
        var ta = new TextArea("Hello World");
        setCursor(ta, 0, 5);
        ta.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertEquals(2, ta.getLineCount());
    }

    @Test
    void getLineCountAfterJoin() {
        var ta = new TextArea("Hello\nWorld");
        setCursor(ta, 1, 0);
        ta.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertEquals(1, ta.getLineCount());
    }

    // ── getText roundtrip ─────────────────────────────────────

    @Test
    void getTextRoundtripPreservesNewlines() {
        String[] tests = {"", "A", "A\nB", "A\nB\nC\nD", "\n", "A\n", "\nA", "\n\n\n"};
        for (String test : tests) {
            var ta = new TextArea(test);
            assertEquals(test, ta.getText(), "Roundtrip failed for: " + test.replace("\n", "\\n"));
        }
    }

    @Test
    void getTextAfterEditing() {
        var ta = new TextArea("Hello\nWorld");
        // Edit: go to line 2, position 0, type "XX "
        setCursor(ta, 1, 0);
        type(ta, "XX ");
        assertEquals("Hello\nXX World", ta.getText());
    }

    // ── Helpers ────────────────────────────────────────────────

    private static KeyStroke ks(char c) {
        return KeyStroke.character(c, false, false, false);
    }

    private static KeyStroke ctrl(char c) {
        return KeyStroke.character(c, true, false, false);
    }

    private static void type(TextArea ta, String text) {
        for (int i = 0; i < text.length(); i++) {
            ta.handleKeyStroke(ks(text.charAt(i)));
        }
    }

    private static int getCursorRow(TextArea ta) {
        try {
            var field = TextArea.class.getDeclaredField("cursorRow");
            field.setAccessible(true);
            return field.getInt(ta);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int getCursorCol(TextArea ta) {
        try {
            var field = TextArea.class.getDeclaredField("cursorCol");
            field.setAccessible(true);
            return field.getInt(ta);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setCursor(TextArea ta, int row, int col) {
        try {
            var rowField = TextArea.class.getDeclaredField("cursorRow");
            rowField.setAccessible(true);
            rowField.setInt(ta, row);
            var colField = TextArea.class.getDeclaredField("cursorCol");
            colField.setAccessible(true);
            colField.setInt(ta, col);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}