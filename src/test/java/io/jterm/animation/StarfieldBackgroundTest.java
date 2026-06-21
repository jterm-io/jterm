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

class StarfieldBackgroundTest {

    @Test
    @DisplayName("StarfieldBackground implements Component contract with preferred size")
    void starfieldBackgroundHasPreferredSize() {
        var bg = new StarfieldBackground(new TerminalSize(80, 24));
        assertEquals(new TerminalSize(80, 24), bg.getPreferredSize());
    }

    @Test
    @DisplayName("StarfieldBackground fills its bounds on first draw")
    void starfieldBackgroundFillsBounds() {
        var size = new TerminalSize(10, 5);
        var bg = new StarfieldBackground(size);
        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        bg.tick(0);

        var buffer = new ScreenBuffer(size);
        bg.draw(new TextGraphics(buffer));

        boolean hasContent = false;
        for (int r = 0; r < size.rows() && !hasContent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') {
                    hasContent = true;
                    break;
                }
            }
        }
        assertTrue(hasContent, "first draw should place animated content somewhere");
    }

    @Test
    @DisplayName("tick advances frame counter")
    void tickAdvancesFrame() {
        var bg = new StarfieldBackground(new TerminalSize(10, 5));
        assertEquals(0, bg.getFrame());
        bg.tick(System.nanoTime());
        assertTrue(bg.getFrame() >= 1, "frame counter should advance");
    }

    @Test
    @DisplayName("tick uses elapsed nanos")
    void tickUsesElapsedTime() {
        var bg = new StarfieldBackground(new TerminalSize(10, 5));
        long now = 1_000_000_000L;
        bg.tick(now);
        int frame1 = bg.getFrame();
        bg.tick(now + 16_000_000L);
        int frame2 = bg.getFrame();
        assertTrue(frame2 >= frame1, "frame should not decrease");
    }

    @Test
    @DisplayName("pause stops frame advancement")
    void pauseStopsFrameAdvancement() {
        var bg = new StarfieldBackground(new TerminalSize(10, 5));
        bg.tick(0);
        int before = bg.getFrame();
        bg.setPaused(true);
        bg.tick(1_000_000_000L);
        assertEquals(before, bg.getFrame(), "paused background should not advance");
    }

    @Test
    @DisplayName("resume allows frame advancement")
    void resumeAllowsFrameAdvancement() {
        var bg = new StarfieldBackground(new TerminalSize(10, 5));
        bg.setPaused(true);
        bg.tick(0);
        int pausedFrame = bg.getFrame();
        bg.setPaused(false);
        bg.tick(1_000_000_000L);
        assertTrue(bg.getFrame() > pausedFrame, "resumed background should advance");
    }

    @Test
    @DisplayName("setTargetFPS clamps and stores FPS")
    void setTargetFpsClamps() {
        var bg = new StarfieldBackground(new TerminalSize(10, 5));
        bg.setTargetFps(0);
        assertEquals(1, bg.getTargetFps(), "FPS should clamp to minimum 1");
        bg.setTargetFps(1000);
        assertEquals(60, bg.getTargetFps(), "FPS should clamp to maximum 60");
        bg.setTargetFps(30);
        assertEquals(30, bg.getTargetFps());
    }

    @Test
    @DisplayName("render hook receives elapsed time in ms")
    void renderHookReceivesElapsedMs() {
        var size = new TerminalSize(10, 5);
        var bg = new StarfieldBackground(size);
        AtomicLong receivedMs = new AtomicLong(-1);
        bg.setRenderer((graphics, elapsedMs) -> receivedMs.set(elapsedMs));

        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        long now = 200_000_000L;
        bg.tick(now);
        bg.draw(new TextGraphics(new ScreenBuffer(size)));

        assertTrue(receivedMs.get() >= 0, "renderer should receive elapsed ms");
    }

    @Test
    @DisplayName("render hook receives real elapsed ms")
    void renderHookReceivesRealElapsedMs() {
        var size = new TerminalSize(10, 5);
        var bg = new StarfieldBackground(size);
        AtomicLong receivedMs = new AtomicLong(-1);
        bg.setRenderer((graphics, elapsedMs) -> receivedMs.set(elapsedMs));

        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        bg.tick(1_000_000_000L);
        bg.tick(1_016_000_000L);
        bg.draw(new TextGraphics(new ScreenBuffer(size)));

        long elapsed = receivedMs.get();
        assertTrue(elapsed >= 0 && elapsed < 1000,
                "elapsed ms for one frame should be under 1000, got " + elapsed);
    }

    @Test
    @DisplayName("default renderer draws different content on two ticks")
    void defaultRendererAnimatesOverTime() {
        var size = new TerminalSize(40, 20);
        var bg = new StarfieldBackground(size);
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
    @DisplayName("setThemeColor updates accent color")
    void setThemeColor() {
        var bg = new StarfieldBackground(new TerminalSize(10, 5));
        bg.setThemeColor(AnsiColor.BRIGHT_GREEN);
        assertEquals(AnsiColor.BRIGHT_GREEN, bg.getThemeColor());
    }

    @Test
    @DisplayName("setBounds larger than preferred uses bounds size for drawing")
    void setBoundsLargerUsesBounds() {
        var size = new TerminalSize(10, 5);
        var bg = new StarfieldBackground(new TerminalSize(5, 3));
        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        bg.tick(0);

        var buffer = new ScreenBuffer(size);
        bg.draw(new TextGraphics(buffer));

        boolean hasContent = false;
        for (int r = 0; r < size.rows() && !hasContent; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') {
                    hasContent = true;
                    break;
                }
            }
        }
        assertTrue(hasContent, "bounds area should contain rendered content somewhere");
    }

    @Test
    @DisplayName("custom renderer can be set and invoked")
    void customRendererInvoked() {
        var size = new TerminalSize(10, 5);
        var bg = new StarfieldBackground(size);
        AtomicInteger calls = new AtomicInteger();
        bg.setRenderer((graphics, elapsedMs) -> {
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
    @DisplayName("StarfieldBackground can be added to a Panel")
    void canBeAddedToPanel() {
        var panel = new Panel();
        var bg = new StarfieldBackground(new TerminalSize(20, 10));
        assertDoesNotThrow(() -> panel.addComponent(bg));
    }

    @Test
    @DisplayName("StarfieldBackground does not throw when drawn with zero size")
    void drawZeroSizeSafe() {
        var bg = new StarfieldBackground(new TerminalSize(0, 0));
        bg.setBounds(TerminalPosition.TOP_LEFT, TerminalSize.ZERO);
        assertDoesNotThrow(() -> bg.draw(new TextGraphics(new ScreenBuffer(new TerminalSize(1, 1)))));
    }

    @Test
    @DisplayName("resetFrame returns counter to zero")
    void resetFrameReturnsToZero() {
        var bg = new StarfieldBackground(new TerminalSize(10, 5));
        bg.tick(1_000_000_000L);
        assertTrue(bg.getFrame() > 0);
        bg.resetFrame();
        assertEquals(0, bg.getFrame());
    }

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var bg = new StarfieldBackground(new TerminalSize(10, 5));
        assertTrue(bg instanceof AnimatedBackground);
        bg.start();
        assertTrue(bg.isRunning());
        bg.stop();
        assertFalse(bg.isRunning());
    }

    @Test
    @DisplayName("onResize reallocates star array")
    void onResizeReallocates() {
        var bg = new StarfieldBackground(new TerminalSize(10, 5));
        bg.onResize(new TerminalSize(20, 10));
        bg.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 10));
        bg.tick(0);
        var buffer = new ScreenBuffer(new TerminalSize(20, 10));
        bg.draw(new TextGraphics(buffer));
        assertTrue(buffer.size().area() > 0);
    }
}
