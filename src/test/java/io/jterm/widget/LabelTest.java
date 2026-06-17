package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LabelTest {
    @Test
    void preferredSizeMatchesText() {
        var label = new Label("Hello");
        assertEquals(new TerminalSize(5, 1), label.getPreferredSize());
    }

    @Test
    void drawStringWritesCells() {
        var label = new Label("Hi");
        label.setBounds(new io.jterm.core.TerminalPosition(0, 0), new TerminalSize(10, 1));
        var buf = new ScreenBuffer(new TerminalSize(10, 1));
        label.draw(new TextGraphics(buf));
        assertEquals('H', buf.getCell(4, 0).character().charAt(0));
        assertEquals('i', buf.getCell(5, 0).character().charAt(0));
    }
}
