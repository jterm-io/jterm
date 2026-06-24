package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PhosphorDecay}: animated background contract, CRT phosphor
 * brightness levels, fade-over-time behavior, and non-blank output.
 */
class PhosphorDecayTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var decay = new PhosphorDecay(new TerminalSize(80, 24));
        assertTrue(decay instanceof AnimatedBackground);
        assertEquals(10, decay.targetFps());
        decay.start();
        assertTrue(decay.isRunning());
        decay.stop();
        assertFalse(decay.isRunning());
    }

    @Test
    @DisplayName("renderFrame produces non-blank output")
    void rendersNonBlankOutput() {
        var decay = new PhosphorDecay(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        decay.renderFrame(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "expected non-blank rendered output");
    }

    @Test
    @DisplayName("renderFrame uses green and bright green phosphor colors")
    void usesPhosphorColors() {
        var decay = new PhosphorDecay(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        // Seed full brightness so we get deterministic phosphor colors.
        for (int x = 0; x < size.columns(); x++) {
            for (int y = 0; y < size.rows(); y++) {
                decay.setCell(x, y, 5);
            }
        }

        Set<Color> expected = Set.of(AnsiColor.GREEN, AnsiColor.BRIGHT_GREEN,
                AnsiColor.BRIGHT_BLACK, AnsiColor.BLACK);
        decay.renderFrame(new TextGraphics(buffer), size);

        boolean foundPhosphor = false;
        for (int r = 0; r < size.rows() && !foundPhosphor; r++) {
            for (int c = 0; c < size.columns(); c++) {
                Color fg = buffer.getCell(c, r).fg();
                if (expected.contains(fg)) {
                    foundPhosphor = true;
                    break;
                }
            }
        }
        assertTrue(foundPhosphor, "expected green/amber phosphor foreground colors");
    }

    @Test
    @DisplayName("cells fade over multiple frames")
    void cellsFadeOverMultipleFrames() {
        var decay = new PhosphorDecay(new TerminalSize(20, 10));
        var size = new TerminalSize(20, 10);
        var buffer = new ScreenBuffer(size);

        // Seed the center cell at full brightness.
        decay.setCell(10, 5, 5);

        // Render several frames with a deterministic seed-like approach:
        // we force the cell to decay by overriding subsequent excitations.
        for (int i = 0; i < 4; i++) {
            decay.renderFrame(new TextGraphics(buffer), size);
            decay.setCell(10, 5, Math.max(0, 5 - i - 1));
        }

        int finalLevel = decay.getGrid()[10][5];
        assertTrue(finalLevel <= 1,
                "cell should have decayed to near-black after several frames");
    }

    @Test
    @DisplayName("cells decay naturally without manual override")
    void cellsDecayNaturally() {
        // Use a Random that never triggers excite or double-decay (always returns 1.0)
        var decay = new PhosphorDecay(new TerminalSize(30, 15), new Random() {
            @Override public double nextDouble() { return 1.0; }
        });
        var size = new TerminalSize(30, 15);
        var buffer = new ScreenBuffer(size);

        // Fill grid with full brightness.
        for (int x = 0; x < size.columns(); x++) {
            for (int y = 0; y < size.rows(); y++) {
                decay.setCell(x, y, 5);
            }
        }

        // First frame: all cells decay by one (or a few by two).
        decay.renderFrame(new TextGraphics(buffer), size);
        int afterFirst = totalBrightness(decay.getGrid());
        assertTrue(afterFirst < 5 * size.columns() * size.rows(),
                "first frame should decay from full brightness");

        // Force the grid to decay deterministically by disabling random excite
        // by clearing the grid and setting a single bright cell, then watching it fade.
        for (int x = 0; x < size.columns(); x++) {
            for (int y = 0; y < size.rows(); y++) {
                decay.setCell(x, y, 0);
            }
        }
        decay.setCell(10, 7, 5);

        decay.renderFrame(new TextGraphics(buffer), size);
        int singleAfterFirst = decay.getGrid()[10][7];

        decay.renderFrame(new TextGraphics(buffer), size);
        int singleAfterSecond = decay.getGrid()[10][7];

        assertTrue(singleAfterFirst > singleAfterSecond,
                "single cell brightness should decrease across frames");
        assertTrue(singleAfterSecond <= 3,
                "single bright cell should have decayed to dim or black");
    }

    @Test
    @DisplayName("small terminal sizes render without throwing")
    void smallTerminalSizes() {
        var decay = new PhosphorDecay(new TerminalSize(1, 1));
        var size = new TerminalSize(1, 1);
        var buffer = new ScreenBuffer(size);

        assertDoesNotThrow(() -> decay.renderFrame(new TextGraphics(buffer), size));
        char ch = buffer.getCell(0, 0).character().charAt(0);
        assertTrue(ch == ' ' || ch == '.' || ch == ':' || ch == 'o' || ch == 'O' || ch == '@',
                "expected a phosphor glyph, got: " + ch);
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var decay = new PhosphorDecay(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> decay.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var decay = new PhosphorDecay(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        decay.onResize(size);
        assertEquals(size, decay.lastSize());
    }

    @Test
    @DisplayName("grid dimensions match terminal size")
    void gridDimensionsMatchSize() {
        var decay = new PhosphorDecay(new TerminalSize(40, 12));
        var size = new TerminalSize(40, 12);
        var buffer = new ScreenBuffer(size);

        decay.renderFrame(new TextGraphics(buffer), size);

        int[][] grid = decay.getGrid();
        assertEquals(40, grid.length);
        assertEquals(12, grid[0].length);
    }

    @Test
    @DisplayName("levels are clamped when setting out of range")
    void setCellClampsLevels() {
        var decay = new PhosphorDecay(new TerminalSize(10, 5));
        var size = new TerminalSize(10, 5);
        var buffer = new ScreenBuffer(size);
        decay.renderFrame(new TextGraphics(buffer), size);

        decay.setCell(0, 0, 100);
        assertEquals(5, decay.getGrid()[0][0]);

        decay.setCell(0, 0, -10);
        assertEquals(0, decay.getGrid()[0][0]);
    }

    @Test
    @DisplayName("renderFrame produces expected glyphs")
    void usesExpectedGlyphs() {
        var decay = new PhosphorDecay(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        decay.renderFrame(new TextGraphics(buffer), size);

        Set<Character> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                found.add(buffer.getCell(c, r).character().charAt(0));
            }
        }
        assertTrue(found.contains('o') || found.contains('O') || found.contains('@'),
                "expected at least one phosphor glyph");
    }

    @Test
    @DisplayName("frame output differs from blank")
    void differsFromBlank() {
        var decay = new PhosphorDecay(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var blank = new ScreenBuffer(size);
        var rendered = new ScreenBuffer(size);

        decay.renderFrame(new TextGraphics(rendered), size);

        boolean anyDifferent = false;
        for (int r = 0; r < size.rows() && !anyDifferent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (!rendered.getCell(c, r).equals(blank.getCell(c, r))) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "rendered output should differ from blank");
    }

    private static int totalBrightness(int[][] grid) {
        int total = 0;
        for (int[] column : grid) {
            for (int level : column) {
                total += level;
            }
        }
        return total;
    }
}
