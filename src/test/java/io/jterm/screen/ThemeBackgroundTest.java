package io.jterm.screen;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.style.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the screen background respects the active theme.
 * Bug: DefaultScreen.clear() filled with TextCell.EMPTY (AnsiColor.DEFAULT),
 * ignoring the theme background — so the main window was always black.
 */
class ThemeBackgroundTest {

    @AfterEach
    void resetTheme() {
        ThemeManager.setActive(Theme.DARK);
    }

    @Test
    void clearUsesThemeBackground() {
        ThemeManager.setActive(Theme.YELLOW_ON_BLUE);
        var screen = new DefaultScreen(new TerminalSize(10, 5));
        screen.setCell(0, 0, new TextCell('X'));
        screen.clear();
        var cell = screen.getBackCell(0, 0);
        // After clear, cells should have the theme background, not AnsiColor.DEFAULT
        assertEquals(Theme.YELLOW_ON_BLUE.background(), cell.bg(),
                "clear() should fill with theme background, not AnsiColor.DEFAULT");
        assertEquals(Theme.YELLOW_ON_BLUE.foreground(), cell.fg(),
                "clear() should fill with theme foreground, not AnsiColor.DEFAULT");
    }

    @Test
    void clearUsesDarkThemeBackground() {
        ThemeManager.setActive(Theme.DARK);
        var screen = new DefaultScreen(new TerminalSize(10, 5));
        screen.clear();
        var cell = screen.getBackCell(3, 2);
        assertEquals(AnsiColor.BLACK, cell.bg(),
                "Dark theme background should be BLACK");
        assertEquals(AnsiColor.WHITE, cell.fg(),
                "Dark theme foreground should be WHITE");
    }

    @Test
    void clearUsesGreenOnBlackThemeBackground() {
        ThemeManager.setActive(Theme.GREEN_ON_BLACK);
        var screen = new DefaultScreen(new TerminalSize(10, 5));
        screen.clear();
        var cell = screen.getBackCell(5, 3);
        assertEquals(AnsiColor.BLACK, cell.bg(),
                "Green-on-black theme background should be BLACK");
        assertEquals(AnsiColor.BRIGHT_GREEN, cell.fg(),
                "Green-on-black theme foreground should be BRIGHT_GREEN");
    }

    @Test
    void clearUsesWhiteOnGreenThemeBackground() {
        ThemeManager.setActive(Theme.WHITE_ON_GREEN);
        var screen = new DefaultScreen(new TerminalSize(10, 5));
        screen.clear();
        var cell = screen.getBackCell(7, 1);
        assertEquals(AnsiColor.BRIGHT_GREEN, cell.bg(),
                "White-on-green theme background should be BRIGHT_GREEN");
        assertEquals(AnsiColor.BLACK, cell.fg(),
                "White-on-green theme foreground should be BLACK");
    }

    @Test
    void completeRefreshEmitsSgrResetToTerminal() throws Exception {
        // Bug: doCompleteRefresh() called sgrState.reset() but discarded the
        // returned reset bytes — the terminal kept its stale SGR state.
        // This test verifies the reset sequence (\033[0m) is written to the terminal.
        var mockTerm = new MockTerminal(new TerminalSize(5, 3));
        var screen = new DefaultScreen(mockTerm);
        screen.startScreen();

        // First render with a colored cell (e.g. white-on-blue selection)
        screen.clear();
        screen.setCell(0, 0, new TextCell('A', AnsiColor.WHITE, AnsiColor.BLUE));
        screen.refresh(RefreshType.COMPLETE);
        mockTerm.clearOutput();

        // Second complete refresh — should emit SGR reset at start
        screen.refresh(RefreshType.COMPLETE);
        String output = mockTerm.getOutput();
        assertTrue(output.contains("\033[0m"),
                "Complete refresh must emit SGR reset (\\033[0m) to terminal, got: " + output);
    }

    @Test
    void completeRefreshEmitsResetBeforeRenderingCells() throws Exception {
        // The reset must come BEFORE cell output, so the terminal's stale state
        // is cleared before new cells are written.
        var mockTerm = new MockTerminal(new TerminalSize(5, 3));
        var screen = new DefaultScreen(mockTerm);
        screen.startScreen();

        // Render with a non-default colored cell first
        screen.setCell(0, 0, new TextCell('X', AnsiColor.RED, AnsiColor.YELLOW));
        screen.refresh(RefreshType.COMPLETE);
        mockTerm.clearOutput();

        // Second complete refresh
        screen.refresh(RefreshType.COMPLETE);
        String output = mockTerm.getOutput();

        int resetPos = output.indexOf("\033[0m");
        int firstCharPos = output.indexOf('X');
        assertTrue(resetPos >= 0, "Reset sequence must be present");
        assertTrue(firstCharPos >= 0, "Cell character must be present");
        assertTrue(resetPos < firstCharPos,
                "Reset must come before cell characters. reset@=" + resetPos + " char@=" + firstCharPos);
    }
}