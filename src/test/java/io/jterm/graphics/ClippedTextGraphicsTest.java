package io.jterm.graphics;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClippedTextGraphicsTest {
    @Test
    void writesWithinClipArea() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var clipped = new ClippedTextGraphics(parent, new TerminalPosition(2, 1), new TerminalSize(3, 3));
        clipped.setCell(0, 0, new TextCell('A'));
        assertEquals('A', parent.getCell(2, 1).character().charAt(0));
    }

    @Test
    void ignoresCellsOutsideClip() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var clipped = new ClippedTextGraphics(parent, new TerminalPosition(2, 1), new TerminalSize(3, 3));
        clipped.setCell(5, 5, new TextCell('A'));
        assertEquals(' ', parent.getCell(7, 6).character().charAt(0));
    }

    @Test
    void fillRectangleRespectsClip() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var clipped = new ClippedTextGraphics(parent, new TerminalPosition(1, 1), new TerminalSize(3, 3));
        clipped.fillRectangle(0, 0, 5, 5, new TextCell('X'));
        assertEquals('X', parent.getCell(1, 1).character().charAt(0));
        assertEquals('X', parent.getCell(3, 3).character().charAt(0));
        assertEquals(' ', parent.getCell(4, 4).character().charAt(0));
    }

    @Test
    void drawStringClipsHorizontally() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var clipped = new ClippedTextGraphics(parent, new TerminalPosition(0, 0), new TerminalSize(3, 1));
        clipped.drawString(0, 0, "Hello", new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT));
        assertEquals('H', parent.getCell(0, 0).character().charAt(0));
        assertEquals('e', parent.getCell(1, 0).character().charAt(0));
        assertEquals('l', parent.getCell(2, 0).character().charAt(0));
        assertEquals(' ', parent.getCell(3, 0).character().charAt(0));
    }

    @Test
    void getSizeReturnsClipSize() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var clipped = new ClippedTextGraphics(parent, new TerminalPosition(5, 5), new TerminalSize(4, 2));
        assertEquals(new TerminalSize(4, 2), clipped.getSize());
    }
}
