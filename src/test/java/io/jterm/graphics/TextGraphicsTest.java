package io.jterm.graphics;

import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextGraphicsTest {
    @Test
    void drawStringWritesCells() {
        var buf = new ScreenBuffer(new TerminalSize(20, 5));
        var g = new TextGraphics(buf);
        g.drawString(1, 2, "Hi", AnsiColor.RED, AnsiColor.DEFAULT, SGR.BOLD);
        assertEquals('H', buf.getCell(1, 2).character().charAt(0));
        assertEquals('i', buf.getCell(2, 2).character().charAt(0));
    }

    @Test
    void fillRectangleFillsRegion() {
        var buf = new ScreenBuffer(new TerminalSize(10, 5));
        var g = new TextGraphics(buf);
        g.fillRectangle(1, 1, 3, 2, new TextCell('#'));
        assertEquals('#', buf.getCell(1, 1).character().charAt(0));
        assertEquals('#', buf.getCell(3, 2).character().charAt(0));
        assertEquals(' ', buf.getCell(4, 2).character().charAt(0));
    }

    @Test
    void drawRectangleDrawsBorder() {
        var buf = new ScreenBuffer(new TerminalSize(10, 5));
        var g = new TextGraphics(buf);
        g.drawRectangle(0, 0, 5, 4, new TextCell('#'));
        assertEquals('#', buf.getCell(0, 0).character().charAt(0));
        assertEquals('#', buf.getCell(4, 3).character().charAt(0));
        assertEquals(' ', buf.getCell(2, 2).character().charAt(0));
    }

    @Test
    void drawLineHorizontal() {
        var buf = new ScreenBuffer(new TerminalSize(10, 5));
        var g = new TextGraphics(buf);
        g.drawLine(0, 2, 4, 2, new TextCell('-'));
        for (int i = 0; i < 5; i++) {
            assertEquals('-', buf.getCell(i, 2).character().charAt(0));
        }
    }
}
