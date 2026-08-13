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
 * <p>Note: in the rendering, ghost text is drawn starting one cell after the
 * cursor's screen column (cursorCol + 1), so the cursor cell keeps its
 * inverted-color highlight and the full ghost text is visible from cursorCol + 1.
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
        var theme = ThemeManager.active();
        // Cursor at (0,2), shown with swapped fg/bg.
        // Ghost text "llo" starts at col 3 (cursor + 1) in BRIGHT_BLACK.
        assertEquals(theme.selectionFg(), buf.getCell(2, 0).fg(), "cursor cell should have selection fg");
        assertEquals(theme.selectionBg(), buf.getCell(2, 0).bg(), "cursor cell should have selection bg");
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(3, 0).fg());
        assertTrue(buf.getCell(3, 0).is('l'));
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(4, 0).fg());
        assertTrue(buf.getCell(4, 0).is('l'));
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(5, 0).fg());
        assertTrue(buf.getCell(5, 0).is('o'));
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
        var theme = ThemeManager.active();
        // Cursor at (1,3), shown with swapped selection fg/bg.
        // Ghost "ld" starts at col 4 (cursor + 1) in BRIGHT_BLACK.
        assertEquals(theme.selectionFg(), buf.getCell(3, 1).fg(), "cursor cell should have selection fg");
        assertEquals(theme.selectionBg(), buf.getCell(3, 1).bg(), "cursor cell should have selection bg");
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(4, 1).fg());
        assertTrue(buf.getCell(4, 1).is('l'));
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(5, 1).fg());
        assertTrue(buf.getCell(5, 1).is('d'));
    }

    @Test
    void ghostTextOnMiddleLineRendersCorrectly() {
        var ta = new TextArea("", 20, 5);
        ta.setText("Line1\nLine2\nLine3");
        setCursor(ta, 1, 5);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "Extra");

        var buf = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        // Cursor at (1,5), shown with swapped selection fg/bg.
        // Ghost "Extra" starts at col 6 (cursor + 1) in BRIGHT_BLACK.
        assertEquals(theme.selectionFg(), buf.getCell(5, 1).fg(), "cursor cell should have selection fg");
        assertEquals(theme.selectionBg(), buf.getCell(5, 1).bg(), "cursor cell should have selection bg");
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(6, 1).fg());
        assertTrue(buf.getCell(6, 1).is('E'));
        assertTrue(buf.getCell(7, 1).is('x'));
        assertTrue(buf.getCell(8, 1).is('t'));
        assertTrue(buf.getCell(9, 1).is('r'));
        assertTrue(buf.getCell(10, 1).is('a'));
    }

    @Test
    void ghostTextOnEmptyLineRendersCorrectly() {
        var ta = new TextArea("", 20, 5);
        ta.setText("Hello\n\nWorld");
        setCursor(ta, 1, 0);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "text");

        var buf = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        // Cursor at (1,0), shown with swapped selection fg/bg.
        // Ghost "text" starts at col 1 (cursor + 1) in BRIGHT_BLACK.
        assertEquals(theme.selectionFg(), buf.getCell(0, 1).fg(), "cursor cell should have selection fg");
        assertEquals(theme.selectionBg(), buf.getCell(0, 1).bg(), "cursor cell should have selection bg");
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(1, 1).fg());
        assertTrue(buf.getCell(1, 1).is('t'));
        assertTrue(buf.getCell(2, 1).is('e'));
        assertTrue(buf.getCell(3, 1).is('x'));
        assertTrue(buf.getCell(4, 1).is('t'));
    }

    @Test
    void ghostTextOnLastLineRendersCorrectly() {
        var ta = new TextArea("", 20, 5);
        ta.setText("A\nB\nC");
        setCursor(ta, 2, 1);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "DE");

        var buf = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        // Cursor at (2,1), shown with swapped selection fg/bg.
        // Ghost "DE" starts at col 2 (cursor + 1) in BRIGHT_BLACK.
        assertEquals(theme.selectionFg(), buf.getCell(1, 2).fg(), "cursor cell should have selection fg");
        assertEquals(theme.selectionBg(), buf.getCell(1, 2).bg(), "cursor cell should have selection bg");
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(2, 2).fg());
        assertTrue(buf.getCell(2, 2).is('D'));
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(3, 2).fg());
        assertTrue(buf.getCell(3, 2).is('E'));
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
        var theme = ThemeManager.active();
        // Ghost "loWorld" starts at col 4 (cursor + 1) in BRIGHT_BLACK.
        // col 4 = 'l' (row 0), wraps to row 1: "oWorl" (cols 0-4), 'd' on row 2 col 0.
        assertEquals(theme.selectionFg(), buf.getCell(3, 0).fg(), "cursor cell should have selection fg");
        assertEquals(theme.selectionBg(), buf.getCell(3, 0).bg(), "cursor cell should have selection bg");
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(4, 0).fg());
        assertTrue(buf.getCell(4, 0).is('l'));
        assertTrue(buf.getCell(0, 1).is('o'));
        assertTrue(buf.getCell(1, 1).is('W'));
        assertTrue(buf.getCell(2, 1).is('o'));
        assertTrue(buf.getCell(3, 1).is('r'));
        assertTrue(buf.getCell(4, 1).is('l'));
        assertTrue(buf.getCell(0, 2).is('d'));
    }

    @Test
    void ghostTextWrapsMultipleRows() {
        var ta = new TextArea("", 3, 5);
        ta.setText("A");
        setCursor(ta, 0, 1);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "BCDEF");

        var buf = drawArea(ta, 3, 5);
        var theme = ThemeManager.active();
        // Ghost "BCDEF" starts at col 2 (cursor + 1) in BRIGHT_BLACK.
        // col 2 = 'B' (row 0), wraps to row 1: "CDE" (cols 0,1,2), 'F' on row 2 col 0.
        assertEquals(theme.selectionFg(), buf.getCell(1, 0).fg(), "cursor cell should have selection fg");
        assertEquals(theme.selectionBg(), buf.getCell(1, 0).bg(), "cursor cell should have selection bg");
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(2, 0).fg());
        assertTrue(buf.getCell(2, 0).is('B'));
        assertTrue(buf.getCell(0, 1).is('C'));
        assertTrue(buf.getCell(1, 1).is('D'));
        assertTrue(buf.getCell(2, 1).is('E'));
        assertTrue(buf.getCell(0, 2).is('F'));
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

    // -- Space dismisses ghost text and inserts literal space --

    @Test
    void spaceDismissesGhostTextAndInsertsLiteralSpaceOnSingleLine() {
        var ta = new TextArea("", 20, 5);
        ta.setText("he");
        setCursor(ta, 0, 2);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "llo");

        drawArea(ta, 20, 5);
        assertEquals("llo", ta.getCurrentGhostText());

        ta.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        assertEquals("he ", ta.getText());
        assertEquals(0, getCursorRow(ta));
        assertEquals(3, getCursorCol(ta));
    }

    @Test
    void spaceDismissesGhostTextAndInsertsLiteralSpaceOnSecondLine() {
        var ta = new TextArea("", 20, 5);
        ta.setText("Hello\nwor");
        setCursor(ta, 1, 3);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> "ld");

        drawArea(ta, 20, 5);
        assertEquals("ld", ta.getCurrentGhostText());

        ta.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        assertEquals("Hello\nwor ", ta.getText());
        assertEquals(1, getCursorRow(ta));
        assertEquals(4, getCursorCol(ta));
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
        var ta = new TextArea("", 7, 3);
        ta.setText("Hello");
        setCursor(ta, 0, 5);
        ta.setFocused(true);
        ta.setCompletionProvider((text, pos) -> " World");

        var buf = drawArea(ta, 7, 3);
        boolean foundGhost = false;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 7; c++) {
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
