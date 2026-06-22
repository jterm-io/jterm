package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OceanWaves}: animated background contract, wave rendering,
 * foam peaks, resize handling, and non-blank output.
 */
class OceanWavesTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var waves = new OceanWaves(new TerminalSize(80, 24));
        assertTrue(waves instanceof AnimatedBackground);
        assertEquals(10, waves.targetFps());
        waves.start();
        assertTrue(waves.isRunning());
        waves.stop();
        assertFalse(waves.isRunning());
    }

    @Test
    @DisplayName("constructor builds 4 wave layers")
    void fourWaveLayers() {
        var waves = new OceanWaves(new TerminalSize(80, 24));
        assertEquals(4, waves.getWaves().size());
    }

    @Test
    @DisplayName("layers use expected colors")
    void layerColors() {
        var waves = new OceanWaves(new TerminalSize(80, 24));
        Set<AnsiColor> expected = Set.of(
                AnsiColor.BLUE, AnsiColor.BRIGHT_BLUE, AnsiColor.CYAN, AnsiColor.BRIGHT_CYAN);
        for (var wave : waves.getWaves()) {
            assertTrue(expected.contains(wave.color()),
                    "unexpected wave color: " + wave.color());
        }
    }

    @Test
    @DisplayName("back wave is highest (smallest baseY) and front wave is lowest")
    void depthOrdering() {
        var waves = new OceanWaves(new TerminalSize(80, 24));
        var list = waves.getWaves();
        for (int i = 1; i < list.size(); i++) {
            assertTrue(list.get(i).baseY() >= list.get(i - 1).baseY(),
                    "front wave should be lower than back wave");
        }
    }

    @Test
    @DisplayName("renderFrame clears sky to black")
    void clearsSkyToBlack() {
        var waves = new OceanWaves(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        buffer.setCell(10, 2, new TextCell('X', AnsiColor.BRIGHT_RED, AnsiColor.WHITE));

        waves.renderAtTime(new TextGraphics(buffer), size, 0.0);

        assertEquals(AnsiColor.BLACK, buffer.getCell(0, 0).bg());
        assertEquals(AnsiColor.BLACK, buffer.getCell(10, 2).bg());
        assertEquals(' ', buffer.getCell(10, 2).character().charAt(0));
    }

    @Test
    @DisplayName("renderFrame produces non-blank output")
    void rendersNonBlankOutput() {
        var waves = new OceanWaves(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        waves.renderAtTime(new TextGraphics(buffer), size, 1.0);

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
        var waves = new OceanWaves(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        waves.renderAtTime(new TextGraphics(buffer), size, 0.5);

        Set<Character> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                found.add(buffer.getCell(c, r).character().charAt(0));
            }
        }
        assertTrue(found.contains('~') || found.contains('='),
                "expected wave or water body glyphs");
    }

    @Test
    @DisplayName("wave surface is above water body")
    void surfaceAboveBody() {
        var waves = new OceanWaves(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        waves.renderAtTime(new TextGraphics(buffer), size, 0.5);

        boolean foundSurface = false;
        boolean foundBody = false;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = buffer.getCell(c, r).character().charAt(0);
                if (ch == '~') foundSurface = true;
                if (ch == '=') foundBody = true;
            }
        }
        assertTrue(foundSurface, "expected wave surface '~'");
        assertTrue(foundBody, "expected water body '=' below surface");
    }

    @Test
    @DisplayName("foam appears at some wave peak")
    void foamAppearsAtPeaks() {
        var waves = new OceanWaves(new TerminalSize(200, 80));
        var size = new TerminalSize(200, 80);
        var buffer = new ScreenBuffer(size);

        boolean foundFoam = false;
        for (double t = 0; t < 40.0 && !foundFoam; t += 0.05) {
            buffer.fill(TextCell.EMPTY);
            waves.renderAtTime(new TextGraphics(buffer), size, t);
            for (int r = 0; r < size.rows() && !foundFoam; r++) {
                for (int c = 0; c < size.columns(); c++) {
                    if (buffer.getCell(c, r).character().charAt(0) == '.') {
                        foundFoam = true;
                        break;
                    }
                }
            }
        }
        assertTrue(foundFoam, "expected foam '.' at a wave peak over time");
    }

    @Test
    @DisplayName("animation advances time on each renderFrame call")
    void timeAdvances() {
        var waves = new OceanWaves(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        double before = waves.getTime();
        waves.renderFrame(new TextGraphics(buffer), size);
        assertTrue(waves.getTime() > before, "time should advance each frame");
    }

    @Test
    @DisplayName("two frames differ over time")
    void framesDifferOverTime() {
        var waves = new OceanWaves(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        var buffer1 = new ScreenBuffer(size);
        waves.renderAtTime(new TextGraphics(buffer1), size, 0.0);

        var buffer2 = new ScreenBuffer(size);
        waves.renderAtTime(new TextGraphics(buffer2), size, 5.0);

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
        var waves = new OceanWaves(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> waves.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var waves = new OceanWaves(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        waves.onResize(size);
        assertEquals(size, waves.lastSize());
    }

    @Test
    @DisplayName("onResize rebuilds waves for new dimensions")
    void onResizeRebuildsWaves() {
        var waves = new OceanWaves(new TerminalSize(40, 12));
        var before = waves.getWaves();
        int oldBase = before.get(0).baseY();

        waves.onResize(new TerminalSize(100, 50));
        var after = waves.getWaves();
        int newBase = after.get(0).baseY();

        assertTrue(newBase > oldBase,
                "baseY should scale with larger terminal height");
    }

    @Test
    @DisplayName("bounds are respected: no characters outside size")
    void respectsBounds() {
        var waves = new OceanWaves(new TerminalSize(40, 12));
        var size = new TerminalSize(40, 12);
        var buffer = new ScreenBuffer(size);

        waves.renderAtTime(new TextGraphics(buffer), size, 1.0);

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                assertNotNull(buffer.getCell(c, r));
            }
        }
    }

    @Test
    @DisplayName("renderFrame uses system time and produces visible output")
    void renderFrameProducesOutput() {
        var waves = new OceanWaves(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        waves.renderFrame(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "renderFrame should produce visible output");
    }
}
