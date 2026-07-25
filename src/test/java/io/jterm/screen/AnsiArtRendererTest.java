package io.jterm.screen;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AnsiArtRenderer}: ANSI art (.ans/.asc) file parsing and rendering.
 */
class AnsiArtRendererTest {

    @Test
    @DisplayName("renders plain text with default colors")
    void rendersPlainText() {
        var size = new TerminalSize(20, 5);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        AnsiArtRenderer.render(g, "Hello");

        assertEquals('H', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('e', buffer.getCell(1, 0).character().charAt(0));
        assertEquals('l', buffer.getCell(2, 0).character().charAt(0));
        assertEquals('l', buffer.getCell(3, 0).character().charAt(0));
        assertEquals('o', buffer.getCell(4, 0).character().charAt(0));
        // Default colors
        assertEquals(AnsiColor.DEFAULT, buffer.getCell(0, 0).fg());
    }

    @Test
    @DisplayName("parses ESC[31m for red foreground")
    void parsesRedForeground() {
        var size = new TerminalSize(20, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        // "Hello" default, then ESC[31m "World" red, then ESC[0m "!" default
        AnsiArtRenderer.render(g, "Hello\u001B[31mWorld\u001B[0m!");

        assertEquals('H', buffer.getCell(0, 0).character().charAt(0));
        assertEquals(AnsiColor.DEFAULT, buffer.getCell(0, 0).fg());

        assertEquals('W', buffer.getCell(5, 0).character().charAt(0));
        assertEquals(AnsiColor.RED, buffer.getCell(5, 0).fg());

        assertEquals('!', buffer.getCell(10, 0).character().charAt(0));
        assertEquals(AnsiColor.DEFAULT, buffer.getCell(10, 0).fg());
    }

    @Test
    @DisplayName("parses ESC[1m for bold and ESC[0m to reset")
    void parsesBoldAndReset() {
        var size = new TerminalSize(20, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        AnsiArtRenderer.render(g, "A\u001B[1mB\u001B[0mC");

        assertFalse(buffer.getCell(0, 0).modifiers().contains(io.jterm.style.SGR.BOLD));
        assertTrue(buffer.getCell(1, 0).modifiers().contains(io.jterm.style.SGR.BOLD));
        assertFalse(buffer.getCell(2, 0).modifiers().contains(io.jterm.style.SGR.BOLD));
    }

    @Test
    @DisplayName("newline increments row and resets column")
    void newlineIncrementsRow() {
        var size = new TerminalSize(10, 5);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        AnsiArtRenderer.render(g, "AB\nCD");

        assertEquals('A', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('B', buffer.getCell(1, 0).character().charAt(0));
        assertEquals('C', buffer.getCell(0, 1).character().charAt(0));
        assertEquals('D', buffer.getCell(1, 1).character().charAt(0));
    }

    @Test
    @DisplayName("carriage return resets column to 0")
    void carriageReturnResetsColumn() {
        var size = new TerminalSize(10, 5);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        AnsiArtRenderer.render(g, "AB\rCD");

        // After \r, col resets to 0, so CD overwrites AB
        assertEquals('C', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('D', buffer.getCell(1, 0).character().charAt(0));
        // B was overwritten by D
        assertEquals('D', buffer.getCell(1, 0).character().charAt(0));
    }

    @Test
    @DisplayName("parses 256-color foreground ESC[38;5;Nm")
    void parses256ColorFg() {
        var size = new TerminalSize(20, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        // ESC[38;5;196m = 256-color index 196 (bright red in xterm)
        AnsiArtRenderer.render(g, "\u001B[38;5;196mX\u001B[0mY");

        var xCell = buffer.getCell(0, 0);
        assertEquals('X', xCell.character().charAt(0));
        // Should have a non-default foreground
        assertNotEquals(AnsiColor.DEFAULT, xCell.fg());
    }

    @Test
    @DisplayName("parses 256-color background ESC[48;5;Nm")
    void parses256ColorBg() {
        var size = new TerminalSize(20, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        AnsiArtRenderer.render(g, "\u001B[48;5;21mX\u001B[0mY");

        var xCell = buffer.getCell(0, 0);
        assertEquals('X', xCell.character().charAt(0));
        assertNotEquals(AnsiColor.DEFAULT, xCell.bg());
    }

    @Test
    @DisplayName("parses background color ESC[41m for red bg")
    void parsesBackgroundRed() {
        var size = new TerminalSize(20, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        AnsiArtRenderer.render(g, "\u001B[41mX\u001B[0mY");

        assertEquals(AnsiColor.RED, buffer.getCell(0, 0).bg());
        assertEquals(AnsiColor.DEFAULT, buffer.getCell(1, 0).bg());
    }

    @Test
    @DisplayName("renders at offset position")
    void rendersAtOffset() {
        var size = new TerminalSize(20, 5);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        AnsiArtRenderer.render(g, "AB\nCD", 5, 2);

        assertEquals('A', buffer.getCell(5, 2).character().charAt(0));
        assertEquals('B', buffer.getCell(6, 2).character().charAt(0));
        assertEquals('C', buffer.getCell(5, 3).character().charAt(0));
        assertEquals('D', buffer.getCell(6, 3).character().charAt(0));
    }

    @Test
    @DisplayName("handles combined SGR codes like ESC[1;31m")
    void handlesCombinedSgr() {
        var size = new TerminalSize(20, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        AnsiArtRenderer.render(g, "\u001B[1;31mX\u001B[0mY");

        var xCell = buffer.getCell(0, 0);
        assertEquals('X', xCell.character().charAt(0));
        assertEquals(AnsiColor.RED, xCell.fg());
        assertTrue(xCell.modifiers().contains(io.jterm.style.SGR.BOLD));
    }

    @Test
    @DisplayName("handles ESC[7m reverse video")
    void handlesReverseVideo() {
        var size = new TerminalSize(20, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        AnsiArtRenderer.render(g, "\u001B[7mX\u001B[0mY");

        var xCell = buffer.getCell(0, 0);
        assertTrue(xCell.modifiers().contains(io.jterm.style.SGR.REVERSE));
    }

    @Test
    @DisplayName("fromFile loads ANSI art from file")
    void fromFileLoadsAnsiArt() {
        try {
            var tempFile = Files.createTempFile("jterm-test", ".ans");
            Files.writeString(tempFile, "Hello\u001B[31mWorld\u001B[0m!");
            String content = AnsiArtRenderer.fromFile(tempFile);
            assertTrue(content.contains("Hello"));
            assertTrue(content.contains("\u001B[31m"));
            assertTrue(content.contains("World"));
            Files.deleteIfExists(tempFile);
        } catch (Exception e) {
            fail("fromFile test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("empty string renders nothing")
    void emptyStringRendersNothing() {
        var size = new TerminalSize(10, 5);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        assertDoesNotThrow(() -> AnsiArtRenderer.render(g, ""));
    }

    @Test
    @DisplayName("handles bright colors ESC[9Xm (90-97)")
    void handlesBrightColors() {
        var size = new TerminalSize(20, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        // ESC[91m = bright red
        AnsiArtRenderer.render(g, "\u001B[91mX\u001B[0mY");

        assertEquals(io.jterm.style.AnsiColor.BRIGHT_RED, buffer.getCell(0, 0).fg());
    }

    @Test
    @DisplayName("clips text at terminal boundary")
    void clipsAtBoundary() {
        var size = new TerminalSize(3, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        // "ABCDE" should be clipped to "ABC"
        AnsiArtRenderer.render(g, "ABCDE");

        assertEquals('A', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('B', buffer.getCell(1, 0).character().charAt(0));
        assertEquals('C', buffer.getCell(2, 0).character().charAt(0));
    }

    @Test
    @DisplayName("ESC[0m resets all attributes")
    void resetClearsAll() {
        var size = new TerminalSize(20, 1);
        var buffer = new ScreenBuffer(size);
        var g = new TextGraphics(buffer);

        AnsiArtRenderer.render(g, "\u001B[1;31;42mA\u001B[0mB");

        var aCell = buffer.getCell(0, 0);
        assertEquals(AnsiColor.RED, aCell.fg());
        assertEquals(AnsiColor.GREEN, aCell.bg());
        assertTrue(aCell.modifiers().contains(io.jterm.style.SGR.BOLD));

        var bCell = buffer.getCell(1, 0);
        assertEquals(AnsiColor.DEFAULT, bCell.fg());
        assertEquals(AnsiColor.DEFAULT, bCell.bg());
        assertTrue(bCell.modifiers().isEmpty());
    }
}