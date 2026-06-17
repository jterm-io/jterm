package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmptySpaceTest {
    @Test
    void defaultPreferredSizeOneByOne() {
        assertEquals(new TerminalSize(1, 1), new EmptySpace().getPreferredSize());
    }

    @Test
    void customPreferredSize() {
        var size = new TerminalSize(4, 3);
        assertEquals(size, new EmptySpace(size).getPreferredSize());
    }

    @Test
    void drawFillsArea() {
        var buffer = new ScreenBuffer(new TerminalSize(4, 3));
        var space = new EmptySpace(new TerminalSize(4, 3));
        space.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(4, 3));
        space.draw(new TextGraphics(buffer));
        assertEquals(' ', buffer.getCell(0, 0).character().charAt(0));
        assertEquals(' ', buffer.getCell(3, 2).character().charAt(0));
    }

    @Test
    void hiddenSpaceNotDrawn() {
        var buffer = new ScreenBuffer(new TerminalSize(2, 2));
        var space = new EmptySpace(new TerminalSize(2, 2));
        space.setVisible(false);
        space.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(2, 2));
        space.draw(new TextGraphics(buffer));
        assertEquals(' ', buffer.getCell(0, 0).character().charAt(0));
    }
}
