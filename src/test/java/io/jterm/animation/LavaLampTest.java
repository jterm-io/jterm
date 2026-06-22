package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.widget.Panel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LavaLamp}: animated background contract, metaball rendering,
 * warm color palette, resize handling, and non-blank output.
 */
class LavaLampTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var lamp = new LavaLamp(new TerminalSize(80, 24));
        assertTrue(lamp instanceof AnimatedBackground);
        assertEquals(8, lamp.targetFps());
        lamp.start();
        assertTrue(lamp.isRunning());
        lamp.stop();
        assertFalse(lamp.isRunning());
    }

    @Test
    @DisplayName("can be added to a Panel as a Component")
    void canBeAddedToPanel() {
        var panel = new Panel();
        var lamp = new LavaLamp(new TerminalSize(20, 10));
        assertDoesNotThrow(() -> panel.addComponent(lamp));
    }

    @Test
    @DisplayName("constructor stores preferred size")
    void preferredSize() {
        var size = new TerminalSize(80, 24);
        var lamp = new LavaLamp(size);
        assertEquals(size, lamp.getPreferredSize());
    }

    @Test
    @DisplayName("resize creates five metaballs")
    void resizeCreatesFiveBalls() {
        var lamp = new LavaLamp(new TerminalSize(80, 24));
        lamp.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 20));
        assertEquals(5, lamp.getBalls().length);
    }

    @Test
    @DisplayName("metaballs have positive radius and vertical drift")
    void ballsHaveRadiusAndDrift() {
        var lamp = new LavaLamp(new TerminalSize(80, 24));
        lamp.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 20));
        for (var ball : lamp.getBalls()) {
            assertTrue(ball.getRadius() >= 3.0 && ball.getRadius() <= 6.0,
                    "expected radius in [3,6], got " + ball.getRadius());
            assertNotEquals(0.0, ball.getVy(), "expected non-zero vertical drift");
        }
    }

    @Test
    @DisplayName("renderFrame clears outside to black")
    void clearsOutsideToBlack() {
        var lamp = new LavaLamp(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        buffer.setCell(10, 2, new TextCell('X', AnsiColor.BRIGHT_RED, AnsiColor.WHITE));

        lamp.renderFrame(new TextGraphics(buffer), size);

        assertEquals(AnsiColor.BLACK, buffer.getCell(0, 0).bg());
        assertEquals(AnsiColor.BLACK, buffer.getCell(10, 2).bg());
        assertEquals(' ', buffer.getCell(10, 2).character().charAt(0));
    }

    @Test
    @DisplayName("renderFrame produces non-blank output")
    void rendersNonBlankOutput() {
        var lamp = new LavaLamp(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        lamp.renderFrame(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "expected non-blank rendered output");
    }

    @Test
    @DisplayName("renderFrame uses expected CP437 glyphs")
    void usesExpectedGlyphs() {
        var lamp = new LavaLamp(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        lamp.renderFrame(new TextGraphics(buffer), size);

        Set<Character> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                found.add(buffer.getCell(c, r).character().charAt(0));
            }
        }
        assertTrue(found.contains((char) 0xDB) || found.contains((char) 0xB0),
                "expected lava lamp glyphs (full block or half block)");
    }

    @Test
    @DisplayName("renderFrame uses warm ANSI colors")
    void usesWarmColors() {
        var lamp = new LavaLamp(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        lamp.renderFrame(new TextGraphics(buffer), size);

        Set<AnsiColor> expected = Set.of(
                AnsiColor.RED, AnsiColor.YELLOW,
                AnsiColor.BRIGHT_RED, AnsiColor.BRIGHT_YELLOW, AnsiColor.BRIGHT_WHITE);
        Set<AnsiColor> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') {
                    found.add((AnsiColor) buffer.getCell(c, r).fg());
                }
            }
        }
        assertFalse(found.isEmpty(), "expected at least one warm color");
        for (var color : found) {
            assertTrue(expected.contains(color), "unexpected color: " + color);
        }
    }

    @Test
    @DisplayName("tick advances frame counter")
    void tickAdvancesFrame() {
        var lamp = new LavaLamp(new TerminalSize(10, 5));
        assertEquals(0, lamp.getFrame());
        lamp.tick(System.nanoTime());
        assertTrue(lamp.getFrame() >= 1);
    }

    @Test
    @DisplayName("pause stops frame advancement")
    void pauseStopsFrameAdvancement() {
        var lamp = new LavaLamp(new TerminalSize(10, 5));
        lamp.tick(0);
        int before = lamp.getFrame();
        lamp.setPaused(true);
        lamp.tick(1_000_000_000L);
        assertEquals(before, lamp.getFrame());
    }

    @Test
    @DisplayName("resume allows frame advancement")
    void resumeAllowsFrameAdvancement() {
        var lamp = new LavaLamp(new TerminalSize(10, 5));
        lamp.setPaused(true);
        lamp.tick(0);
        int pausedFrame = lamp.getFrame();
        lamp.setPaused(false);
        lamp.tick(1_000_000_000L);
        assertTrue(lamp.getFrame() > pausedFrame);
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var lamp = new LavaLamp(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        lamp.onResize(size);
        assertEquals(size, lamp.lastSize());
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var lamp = new LavaLamp(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> lamp.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("bounds are respected: no null cells inside size")
    void respectsBounds() {
        var lamp = new LavaLamp(new TerminalSize(40, 12));
        var size = new TerminalSize(40, 12);
        var buffer = new ScreenBuffer(size);

        lamp.renderFrame(new TextGraphics(buffer), size);

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                assertNotNull(buffer.getCell(c, r));
            }
        }
    }

    @Test
    @DisplayName("component draw produces visible output")
    void componentDrawProducesOutput() {
        var size = new TerminalSize(40, 12);
        var lamp = new LavaLamp(size);
        lamp.setBounds(TerminalPosition.TOP_LEFT, size);
        lamp.tick(0);

        var buffer = new ScreenBuffer(size);
        lamp.draw(new TextGraphics(buffer));

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "component draw should produce visible output");
    }
}
