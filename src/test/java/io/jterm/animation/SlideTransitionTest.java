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
 * Tests for {@link SlideTransition}: slide old screen up, new enters from bottom.
 */
class SlideTransitionTest {

    @Test
    @DisplayName("implements TransitionEffect")
    void implementsTransitionEffect() {
        assertTrue(new SlideTransition(500, new ScreenBuffer(new TerminalSize(1, 1))) instanceof TransitionEffect);
    }

    @Test
    @DisplayName("durationMs returns configured value")
    void durationMsReturnsConfigured() {
        assertEquals(500, new SlideTransition(500, new ScreenBuffer(new TerminalSize(1, 1))).durationMs());
    }

    @Test
    @DisplayName("progress 0.0 shows old content")
    void progress0ShowsOldContent() {
        var size = new TerminalSize(4, 4);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        // New content
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var slide = new SlideTransition(500, oldBuffer);
        slide.renderFrame(graphics, size, 0.0);

        // At progress 0, old content should be fully visible
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                assertEquals('B', graphics.getCell(c, r).character().charAt(0));
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

        var slide = new SlideTransition(500, oldBuffer);
        slide.renderFrame(graphics, size, 1.0);

        // At progress 1, new content should be fully visible
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                assertEquals('A', graphics.getCell(c, r).character().charAt(0));
    }

    @Test
    @DisplayName("progress 0.5 shifts old up by half, new from bottom half")
    void progress5ShiftsHalf() {
        var size = new TerminalSize(2, 4);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        // New content: 'A' everywhere
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        // Old content: distinct per row for verification
        var oldBuffer = new ScreenBuffer(size);
        char[] oldChars = {'B', 'C', 'D', 'E'};
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell(oldChars[r], AnsiColor.GREEN, AnsiColor.BLACK));

        var slide = new SlideTransition(500, oldBuffer);
        slide.renderFrame(graphics, size, 0.5);

        // At progress 0.5, shift = 0.5 * 4 = 2 rows
        // Old rows shift up by 2: old row 0 → gone, row 1 → gone, row 2 → screen row 0, row 3 → screen row 1
        // New content enters from bottom: rows 2-3 are new
        assertEquals(oldChars[2], graphics.getCell(0, 0).character().charAt(0), "row 0 = old row 2");
        assertEquals(oldChars[3], graphics.getCell(0, 1).character().charAt(0), "row 1 = old row 3");
        assertEquals('A', graphics.getCell(0, 2).character().charAt(0), "row 2 = new");
        assertEquals('A', graphics.getCell(0, 3).character().charAt(0), "row 3 = new");
    }

    @Test
    @DisplayName("progress 0.25 shifts old up by quarter, new from bottom")
    void progress25ShiftsQuarter() {
        var size = new TerminalSize(2, 4);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        char[] oldChars = {'B', 'C', 'D', 'E'};
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell(oldChars[r], AnsiColor.GREEN, AnsiColor.BLACK));

        var slide = new SlideTransition(500, oldBuffer);
        slide.renderFrame(graphics, size, 0.25);

        // At progress 0.25, shift = 1 row
        // Old: row 1 → screen row 0, row 2 → screen row 1, row 3 → screen row 2
        // New: row 3
        assertEquals(oldChars[1], graphics.getCell(0, 0).character().charAt(0));
        assertEquals(oldChars[2], graphics.getCell(0, 1).character().charAt(0));
        assertEquals(oldChars[3], graphics.getCell(0, 2).character().charAt(0));
        assertEquals('A', graphics.getCell(0, 3).character().charAt(0));
    }

    @Test
    @DisplayName("handles zero-size terminal gracefully")
    void handlesZeroSize() {
        var size = TerminalSize.ZERO;
        var graphics = new TextGraphics(new ScreenBuffer(new TerminalSize(1, 1)));
        var oldBuffer = new ScreenBuffer(new TerminalSize(1, 1));
        var slide = new SlideTransition(500, oldBuffer);
        assertDoesNotThrow(() -> slide.renderFrame(graphics, size, 0.5));
    }
}