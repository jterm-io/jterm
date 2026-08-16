package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextStyleResolver;
import io.jterm.style.ThemeManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for per-character style resolver integration in {@link TextArea}.
 */
class TextAreaStyledTextTest {

    @Test
    void styleResolverColorsEverythingYellow() {
        var ta = new TextArea("", 20, 5);
        ta.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        ta.setText("Hello\nWorld");
        ta.setFocused(false);
        ta.setStyleResolver((charIndex, c, defaultStyle) ->
                defaultStyle.withForeground(AnsiColor.YELLOW));
        var buf = drawArea(ta, 20, 5);
        // Line 1: "Hello" on row 0
        for (int i = 0; i < 5; i++) {
            assertEquals(AnsiColor.YELLOW, buf.getCell(i, 0).fg(),
                    "line 1 char " + i + " should be yellow");
        }
        // Line 2: "World" on row 1
        for (int i = 0; i < 5; i++) {
            assertEquals(AnsiColor.YELLOW, buf.getCell(i, 1).fg(),
                    "line 2 char " + i + " should be yellow");
        }
    }

    @Test
    void noResolverIsBackwardCompatible() {
        var ta = new TextArea("", 20, 5);
        ta.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        ta.setText("Hello\nWorld");
        ta.setFocused(false);
        var buf = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        for (int i = 0; i < 5; i++) {
            assertEquals(theme.foreground(), buf.getCell(i, 0).fg(),
                    "line 1 char " + i + " should use theme foreground");
        }
    }

    @Test
    void resolverChangesPickedUpOnRerender() {
        var ta = new TextArea("", 20, 5);
        ta.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        ta.setText("Hello");
        ta.setFocused(false);

        // First render with no resolver
        var buf1 = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        assertEquals(theme.foreground(), buf1.getCell(0, 0).fg());

        // Set resolver and re-render
        ta.setStyleResolver((charIndex, c, defaultStyle) ->
                defaultStyle.withForeground(AnsiColor.GREEN));
        var buf2 = drawArea(ta, 20, 5);
        assertEquals(AnsiColor.GREEN, buf2.getCell(0, 0).fg());
        assertEquals(AnsiColor.GREEN, buf2.getCell(4, 0).fg());
    }

    @Test
    void getStyleResolverReturnsNullByDefault() {
        var ta = new TextArea();
        assertNull(ta.getStyleResolver());
    }

    @Test
    void setStyleResolverToNullClearsIt() {
        var ta = new TextArea();
        ta.setStyleResolver((charIndex, c, defaultStyle) ->
                defaultStyle.withForeground(AnsiColor.YELLOW));
        assertNotNull(ta.getStyleResolver());
        ta.setStyleResolver(null);
        assertNull(ta.getStyleResolver());
    }

    @Test
    void resolverColorsSpecificRangeOnMultiLine() {
        var ta = new TextArea("", 20, 5);
        ta.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        ta.setText("Hello\nWorld");
        ta.setFocused(false);
        // Color first 3 chars of each line red
        ta.setStyleResolver((charIndex, c, defaultStyle) ->
                charIndex < 3 ? defaultStyle.withForeground(AnsiColor.RED) : null);
        var buf = drawArea(ta, 20, 5);
        var theme = ThemeManager.active();
        // Line 1: "Hel" red, "lo" default
        assertEquals(AnsiColor.RED, buf.getCell(0, 0).fg());
        assertEquals(AnsiColor.RED, buf.getCell(1, 0).fg());
        assertEquals(AnsiColor.RED, buf.getCell(2, 0).fg());
        assertEquals(theme.foreground(), buf.getCell(3, 0).fg());
        assertEquals(theme.foreground(), buf.getCell(4, 0).fg());
        // Line 2: "Wor" red, "ld" default
        assertEquals(AnsiColor.RED, buf.getCell(0, 1).fg());
        assertEquals(AnsiColor.RED, buf.getCell(1, 1).fg());
        assertEquals(AnsiColor.RED, buf.getCell(2, 1).fg());
        assertEquals(theme.foreground(), buf.getCell(3, 1).fg());
        assertEquals(theme.foreground(), buf.getCell(4, 1).fg());
    }

    private ScreenBuffer drawArea(TextArea ta, int cols, int rows) {
        var size = new TerminalSize(cols, rows);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        ta.draw(g);
        return buf;
    }
}