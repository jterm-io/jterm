package io.jterm.graphics;

import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubTextGraphicsTest {
    @Test
    void setCellTranslatesToParent() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 2, 1, 4, 3);
        sub.setCell(0, 0, new TextCell('A'));
        assertEquals('A', parent.getCell(2, 1).character().charAt(0));
    }

    @Test
    void setCellIgnoresOutsideSubArea() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 0, 0, 2, 2);
        sub.setCell(2, 0, new TextCell('A'));
        assertEquals(' ', parent.getCell(2, 0).character().charAt(0));
    }

    @Test
    void getCellReadsFromParent() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        parent.setCell(3, 2, new TextCell('A'));
        var sub = new SubTextGraphics(parent, 1, 1, 5, 3);
        assertEquals('A', sub.getCell(2, 1).character().charAt(0));
    }

    @Test
    void getCellReturnsEmptyOutsideBounds() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 0, 0, 2, 2);
        assertEquals(TextCell.EMPTY, sub.getCell(3, 3));
    }

    @Test
    void fillRectangleClipsToSubArea() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 1, 1, 3, 3);
        sub.fillRectangle(0, 0, 10, 10, new TextCell('X'));
        assertEquals('X', parent.getCell(1, 1).character().charAt(0));
        assertEquals('X', parent.getCell(3, 3).character().charAt(0));
        assertEquals(' ', parent.getCell(4, 4).character().charAt(0));
    }

    @Test
    void drawStringPassesToParentWithOffset() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = new SubTextGraphics(parent, 1, 1, 5, 3);
        sub.drawString(0, 0, "Hi", new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT));
        assertEquals('H', parent.getCell(1, 1).character().charAt(0));
        assertEquals('i', parent.getCell(2, 1).character().charAt(0));
    }

    @Test
    void getGraphicsReturnsNestedSubGraphics() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = parent.getGraphics(1, 1, 5, 3);
        var nested = sub.getGraphics(1, 1, 2, 2);
        nested.setCell(0, 0, new TextCell('A'));
        assertEquals('A', parent.getCell(2, 2).character().charAt(0));
    }

    @Test
    void getSizeReturnsSubSize() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(20, 10)));
        var sub = new SubTextGraphics(parent, 5, 5, 4, 2);
        assertEquals(new TerminalSize(4, 2), sub.getSize());
    }

    @Test
    void nullParentThrows() {
        assertThrows(IllegalArgumentException.class, () -> new SubTextGraphics(null, 0, 0, 1, 1));
    }
}
