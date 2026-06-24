package io.jterm.graphics;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClippedTextGraphicsTest {
    @Test
    void delegatesSetCellToParentWithOffset() {
        var parent = new ScreenBuffer(new TerminalSize(10, 10));
        var g = new ClippedTextGraphics(new TextGraphics(parent), new TerminalPosition(2, 3), new TerminalSize(4, 4));
        g.setCell(1, 1, new TextCell('X'));
        assertEquals('X', parent.getCell(3, 4).character().charAt(0));
    }

    @Test
    void ignoresOutOfBoundsSetCell() {
        var parent = new ScreenBuffer(new TerminalSize(10, 10));
        var g = new ClippedTextGraphics(new TextGraphics(parent), new TerminalPosition(0, 0), new TerminalSize(2, 2));
        g.setCell(5, 5, new TextCell('X'));
        assertEquals(' ', parent.getCell(5, 5).character().charAt(0));
    }

    @Test
    void drawLineWithinClipUpdatesParent() {
        var parent = new ScreenBuffer(new TerminalSize(10, 10));
        var g = new ClippedTextGraphics(new TextGraphics(parent), new TerminalPosition(1, 1), new TerminalSize(5, 5));
        g.drawLine(0, 0, 2, 0, new TextCell('-'));
        assertEquals('-', parent.getCell(1, 1).character().charAt(0));
        assertEquals('-', parent.getCell(3, 1).character().charAt(0));
    }

    @Test
    void fillRectangleWithinClipUpdatesParent() {
        var parent = new ScreenBuffer(new TerminalSize(10, 10));
        var g = new ClippedTextGraphics(new TextGraphics(parent), new TerminalPosition(2, 2), new TerminalSize(4, 4));
        g.fillRectangle(0, 0, 2, 2, new TextCell('#'));
        assertEquals('#', parent.getCell(2, 2).character().charAt(0));
        assertEquals('#', parent.getCell(3, 3).character().charAt(0));
        assertEquals(' ', parent.getCell(4, 4).character().charAt(0));
    }

    @Test
    void drawStringWithinClipUpdatesParent() {
        var parent = new ScreenBuffer(new TerminalSize(20, 5));
        var g = new ClippedTextGraphics(new TextGraphics(parent), new TerminalPosition(5, 2), new TerminalSize(10, 1));
        g.drawString(0, 0, "hi", new TextCell(' '));
        assertEquals('h', parent.getCell(5, 2).character().charAt(0));
        assertEquals('i', parent.getCell(6, 2).character().charAt(0));
    }
}
