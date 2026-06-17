package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BorderTest {
    @Test
    void preferredSizeAddsBorder() {
        var border = new Border(new EmptySpace(new TerminalSize(4, 3)));
        assertEquals(new TerminalSize(6, 5), border.getPreferredSize());
    }

    @Test
    void innerChildPositionedOneOne() {
        var border = new Border(new EmptySpace(new TerminalSize(4, 3)));
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(6, 5));
        var child = border.getChildren().get(0);
        assertEquals(new TerminalPosition(1, 1), child.getPosition());
        assertEquals(new TerminalSize(4, 3), child.getSize());
    }

    @Test
    void drawBorderRendersCorners() {
        var buffer = new ScreenBuffer(new TerminalSize(6, 5));
        var border = new Border(new EmptySpace(new TerminalSize(4, 3)));
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(6, 5));
        border.draw(new TextGraphics(buffer));
        assertEquals('┌', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('┐', buffer.getCell(5, 0).character().charAt(0));
        assertEquals('└', buffer.getCell(0, 4).character().charAt(0));
        assertEquals('┘', buffer.getCell(5, 4).character().charAt(0));
    }

    @Test
    void titleRendersInTopBorder() {
        var buffer = new ScreenBuffer(new TerminalSize(12, 5));
        var border = new Border(new EmptySpace(new TerminalSize(4, 3)));
        border.setTitle("Test");
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(12, 5));
        border.draw(new TextGraphics(buffer));
        assertEquals('T', buffer.getCell(3, 0).character().charAt(0));
    }

    @Test
    void doubleLineStyleUsesDoubleCorners() {
        var buffer = new ScreenBuffer(new TerminalSize(6, 5));
        var border = new Border(new EmptySpace(new TerminalSize(4, 3)), Border.BorderStyle.DOUBLE_LINE);
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(6, 5));
        border.draw(new TextGraphics(buffer));
        assertEquals('╔', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    void roundedStyleUsesRoundedCorners() {
        var buffer = new ScreenBuffer(new TerminalSize(6, 5));
        var border = new Border(new EmptySpace(new TerminalSize(4, 3)), Border.BorderStyle.ROUNDED);
        border.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(6, 5));
        border.draw(new TextGraphics(buffer));
        assertEquals('╭', buffer.getCell(0, 0).character().charAt(0));
    }
}
