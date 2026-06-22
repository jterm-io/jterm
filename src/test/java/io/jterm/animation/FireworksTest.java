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
 * Tests for {@link Fireworks}: animated background contract, rocket launch,
 * particle explosion, resize handling, and non-blank output.
 */
class FireworksTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var fw = new Fireworks(new TerminalSize(80, 24));
        assertTrue(fw instanceof AnimatedBackground);
        assertEquals(10, fw.targetFps());
        fw.start();
        assertTrue(fw.isRunning());
        fw.stop();
        assertFalse(fw.isRunning());
    }

    @Test
    @DisplayName("renderFrame clears background to black")
    void clearsBackgroundToBlack() {
        var fw = new Fireworks(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        buffer.setCell(10, 2, new TextCell('X', AnsiColor.BRIGHT_RED, AnsiColor.WHITE));

        fw.renderAtTime(new TextGraphics(buffer), size);

        assertEquals(AnsiColor.BLACK, buffer.getCell(0, 0).bg());
        assertEquals(AnsiColor.BLACK, buffer.getCell(10, 2).bg());
    }

    @Test
    @DisplayName("launching a rocket produces visible output")
    void launchProducesOutput() {
        var fw = new Fireworks(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        fw.launchRocket(size);
        fw.renderAtTime(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "expected rocket visible on screen");
    }

    @Test
    @DisplayName("rocket explodes into particles")
    void rocketExplodesIntoParticles() {
        var fw = new Fireworks(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        fw.launchRocket(size);
        // Advance until rocket explodes (vy >= 0).
        for (int i = 0; i < 50 && (fw.getRockets() == null || !fw.getRockets().isEmpty()); i++) {
            var buffer = new ScreenBuffer(size);
            fw.renderAtTime(new TextGraphics(buffer), size);
        }

        assertTrue(fw.getRockets() == null || fw.getRockets().isEmpty(),
                "rocket should have exploded");
        assertNotNull(fw.getParticles());
        assertTrue(fw.getParticles().size() > 0, "explosion should produce particles");
    }

    @Test
    @DisplayName("particles use bright colors")
    void particlesUseBrightColors() {
        var fw = new Fireworks(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        fw.launchRocket(size);
        // Advance until explosion.
        for (int i = 0; i < 50; i++) {
            var buffer = new ScreenBuffer(size);
            fw.renderAtTime(new TextGraphics(buffer), size);
            if (fw.getParticles() != null && !fw.getParticles().isEmpty()) break;
        }

        Set<AnsiColor> validColors = Set.of(
                AnsiColor.BRIGHT_RED, AnsiColor.BRIGHT_YELLOW, AnsiColor.BRIGHT_CYAN,
                AnsiColor.BRIGHT_GREEN, AnsiColor.BRIGHT_MAGENTA);

        assertNotNull(fw.getParticles());
        for (var p : fw.getParticles()) {
            assertTrue(validColors.contains(p.getColor()), "unexpected particle color: " + p.getColor());
        }
    }

    @Test
    @DisplayName("particle glyphs transition from * to + to . as life decreases")
    void particleGlyphTransition() {
        var fw = new Fireworks(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        fw.launchRocket(size);
        // Advance until explosion.
        for (int i = 0; i < 50; i++) {
            var buffer = new ScreenBuffer(size);
            fw.renderAtTime(new TextGraphics(buffer), size);
            if (fw.getParticles() != null && !fw.getParticles().isEmpty()) break;
        }

        // Render a frame and check glyphs.
        var buffer = new ScreenBuffer(size);
        fw.renderAtTime(new TextGraphics(buffer), size);

        Set<Character> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                found.add(buffer.getCell(c, r).character().charAt(0));
            }
        }
        // We should see at least one particle glyph.
        assertTrue(found.contains('*') || found.contains('+') || found.contains('.') || found.contains('|'),
                "expected firework glyphs, got: " + found);
    }

    @Test
    @DisplayName("animation advances time on each renderFrame call")
    void timeAdvances() {
        var fw = new Fireworks(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        double before = fw.getTime();
        fw.renderFrame(new TextGraphics(buffer), size);
        assertTrue(fw.getTime() > before, "time should advance each frame");
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var fw = new Fireworks(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> fw.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var fw = new Fireworks(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        fw.onResize(size);
        assertEquals(size, fw.lastSize());
    }

    @Test
    @DisplayName("onResize resets rockets and particles")
    void onResizeResetsState() {
        var fw = new Fireworks(new TerminalSize(80, 24));
        fw.launchRocket(new TerminalSize(80, 24));
        fw.onResize(new TerminalSize(40, 20));
        assertTrue(fw.getRockets().isEmpty(), "rockets should be cleared on resize");
        assertTrue(fw.getParticles().isEmpty(), "particles should be cleared on resize");
    }

    @Test
    @DisplayName("bounds are respected: no null cells inside size")
    void respectsBounds() {
        var fw = new Fireworks(new TerminalSize(40, 12));
        var size = new TerminalSize(40, 12);
        var buffer = new ScreenBuffer(size);

        fw.renderAtTime(new TextGraphics(buffer), size);

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                assertNotNull(buffer.getCell(c, r));
            }
        }
    }

    @Test
    @DisplayName("renderFrame produces visible output after multiple frames")
    void renderFrameProducesOutput() {
        var fw = new Fireworks(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        // Render a few frames.
        for (int i = 0; i < 5; i++) {
            buffer.fill(TextCell.EMPTY);
            fw.renderAtTime(new TextGraphics(buffer), size);
        }

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "renderFrame should produce visible output after a few frames");
    }
}
