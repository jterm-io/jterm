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
 * Tests for {@link VoronoiCells}: animated background contract, cell
 * rendering, boundary detection, resize handling, and non-blank output.
 */
class VoronoiCellsTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var vc = new VoronoiCells(new TerminalSize(80, 24));
        assertTrue(vc instanceof AnimatedBackground);
        assertEquals(8, vc.targetFps());
        vc.start();
        assertTrue(vc.isRunning());
        vc.stop();
        assertFalse(vc.isRunning());
    }

    @Test
    @DisplayName("constructor creates 8 seeds")
    void createsSeeds() {
        var vc = new VoronoiCells(new TerminalSize(80, 24));
        assertEquals(8, vc.getSeedCount());
    }

    @Test
    @DisplayName("seeds have valid positions and velocities")
    void seedsHaveValidProperties() {
        var vc = new VoronoiCells(new TerminalSize(80, 24));
        for (var seed : vc.getSeeds()) {
            assertTrue(seed.getX() >= 0 && seed.getX() < 80, "x in bounds");
            assertTrue(seed.getY() >= 0 && seed.getY() < 24, "y in bounds");
            assertNotEquals(0.0, seed.getVx() * seed.getVx() + seed.getVy() * seed.getVy(),
                    "expected non-zero velocity");
            assertNotNull(seed.getColor());
        }
    }

    @Test
    @DisplayName("renderFrame clears background to black")
    void clearsBackgroundToBlack() {
        var vc = new VoronoiCells(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        buffer.setCell(10, 2, new TextCell('X', AnsiColor.BRIGHT_RED, AnsiColor.WHITE));

        vc.renderAtTime(new TextGraphics(buffer), size);

        assertEquals(AnsiColor.BLACK, buffer.getCell(0, 0).bg());
        assertEquals(AnsiColor.BLACK, buffer.getCell(10, 2).bg());
    }

    @Test
    @DisplayName("renderFrame produces non-blank output")
    void rendersNonBlankOutput() {
        var vc = new VoronoiCells(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        vc.renderAtTime(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "expected non-blank rendered output");
    }

    @Test
    @DisplayName("renderFrame uses CP437 glyphs")
    void usesExpectedGlyphs() {
        var vc = new VoronoiCells(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        vc.renderAtTime(new TextGraphics(buffer), size);

        Set<Character> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                found.add(buffer.getCell(c, r).character().charAt(0));
            }
        }
        assertTrue(found.contains((char) 0xB0) || found.contains((char) 0xB1),
                "expected Voronoi glyphs (interior or boundary)");
    }

    @Test
    @DisplayName("renderFrame fills most of the screen with cell colors")
    void fillsScreenWithCells() {
        var vc = new VoronoiCells(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        vc.renderAtTime(new TextGraphics(buffer), size);

        int nonBlank = 0;
        int total = size.rows() * size.columns();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        // Voronoi should fill the vast majority of the screen.
        assertTrue(nonBlank > total * 0.8, "expected >80% of screen filled, got " + nonBlank + "/" + total);
    }

    @Test
    @DisplayName("two frames differ over time as seeds drift")
    void framesDifferOverTime() {
        var vc = new VoronoiCells(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        var buffer1 = new ScreenBuffer(size);
        vc.renderAtTime(new TextGraphics(buffer1), size);

        // Advance seeds by rendering many frames.
        for (int i = 0; i < 20; i++) {
            vc.renderAtTime(new TextGraphics(new ScreenBuffer(size)), size);
        }

        var buffer2 = new ScreenBuffer(size);
        vc.renderAtTime(new TextGraphics(buffer2), size);

        boolean anyDifferent = false;
        for (int r = 0; r < size.rows() && !anyDifferent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (!buffer1.getCell(c, r).equals(buffer2.getCell(c, r))) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "frames should differ as seeds drift");
    }

    @Test
    @DisplayName("animation advances time on each renderFrame call")
    void timeAdvances() {
        var vc = new VoronoiCells(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        double before = vc.getTime();
        vc.renderFrame(new TextGraphics(buffer), size);
        assertTrue(vc.getTime() > before, "time should advance each frame");
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var vc = new VoronoiCells(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> vc.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var vc = new VoronoiCells(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        vc.onResize(size);
        assertEquals(size, vc.lastSize());
    }

    @Test
    @DisplayName("onResize regenerates seeds for new dimensions")
    void onResizeRegeneratesSeeds() {
        var vc = new VoronoiCells(new TerminalSize(40, 12));
        var oldSeeds = vc.getSeeds();
        vc.onResize(new TerminalSize(100, 50));
        assertEquals(8, vc.getSeedCount());
        assertNotSame(oldSeeds, vc.getSeeds());
    }

    @Test
    @DisplayName("bounds are respected: no null cells inside size")
    void respectsBounds() {
        var vc = new VoronoiCells(new TerminalSize(40, 12));
        var size = new TerminalSize(40, 12);
        var buffer = new ScreenBuffer(size);

        vc.renderAtTime(new TextGraphics(buffer), size);

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                assertNotNull(buffer.getCell(c, r));
            }
        }
    }

    @Test
    @DisplayName("renderFrame produces visible output")
    void renderFrameProducesOutput() {
        var vc = new VoronoiCells(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        vc.renderFrame(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "renderFrame should produce visible output");
    }
}
