package io.jterm.screen;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultScreenExtraTest {
    @Test
    void refreshWithoutStartCopiesBackToFront() throws Exception {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        screen.setCell(5, 5, new TextCell('X'));
        screen.refresh();
        assertEquals('X', screen.getFrontCell(5, 5).character().charAt(0));
    }

    @Test
    void getBackBufferReturnsWritableBuffer() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var bb = screen.getBackBuffer();
        bb.setCell(1, 1, new TextCell('Y'));
        assertEquals('Y', screen.getBackCell(1, 1).character().charAt(0));
    }

    @Test
    void setCellByPositionWorks() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        screen.setCell(new TerminalPosition(7, 8), new TextCell('Z'));
        assertEquals('Z', screen.getBackCell(7, 8).character().charAt(0));
    }

    @Test
    void refreshCompleteMarksAllUpdated() throws Exception {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(10, 10)));
        screen.setCell(0, 0, new TextCell('A'));
        screen.refresh(RefreshType.COMPLETE);
        assertEquals('A', screen.getFrontCell(0, 0).character().charAt(0));
    }

    @Test
    void refreshDeltaMarksUpdated() throws Exception {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(10, 10)));
        screen.refresh(RefreshType.COMPLETE);
        screen.setCell(0, 0, new TextCell('B'));
        screen.refresh(RefreshType.DELTA);
        assertEquals('B', screen.getFrontCell(0, 0).character().charAt(0));
    }
}
