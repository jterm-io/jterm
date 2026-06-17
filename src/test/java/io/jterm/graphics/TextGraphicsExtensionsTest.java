package io.jterm.graphics;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextGraphicsExtensionsTest {
    @Test
    void subGraphicsReturnsClippedGraphics() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = TextGraphicsExtensions.subGraphics(parent, new TerminalPosition(2, 1), new TerminalSize(3, 3));
        sub.setCell(0, 0, new io.jterm.style.TextCell('A'));
        assertEquals('A', parent.getCell(2, 1).character().charAt(0));
    }

    @Test
    void subGraphicsHasClipSize() {
        var parent = new TextGraphics(new ScreenBuffer(new TerminalSize(10, 5)));
        var sub = TextGraphicsExtensions.subGraphics(parent, new TerminalPosition(1, 1), new TerminalSize(4, 2));
        assertEquals(new TerminalSize(4, 2), sub.getSize());
    }
}
