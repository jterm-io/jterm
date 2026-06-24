package io.jterm.graphics;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers public graphics methods with zero line coverage.
 */
class GraphicsCoverageGapsTest {

    @Test
    void textGraphicsPutCell() {
        var buffer = new ScreenBuffer(new TerminalSize(5, 3));
        var g = new TextGraphics(buffer);
        var cell = new TextCell('X', AnsiColor.WHITE, AnsiColor.BLACK);
        g.putCell(2, 1, cell);
        assertEquals('X', buffer.getCell(2, 1).character().charAt(0));
    }

    @Test
    void subTextGraphicsDrawLineSmoothDelegatesToParent() {
        var parent = new ScreenBuffer(new TerminalSize(10, 10));
        var g = new TextGraphics(parent);
        var sub = new SubTextGraphics(g, 1, 1, 5, 5);
        var cell = new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLACK);
        sub.drawLineSmooth(0, 0, 3, 3, cell);
        assertNotEquals(' ', parent.getCell(1, 1).character().charAt(0));
    }

    @Test
    void clippedTextGraphicsDrawRectangle() {
        var parent = new ScreenBuffer(new TerminalSize(10, 10));
        var g = new ClippedTextGraphics(new TextGraphics(parent), TerminalPosition.TOP_LEFT, new TerminalSize(5, 5));
        var cell = new TextCell('#', AnsiColor.WHITE, AnsiColor.BLACK);
        g.drawRectangle(0, 0, 4, 4, cell);
        assertEquals('#', parent.getCell(0, 0).character().charAt(0));
        assertEquals('#', parent.getCell(3, 0).character().charAt(0));
        assertEquals('#', parent.getCell(0, 3).character().charAt(0));
        assertEquals('#', parent.getCell(3, 3).character().charAt(0));
    }
}
