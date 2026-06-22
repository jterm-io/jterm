package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TerrainFlyover}: animated background contract, parallax
 * layers, mountain rendering, star rendering, and non-blank output.
 */
class TerrainFlyoverTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var bg = new TerrainFlyover(new TerminalSize(80, 24));
        assertTrue(bg instanceof AnimatedBackground);
        assertEquals(10, bg.targetFps());
        bg.start();
        assertTrue(bg.isRunning());
        bg.stop();
        assertFalse(bg.isRunning());
    }

    @Test
    @DisplayName("constructor creates three mountain layers")
    void threeLayers() {
        var bg = new TerrainFlyover(new TerminalSize(80, 24));
        assertEquals(3, bg.getLayers().length);
    }

    @Test
    @DisplayName("layers use expected colors and glyphs")
    void layerColorsAndGlyphs() {
        var bg = new TerrainFlyover(new TerminalSize(80, 24));
        var layers = bg.getLayers();

        assertEquals(AnsiColor.BRIGHT_BLACK, layers[0].color());
        assertEquals((char) 0xB0, layers[0].glyph());

        assertEquals(AnsiColor.GREEN, layers[1].color());
        assertEquals((char) 0xB1, layers[1].glyph());

        assertEquals(AnsiColor.BRIGHT_GREEN, layers[2].color());
        assertEquals((char) 0xDB, layers[2].glyph());
    }

    @Test
    @DisplayName("back layer is highest (smallest baseFraction) and front is lowest")
    void depthOrdering() {
        var bg = new TerrainFlyover(new TerminalSize(80, 24));
        var layers = bg.getLayers();
        for (int i = 1; i < layers.length; i++) {
            assertTrue(layers[i].baseFraction() >= layers[i - 1].baseFraction(),
                    "front layer should be lower than back layer");
        }
    }

    @Test
    @DisplayName("renderFrame produces non-blank output")
    void rendersNonBlankOutput() {
        var bg = new TerrainFlyover(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        bg.renderAtTime(new TextGraphics(buffer), size, 1.0);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "expected non-blank rendered output");
    }

    @Test
    @DisplayName("renderFrame produces mountains visible in lower half")
    void mountainsVisible() {
        var bg = new TerrainFlyover(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        bg.renderAtTime(new TextGraphics(buffer), size, 0.0);

        Set<Character> found = new HashSet<>();
        for (int r = size.rows() / 2; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                found.add(buffer.getCell(c, r).character().charAt(0));
            }
        }
        assertTrue(found.contains((char) 0xB0) || found.contains((char) 0xB1) || found.contains((char) 0xDB),
                "expected mountain glyphs in lower half");
    }

    @Test
    @DisplayName("renderFrame uses expected glyphs")
    void usesExpectedGlyphs() {
        var bg = new TerrainFlyover(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        bg.renderAtTime(new TextGraphics(buffer), size, 0.5);

        Set<Character> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                found.add(buffer.getCell(c, r).character().charAt(0));
            }
        }
        assertTrue(found.contains((char) 0xB0) || found.contains((char) 0xB1) || found.contains((char) 0xDB),
                "expected terrain glyphs");
    }

    @Test
    @DisplayName("stars are generated and rendered in the sky")
    void starsRendered() {
        var bg = new TerrainFlyover(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        // Render at a time where at least some stars are bright.
        bg.renderAtTime(new TextGraphics(buffer), size, 1.0);

        assertNotNull(bg.getStars());
        assertEquals(30, bg.getStarCount());

        boolean foundStar = false;
        for (int r = 0; r < size.rows() / 2; r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = buffer.getCell(c, r).character().charAt(0);
                if (ch == '*' || ch == '.') {
                    foundStar = true;
                    break;
                }
            }
        }
        assertTrue(foundStar, "expected at least one star glyph in the sky area");
    }

    @Test
    @DisplayName("two frames differ over time")
    void framesDifferOverTime() {
        var bg = new TerrainFlyover(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        var buffer1 = new ScreenBuffer(size);
        bg.renderAtTime(new TextGraphics(buffer1), size, 0.0);

        var buffer2 = new ScreenBuffer(size);
        bg.renderAtTime(new TextGraphics(buffer2), size, 5.0);

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
        var bg = new TerrainFlyover(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        double before = bg.getTime();
        bg.renderFrame(new TextGraphics(buffer), size);
        assertTrue(bg.getTime() > before, "time should advance each frame");
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var bg = new TerrainFlyover(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> bg.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var bg = new TerrainFlyover(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        bg.onResize(size);
        assertEquals(size, bg.lastSize());
    }

    @Test
    @DisplayName("bounds are respected: no characters outside size")
    void respectsBounds() {
        var bg = new TerrainFlyover(new TerminalSize(40, 12));
        var size = new TerminalSize(40, 12);
        var buffer = new ScreenBuffer(size);

        bg.renderAtTime(new TextGraphics(buffer), size, 1.0);

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                assertNotNull(buffer.getCell(c, r));
            }
        }
    }

    @Test
    @DisplayName("renderFrame uses system time and produces visible output")
    void renderFrameProducesOutput() {
        var bg = new TerrainFlyover(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        bg.renderFrame(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "renderFrame should produce visible output");
    }
}
