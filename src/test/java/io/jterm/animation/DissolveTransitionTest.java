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
 * Tests for {@link DissolveTransition}: BBS-style random dissolve.
 */
class DissolveTransitionTest {

    @Test
    @DisplayName("implements TransitionEffect")
    void implementsTransitionEffect() {
        assertTrue(new DissolveTransition(500, new ScreenBuffer(new TerminalSize(1, 1)), 42L) instanceof TransitionEffect);
    }

    @Test
    @DisplayName("durationMs returns configured value")
    void durationMsReturnsConfigured() {
        assertEquals(500, new DissolveTransition(500, new ScreenBuffer(new TerminalSize(1, 1)), 42L).durationMs());
    }

    @Test
    @DisplayName("progress 0.0 shows old content")
    void progress0ShowsOldContent() {
        var size = new TerminalSize(4, 4);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var dissolve = new DissolveTransition(500, oldBuffer, 42L);
        dissolve.renderFrame(graphics, size, 0.0);

        // At progress 0, no cells should show new content
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

        var dissolve = new DissolveTransition(500, oldBuffer, 42L);
        dissolve.renderFrame(graphics, size, 1.0);

        // At progress 1, all cells should show new content
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                assertEquals('A', graphics.getCell(c, r).character().charAt(0));
    }

    @Test
    @DisplayName("progress 0.5 shows roughly half cells as new content")
    void progress5ShowsAboutHalfNew() {
        var size = new TerminalSize(10, 10);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                graphics.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));

        var oldBuffer = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                oldBuffer.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));

        var dissolve = new DissolveTransition(500, oldBuffer, 42L);
        dissolve.renderFrame(graphics, size, 0.5);

        int newCount = 0;
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                if (graphics.getCell(c, r).character().charAt(0) == 'A') newCount++;

        // 100 cells total, ~50 should be new. Allow some slack.
        assertTrue(newCount >= 40 && newCount <= 60,
            "expected ~50 new cells, got " + newCount);
    }

    @Test
    @DisplayName("dissolve is deterministic with same seed")
    void dissolveIsDeterministic() {
        var size = new TerminalSize(10, 10);

        // First render
        var g1 = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                g1.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));
        var old1 = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                old1.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));
        var d1 = new DissolveTransition(500, old1, 99L);
        d1.renderFrame(g1, size, 0.3);

        // Second render with same seed
        var g2 = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                g2.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));
        var old2 = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                old2.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));
        var d2 = new DissolveTransition(500, old2, 99L);
        d2.renderFrame(g2, size, 0.3);

        // Results should be identical
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                assertEquals(g1.getCell(c, r), g2.getCell(c, r),
                    "cell (" + c + "," + r + ") differs");
    }

    @Test
    @DisplayName("different seeds produce different patterns")
    void differentSeedsProduceDifferentPatterns() {
        var size = new TerminalSize(10, 10);

        var g1 = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                g1.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));
        var old1 = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                old1.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));
        new DissolveTransition(500, old1, 1L).renderFrame(g1, size, 0.3);

        var g2 = new TextGraphics(new ScreenBuffer(size));
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                g2.setCell(c, r, new TextCell('A', AnsiColor.RED, AnsiColor.BLACK));
        var old2 = new ScreenBuffer(size);
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                old2.setCell(c, r, new TextCell('B', AnsiColor.GREEN, AnsiColor.BLACK));
        new DissolveTransition(500, old2, 2L).renderFrame(g2, size, 0.3);

        boolean different = false;
        for (int r = 0; r < size.rows() && !different; r++)
            for (int c = 0; c < size.columns() && !different; c++)
                if (!g1.getCell(c, r).equals(g2.getCell(c, r))) different = true;

        assertTrue(different, "different seeds should produce different patterns");
    }

    @Test
    @DisplayName("handles zero-size terminal gracefully")
    void handlesZeroSize() {
        var size = TerminalSize.ZERO;
        var graphics = new TextGraphics(new ScreenBuffer(new TerminalSize(1, 1)));
        var oldBuffer = new ScreenBuffer(new TerminalSize(1, 1));
        var dissolve = new DissolveTransition(500, oldBuffer, 42L);
        assertDoesNotThrow(() -> dissolve.renderFrame(graphics, size, 0.5));
    }
}