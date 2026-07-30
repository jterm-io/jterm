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
 * Tests for {@link FadeTransition}: gradual block-character fade.
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

        // At 0.0, no cells have faded — all show old content at full brightness
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
    @DisplayName("progress 0.25 (mid fade-out) has a mix of full and faded cells")
    void progress25HasMixOfFadedAndFull() {
        var size = new TerminalSize(20, 5);  // 100 cells
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.25);

        int fullCount = 0;    // still showing 'B' at full brightness
        int fadedCount = 0;   // showing a block char or space (dimmed)
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = graphics.getCell(c, r).character().charAt(0);
                if (ch == 'B') {
                    fullCount++;
                } else {
                    fadedCount++;
                }
            }
        }
        // At 0.25 (halfway through fade-out), ~50 cells should be faded.
        // The wave front adds a few partially-dimmed cells, so allow slack.
        assertTrue(fadedCount > 0, "expected some faded cells at 0.25");
        assertTrue(fullCount > 0, "expected some full-brightness cells at 0.25");
    }

    @Test
    @DisplayName("progress 0.5 (midpoint) is fully faded — no original characters visible")
    void progress5FullyFaded() {
        var size = new TerminalSize(10, 10);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.5);

        // At exactly 0.5, all old content should be faded to space.
        // No 'A' or 'B' characters should be visible.
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = graphics.getCell(c, r).character().charAt(0);
                assertNotEquals('A', ch, "new content should not be visible at 0.5");
                assertNotEquals('B', ch, "old content should not be visible at 0.5");
            }
        }
    }

    @Test
    @DisplayName("progress 0.75 (mid fade-in) has a mix of restored and faded new cells")
    void progress75HasMixOfRestoredAndFaded() {
        var size = new TerminalSize(20, 5);  // 100 cells
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.75);

        int restoredCount = 0; // showing 'A' at full brightness
        int fadedCount = 0;    // showing a block char or space
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = graphics.getCell(c, r).character().charAt(0);
                if (ch == 'A') {
                    restoredCount++;
                } else {
                    fadedCount++;
                }
            }
        }
        assertTrue(restoredCount > 0, "expected some restored new cells at 0.75");
        assertTrue(fadedCount > 0, "expected some still-faded cells at 0.75");
    }

    @Test
    @DisplayName("fade is deterministic with same seed")
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
                    "cell (" + c + "," + r + ") differs with same seed");
    }

    @Test
    @DisplayName("different seeds produce different fade patterns")
    void differentSeedsProduceDifferentPatterns() {
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

        boolean different = false;
        for (int r = 0; r < size.rows() && !different; r++)
            for (int c = 0; c < size.columns() && !different; c++)
                if (!g1.getCell(c, r).equals(g2.getCell(c, r))) different = true;

        assertTrue(different, "different seeds should produce different patterns");
    }

    @Test
    @DisplayName("faded cells use BRIGHT_BLACK (dim gray) foreground")
    void fadedCellsUseDimGray() {
        var size = new TerminalSize(10, 10);
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.25);

        // At 0.25, some cells should be faded. Faded cells (block chars)
        // must use BRIGHT_BLACK as their foreground color.
        boolean foundFaded = false;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                var cell = graphics.getCell(c, r);
                char ch = cell.character().charAt(0);
                if (ch != 'A' && ch != 'B') {
                    // This is a faded cell
                    foundFaded = true;
                    assertEquals(AnsiColor.BRIGHT_BLACK, cell.fg(),
                        "faded cell should use BRIGHT_BLACK, got " + cell.fg() + " for char " + ch);
                }
            }
        }
        assertTrue(foundFaded, "expected at least one faded cell at 0.25");
    }

    @Test
    @DisplayName("progressive fade uses block characters (█▓▒░)")
    void progressiveFadeUsesBlockCharacters() {
        var size = new TerminalSize(30, 10);  // 300 cells, enough for wave front
        var graphics = new TextGraphics(new ScreenBuffer(size));
        fillGraphics(graphics, size, 'A', AnsiColor.RED);

        var oldBuffer = new ScreenBuffer(size);
        fillBuffer(oldBuffer, size, 'B', AnsiColor.GREEN);

        var fade = new FadeTransition(800, oldBuffer);
        fade.renderFrame(graphics, size, 0.15);  // early fade-out, wave front visible

        // Collect all unique characters used
        var chars = new java.util.HashSet<Character>();
        for (int r = 0; r < size.rows(); r++)
            for (int c = 0; c < size.columns(); c++)
                chars.add(graphics.getCell(c, r).character().charAt(0));

        // At an intermediate fade point we should see at least some block chars
        // (not just 'B' and spaces). The wave front introduces ░▒▓.
        boolean hasBlockChar = false;
        for (char ch : chars) {
            if (ch == '▓' || ch == '▒' || ch == '░') {
                hasBlockChar = true;
                break;
            }
        }
        assertTrue(hasBlockChar,
            "expected block chars (▓▒░) at wave front, got: " + chars);
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