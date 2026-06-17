package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BordersTest {

    private TextGraphics graphicsFor(TerminalSize size) {
        return new TextGraphics(new ScreenBuffer(size));
    }

    @Test
    void singleLineDrawsCorrectCorners() {
        var label = new Label("");
        var border = Borders.singleLine(label);
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(4, 4));
        var g = graphicsFor(border.getSize());
        border.draw(g);

        assertEquals('┌', g.getCell(0, 0).character().charAt(0));
        assertEquals('┐', g.getCell(3, 0).character().charAt(0));
        assertEquals('└', g.getCell(0, 3).character().charAt(0));
        assertEquals('┘', g.getCell(3, 3).character().charAt(0));
        assertEquals('─', g.getCell(1, 0).character().charAt(0));
        assertEquals('│', g.getCell(0, 1).character().charAt(0));
    }

    @Test
    void doubleLineDrawsCorrectCorners() {
        var label = new Label("");
        var border = Borders.doubleLine(label);
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(4, 4));
        var g = graphicsFor(border.getSize());
        border.draw(g);

        assertEquals('╔', g.getCell(0, 0).character().charAt(0));
        assertEquals('╗', g.getCell(3, 0).character().charAt(0));
        assertEquals('╚', g.getCell(0, 3).character().charAt(0));
        assertEquals('╝', g.getCell(3, 3).character().charAt(0));
    }

    @Test
    void roundedDrawsRoundedCorners() {
        var label = new Label("");
        var border = Borders.rounded(label);
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(4, 4));
        var g = graphicsFor(border.getSize());
        border.draw(g);

        assertEquals('╭', g.getCell(0, 0).character().charAt(0));
        assertEquals('╮', g.getCell(3, 0).character().charAt(0));
        assertEquals('╰', g.getCell(0, 3).character().charAt(0));
        assertEquals('╯', g.getCell(3, 3).character().charAt(0));
    }

    @Test
    void emptyBorderLeavesInnerAreaUntouched() {
        var inner = new EmptySpace(new TerminalSize(4, 3));
        var border = Borders.empty(inner);
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(4, 3));
        assertEquals(new TerminalSize(4, 3), inner.getSize());
        assertEquals(TerminalPosition.TOP_LEFT, inner.getPosition());
        assertEquals(new TerminalSize(4, 3), border.getPreferredSize());
    }

    @Test
    void borderReducesInnerSize() {
        var label = new Label("AB");
        var border = Borders.singleLine(label);
        assertEquals(new TerminalSize(4, 3), border.getPreferredSize()); // 2+2 x 1+2
    }

    @Test
    void titleAppearsInTopLine() {
        var label = new Label("");
        var border = Borders.titled(Borders.singleLine(label), "Hi");
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 4));
        var g = graphicsFor(border.getSize());
        border.draw(g);

        assertEquals('H', g.getCell(3, 0).character().charAt(0));
        assertEquals('i', g.getCell(4, 0).character().charAt(0));
    }
}
