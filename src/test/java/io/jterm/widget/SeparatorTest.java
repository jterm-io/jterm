package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeparatorTest {
    @Test
    void horizontalPreferredSize() {
        var sep = new Separator(false);
        assertEquals(new TerminalSize(1, 1), sep.getPreferredSize());
    }

    @Test
    void verticalPreferredSize() {
        var sep = new Separator(true);
        assertEquals(new TerminalSize(1, 1), sep.getPreferredSize());
    }

    @Test
    void horizontalDrawsLine() {
        var buffer = new ScreenBuffer(new TerminalSize(5, 1));
        var sep = new Separator(false);
        sep.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 1));
        sep.draw(new TextGraphics(buffer));
        assertEquals('─', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('─', buffer.getCell(4, 0).character().charAt(0));
    }

    @Test
    void verticalDrawsLine() {
        var buffer = new ScreenBuffer(new TerminalSize(1, 4));
        var sep = new Separator(true);
        sep.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(1, 4));
        sep.draw(new TextGraphics(buffer));
        assertEquals('│', buffer.getCell(0, 0).character().charAt(0));
        assertEquals('│', buffer.getCell(0, 3).character().charAt(0));
    }
}
