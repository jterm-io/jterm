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
 * Tests for ghost text (inline completion) rendering and acceptance in {@link TextBox}.
 */
class TextBoxGhostTextTest {

    // ── Provider set/get ──────────────────────────────────────

    @Test
    void setCompletionProviderStoresIt() {
        var box = new TextBox(20);
        CompletionProvider provider = (text, pos) -> "llo";
        assertNull(box.getCompletionProvider());
        box.setCompletionProvider(provider);
        assertSame(provider, box.getCompletionProvider());
    }

    @Test
    void clearCompletionProviderBySettingNull() {
        var box = new TextBox(20);
        box.setCompletionProvider((text, pos) -> "llo");
        box.setCompletionProvider(null);
        assertNull(box.getCompletionProvider());
    }

    // ── Ghost text rendering ───────────────────────────────────

    @Test
    void ghostTextAppearsInBrightBlackAfterCursor() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);
        box.setCompletionProvider((text, pos) -> "llo");

        var buf = drawBox(box, 20);
        var theme = ThemeManager.active();
        // Cursor is at column 2 (end of "he"), shown with swapped fg/bg.
        // Ghost text "llo" starts at column 3 (cursor + 1) in BRIGHT_BLACK.
        assertEquals(theme.background(), buf.getCell(2, 0).fg(), "cursor cell should have swapped fg");
        assertEquals(theme.foreground(), buf.getCell(2, 0).bg(), "cursor cell should have swapped bg");
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(3, 0).fg(), "ghost char 'l' should be BRIGHT_BLACK");
        assertTrue(buf.getCell(3, 0).is('l'));
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(4, 0).fg(), "ghost char 'l' should be BRIGHT_BLACK");
        assertTrue(buf.getCell(4, 0).is('l'));
        assertEquals(AnsiColor.BRIGHT_BLACK, buf.getCell(5, 0).fg(), "ghost char 'o' should be BRIGHT_BLACK");
        assertTrue(buf.getCell(5, 0).is('o'));
    }

    @Test
    void noGhostTextWhenProviderReturnsNull() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);
        box.setCompletionProvider((text, pos) -> null);

        var buf = drawBox(box, 20);
        var theme = ThemeManager.active();
        // No ghost text — the cursor cell at col 2 has swapped fg/bg, col 3 should be normal
        assertEquals(theme.foreground(), buf.getCell(3, 0).fg());
    }

    @Test
    void noGhostTextWhenNoProviderSet() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);

        var buf = drawBox(box, 20);
        var theme = ThemeManager.active();
        assertEquals(theme.foreground(), buf.getCell(3, 0).fg());
    }

    @Test
    void ghostTextDoesNotAffectGetValue() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);
        box.setCompletionProvider((text, pos) -> "llo");

        drawBox(box, 20); // trigger draw
        assertEquals("he", box.getValue(), "ghost text is visual only, not part of buffer");
    }

    @Test
    void ghostTextUsesThemeBackground() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);
        box.setCompletionProvider((text, pos) -> "llo");

        var buf = drawBox(box, 20);
        var theme = ThemeManager.active();
        assertEquals(theme.background(), buf.getCell(3, 0).bg(), "ghost text should use theme background");
    }

    // ── Space dismisses ghost text and inserts literal space ──

    @Test
    void spaceDismissesGhostTextAndInsertsLiteralSpace() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);
        // Provider that always suggests "llo"
        box.setCompletionProvider((text, pos) -> "llo");

        // First, trigger the suggestion by drawing (simulates the query on focus)
        drawBox(box, 20);
        assertEquals("llo", box.getCurrentGhostText(), "ghost text should be set after draw");

        // Press space — dismisses ghost text and inserts a literal space
        box.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        assertEquals("he ", box.getValue(), "space should dismiss ghost text and insert literal space");
        assertNull(box.getCurrentGhostText(), "ghost text should be cleared after space");
        assertEquals(3, getCursor(box), "cursor should advance past the space");
    }

    @Test
    void spaceDismissesGhostTextAndInsertsLiteralSpaceVerifyValue() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);
        box.setCompletionProvider((text, pos) -> "llo");

        drawBox(box, 20);
        box.handleKeyStroke(KeyStroke.character(' ', false, false, false));
        // Space dismisses ghost text and inserts a literal space (no completion accepted)
        assertEquals("he ", box.getValue());
    }

    // ── Tab accepts completion ─────────────────────────────────

    @Test
    void tabAcceptsCompletion() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);
        box.setCompletionProvider((text, pos) -> "llo");

        drawBox(box, 20);
        box.handleKeyStroke(new KeyStroke(KeyType.TAB));
        assertEquals("hello", box.getValue(), "tab should accept completion");
        assertEquals(5, getCursor(box));
    }

    @Test
    void tabDoesNotInsertTabCharacterWhenAccepting() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);
        box.setCompletionProvider((text, pos) -> "llo");

        drawBox(box, 20);
        box.handleKeyStroke(new KeyStroke(KeyType.TAB));
        assertFalse(box.getValue().contains("\t"), "tab accept should not insert tab char");
    }

    // ── Other keypress clears ghost text ───────────────────────

    @Test
    void otherKeystrokeClearsGhostTextAndProcessesNormally() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);
        box.setCompletionProvider((text, pos) -> "llo");

        drawBox(box, 20);
        assertEquals("llo", box.getCurrentGhostText());

        // Press 'x' — should clear ghost text and insert 'x'
        box.handleKeyStroke(KeyStroke.character('x', false, false, false));
        assertEquals("hex", box.getValue(), "character should be inserted normally");
        assertNull(box.getCurrentGhostText(), "ghost text should be cleared after other key");
    }

    @Test
    void backspaceClearsGhostTextAndProcessesNormally() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);
        box.setCompletionProvider((text, pos) -> "llo");

        drawBox(box, 20);
        box.handleKeyStroke(new KeyStroke(KeyType.BACKSPACE));
        assertEquals("h", box.getValue(), "backspace should work normally");
        assertNull(box.getCurrentGhostText(), "ghost text should be cleared after backspace");
    }

    // ── Escape clears ghost text ───────────────────────────────

    @Test
    void escapeClearsGhostText() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);
        box.setCompletionProvider((text, pos) -> "llo");

        drawBox(box, 20);
        assertEquals("llo", box.getCurrentGhostText());

        box.handleKeyStroke(new KeyStroke(KeyType.ESCAPE));
        assertNull(box.getCurrentGhostText(), "escape should clear ghost text");
        assertEquals("he", box.getValue(), "escape should not modify text");
    }

    // ── Ghost text re-queried after keystroke ──────────────────

    @Test
    void ghostTextRequeriedAfterKeystroke() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setFocused(true);
        // Provider that suggests based on current text
        box.setCompletionProvider((text, pos) -> {
            if (text.equals("he")) return "llo";
            if (text.equals("hel")) return "lo";
            return null;
        });

        // Type 'h'
        box.handleKeyStroke(KeyStroke.character('h', false, false, false));
        drawBox(box, 20);
        assertNull(box.getCurrentGhostText(), "no suggestion for 'h'");

        // Type 'e' — now text is "he", should suggest "llo"
        box.handleKeyStroke(KeyStroke.character('e', false, false, false));
        drawBox(box, 20);
        assertEquals("llo", box.getCurrentGhostText(), "should suggest 'llo' after typing 'he'");

        // Type 'l' — now text is "hel", should suggest "lo"
        box.handleKeyStroke(KeyStroke.character('l', false, false, false));
        drawBox(box, 20);
        assertEquals("lo", box.getCurrentGhostText(), "should suggest 'lo' after typing 'hel'");
    }

    // ── Ghost text not shown when not focused ──────────────────

    @Test
    void ghostTextNotShownWhenNotFocused() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(false);
        box.setCompletionProvider((text, pos) -> "llo");

        var buf = drawBox(box, 20);
        var theme = ThemeManager.active();
        // No ghost text when not focused
        assertEquals(theme.foreground(), buf.getCell(2, 0).fg(), "no ghost text when not focused");
    }

    // ── Ghost text cleared on setValue ─────────────────────────

    @Test
    void setValueClearsGhostText() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("he");
        setCursor(box, 2);
        box.setFocused(true);
        box.setCompletionProvider((text, pos) -> "llo");

        drawBox(box, 20);
        assertEquals("llo", box.getCurrentGhostText());

        box.setValue("new");
        assertNull(box.getCurrentGhostText(), "setValue should clear ghost text");
    }

    // ── Helpers ────────────────────────────────────────────────

    private void setCursor(TextBox box, int pos) {
        try {
            var field = TextBox.class.getDeclaredField("cursorPosition");
            field.setAccessible(true);
            field.setInt(box, pos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getCursor(TextBox box) {
        box.setFocused(true);
        var size = new TerminalSize(20, 1);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        box.draw(g);
        var theme = ThemeManager.active();
        for (int c = 0; c < size.columns(); c++) {
            var cell = buf.getCell(c, 0);
            if (!cell.fg().equals(theme.foreground()) || !cell.bg().equals(theme.background())) {
                // Skip ghost text cells (BRIGHT_BLACK fg)
                if (cell.fg().equals(AnsiColor.BRIGHT_BLACK)) continue;
                return c;
            }
        }
        return -1;
    }

    private ScreenBuffer drawBox(TextBox box, int columns) {
        var size = new TerminalSize(columns, 1);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        box.draw(g);
        return buf;
    }
}