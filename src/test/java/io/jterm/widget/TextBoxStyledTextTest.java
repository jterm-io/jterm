package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.style.TextStyleResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for per-character style resolver integration in {@link TextBox}.
 */
class TextBoxStyledTextTest {

    @Test
    void styleResolverColorsEverythingYellow() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        box.setFocused(false); // no cursor highlight to interfere
        box.setStyleResolver((charIndex, c, defaultStyle) ->
                defaultStyle.withForeground(AnsiColor.YELLOW));
        var buf = drawBox(box, 20);
        for (int i = 0; i < 5; i++) {
            assertEquals(AnsiColor.YELLOW, buf.getCell(i, 0).fg(),
                    "char at " + i + " should be yellow");
        }
    }

    @Test
    void noResolverIsBackwardCompatible() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        box.setFocused(false);
        // No resolver set — should behave as before
        var buf = drawBox(box, 20);
        var theme = ThemeManager.active();
        for (int i = 0; i < 5; i++) {
            assertEquals(theme.foreground(), buf.getCell(i, 0).fg(),
                    "char at " + i + " should use theme foreground");
        }
    }

    @Test
    void resolverChangesAfterSetTextPickedUpOnRerender() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        box.setFocused(false);

        // First render with no resolver
        var buf1 = drawBox(box, 20);
        var theme = ThemeManager.active();
        assertEquals(theme.foreground(), buf1.getCell(0, 0).fg());

        // Set resolver and re-render
        box.setStyleResolver((charIndex, c, defaultStyle) ->
                defaultStyle.withForeground(AnsiColor.GREEN));
        var buf2 = drawBox(box, 20);
        assertEquals(AnsiColor.GREEN, buf2.getCell(0, 0).fg());
        assertEquals(AnsiColor.GREEN, buf2.getCell(4, 0).fg());
    }

    @Test
    void getStyleResolverReturnsNullByDefault() {
        var box = new TextBox(20);
        assertNull(box.getStyleResolver());
    }

    @Test
    void setStyleResolverToNullClearsIt() {
        var box = new TextBox(20);
        box.setStyleResolver((charIndex, c, defaultStyle) ->
                defaultStyle.withForeground(AnsiColor.YELLOW));
        assertNotNull(box.getStyleResolver());
        box.setStyleResolver(null);
        assertNull(box.getStyleResolver());
    }

    @Test
    void resolverColorsOnlyFirstTwoChars() {
        var box = new TextBox(20);
        box.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        box.setValue("Hello");
        box.setFocused(false);
        box.setStyleResolver((charIndex, c, defaultStyle) ->
                charIndex < 2 ? defaultStyle.withForeground(AnsiColor.RED) : null);
        var buf = drawBox(box, 20);
        var theme = ThemeManager.active();
        assertEquals(AnsiColor.RED, buf.getCell(0, 0).fg());
        assertEquals(AnsiColor.RED, buf.getCell(1, 0).fg());
        assertEquals(theme.foreground(), buf.getCell(2, 0).fg());
        assertEquals(theme.foreground(), buf.getCell(3, 0).fg());
        assertEquals(theme.foreground(), buf.getCell(4, 0).fg());
    }

    private ScreenBuffer drawBox(TextBox box, int columns) {
        var size = new TerminalSize(columns, 1);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        box.draw(g);
        return buf;
    }
}