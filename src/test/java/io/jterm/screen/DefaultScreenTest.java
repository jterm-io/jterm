package io.jterm.screen;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.style.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultScreenTest {
    @Test
    void setCellAffectsBackBuffer() {
        var screen = new DefaultScreen(new TerminalSize(10, 5));
        screen.setCell(2, 3, new TextCell('X'));
        assertEquals('X', screen.getBackCell(2, 3).character().charAt(0));
    }

    @Test
    void clearResetsBackBuffer() {
        var screen = new DefaultScreen(new TerminalSize(10, 5));
        screen.setCell(0, 0, new TextCell('X'));
        screen.clear();
        // clear() now fills with theme colors (DARK theme = WHITE on BLACK by default)
        var cell = screen.getBackCell(0, 0);
        assertEquals(' ', cell.character().charAt(0));
        assertEquals(AnsiColor.WHITE, cell.fg());
        assertEquals(AnsiColor.BLACK, cell.bg());
    }

    @Test
    void deltaRefreshOnlyOutputsChangedCells() throws Exception {
        var mockTerm = new MockTerminal(new TerminalSize(10, 5));
        var screen = new DefaultScreen(mockTerm);
        screen.startScreen();
        screen.refresh(RefreshType.COMPLETE);
        mockTerm.clearOutput();
        screen.setCell(3, 2, new TextCell('X'));
        screen.refresh(RefreshType.DELTA);
        String output = mockTerm.getOutput();
        assertTrue(output.contains("X"));
        assertTrue(output.contains("\033[3;4H"));
    }

    @Test
    void noChangesProducesNoOutput() throws Exception {
        var mockTerm = new MockTerminal(new TerminalSize(10, 5));
        var screen = new DefaultScreen(mockTerm);
        screen.startScreen();
        screen.refresh(RefreshType.COMPLETE);
        mockTerm.clearOutput();
        screen.refresh(RefreshType.DELTA);
        assertTrue(mockTerm.getOutput().isEmpty());
    }
}
