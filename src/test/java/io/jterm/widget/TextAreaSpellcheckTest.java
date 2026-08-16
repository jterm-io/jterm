package io.jterm.widget;

import io.jterm.completion.SpellcheckDictionary;
import io.jterm.completion.SpellcheckResolver;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.ThemeManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for spellcheck integration in {@link TextArea}.
 */
class TextAreaSpellcheckTest {

    private SpellcheckDictionary createDictionary() {
        return SpellcheckDictionary.of(Set.of("hello", "world"));
    }

    private ScreenBuffer drawArea(TextArea ta, int cols, int rows) {
        var size = new TerminalSize(cols, rows);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        ta.draw(g);
        return buf;
    }

    @Test
    void misspelledWordOnFirstLineHighlighted() {
        var ta = new TextArea("", 20, 5);
        ta.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        ta.setText("helo\nworld");
        ta.setFocused(false);
        ta.setSpellcheckDictionary(createDictionary());
        var buf = drawArea(ta, 20, 5);
        // Line 1: "helo" misspelled — yellow
        for (int i = 0; i < 4; i++) {
            assertEquals(AnsiColor.YELLOW, buf.getCell(i, 0).fg(),
                    "line 1 char " + i + " should be yellow (misspelled)");
        }
        // Line 2: "world" valid — theme foreground
        var theme = ThemeManager.active();
        for (int i = 0; i < 5; i++) {
            assertEquals(theme.foreground(), buf.getCell(i, 1).fg(),
                    "line 2 char " + i + " should be theme fg (valid)");
        }
    }

    @Test
    void validWordsNotHighlighted() {
        var ta = new TextArea("", 20, 5);
        ta.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        ta.setText("hello\nworld");
        ta.setFocused(false);
        ta.setSpellcheckDictionary(createDictionary());
        var buf = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        for (int i = 0; i < 5; i++) {
            assertEquals(theme.foreground(), buf.getCell(i, 0).fg(),
                    "line 1 char " + i + " should be theme fg (valid)");
            assertEquals(theme.foreground(), buf.getCell(i, 1).fg(),
                    "line 2 char " + i + " should be theme fg (valid)");
        }
    }

    @Test
    void noDictionaryBackwardCompatible() {
        var ta = new TextArea("", 20, 5);
        ta.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        ta.setText("helo\nwrld");
        ta.setFocused(false);
        var buf = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        for (int i = 0; i < 4; i++) {
            assertEquals(theme.foreground(), buf.getCell(i, 0).fg(),
                    "line 1 char " + i + " should be theme fg (no spellcheck)");
        }
    }

    @Test
    void setSpellcheckDictionaryNullClearsResolver() {
        var ta = new TextArea();
        ta.setSpellcheckDictionary(createDictionary());
        assertNotNull(ta.getStyleResolver());
        ta.setSpellcheckDictionary(null);
        assertNull(ta.getStyleResolver());
    }

    @Test
    void styleResolverIsSpellcheckResolver() {
        var ta = new TextArea();
        ta.setSpellcheckDictionary(createDictionary());
        assertTrue(ta.getStyleResolver() instanceof SpellcheckResolver);
    }

    @Test
    void mixedValidAndMisspelledOnSameLine() {
        var ta = new TextArea("", 20, 5);
        ta.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        ta.setText("hello helo\nworld wrld");
        ta.setFocused(false);
        ta.setSpellcheckDictionary(createDictionary());
        var buf = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        // Line 1: "hello" (0-4) valid, " " (5), "helo" (6-9) misspelled
        for (int i = 0; i < 5; i++) {
            assertEquals(theme.foreground(), buf.getCell(i, 0).fg(),
                    "line 1 char " + i + " should be theme fg (valid)");
        }
        for (int i = 6; i < 10; i++) {
            assertEquals(AnsiColor.YELLOW, buf.getCell(i, 0).fg(),
                    "line 1 char " + i + " should be yellow (misspelled)");
        }
        // Line 2: "world" (0-4) valid, " " (5), "wrld" (6-9) misspelled
        for (int i = 0; i < 5; i++) {
            assertEquals(theme.foreground(), buf.getCell(i, 1).fg(),
                    "line 2 char " + i + " should be theme fg (valid)");
        }
        for (int i = 6; i < 10; i++) {
            assertEquals(AnsiColor.YELLOW, buf.getCell(i, 1).fg(),
                    "line 2 char " + i + " should be yellow (misspelled)");
        }
    }
}