package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FadeTransition}: fade-through-black screen transition
 * using discrete SGR.DIM steps.
 */
class FadeTransitionTest {

    @Test
    @DisplayName("implements TransitionEffect")
    void implementsTransitionEffect() {
        var fade = new FadeTransition(500, new ScreenBuffer(new TerminalSize(1, 1)));
        assertTrue(fade instanceof TransitionEffect);
    }

    @Test
    @DisplayName("durationMs returns configured value")
    void durationMsReturnsConfigured() {
        assertEquals(500, new FadeTransition(500, new ScreenBuffer(new TerminalSize(1, 1))).durationMs());
    }

    @Test
    @DisplayName("targetFps is 10 (low FPS for terminal I/O)")
    void defaultTargetFps() {
        assertEquals(10, new FadeTransition(500, new ScreenBuffer(new TerminalSize(1, 1))).targetFps());
    }

    @Test
    @DisplayName("progress 0.0 keeps original content visible at full intensity")
    void progress0KeepsOriginal() {
        var size = new TerminalSize(4, 2);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var fade = new FadeTransition(500, oldBuffer);
        fade.renderFrame(graphics, size, 0.0);

        // At progress 0, old content visible at full intensity (no DIM)
        assertEquals('B', graphics.getCell(0, 0).character().charAt(0));
        assertEquals(AnsiColor.GREEN, graphics.getCell(0, 0).fg());
        assertFalse(graphics.getCell(0, 0).modifiers().contains(SGR.DIM));
    }

    @Test
    @DisplayName("progress 0.25 dims old content with SGR.DIM")
    void progress25DimsOldContent() {
        var size = new TerminalSize(2, 1);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        graphics.setCell(0, 0, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        oldBuffer.setCell(0, 0, new TextCell('B', AnsiColor.WHITE, AnsiColor.BLACK));

        var fade = new FadeTransition(500, oldBuffer);
        fade.renderFrame(graphics, size, 0.25);

        // At progress 0.25, old content should have DIM modifier
        var cell = graphics.getCell(0, 0);
        assertEquals('B', cell.character().charAt(0));
        assertTrue(cell.modifiers().contains(SGR.DIM), "should have SGR.DIM modifier");
    }

    @Test
    @DisplayName("progress 0.45 shows old content as black-on-black")
    void progress45IsBlackOld() {
        var size = new TerminalSize(4, 2);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var fade = new FadeTransition(500, oldBuffer);
        fade.renderFrame(graphics, size, 0.45);

        // At progress 0.45 (first half, > 0.40), everything black
        var cell = graphics.getCell(0, 0);
        assertEquals(AnsiColor.BLACK, cell.fg());
        assertEquals(AnsiColor.BLACK, cell.bg());
    }

    @Test
    @DisplayName("progress 0.5 is black (second half starts)")
    void progress5IsBlack() {
        var size = new TerminalSize(4, 2);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var fade = new FadeTransition(500, oldBuffer);
        fade.renderFrame(graphics, size, 0.5);

        // At progress 0.5 (second half, < 0.60), everything black
        var cell = graphics.getCell(0, 0);
        assertEquals(AnsiColor.BLACK, cell.fg());
        assertEquals(AnsiColor.BLACK, cell.bg());
    }

    @Test
    @DisplayName("progress 0.70 dims new content with SGR.DIM")
    void progress70DimsNewContent() {
        var size = new TerminalSize(2, 1);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        graphics.setCell(0, 0, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        oldBuffer.setCell(0, 0, new TextCell('B', AnsiColor.WHITE, AnsiColor.BLACK));

        var fade = new FadeTransition(500, oldBuffer);
        fade.renderFrame(graphics, size, 0.70);

        // At progress 0.70 (second half, 0.60-0.75), new content with DIM
        var cell = graphics.getCell(0, 0);
        assertEquals('A', cell.character().charAt(0));
        assertTrue(cell.modifiers().contains(SGR.DIM), "should have SGR.DIM modifier");
    }

    @Test
    @DisplayName("progress 1.0 shows new content at full intensity")
    void progress1ShowsNewContent() {
        var size = new TerminalSize(4, 2);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var fade = new FadeTransition(500, oldBuffer);
        fade.renderFrame(graphics, size, 1.0);

        // At progress 1.0, new content at full intensity (no DIM)
        assertEquals('A', graphics.getCell(0, 0).character().charAt(0));
        assertEquals(AnsiColor.RED, graphics.getCell(0, 0).fg());
        assertFalse(graphics.getCell(0, 0).modifiers().contains(SGR.DIM));
    }

    @Test
    @DisplayName("handles zero-size terminal gracefully")
    void handlesZeroSize() {
        var size = TerminalSize.ZERO;
        var graphics = new TextGraphics(new ScreenBuffer(new TerminalSize(1, 1)));
        var oldBuffer = new ScreenBuffer(new TerminalSize(1, 1));
        var fade = new FadeTransition(500, oldBuffer);
        assertDoesNotThrow(() -> fade.renderFrame(graphics, size, 0.5));
    }
}