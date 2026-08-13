package io.jterm.widget;

import io.jterm.completion.CompletionProvider;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.ThemeManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ghost text (inline completion) rendering and acceptance in
 * {@link TextArea}, including multi-line contexts.
 *
 * <p>Note: in the rendering, ghost text is drawn starting at the cursor's
 * screen column, then the cursor cell is drawn on top, overwriting the first
 * ghost character. So visible ghost characters start at cursorCol + 1.
 */
class TextAreaGhostTextTest {

    @Test
    void setCompletionProviderStoresIt() {
        var ta = new TextArea("", 20, 5);
        CompletionProvider provider = (text, pos) -> "llo";
        assertNull(ta.getCompletionProvider());
        ta.setCompletionProvider(provider);
        assertSame(provider, ta.getCompletionProvider());
    }

    @Test
    void clearCompletionProviderBySettingNull() {
        var ta = new TextArea("", 20, 5);
        ta.setCompletionProvider((text, pos) -> "llo");
        ta.setCompletionProvider(null);
        assertNull(ta.getCompletionProvider());
    }

    // -- Single-line ghost text rendering --

    @Test
    void ghostTextAppearsInBrightBlackAfterCursorOnSingleLine() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        var buf = drawArea(ta, 20, 5);
        // Ghost text "llo" drawn at cols 2,3,4. Cursor overwrites col 2.
        // Visible ghost: col 3 = 'l' (2nd l), col 4 = 'o'
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(3, 0).fg());
        assertTrue(buf.getCell(3, 0).is('l'));
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(4, 0).fg());
        assertTrue(buf.getCell(4, 0).is('o'));
    }

    @Test
    void noGhostTextWhenProviderReturnsNull() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> null);

        var buf = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        assertEquals(theme.foreground(), buf.getCell(3, 0).fg());
    }

    @Test
    void noGhostTextWhenNoProviderSet() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);

        var buf = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        assertEquals(theme.foreground(), buf.getCell(3, 0).fg());
    }

    @Test
    void ghostTextDoesNotAffectGetText() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        drawArea(ta, 20, 5);
        assertEquals("he", ta.getText());
    }

    @Test
    void ghostTextUsesThemeBackground() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        var buf = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        assertEquals(theme.background(), buf.getCell(3, 0).bg());
    }

    @Test
    void ghostTextNotShownWhenNotFocused() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(false);
        ta.setCompletionProvider((text, pos) -> "llo");

        var buf = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        assertEquals(theme.foreground(), buf.getCell(3, 0).fg());
    }

    // -- Multi-line rendering --

    @Test
    void ghostTextOnSecondLineRendersCorrectly() {
        var ta = new TextArea("", 20, 5);
        ta.setText("Hello\nWor");
        setCursor(ta, 1, 3);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "ld");

        var buf = drawArea(ta, 20, 5);
        // Ghost "ld" at row 1, cols 3,4. Cursor overwrites col 3.
        // Visible: col 4 = 'd'
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(4, 1).fg());
        assertTrue(buf.getCell(4, 1).is('d'));
    }

    @Test
    void ghostTextOnMiddleLineRendersCorrectly() {
        var ta = new TextArea("", 20, 5);
        ta.setText("Line1\nLine2\nLine3");
        setCursor(ta, 1, 5);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "Extra");

        var buf = drawArea(ta, 20, 5);
        // Ghost "Extra" at row 1, cols 5,6,7,8,9. Cursor overwrites col 5.
        // Visible: col 6='x', col 7='t', col 8='r', col 9='a'
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(6, 1).fg());
        assertTrue(buf.getCell(6, 1).is('x'));
        assertTrue(buf.getCell(7, 1).is('t'));
        assertTrue(buf.getCell(8, 1).is('r'));
        assertTrue(buf.getCell(9, 1).is('a'));
    }

    @Test
    void ghostTextOnEmptyLineRendersCorrectly() {
        var ta = new TextArea("", 20, 5);
        ta.setText("Hello\n\nWorld");
        setCursor(ta, 1, 0);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "text");

        var buf = drawArea(ta, 20, 5);
        // Ghost "text" at row 1, cols 0,1,2,3. Cursor overwrites col 0.
        // Visible: col 1='e', col 2='x', col 3='t'
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(1, 1).fg());
        assertTrue(buf.getCell(1, 1).is('e'));
        assertTrue(buf.getCell(2, 1).is('x'));
        assertTrue(buf.getCell(3, 1).is('t'));
    }

    @Test
    void ghostTextOnLastLineRendersCorrectly() {
        var ta = new TextArea("", 20, 5);
        ta.setText("A\nB\nC");
        setCursor(ta, 2, 1);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "DE");

        var buf = drawArea(ta, 20, 5);
        // Ghost "DE" at row 2, cols 1,2. Cursor overwrites col 1.
        // Visible: col 2 = 'E'
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(2, 2).fg());
        assertTrue(buf.getCell(2, 2).is('E'));
    }

    // -- Ghost text wrapping --

    @Test
    void ghostTextWrapsToNextRowWhenExceedingWidth() {
        var ta = new TextArea("", 5, 3);
        ta.setText("Hel");
        setCursor(ta, 0, 3);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "loWorld");

        var buf = drawArea(ta, 5, 3);
        // Ghost "loWorld" starts at col 3 (row 0). Cols 3,4 = "lo", wraps to row 1 cols 0-4 = "World"
        // Cursor overwrites col 3. Visible: col 4='o' (row 0), "World" on row 1
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(4, 0).fg());
        assertTrue(buf.getCell(4, 0).is('o'));
        assertTrue(buf.getCell(0, 1).is('W'));
        assertTrue(buf.getCell(1, 1).is('o'));
        assertTrue(buf.getCell(2, 1).is('r'));
        assertTrue(buf.getCell(3, 1).is('l'));
        assertTrue(buf.getCell(4, 1).is('d'));
    }

    @Test
    void ghostTextWrapsMultipleRows() {
        var ta = new TextArea("", 3, 5);
        ta.setText("A");
        setCursor(ta, 0, 1);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "BCDEF");

        var buf = drawArea(ta, 3, 5);
        // Ghost "BCDEF" starts at col 1 (row 0). Cols 1,2 = "BC", wraps to row 1 cols 0,1,2 = "DEF"
        // Cursor overwrites col 1. Visible: col 2='C' (row 0), "DEF" on row 1
        assertTrue(buf.getCell(2, 0).is('C'));
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(2, 0).fg());
        assertTrue(buf.getCell(0, 1).is('D'));
        assertTrue(buf.getCell(1, 1).is('E'));
        assertTrue(buf.getCell(2, 1).is('F'));
    }

    // -- Provider receives current line text --

    @Test
    void providerReceivesCurrentLineText() {
        var ta = new TextArea("", 20, 5);
        ta.setText("Hello\nWor");
        setCursor(ta, 1, 3);
        ta.setFocused(true);

        var captured = new java.util.concurrent.atomic.AtomicReference<String>();
        var capturedPos = new java.util.concurrent.atomic.AtomicInteger();
        ta.setCompletionProvider((text, pos) -> {
            captured.set(text);
            capturedPos.set(pos);
            return "ld";
        });

        drawArea(ta, 20, 5);
        assertEquals("Wor", captured.get());
        assertEquals(3, capturedPos.get());
    }

    @Test
    void providerReceivesCorrectLineAfterMovingCursor() {
        var ta = new TextArea("", 20, 5);
        ta.setText("Hello\nWorld");
        setCursor(ta, 1, 3);
        ta.setFocused(true);

        var captured = new java.util.concurrent.atomic.AtomicReference<String>();
        ta.setCompletionProvider((text, pos) -> {
            captured.set(text);
            return null;
        });

        drawArea(ta, 20, 5);
        assertEquals("World", captured.get());
    }

    // -- Space accepts completion --

    @Test
    void spaceAcceptsCompletionOnSingleLine() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        drawArea(ta, 20, 5);
        assertEquals("llo", ta.getCurrentGhostText());

        ta.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        assertEquals("hello ", ta.getText());
        assertEquals(0, getCursorRow(ta));
        assertEquals(6, getCursorCol(ta));
    }

    @Test
    void spaceAcceptsCompletionOnSecondLine() {
        var ta = new TextArea("", 20, 5);
        ta.setText("Hello\nwor");
        setCursor(ta, 1, 3);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "ld");

        drawArea(ta, 20, 5);
        assertEquals("ld", ta.getCurrentGhostText());

        ta.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        assertEquals("Hello\nworld ", ta.getText());
        assertEquals(1, getCursorRow(ta));
        assertEquals(6, getCursorCol(ta));
    }

    @Test
    void spaceAcceptsCompletionAndInsertsSpace() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        drawArea(ta, 20, 5);
        ta.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        assertTrue(ta.getText().endsWith(" "));
    }

    // -- Tab accepts completion --

    @Test
    void tabAcceptsCompletionOnSingleLine() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        drawArea(ta, 20, 5);
        ta.handleKeyStroke(new KeyStroke(KeyType.TAB));
        assertEquals("hello", ta.getText());
        assertEquals(5, getCursorCol(ta));
    }

    @Test
    void tabAcceptsCompletionOnSecondLine() {
        var ta = new TextArea("", 20, 5);
        ta.setText("Hello\nwor");
        setCursor(ta, 1, 3);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "ld");

        drawArea(ta, 20, 5);
        ta.handleKeyStroke(new KeyStroke(KeyType.TAB));
        assertEquals("Hello\nworld", ta.getText());
        assertEquals(1, getCursorRow(ta));
        assertEquals(5, getCursorCol(ta));
    }

    @Test
    void tabDoesNotInsertTabCharacterWhenAccepting() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        drawArea(ta, 20, 5);
        ta.handleKeyStroke(new KeyStroke(KeyType.TAB));
        assertFalse(ta.getText().contains("\t"));
    }

    @Test
    void tabWithoutGhostTextIsNotConsumed() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);

        boolean consumed = ta.handleKeyStroke(new KeyStroke(KeyType.TAB));
        assertFalse(consumed);
    }

    // -- Other keypress clears ghost text --

    @Test
    void otherKeystrokeClearsGhostTextAndProcessesNormally() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        drawArea(ta, 20, 5);
        assertEquals("llo", ta.getCurrentGhostText());

        ta.handleKeyStroke(KeyStroke.character('x', false, false, false));
        assertEquals("hex", ta.getText());
        assertNull(ta.getCurrentGhostText());
    }

    @Test
    void backspaceClearsGhostTextAndProcessesNormally() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        drawArea(ta, 20, 5);
        ta.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertEquals("h", ta.getText());
        assertNull(ta.getCurrentGhostText());
    }

    @Test
    void enterClearsGhostTextAndProcessesNormally() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        drawArea(ta, 20, 5);
        ta.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertEquals("he\n", ta.getText());
        assertNull(ta.getCurrentGhostText());
    }

    @Test
    void arrowKeysClearGhostText() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        drawArea(ta, 20, 5);
        assertEquals("llo", ta.getCurrentGhostText());

        ta.handleKeyStroke(new KeyStroke(KeyType.ARROW_LEFT));
        assertNull(ta.getCurrentGhostText());
    }

    // -- Escape clears ghost text --

    @Test
    void escapeClearsGhostText() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        drawArea(ta, 20, 5);
        assertEquals("llo", ta.getCurrentGhostText());

        ta.handleKeyStroke(new KeyStroke(KeyType.ESCAPE));
        assertNull(ta.getCurrentGhostText());
        assertEquals("he", ta.getText());
    }

    // -- Ghost text re-queried after keystroke --

    @Test
    void ghostTextRequeriedAfterKeystroke() {
        var ta = new TextArea("", 20, 5);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> {
            if (text.equals("he")) return "llo";
            if (text.equals("hel")) return "lo";
            return null;
        });

        ta.handleKeyStroke(KeyStroke.character('h', false, false, false));
        drawArea(ta, 20, 5);
        assertNull(ta.getCurrentGhostText());

        ta.handleKeyStroke(KeyStroke.character('e', false, false, false));
        drawArea(ta, 20, 5);
        assertEquals("llo", ta.getCurrentGhostText());

        ta.handleKeyStroke(KeyStroke.character('l', false, false, false));
        drawArea(ta, 20, 5);
        assertEquals("lo", ta.getCurrentGhostText());
    }

    // -- Ghost text cleared on setText --

    @Test
    void setTextClearsGhostText() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        drawArea(ta, 20, 5);
        assertEquals("llo", ta.getCurrentGhostText());

        ta.setText("new");
        assertNull(ta.getCurrentGhostText());
    }

    // -- Ghost text with horizontal scroll --

    @Test
    void ghostTextRendersWhenCursorIsAtEndOfVisibleArea() {
        var ta = new TextArea("", 5, 3);
        ta.setText("Hello");
        setCursor(ta, 0, 5);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> " World");

        var buf = drawArea(ta, 5, 3);
        boolean foundGhost = false;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 5; c++) {
                if (buf.getCell(c, r).fg() == AnsiColor.BRIGHT_BLACK) {
                    foundGhost = true;
                    break;
                }
            }
            if (foundGhost) break;
        }
        assertTrue(foundGhost, "ghost text should be rendered in BRIGHT_BLACK somewhere");
    }

    // -- Helpers --

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

    private ScreenBuffer drawArea(TextArea ta, int cols, int rows) {
        var size = new TerminalSize(cols, rows);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        ta.setBounds(TerminalPosition.TOP_LEFT, size);
        ta.draw(g);
        return buf;
    }
}
