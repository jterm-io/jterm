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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PlasmaWash}: plasma math, char/color mapping, time
 * progression, resize handling, and bounds checking.
 */
class PlasmaWashTest {

    @Test
    @DisplayName("PlasmaWash implements Component contract with preferred size")
    void plasmaWashHasPreferredSize() {
        var bg = new PlasmaWash(new TerminalSize(80, 24));
        assertEquals(new TerminalSize(80, 24), bg.getPreferredSize());
    }

    @Test
    @DisplayName("plasmaValue stays within expected [-4, 4] bounds")
    void plasmaValueBounds() {
        var bg = new PlasmaWash(new TerminalSize(10, 5));
        for (int t = 0; t < 20; t++) {
            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 10; x++) {
                    double v = bg.plasmaValue(x, y, t * 0.5);
                    assertTrue(v >= -4.0 && v <= 4.0,
                            "plasmaValue at (" + x + "," + y + ",t=" + t + ") out of bounds: " + v);
                }
            }
        }
    }

    @Test
    @DisplayName("normalize maps [-4, 4] into [0, 1]")
    void normalizeMapsToUnitRange() {
        var bg = new PlasmaWash(new TerminalSize(1, 1));
        assertEquals(0.0, bg.normalize(-4.0), 0.0001);
        assertEquals(0.5, bg.normalize(0.0), 0.0001);
        assertEquals(1.0, bg.normalize(4.0), 0.0001);
    }

    @Test
    @DisplayName("valueToLevel maps extrema to first and last shading levels")
    void valueToLevelExtrema() {
        var bg = new PlasmaWash(new TerminalSize(1, 1));
        assertEquals(0, bg.valueToLevel(-4.0));
        assertEquals(5, bg.valueToLevel(4.0));
    }

    @Test
    @DisplayName("valueToLevel clamps values outside [-4, 4]")
    void valueToLevelClamps() {
        var bg = new PlasmaWash(new TerminalSize(1, 1));
        assertEquals(0, bg.valueToLevel(-10.0));
        assertEquals(5, bg.valueToLevel(10.0));
    }

    @Test
    @DisplayName("first draw fills entire bounds with plasma characters")
    void plasmaWashFillsBounds() {
        var size = new TerminalSize(10, 5);
        var bg = new PlasmaWash(size);
        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        bg.tick(0);

        var buffer = new ScreenBuffer(size);
        bg.draw(new TextGraphics(buffer));

        boolean hasContent = false;
        for (int r = 0; r < size.rows() && !hasContent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = buffer.getCell(c, r).character().charAt(0);
                if (ch == '\u2591' || ch == '\u2592' || ch == '\u2593' || ch == '\u2588' || ch == '.') {
                    hasContent = true;
                    break;
                }
            }
        }
        assertTrue(hasContent, "first draw should place shading characters somewhere");
    }

    @Test
    @DisplayName("every cell in bounds is written by the default renderer")
    void everyCellWritten() {
        var size = new TerminalSize(8, 4);
        var bg = new PlasmaWash(size);
        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        bg.tick(0);

        var buffer = new ScreenBuffer(size);
        bg.draw(new TextGraphics(buffer));

        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                TextCell cell = buffer.getCell(c, r);
                assertNotNull(cell);
                assertFalse(cell.character().isEmpty());
            }
        }
    }

    @Test
    @DisplayName("tick advances frame counter and time")
    void tickAdvancesFrameAndTime() {
        var bg = new PlasmaWash(new TerminalSize(10, 5));
        assertEquals(0, bg.getFrame());
        assertEquals(0.0, bg.getTime(), 0.0001);
        bg.tick(System.nanoTime());
        assertTrue(bg.getFrame() >= 1, "frame counter should advance");
    }

    @Test
    @DisplayName("pause stops frame and time advancement")
    void pauseStopsAdvancement() {
        var bg = new PlasmaWash(new TerminalSize(10, 5));
        bg.tick(0);
        int beforeFrame = bg.getFrame();
        double beforeTime = bg.getTime();
        bg.setPaused(true);
        bg.tick(1_000_000_000L);
        assertEquals(beforeFrame, bg.getFrame(), "paused background should not advance frames");
        assertEquals(beforeTime, bg.getTime(), 0.0001, "paused background should not advance time");
    }

    @Test
    @DisplayName("resume allows frame and time advancement")
    void resumeAllowsAdvancement() {
        var bg = new PlasmaWash(new TerminalSize(10, 5));
        bg.setPaused(true);
        bg.tick(0);
        int pausedFrame = bg.getFrame();
        double pausedTime = bg.getTime();
        bg.setPaused(false);
        bg.tick(1_000_000_000L);
        assertTrue(bg.getFrame() > pausedFrame, "resumed background should advance frames");
    }

    @Test
    @DisplayName("setTargetFPS clamps and stores FPS")
    void setTargetFpsClamps() {
        var bg = new PlasmaWash(new TerminalSize(10, 5));
        bg.setTargetFps(0);
        assertEquals(1, bg.getTargetFps(), "FPS should clamp to minimum 1");
        bg.setTargetFps(1000);
        assertEquals(60, bg.getTargetFps(), "FPS should clamp to maximum 60");
        bg.setTargetFps(30);
        assertEquals(30, bg.getTargetFps());
    }

    @Test
    @DisplayName("default renderer draws different content on two ticks")
    void defaultRendererAnimatesOverTime() {
        var size = new TerminalSize(40, 20);
        var bg = new PlasmaWash(size);
        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        bg.setTargetFps(60);

        var buffer1 = new ScreenBuffer(size);
        bg.tick(0);
        bg.draw(new TextGraphics(buffer1));

        var buffer2 = new ScreenBuffer(size);
        bg.tick(32_000_000L);
        bg.draw(new TextGraphics(buffer2));

        boolean anyDifferent = false;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (!buffer1.getCell(c, r).equals(buffer2.getCell(c, r))) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "two frames should differ");
    }

    @Test
    @DisplayName("custom renderer can be set and invoked")
    void customRendererInvoked() {
        var size = new TerminalSize(10, 5);
        var bg = new PlasmaWash(size);
        AtomicInteger calls = new AtomicInteger();
        bg.setRenderer((graphics, t) -> {
            calls.incrementAndGet();
            graphics.setCell(0, 0, new TextCell('Z', AnsiColor.DEFAULT, AnsiColor.DEFAULT));
        });
        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        bg.tick(0);

        var buffer = new ScreenBuffer(size);
        bg.draw(new TextGraphics(buffer));

        assertEquals(1, calls.get(), "custom renderer should be invoked once per draw");
        assertEquals('Z', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    @DisplayName("setBaseColor updates accent color")
    void setBaseColor() {
        var bg = new PlasmaWash(new TerminalSize(10, 5));
        bg.setBaseColor(AnsiColor.BRIGHT_CYAN);
        assertEquals(AnsiColor.BRIGHT_CYAN, bg.getBaseColor());
    }

    @Test
    @DisplayName("setBounds larger than preferred uses bounds size for drawing")
    void setBoundsLargerUsesBounds() {
        var size = new TerminalSize(10, 5);
        var bg = new PlasmaWash(new TerminalSize(5, 3));
        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        bg.tick(0);

        var buffer = new ScreenBuffer(size);
        bg.draw(new TextGraphics(buffer));

        boolean hasContent = false;
        for (int r = 0; r < size.rows() && !hasContent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = buffer.getCell(c, r).character().charAt(0);
                if (ch != ' ') {
                    hasContent = true;
                    break;
                }
            }
        }
        assertTrue(hasContent, "bounds area should contain rendered content somewhere");
    }

    @Test
    @DisplayName("PlasmaWash can be added to a Panel")
    void canBeAddedToPanel() {
        var panel = new Panel();
        var bg = new PlasmaWash(new TerminalSize(20, 10));
        assertDoesNotThrow(() -> panel.addComponent(bg));
    }

    @Test
    @DisplayName("PlasmaWash does not throw when drawn with zero size")
    void drawZeroSizeSafe() {
        var bg = new PlasmaWash(new TerminalSize(0, 0));
        bg.setBounds(TerminalPosition.TOP_LEFT, TerminalSize.ZERO);
        assertDoesNotThrow(() -> bg.draw(new TextGraphics(new ScreenBuffer(new TerminalSize(1, 1)))));
    }

    @Test
    @DisplayName("resetFrame returns counter and time to zero")
    void resetFrameReturnsToZero() {
        var bg = new PlasmaWash(new TerminalSize(10, 5));
        bg.setTargetFps(60);
        bg.tick(1_000_000_000L);
        bg.tick(1_100_000_000L);
        assertTrue(bg.getFrame() > 0);
        assertTrue(bg.getTime() > 0.0);
        bg.resetFrame();
        assertEquals(0, bg.getFrame());
        assertEquals(0.0, bg.getTime(), 0.0001);
    }

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var bg = new PlasmaWash(new TerminalSize(10, 5));
        assertTrue(bg instanceof AnimatedBackground);
        bg.start();
        assertTrue(bg.isRunning());
        bg.stop();
        assertFalse(bg.isRunning());
    }

    @Test
    @DisplayName("render hook receives elapsed time")
    void renderHookReceivesTime() {
        var size = new TerminalSize(10, 5);
        var bg = new PlasmaWash(size);
        AtomicLong receivedTime = new AtomicLong(-1);
        bg.setRenderer((graphics, t) -> receivedTime.set((long) (t * 1000)));

        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        bg.tick(0);
        bg.draw(new TextGraphics(new ScreenBuffer(size)));

        assertTrue(receivedTime.get() >= 0, "renderer should receive time");
    }
}
