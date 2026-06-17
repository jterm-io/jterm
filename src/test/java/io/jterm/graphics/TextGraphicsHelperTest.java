package io.jterm.graphics;

import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextGraphicsHelperTest {
    @Test
    void createWrapsBuffer() {
        var buffer = new ScreenBuffer(new TerminalSize(5, 3));
        var g = TextGraphicsHelper.create(buffer);
        assertEquals(new TerminalSize(5, 3), g.getSize());
    }

    @Test
    void subClipsToParentSize() {
        var g = new TextGraphics(new ScreenBuffer(new TerminalSize(5, 4)));
        var sub = TextGraphicsHelper.sub(g, 1, 1, 10, 10);
        assertEquals(new TerminalSize(4, 3), sub.getSize());
    }

    @Test
    void subAtRightEdge() {
        var g = new TextGraphics(new ScreenBuffer(new TerminalSize(5, 4)));
        var sub = TextGraphicsHelper.sub(g, 5, 0, 2, 2);
        assertEquals(new TerminalSize(0, 2), sub.getSize());
    }
}
