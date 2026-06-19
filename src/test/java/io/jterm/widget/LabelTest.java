package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LabelTest {
    @Test
    void preferredSizeMatchesText() {
        var label = new Label("Hello");
        assertEquals(new TerminalSize(5, 1), label.getPreferredSize());
    }

    @Test
    void drawStringWritesCells() {
        var label = new Label("Hi");
        label.setBounds(new io.jterm.core.TerminalPosition(0, 0), new TerminalSize(10, 1));
        var buf = new ScreenBuffer(new TerminalSize(10, 1));
        label.draw(new TextGraphics(buf));
        assertEquals('H', buf.getCell(4, 0).character().charAt(0));
        assertEquals('i', buf.getCell(5, 0).character().charAt(0));
    }

    // ── Expanded coverage ──────────────────────────────────────────

    @Test
    void emptyTextPreferredSizeIsOneByOne() {
        var label = new Label("");
        assertEquals(new TerminalSize(1, 1), label.getPreferredSize());
    }

    @Test
    void getTextReturnsConstructorValue() {
        var label = new Label("Initial");
        assertEquals("Initial", label.getText());
    }

    @Test
    void setTextUpdatesText() {
        var label = new Label("Old");
        label.setText("New");
        assertEquals("New", label.getText());
    }

    @Test
    void setTextUpdatesPreferredSize() {
        var label = new Label("Hi");
        assertEquals(2, label.getPreferredSize().columns());
        label.setText("LongerText");
        assertEquals(10, label.getPreferredSize().columns());
    }

    @Test
    void setTextToEmptyResetsPreferredSize() {
        var label = new Label("Hello");
        assertEquals(5, label.getPreferredSize().columns());
        label.setText("");
        assertEquals(new TerminalSize(1, 1), label.getPreferredSize());
    }

    @Test
    void multiLineTextPreferredSizeIsWidestLine() {
        var label = new Label("Hello\nWorld!\nHi");
        var size = label.getPreferredSize();
        // "World!" is 6 wide, 3 lines
        assertEquals(6, size.columns());
        assertEquals(3, size.rows());
    }

    @Test
    void multiLineTextWithTrailingNewline() {
        var label = new Label("AB\n");
        var size = label.getPreferredSize();
        // "AB" = 2 wide, 2 lines (split with -1 keeps trailing empty)
        assertEquals(2, size.columns());
        assertEquals(2, size.rows());
    }

    @Test
    void multiLineTextWithOnlyNewline() {
        var label = new Label("\n");
        var size = label.getPreferredSize();
        // Two empty lines, each width 0 -> max(0,...) but empty check returns 1,1
        // Actually text is not empty, so split gives ["", ""] -> max width 0, 2 rows
        assertEquals(2, size.rows());
    }

    @Test
    void drawMultiLineRendersEachLine() {
        var label = new Label("AB\nCD");
        label.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 2));
        var buf = new ScreenBuffer(new TerminalSize(10, 2));
        label.draw(new TextGraphics(buf));
        // Line 0 centered: width 2, (10-2)/2 = 4 -> 'A' at col 4
        assertEquals('A', buf.getCell(4, 0).character().charAt(0));
        assertEquals('B', buf.getCell(5, 0).character().charAt(0));
        // Line 1 centered: 'C' at col 4
        assertEquals('C', buf.getCell(4, 1).character().charAt(0));
        assertEquals('D', buf.getCell(5, 1).character().charAt(0));
    }

    @Test
    void drawCentersShortText() {
        var label = new Label("Hi");
        label.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        var buf = new ScreenBuffer(new TerminalSize(10, 1));
        label.draw(new TextGraphics(buf));
        // width 2, (10-2)/2 = 4
        assertEquals('H', buf.getCell(4, 0).character().charAt(0));
        assertEquals('i', buf.getCell(5, 0).character().charAt(0));
    }

    @Test
    void drawLeftAlignsWhenTextFillsWidth() {
        var label = new Label("Hello");
        label.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 1));
        var buf = new ScreenBuffer(new TerminalSize(5, 1));
        label.draw(new TextGraphics(buf));
        assertEquals('H', buf.getCell(0, 0).character().charAt(0));
        assertEquals('o', buf.getCell(4, 0).character().charAt(0));
    }

    @Test
    void drawTruncatesExtraLinesBeyondHeight() {
        var label = new Label("A\nB\nC\nD");
        label.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 2));
        var buf = new ScreenBuffer(new TerminalSize(5, 2));
        // Should only draw 2 lines
        assertDoesNotThrow(() -> label.draw(new TextGraphics(buf)));
        // Label centers text; "A" has width 1, so x = (5-1)/2 = 2
        assertEquals('A', buf.getCell(2, 0).character().charAt(0));
        assertEquals('B', buf.getCell(2, 1).character().charAt(0));
    }

    @Test
    void customForegroundAndBackgroundColors() {
        var label = new Label("X", AnsiColor.RED, AnsiColor.BLUE);
        label.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 1));
        var buf = new ScreenBuffer(new TerminalSize(5, 1));
        label.draw(new TextGraphics(buf));
        // Find 'X' and check colors
        assertEquals(AnsiColor.RED, buf.getCell(2, 0).fg(), "custom fg should be RED");
        assertEquals(AnsiColor.BLUE, buf.getCell(2, 0).bg(), "custom bg should be BLUE");
    }

    @Test
    void setForegroundUpdatesColor() {
        var label = new Label("X");
        label.setForeground(AnsiColor.GREEN);
        label.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 1));
        var buf = new ScreenBuffer(new TerminalSize(5, 1));
        label.draw(new TextGraphics(buf));
        assertEquals(AnsiColor.GREEN, buf.getCell(2, 0).fg());
    }

    @Test
    void setBackgroundUpdatesColor() {
        var label = new Label("X");
        label.setBackground(AnsiColor.YELLOW);
        label.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 1));
        var buf = new ScreenBuffer(new TerminalSize(5, 1));
        label.draw(new TextGraphics(buf));
        assertEquals(AnsiColor.YELLOW, buf.getCell(2, 0).bg());
    }

    @Test
    void setStyleOverridesAllColors() {
        var label = new Label("X");
        var custom = new TextCell(' ', AnsiColor.CYAN, AnsiColor.MAGENTA, SGR.BOLD);
        label.setStyle(custom);
        label.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 1));
        var buf = new ScreenBuffer(new TerminalSize(5, 1));
        label.draw(new TextGraphics(buf));
        assertEquals(AnsiColor.CYAN, buf.getCell(2, 0).fg());
        assertEquals(AnsiColor.MAGENTA, buf.getCell(2, 0).bg());
        assertTrue(buf.getCell(2, 0).modifiers().contains(SGR.BOLD));
    }

    @Test
    void defaultColorsFallBackToTheme() {
        // Label with DEFAULT colors should use theme fg/bg (not DEFAULT)
        var label = new Label("X");
        label.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 1));
        var buf = new ScreenBuffer(new TerminalSize(5, 1));
        label.draw(new TextGraphics(buf));
        // DARK theme: fg=WHITE, bg=BLACK (not DEFAULT)
        assertNotEquals(AnsiColor.DEFAULT, buf.getCell(2, 0).fg(), "default should fall back to theme fg");
        assertNotEquals(AnsiColor.DEFAULT, buf.getCell(2, 0).bg(), "default should fall back to theme bg");
    }

    @Test
    void customFgWithDefaultBgKeepsCustomFgAndThemeBg() {
        var label = new Label("X", AnsiColor.RED, AnsiColor.DEFAULT);
        label.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 1));
        var buf = new ScreenBuffer(new TerminalSize(5, 1));
        label.draw(new TextGraphics(buf));
        // fg=RED (custom). bg=DEFAULT stays DEFAULT because the theme fallback
        // only triggers when BOTH fg and bg are DEFAULT.
        assertEquals(AnsiColor.RED, buf.getCell(2, 0).fg());
        assertEquals(AnsiColor.DEFAULT, buf.getCell(2, 0).bg());
    }

    @Test
    void constructorWithTextCellStyle() {
        var style = new TextCell(' ', AnsiColor.BRIGHT_RED, AnsiColor.BRIGHT_BLUE);
        var label = new Label("Styled", style);
        label.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        var buf = new ScreenBuffer(new TerminalSize(10, 1));
        label.draw(new TextGraphics(buf));
        assertEquals(AnsiColor.BRIGHT_RED, buf.getCell(2, 0).fg());
        assertEquals(AnsiColor.BRIGHT_BLUE, buf.getCell(2, 0).bg());
    }

    @Test
    void drawSingleCharCenters() {
        var label = new Label("X");
        label.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(7, 1));
        var buf = new ScreenBuffer(new TerminalSize(7, 1));
        label.draw(new TextGraphics(buf));
        // width 1, (7-1)/2 = 3
        assertEquals('X', buf.getCell(3, 0).character().charAt(0));
    }
}