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
 * Tests for {@link FadeTransition}: 3-step fade (old → black → new).
 */
class FadeTransitionTest {

    @Test
    @DisplayName("implements TransitionEffect")
    void implementsTransitionEffect() {
        var fade = new FadeTransition(800, new ScreenBuffer(new TerminalSize(1, 1)));
        assertTrue(fade instanceof TransitionEffect);
    }

    @Test
    @DisplayName("durationMs returns configured value")
    void durationMsReturnsConfigured() {
        assertEquals(800, new FadeTransition(800, new ScreenBuffer(new TerminalSize(1, 1))).durationMs());
    }

    @Test
    @DisplayName("targetFps is 8")
    void targetFpsIs8() {
        assertEquals(8, new FadeTransition(800, new ScreenBuffer(new TerminalSize(1, 1))).targetFps());
    }

    @Test
    @DisplayName("progress 0.0 shows old screen content")
    void progress0ShowsOldContent() {
        var size = new TerminalSize(4, 2);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.0);

        // First third: old content visible
        assertEquals('B', graphics.getCell(0, 0).character().charAt(0));
        assertEquals(AnsiColor.GREEN, graphics.getCell(0, 0).fg());
    }

    @Test
    @DisplayName("progress 0.20 shows old screen content (still first third)")
    void progress20ShowsOldContent() {
        var size = new TerminalSize(4, 2);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.20);

        assertEquals('B', graphics.getCell(0, 0).character().charAt(0));
        assertEquals(AnsiColor.GREEN, graphics.getCell(0, 0).fg());
    }

    @Test
    @DisplayName("progress 0.40 is black (middle third)")
    void progress40IsBlack() {
        var size = new TerminalSize(4, 2);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.40);

        // Middle third: black
        var cell = graphics.getCell(0, 0);
        assertEquals(AnsiColor.BLACK, cell.fg());
        assertEquals(AnsiColor.BLACK, cell.bg());
    }

    @Test
    @DisplayName("progress 0.60 is still black (middle third)")
    void progress60IsBlack() {
        var size = new TerminalSize(4, 2);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.60);

        var cell = graphics.getCell(0, 0);
        assertEquals(AnsiColor.BLACK, cell.fg());
        assertEquals(AnsiColor.BLACK, cell.bg());
    }

    @Test
    @DisplayName("progress 0.80 shows new screen content (last third)")
    void progress80ShowsNewContent() {
        var size = new TerminalSize(4, 2);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.80);

        // Last third: new content visible (from graphics buffer)
        assertEquals('A', graphics.getCell(0, 0).character().charAt(0));
        assertEquals(AnsiColor.RED, graphics.getCell(0, 0).fg());
    }

    @Test
    @DisplayName("progress 1.0 shows new screen content")
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

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 1.0);

        assertEquals('A', graphics.getCell(0, 0).character().charAt(0));
        assertEquals(AnsiColor.RED, graphics.getCell(0, 0).fg());
    }

    @Test
    @DisplayName("handles zero-size terminal gracefully")
    void handlesZeroSize() {
        var size = TerminalSize.ZERO;
        var graphics = new TextGraphics(new ScreenBuffer(new TerminalSize(1, 1)));
        var oldBuffer = new ScreenBuffer(new TerminalSize(1, 1));
        var fade = new FadeTransition(800, oldBuffer);
        assertDoesNotThrow(() -> fade.renderFrame(graphics, size, 0.5));
    }
}