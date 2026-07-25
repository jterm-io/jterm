package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WipeTransition}: top-to-bottom wipe transition.
 */
class WipeTransitionTest {

    @Test
    @DisplayName("implements TransitionEffect")
    void implementsTransitionEffect() {
        assertTrue(new WipeTransition(500, new ScreenBuffer(new TerminalSize(1, 1))) instanceof TransitionEffect);
    }

    @Test
    @DisplayName("durationMs returns configured value")
    void durationMsReturnsConfigured() {
        assertEquals(500, new WipeTransition(500, new ScreenBuffer(new TerminalSize(1, 1))).durationMs());
    }

    @Test
    @DisplayName("progress 0.0 shows old content")
    void progress0ShowsOldContent() {
        var size = new TerminalSize(4, 4);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        // New content = 'A'
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var wipe = new WipeTransition(500, oldBuffer);
        wipe.renderFrame(graphics, size, 0.0);

        // At progress 0, wipe line is at row 0 — old content should be visible (except the sweep line)
        // Row 0 has the sweep line, rows 1-3 should be old
        assertEquals('B', graphics.getCell(0, 1).character().charAt(0));
        assertEquals('B', graphics.getCell(0, 2).character().charAt(0));
    }

    @Test
    @DisplayName("progress 0.5 shows new content on top half, old on bottom half")
    void progress5ShowsTopNewBottomOld() {
        var size = new TerminalSize(4, 4);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var wipe = new WipeTransition(500, oldBuffer);
        wipe.renderFrame(graphics, size, 0.5);

        // At progress 0.5, wipe line at row = 0.5 * 4 = 2
        // Rows 0-1: new content ('A'), row 2: sweep line, row 3: old content ('B')
        assertEquals('A', graphics.getCell(0, 0).character().charAt(0));
        assertEquals('A', graphics.getCell(0, 1).character().charAt(0));
        assertEquals('B', graphics.getCell(0, 3).character().charAt(0));
    }

    @Test
    @DisplayName("progress 1.0 shows new content")
    void progress1ShowsNewContent() {
        var size = new TerminalSize(4, 4);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var wipe = new WipeTransition(500, oldBuffer);
        wipe.renderFrame(graphics, size, 1.0);

        // At progress 1, wipe line is at bottom or past — all new content
        assertEquals('A', graphics.getCell(0, 0).character().charAt(0));
        assertEquals('A', graphics.getCell(0, 3).character().charAt(0));
    }

    @Test
    @DisplayName("sweep line uses block character")
    void sweepLineUsesBlockChar() {
        var size = new TerminalSize(4, 4);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var wipe = new WipeTransition(500, oldBuffer);
        wipe.renderFrame(graphics, size, 0.25);

        // At progress 0.25, sweep line at row = 0.25 * 4 = 1
        // The sweep line should have block characters
        assertEquals('\u2588', graphics.getCell(0, 1).character().charAt(0));
    }

    @Test
    @DisplayName("handles zero-size terminal gracefully")
    void handlesZeroSize() {
        var size = TerminalSize.ZERO;
        var graphics = new TextGraphics(new ScreenBuffer(new TerminalSize(1, 1)));
        var oldBuffer = new ScreenBuffer(new TerminalSize(1, 1));
        var wipe = new WipeTransition(500, oldBuffer);
        assertDoesNotThrow(() -> wipe.renderFrame(graphics, size, 0.5));
    }
}