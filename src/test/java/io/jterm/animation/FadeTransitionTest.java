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
 * Tests for {@link FadeTransition}: fade-through-black screen transition.
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
    @DisplayName("default targetFps is 30")
    void defaultTargetFpsIs30() {
        assertEquals(30, new FadeTransition(500, new ScreenBuffer(new TerminalSize(1, 1))).targetFps());
    }

    @Test
    @DisplayName("progress 0.0 keeps original content visible")
    void progress0KeepsOriginal() {
        var size = new TerminalSize(4, 2);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        // Draw "new" content: all 'A' in red
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        // Capture old content (before transition)
        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var fade = new FadeTransition(500, oldBuffer);
        fade.renderFrame(graphics, size, 0.0);

        // At progress 0, old content should be visible
        assertEquals('B', graphics.getCell(0, 0).character().charAt(0));
        assertEquals(AnsiColor.GREEN, graphics.getCell(0, 0).fg());
    }

    @Test
    @DisplayName("progress 0.5 is black (dimmed to black)")
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

        // At progress 0.5, everything should be black
        var cell = graphics.getCell(0, 0);
        assertEquals(AnsiColor.BLACK, cell.fg());
        assertEquals(AnsiColor.BLACK, cell.bg());
    }

    @Test
    @DisplayName("progress 1.0 shows new content")
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

        // At progress 1, new content should be visible
        assertEquals('A', graphics.getCell(0, 0).character().charAt(0));
        assertEquals(AnsiColor.RED, graphics.getCell(0, 0).fg());
    }

    @Test
    @DisplayName("progress 0.25 dims old content toward black")
    void progress25DimsOldContent() {
        var size = new TerminalSize(2, 1);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        graphics.setCell(0, 0, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        oldBuffer.setCell(0, 0, new TextCell('B', AnsiColor.WHITE, AnsiColor.BLACK));

        var fade = new FadeTransition(500, oldBuffer);
        fade.renderFrame(graphics, size, 0.25);

        // At progress 0.25 (first half), old content should be dimmed
        // intensity factor = 1 - 0.25*2 = 0.5
        var cell = graphics.getCell(0, 0);
        assertEquals('B', cell.character().charAt(0));
        // The fg should be dimmed — not full WHITE anymore
        assertNotEquals(AnsiColor.WHITE, cell.fg(), "fg should be dimmed below white");
    }

    @Test
    @DisplayName("progress 0.75 shows new content brightening from black")
    void progress75BrightensNewContent() {
        var size = new TerminalSize(2, 1);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        graphics.setCell(0, 0, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        oldBuffer.setCell(0, 0, new TextCell('B', AnsiColor.WHITE, AnsiColor.BLACK));

        var fade = new FadeTransition(500, oldBuffer);
        fade.renderFrame(graphics, size, 0.75);

        // At progress 0.75 (second half), new content should be brightening
        // intensity factor = 0.75*2 - 1 = 0.5
        var cell = graphics.getCell(0, 0);
        assertEquals('A', cell.character().charAt(0));
        // The fg should be dimmed — not full RED yet
        assertNotEquals(AnsiColor.RED, cell.fg(), "fg should be dimmed below red");
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