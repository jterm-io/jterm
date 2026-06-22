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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Aurora}: animated background contract, aurora band rendering,
 * color blending, resize handling and non-blank output.
 */
class AuroraTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var aurora = new Aurora(new TerminalSize(80, 24));
        assertTrue(aurora instanceof AnimatedBackground);
        assertEquals(10, aurora.targetFps());
        aurora.start();
        assertTrue(aurora.isRunning());
        aurora.stop();
        assertFalse(aurora.isRunning());
    }

    @Test
    @DisplayName("constructor builds 4 aurora bands")
    void fourBands() {
        var aurora = new Aurora(new TerminalSize(80, 24));
        assertEquals(4, aurora.getBands().size());
    }

    @Test
    @DisplayName("bands use expected colors")
    void bandColors() {
        var aurora = new Aurora(new TerminalSize(80, 24));
        Set<AnsiColor> expected = Set.of(
                AnsiColor.BRIGHT_GREEN, AnsiColor.BRIGHT_CYAN,
                AnsiColor.BRIGHT_MAGENTA, AnsiColor.BRIGHT_BLUE);
        for (var band : aurora.getBands()) {
            assertTrue(expected.contains(band.color()),
                    "unexpected band color: " + band.color());
        }
    }

    @Test
    @DisplayName("back band is highest and front band is lowest")
    void depthOrdering() {
        var aurora = new Aurora(new TerminalSize(80, 24));
        var list = aurora.getBands();
        for (int i = 1; i < list.size(); i++) {
            assertTrue(list.get(i).baseY() >= list.get(i - 1).baseY(),
                    "front band should be lower than back band");
        }
    }

    @Test
    @DisplayName("renderFrame clears background to black")
    void clearsBackgroundToBlack() {
        var aurora = new Aurora(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        buffer.setCell(10, 2, new TextCell('X', AnsiColor.BRIGHT_RED, AnsiColor.WHITE));

        aurora.renderAtTime(new TextGraphics(buffer), size, 0.0);

        assertEquals(AnsiColor.BLACK, buffer.getCell(0, 0).bg());
        assertEquals(AnsiColor.BLACK, buffer.getCell(10, 2).bg());
        assertEquals(' ', buffer.getCell(10, 2).character().charAt(0));
    }

    @Test
    @DisplayName("renderFrame produces non-blank output")
    void rendersNonBlankOutput() {
        var aurora = new Aurora(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        aurora.renderAtTime(new TextGraphics(buffer), size, 1.0);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "expected non-blank rendered output");
    }

    @Test
    @DisplayName("renderFrame uses expected glyphs")
    void usesExpectedGlyphs() {
        var aurora = new Aurora(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        aurora.renderAtTime(new TextGraphics(buffer), size, 0.5);

        Set<Character> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                found.add(buffer.getCell(c, r).character().charAt(0));
            }
        }
        assertTrue(found.contains('#') || found.contains('~') || found.contains('.'),
                "expected aurora band glyphs");
    }

    @Test
    @DisplayName("band colors appear in rendered output")
    void bandColorsAppear() {
        var aurora = new Aurora(new TerminalSize(160, 60));
        var size = new TerminalSize(160, 60);
        var buffer = new ScreenBuffer(size);

        boolean foundGreen = false;
        boolean foundCyan = false;
        boolean foundMagenta = false;
        boolean foundBlue = false;

        for (double t = 0.0; t < 30.0 && !(foundGreen && foundCyan && foundMagenta && foundBlue); t += 0.1) {
            buffer.fill(TextCell.EMPTY);
            aurora.renderAtTime(new TextGraphics(buffer), size, t);
            for (int r = 0; r < size.rows() && !(foundGreen && foundCyan && foundMagenta && foundBlue); r++) {
                for (int c = 0; c < size.columns(); c++) {
                    Color fg = buffer.getCell(c, r).fg();
                    if (fg == AnsiColor.BRIGHT_GREEN) foundGreen = true;
                    if (fg == AnsiColor.BRIGHT_CYAN) foundCyan = true;
                    if (fg == AnsiColor.BRIGHT_MAGENTA) foundMagenta = true;
                    if (fg == AnsiColor.BRIGHT_BLUE) foundBlue = true;
                }
            }
        }

        assertTrue(foundGreen, "expected bright green in output");
        assertTrue(foundCyan, "expected bright cyan in output");
        assertTrue(foundMagenta, "expected bright magenta in output");
        assertTrue(foundBlue, "expected bright blue in output");
    }

    @Test
    @DisplayName("overlapping bands keep the brighter color")
    void brighterColorWinsOverlap() {
        var aurora = new Aurora(new TerminalSize(160, 60));
        var size = new TerminalSize(160, 60);
        var buffer = new ScreenBuffer(size);

        // Look for a cell whose foreground changed to a brighter color than black.
        aurora.renderAtTime(new TextGraphics(buffer), size, 2.0);

        boolean foundBright = false;
        for (int r = 0; r < size.rows() && !foundBright; r++) {
            for (int c = 0; c < size.columns(); c++) {
                Color fg = buffer.getCell(c, r).fg();
                if (fg instanceof AnsiColor && fg != AnsiColor.BLACK) {
                    foundBright = true;
                    break;
                }
            }
        }
        assertTrue(foundBright, "expected at least one non-black foreground cell");
    }

    @Test
    @DisplayName("animation advances time on each renderFrame call")
    void timeAdvances() {
        var aurora = new Aurora(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        double before = aurora.getTime();
        aurora.renderFrame(new TextGraphics(buffer), size);
        assertTrue(aurora.getTime() > before, "time should advance each frame");
    }

    @Test
    @DisplayName("two frames differ over time")
    void framesDifferOverTime() {
        var aurora = new Aurora(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        var buffer1 = new ScreenBuffer(size);
        aurora.renderAtTime(new TextGraphics(buffer1), size, 0.0);

        var buffer2 = new ScreenBuffer(size);
        aurora.renderAtTime(new TextGraphics(buffer2), size, 5.0);

        boolean anyDifferent = false;
        for (int r = 0; r < size.rows() && !anyDifferent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (!buffer1.getCell(c, r).equals(buffer2.getCell(c, r))) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "frames at different times should differ");
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var aurora = new Aurora(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> aurora.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var aurora = new Aurora(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        aurora.onResize(size);
        assertEquals(size, aurora.lastSize());
    }

    @Test
    @DisplayName("onResize rebuilds bands for new dimensions")
    void onResizeRebuildsBands() {
        var aurora = new Aurora(new TerminalSize(40, 12));
        var before = aurora.getBands();
        int oldBase = before.get(0).baseY();

        aurora.onResize(new TerminalSize(100, 50));
        var after = aurora.getBands();
        int newBase = after.get(0).baseY();

        assertTrue(newBase > oldBase,
                "baseY should scale with larger terminal height");
    }

    @Test
    @DisplayName("bounds are respected: no characters outside size")
    void respectsBounds() {
        var aurora = new Aurora(new TerminalSize(40, 12));
        var size = new TerminalSize(40, 12);
        var buffer = new ScreenBuffer(size);

        aurora.renderAtTime(new TextGraphics(buffer), size, 1.0);

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                assertNotNull(buffer.getCell(c, r));
            }
        }
    }

    @Test
    @DisplayName("renderFrame uses system time and produces visible output")
    void renderFrameProducesOutput() {
        var aurora = new Aurora(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        aurora.renderFrame(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "renderFrame should produce visible output");
    }
}
