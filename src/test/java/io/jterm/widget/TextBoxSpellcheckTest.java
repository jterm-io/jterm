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
 * Tests for spellcheck integration in {@link TextBox}.
 */
class TextBoxSpellcheckTest {

    private SpellcheckDictionary createDictionary() {
        return SpellcheckDictionary.of(Set.of("hello", "world"));
    }

    private ScreenBuffer drawBox(TextBox box, int columns) {
        var size = new TerminalSize(columns, 1);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        box.draw(g);
        return buf;
    }

    @Test
    void misspelledWordHighlightedInYellow() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("helo");
        box.setFocused(false);
        box.setSpellcheckDictionary(createDictionary());
        var buf = drawBox(box, 20);
        // "helo" is misspelled — all 4 chars should be yellow
        for (int i = 0; i < 4; i++) {
            assertEquals(AnsiColor.YELLOW, buf.getCell(i, 0).fg(),
                    "char at " + i + " should be yellow (misspelled)");
        }
    }

    @Test
    void validWordNotHighlighted() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("hello");
        box.setFocused(false);
        box.setSpellcheckDictionary(createDictionary());
        var buf = drawBox(box, 20);
        var theme = ThemeManager.active();
        for (int i = 0; i < 5; i++) {
            assertEquals(theme.foreground(), buf.getCell(i, 0).fg(),
                    "char at " + i + " should use theme foreground (valid word)");
        }
    }

    @Test
    void mixedValidAndMisspelled() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("hello helo");
        box.setFocused(false);
        box.setSpellcheckDictionary(createDictionary());
        var buf = drawBox(box, 20);
        var theme = ThemeManager.active();
        // "hello" (0-4) valid — theme foreground
        for (int i = 0; i < 5; i++) {
            assertEquals(theme.foreground(), buf.getCell(i, 0).fg(),
                    "char at " + i + " should be theme fg (valid)");
        }
        // space at 5 — theme foreground
        assertEquals(theme.foreground(), buf.getCell(5, 0).fg());
        // "helo" (6-9) misspelled — yellow
        for (int i = 6; i < 10; i++) {
            assertEquals(AnsiColor.YELLOW, buf.getCell(i, 0).fg(),
                    "char at " + i + " should be yellow (misspelled)");
        }
    }

    @Test
    void noDictionaryBackwardCompatible() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("helo");
        box.setFocused(false);
        // No spellcheck dictionary set
        var buf = drawBox(box, 20);
        var theme = ThemeManager.active();
        for (int i = 0; i < 4; i++) {
            assertEquals(theme.foreground(), buf.getCell(i, 0).fg(),
                    "char at " + i + " should use theme foreground (no spellcheck)");
        }
    }

    @Test
    void setSpellcheckDictionaryNullClearsResolver() {
        var box = new TextBox(20);
        box.setSpellcheckDictionary(createDictionary());
        assertNotNull(box.getStyleResolver());
        box.setSpellcheckDictionary(null);
        assertNull(box.getStyleResolver());
    }

    @Test
    void styleResolverIsSpellcheckResolver() {
        var box = new TextBox(20);
        box.setSpellcheckDictionary(createDictionary());
        assertTrue(box.getStyleResolver() instanceof SpellcheckResolver);
    }
}