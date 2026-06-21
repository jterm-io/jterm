package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.widget.Panel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CircuitBoard}: grid generation, trace layout, pulse movement,
 * color choices, and Component / AnimatedBackground contract.
 */
class CircuitBoardTest {

    @Test
    @DisplayName("CircuitBoard implements Component contract with preferred size")
    void circuitBoardHasPreferredSize() {
        var board = new CircuitBoard(new TerminalSize(80, 24));
        assertEquals(new TerminalSize(80, 24), board.getPreferredSize());
    }

    @Test
    @DisplayName("CircuitBoard can be added to a Panel")
    void canBeAddedToPanel() {
        var panel = new Panel();
        var board = new CircuitBoard(new TerminalSize(20, 10));
        assertDoesNotThrow(() -> panel.addComponent(board));
    }

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var board = new CircuitBoard(new TerminalSize(10, 5));
        assertTrue(board instanceof AnimatedBackground);
        assertEquals(8, board.targetFps());
        board.start();
        assertTrue(board.isRunning());
        board.stop();
        assertFalse(board.isRunning());
    }

    @Test
    @DisplayName("setTargetFPS clamps and stores FPS")
    void setTargetFpsClamps() {
        var board = new CircuitBoard(new TerminalSize(10, 5));
        board.setTargetFps(0);
        assertEquals(1, board.getTargetFps());
        board.setTargetFps(1000);
        assertEquals(60, board.getTargetFps());
        board.setTargetFps(30);
        assertEquals(30, board.getTargetFps());
    }

    @Test
    @DisplayName("tick advances frame counter")
    void tickAdvancesFrame() {
        var board = new CircuitBoard(new TerminalSize(10, 5));
        assertEquals(0, board.getFrame());
        board.tick(System.nanoTime());
        assertTrue(board.getFrame() >= 1);
    }

    @Test
    @DisplayName("pause stops frame advancement")
    void pauseStopsFrameAdvancement() {
        var board = new CircuitBoard(new TerminalSize(10, 5));
        board.tick(0);
        int before = board.getFrame();
        board.setPaused(true);
        board.tick(1_000_000_000L);
        assertEquals(before, board.getFrame());
    }

    @Test
    @DisplayName("resume allows frame advancement")
    void resumeAllowsFrameAdvancement() {
        var board = new CircuitBoard(new TerminalSize(10, 5));
        board.setPaused(true);
        board.tick(0);
        int pausedFrame = board.getFrame();
        board.setPaused(false);
        board.tick(1_000_000_000L);
        assertTrue(board.getFrame() > pausedFrame);
    }

    @Test
    @DisplayName("resize generates traces")
    void resizeGeneratesTraces() {
        var board = new CircuitBoard(new TerminalSize(10, 5));
        assertTrue(board.getTraces().isEmpty());
        board.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 20));
        assertFalse(board.getTraces().isEmpty(), "traces should be generated after resize");
    }

    @Test
    @DisplayName("traces are axis-aligned and fit grid")
    void tracesAreAxisAligned() {
        var board = new CircuitBoard(new TerminalSize(10, 5));
        board.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 20));
        for (var trace : board.getTraces()) {
            assertTrue(trace.gx0() == trace.gx1() || trace.gy0() == trace.gy1(),
                    "trace should be horizontal or vertical");
            assertTrue(trace.gx0() >= 0 && trace.gx1() >= 0 && trace.gy0() >= 0 && trace.gy1() >= 0,
                    "trace grid coordinates should be non-negative");
        }
    }

    @Test
    @DisplayName("draw fills bounds with circuit characters")
    void drawFillsBounds() {
        var size = new TerminalSize(40, 20);
        var board = new CircuitBoard(size);
        board.setBounds(TerminalPosition.TOP_LEFT, size);
        board.tick(0);

        var buffer = new ScreenBuffer(size);
        board.draw(new TextGraphics(buffer));

        boolean hasTrace = false;
        boolean hasPad = false;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                char ch = buffer.getCell(c, r).character().charAt(0);
                if (ch == '\u2500' || ch == '\u2502' || ch == '\u2510' || ch == '\u2518' || ch == '\u2514') {
                    hasTrace = true;
                }
                if (ch == '+' || ch == '\u00B7') {
                    hasPad = true;
                }
            }
        }
        assertTrue(hasTrace, "should draw trace line characters");
        assertTrue(hasPad, "should draw pad/via characters");
    }

    @Test
    @DisplayName("pulses spawn and advance")
    void pulsesSpawnAndAdvance() {
        var size = new TerminalSize(40, 20);
        var board = new CircuitBoard(size);
        board.setBounds(TerminalPosition.TOP_LEFT, size);
        board.tick(0);

        // Force a pulse to exist by drawing many frames.
        for (int i = 0; i < 200 && board.getPulses().isEmpty(); i++) {
            var buffer = new ScreenBuffer(size);
            board.tick(i * 80_000_000L);
            board.draw(new TextGraphics(buffer));
        }

        assertFalse(board.getPulses().isEmpty(), "pulses should spawn over many frames");

        int before = board.getPulses().get(0).progress;
        for (int i = 0; i < 5; i++) {
            var buffer = new ScreenBuffer(size);
            board.tick(System.nanoTime() + i * 80_000_000L);
            board.draw(new TextGraphics(buffer));
        }
        assertTrue(board.getPulses().get(0).progress > before, "pulse should advance");
    }

    @Test
    @DisplayName("pulse head is bright and bold")
    void pulseHeadIsBright() {
        var size = new TerminalSize(40, 20);
        var board = new CircuitBoard(size);
        board.setBounds(TerminalPosition.TOP_LEFT, size);
        board.tick(0);

        var buffer = new ScreenBuffer(size);
        // Run enough frames to guarantee a pulse appears somewhere.
        for (int i = 0; i < 300; i++) {
            board.tick(i * 80_000_000L);
            board.draw(new TextGraphics(buffer));
        }

        boolean foundBright = false;
        for (int r = 0; r < size.rows() && !foundBright; r++) {
            for (int c = 0; c < size.columns(); c++) {
                var cell = buffer.getCell(c, r);
                if (cell.fg() == AnsiColor.BRIGHT_GREEN && cell.modifiers().contains(SGR.BOLD)) {
                    foundBright = true;
                    break;
                }
            }
        }
        assertTrue(foundBright, "a pulse head should be bright green and bold");
    }

    @Test
    @DisplayName("two frames differ over time")
    void framesDifferOverTime() {
        var size = new TerminalSize(40, 20);
        var board = new CircuitBoard(size);
        board.setBounds(TerminalPosition.TOP_LEFT, size);
        board.setTargetFps(60);

        var buffer1 = new ScreenBuffer(size);
        board.tick(0);
        board.draw(new TextGraphics(buffer1));

        var buffer2 = new ScreenBuffer(size);
        for (int i = 0; i < 120; i++) {
            board.tick(i * 30_000_000L);
            board.draw(new TextGraphics(buffer2));
        }

        boolean anyDifferent = false;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (!buffer1.getCell(c, r).equals(buffer2.getCell(c, r))) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "frames should differ as pulses move");
    }

    @Test
    @DisplayName("resetFrame clears traces and pulses")
    void resetFrameClearsState() {
        var board = new CircuitBoard(new TerminalSize(40, 20));
        board.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 20));
        board.tick(0);
        assertFalse(board.getTraces().isEmpty());
        board.resetFrame();
        assertEquals(0, board.getFrame());
        assertTrue(board.getTraces().isEmpty());
        assertTrue(board.getPulses().isEmpty());
    }

    @Test
    @DisplayName("drawing empty screen does not throw")
    void drawZeroSizeSafe() {
        var board = new CircuitBoard(new TerminalSize(0, 0));
        board.setBounds(TerminalPosition.TOP_LEFT, TerminalSize.ZERO);
        assertDoesNotThrow(() -> board.draw(new TextGraphics(new ScreenBuffer(new TerminalSize(1, 1)))));
    }

    @Test
    @DisplayName("setBounds larger than preferred uses bounds size for drawing")
    void setBoundsLargerUsesBounds() {
        var size = new TerminalSize(40, 20);
        var board = new CircuitBoard(new TerminalSize(5, 3));
        board.setBounds(TerminalPosition.TOP_LEFT, size);
        board.tick(0);

        var buffer = new ScreenBuffer(size);
        board.draw(new TextGraphics(buffer));

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
}
