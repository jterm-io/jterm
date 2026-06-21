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

    // ── drawLineSmooth ─────────────────────────────────────────

    @Test
    void drawLineSmoothHorizontalUsesDashes() {
        var buf = new ScreenBuffer(new TerminalSize(10, 5));
        var g = new TextGraphics(buf);
        g.drawLineSmooth(0, 2, 4, 2, new TextCell(' '));
        for (int i = 0; i < 5; i++) {
            assertEquals('-', buf.getCell(i, 2).character().charAt(0),
                "horizontal line should use '-' at column " + i);
        }
    }

    @Test
    void drawLineSmoothVerticalUsesPipes() {
        var buf = new ScreenBuffer(new TerminalSize(5, 10));
        var g = new TextGraphics(buf);
        g.drawLineSmooth(2, 0, 2, 4, new TextCell(' '));
        for (int r = 0; r < 5; r++) {
            assertEquals('|', buf.getCell(2, r).character().charAt(0),
                "vertical line should use '|' at row " + r);
        }
    }

    @Test
    void drawLineSmoothPerfectDiagonalUsesBackslash() {
        var buf = new ScreenBuffer(new TerminalSize(10, 10));
        var g = new TextGraphics(buf);
        g.drawLineSmooth(0, 0, 4, 4, new TextCell(' '));
        for (int i = 0; i < 5; i++) {
            assertEquals('\\', buf.getCell(i, i).character().charAt(0),
                "45° diagonal should use '\\' at (" + i + "," + i + ")");
        }
    }

    @Test
    void drawLineSmoothShallowDiagonalUsesDotsAtCorners() {
        var buf = new ScreenBuffer(new TerminalSize(20, 8));
        var g = new TextGraphics(buf);
        g.drawLineSmooth(0, 0, 19, 5, new TextCell(' '));
        // The line should contain at least some '.' characters at transition points
        boolean hasDot = false;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 20; c++) {
                if (buf.getCell(c, r).character().charAt(0) == '.') hasDot = true;
            }
        }
        assertTrue(hasDot, "shallow diagonal should have '.' at corner transitions");
    }

    @Test
    void drawLineSmoothPreservesColors() {
        var buf = new ScreenBuffer(new TerminalSize(10, 5));
        var g = new TextGraphics(buf);
        var cell = new TextCell(' ', AnsiColor.RED, AnsiColor.BLUE);
        g.drawLineSmooth(0, 0, 4, 0, cell);
        var c0 = buf.getCell(0, 0);
        assertEquals(AnsiColor.RED, c0.fg());
        assertEquals(AnsiColor.BLUE, c0.bg());
    }

    @Test
    void drawLineSmoothSinglePoint() {
        var buf = new ScreenBuffer(new TerminalSize(5, 5));
        var g = new TextGraphics(buf);
        g.drawLineSmooth(2, 2, 2, 2, new TextCell(' '));
        // Single point — no neighbours, should produce '*'
        assertEquals('*', buf.getCell(2, 2).character().charAt(0));
    }
}
