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
 * Tests for {@link FadeTransition}: top-to-bottom sweep (curtain) fade.
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
    @DisplayName("targetFps is 10")
    void targetFpsIs10() {
        assertEquals(10, new FadeTransition(800, new ScreenBuffer(new TerminalSize(1, 1))).targetFps());
    }

    @Test
    @DisplayName("progress 0.0 shows old screen content at full brightness")
    void progress0ShowsOldContent() {
        var size = new TerminalSize(4, 2);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.0);

        // At 0.0, the sweep hasn't started — all cells show old content.
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                var cell = graphics.getCell(c, r);
                assertEquals('B', cell.character().charAt(0));
                assertEquals(AnsiColor.GREEN, cell.fg());
            }
        }
    }

    @Test
    @DisplayName("progress 1.0 shows new screen content at full brightness")
    void progress1ShowsNewContent() {
        var size = new TerminalSize(4, 2);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 1.0);

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                var cell = graphics.getCell(c, r);
                assertEquals('A', cell.character().charAt(0));
                assertEquals(AnsiColor.RED, cell.fg());
            }
        }
    }

    @Test
    @DisplayName("progress 0.25 (sweep near top) has new content above, old content below, fade band between")
    void progress25HasSweepMix() {
        var size = new TerminalSize(20, 20);  // tall enough for all three regions
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.25);

        int newCount = 0;    // 'A' — above the sweep line
        int oldCount = 0;    // 'B' — below the sweep line
        int fadedCount = 0;  // block char or space — in the fade band
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = graphics.getCell(c, r).character().charAt(0);
                if (ch == 'A') newCount++;
                else if (ch == 'B') oldCount++;
                else fadedCount++;
            }
        }
        // At 0.25 the sweep is near the top, so we expect some new content
        // above it and some old content below it, with a fade band in between.
        assertTrue(newCount > 0, "expected some new 'A' cells above the sweep at 0.25");
        assertTrue(oldCount > 0, "expected some old 'B' cells below the sweep at 0.25");
        assertTrue(fadedCount > 0, "expected some faded band cells at the sweep line at 0.25");
    }

    @Test
    @DisplayName("progress 0.5 (midpoint) shows new content above sweep, old content below")
    void progress5MidSweep() {
        var size = new TerminalSize(10, 10);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.5);

        int newCount = 0;
        int oldCount = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = graphics.getCell(c, r).character().charAt(0);
                if (ch == 'A') newCount++;
                else if (ch == 'B') oldCount++;
            }
        }
        // At 0.5 the sweep is roughly in the middle. We expect new content
        // above the band and old content below it.
        assertTrue(newCount > 0, "expected new 'A' content above the sweep at 0.5");
        assertTrue(oldCount > 0, "expected old 'B' content below the sweep at 0.5");
    }

    @Test
    @DisplayName("progress 0.75 (late sweep) has new content above, old content below, fade band between")
    void progress75HasSweepMix() {
        var size = new TerminalSize(20, 20);  // tall enough for all three regions
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.75);

        int newCount = 0;
        int oldCount = 0;
        int fadedCount = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = graphics.getCell(c, r).character().charAt(0);
                if (ch == 'A') newCount++;
                else if (ch == 'B') oldCount++;
                else fadedCount++;
            }
        }
        assertTrue(newCount > 0, "expected some new 'A' cells above the sweep at 0.75");
        assertTrue(oldCount > 0, "expected some old 'B' cells below the sweep at 0.75");
        assertTrue(fadedCount > 0, "expected some faded band cells at the sweep line at 0.75");
    }

    @Test
    @DisplayName("sweep fade is deterministic (same params → same output)")
    void fadeIsDeterministic() {
        var size = new TerminalSize(10, 10);

        var g1 = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(g1, size, 'A', AnsiColor.RED);
        var old1 = new ScreenBuffer(size);
        fillBuffer(old1, size, 'B', AnsiColor.GREEN);
        var f1 = new FadeTransition(800, old1, 99L);
        f1.renderFrame(g1, size, 0.3);

        var g2 = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(g2, size, 'A', AnsiColor.RED);
        var old2 = new ScreenBuffer(size);
        fillBuffer(old2, size, 'B', AnsiColor.GREEN);
        var f2 = new FadeTransition(800, old2, 99L);
        f2.renderFrame(g2, size, 0.3);

        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                assertEquals(g1.getCell(c, r), g2.getCell(c, r),
                    "cell (" + c + "," + r + ") differs for identical params");
    }

    @Test
    @DisplayName("sweep is positional — different seeds produce identical output")
    void differentSeedsProduceIdenticalPatterns() {
        var size = new TerminalSize(10, 10);

        var g1 = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(g1, size, 'A', AnsiColor.RED);
        var old1 = new ScreenBuffer(size);
        fillBuffer(old1, size, 'B', AnsiColor.GREEN);
        new FadeTransition(800, old1, 1L).renderFrame(g1, size, 0.3);

        var g2 = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(g2, size, 'A', AnsiColor.RED);
        var old2 = new ScreenBuffer(size);
        fillBuffer(old2, size, 'B', AnsiColor.GREEN);
        new FadeTransition(800, old2, 2L).renderFrame(g2, size, 0.3);

        // The sweep is purely positional, so the seed has no effect.
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                assertEquals(g1.getCell(c, r), g2.getCell(c, r),
                    "sweep should be seed-independent, cell (" + c + "," + r + ") differs");
    }

    @Test
    @DisplayName("faded band cells use BRIGHT_BLACK (dim gray) foreground")
    void fadedCellsUseDimGray() {
        var size = new TerminalSize(10, 10);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.25);

        // At 0.25 the fade band is near the top. Band cells (block chars)
        // must use BRIGHT_BLACK as their foreground color.
        boolean foundFaded = false;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                var cell = graphics.getCell(c, r);
                char ch = cell.character().charAt(0);
                if (ch != 'A' && ch != 'B') {
                    // This is a faded band cell
                    foundFaded = true;
                    assertEquals(AnsiColor.BRIGHT_BLACK, cell.fg(),
                        "faded cell should use BRIGHT_BLACK, got " + cell.fg() + " for char " + ch);
                }
            }
        }
        assertTrue(foundFaded, "expected at least one faded band cell at 0.25");
    }

    @Test
    @DisplayName("fade band uses progressive block characters (█▓▒░)")
    void progressiveFadeUsesBlockCharacters() {
        var size = new TerminalSize(30, 10);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.15);  // early sweep, band visible

        // Collect all unique characters used
        var chars = new java.util.HashSet<Character>();
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                chars.add(graphics.getCell(c, r).character().charAt(0));

        // The fade band should introduce intermediate block chars (▓▒░),
        // not just 'A'/'B' and solid '█'.
        boolean hasIntermediateBlock = false;
        for (char ch : chars) {
            if (ch == '▓' || ch == '▒' || ch == '░') {
                hasIntermediateBlock = true;
                break;
            }
        }
        assertTrue(hasIntermediateBlock,
            "expected intermediate block chars (▓▒░) in the fade band, got: " + chars);
    }

    @Test
    @DisplayName("sweep moves top-to-bottom: top rows transition before bottom rows")
    void sweepIsTopToBottom() {
        var size = new TerminalSize(10, 20);  // tall grid
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);

        // At 0.25, the top portion should have transitioned (new 'A' content)
        // while the bottom portion should still show old 'B' content.
        fade.renderFrame(graphics, size, 0.25);

        boolean topHasNew = false;
        boolean bottomHasOld = false;
        for (int c = 0; c < size.columns(); c++) {
            if (graphics.getCell(c, 0).character().charAt(0) == 'A') topHasNew = true;
            if (graphics.getCell(c, size.rows() - 1).character().charAt(0) == 'B') bottomHasOld = true;
        }
        assertTrue(topHasNew, "top row should show new 'A' content at 0.25");
        assertTrue(bottomHasOld, "bottom row should still show old 'B' content at 0.25");
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

    // ── Helpers ──────────────────────────────────────────────

    private static void fillGraphics(TextGraphics g, TerminalSize size, char ch, AnsiColor fg) {
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                g.setCell(c, r, new TextCell(ch, fg, AnsiColor.BLACK));
    }

    private static void fillBuffer(ScreenBuffer buf, TerminalSize size, char ch, AnsiColor fg) {
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                buf.setCell(c, r, new TextCell(ch, fg, AnsiColor.BLACK));
    }
}