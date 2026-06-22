package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DNAHelix}: animated background contract, helix rendering,
 * rung connections, resize handling, and non-blank output.
 */
class DNAHelixTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var helix = new DNAHelix(new TerminalSize(80, 24));
        assertTrue(helix instanceof AnimatedBackground);
        assertEquals(10, helix.targetFps());
        helix.start();
        assertTrue(helix.isRunning());
        helix.stop();
        assertFalse(helix.isRunning());
    }

    @Test
    @DisplayName("renderFrame clears background to black")
    void clearsBackgroundToBlack() {
        var helix = new DNAHelix(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        buffer.setCell(10, 2, new TextCell('X', AnsiColor.BRIGHT_RED, AnsiColor.WHITE));

        helix.renderAtTime(new TextGraphics(buffer), size, 0.0);

        assertEquals(AnsiColor.BLACK, buffer.getCell(0, 0).bg());
        assertEquals(AnsiColor.BLACK, buffer.getCell(10, 2).bg());
        assertEquals(' ', buffer.getCell(10, 2).character().charAt(0));
    }

    @Test
    @DisplayName("renderFrame produces non-blank output")
    void rendersNonBlankOutput() {
        var helix = new DNAHelix(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        helix.renderAtTime(new TextGraphics(buffer), size, 0.0);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "expected non-blank rendered output");
    }

    @Test
    @DisplayName("renderFrame uses strand glyph 'o'")
    void usesStrandGlyph() {
        var helix = new DNAHelix(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        helix.renderAtTime(new TextGraphics(buffer), size, 0.0);

        Set<Character> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                found.add(buffer.getCell(c, r).character().charAt(0));
            }
        }
        assertTrue(found.contains('o'), "expected strand glyph 'o'");
    }

    @Test
    @DisplayName("renderFrame uses rung glyph '-'")
    void usesRungGlyph() {
        var helix = new DNAHelix(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        helix.renderAtTime(new TextGraphics(buffer), size, 0.0);

        Set<Character> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                found.add(buffer.getCell(c, r).character().charAt(0));
            }
        }
        assertTrue(found.contains('-'), "expected rung glyph '-'");
    }

    @Test
    @DisplayName("renderFrame uses green and cyan colors for strands")
    void usesStrandColors() {
        var helix = new DNAHelix(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        helix.renderAtTime(new TextGraphics(buffer), size, 0.0);

        Set<AnsiColor> expected = Set.of(
                AnsiColor.BRIGHT_GREEN, AnsiColor.GREEN,
                AnsiColor.BRIGHT_CYAN, AnsiColor.CYAN,
                AnsiColor.BRIGHT_BLACK);
        Set<AnsiColor> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') {
                    found.add((AnsiColor) buffer.getCell(c, r).fg());
                }
            }
        }
        assertFalse(found.isEmpty(), "expected strand colors");
        for (var color : found) {
            assertTrue(expected.contains(color), "unexpected color: " + color);
        }
    }

    @Test
    @DisplayName("two frames differ over time")
    void framesDifferOverTime() {
        var helix = new DNAHelix(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        var buffer1 = new ScreenBuffer(size);
        helix.renderAtTime(new TextGraphics(buffer1), size, 0.0);

        var buffer2 = new ScreenBuffer(size);
        helix.renderAtTime(new TextGraphics(buffer2), size, 5.0);

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
    @DisplayName("animation advances time on each renderFrame call")
    void timeAdvances() {
        var helix = new DNAHelix(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        double before = helix.getTime();
        helix.renderFrame(new TextGraphics(buffer), size);
        assertTrue(helix.getTime() > before, "time should advance each frame");
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var helix = new DNAHelix(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> helix.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var helix = new DNAHelix(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        helix.onResize(size);
        assertEquals(size, helix.lastSize());
    }

    @Test
    @DisplayName("bounds are respected: no null cells inside size")
    void respectsBounds() {
        var helix = new DNAHelix(new TerminalSize(40, 12));
        var size = new TerminalSize(40, 12);
        var buffer = new ScreenBuffer(size);

        helix.renderAtTime(new TextGraphics(buffer), size, 0.0);

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                assertNotNull(buffer.getCell(c, r));
            }
        }
    }

    @Test
    @DisplayName("renderFrame produces visible output")
    void renderFrameProducesOutput() {
        var helix = new DNAHelix(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        helix.renderFrame(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "renderFrame should produce visible output");
    }
}
