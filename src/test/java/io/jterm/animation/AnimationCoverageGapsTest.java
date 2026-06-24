package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.widget.Panel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers public animation methods with zero line coverage.
 */
class AnimationCoverageGapsTest {

    @Test
    void packetFlowRenderAtTimeAndPulseFrames() {
        var flow = new PacketFlow(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        flow.renderAtTime(new TextGraphics(buffer), size, 0.0);
        assertFalse(flow.getPulseFrames().isEmpty());
    }

    @Test
    void lightningStormFramesUntilNextStrike() {
        var storm = new LightningStorm(new TerminalSize(80, 24));
        assertTrue(storm.getFramesUntilNextStrike() >= 0);
    }

    @Test
    void matrixRainIsPaused() {
        var rain = new MatrixRain(new TerminalSize(40, 20));
        assertFalse(rain.isPaused());
        rain.setPaused(true);
        assertTrue(rain.isPaused());
    }

    @Test
    void typewriterEffectLineDelayAndColors() {
        var tw = new TypewriterEffect(List.of("A"), AnsiColor.WHITE, AnsiColor.BLACK);
        assertEquals(0, tw.getLineDelay());
        tw.setLineDelay(5);
        assertEquals(5, tw.getLineDelay());
        tw.setColor(AnsiColor.RED);
        tw.setBackgroundColor(AnsiColor.BLUE);
        assertEquals(AnsiColor.RED, tw.getColor());
        assertEquals(AnsiColor.BLUE, tw.getBackgroundColor());
        assertTrue(tw.isCursorShown());
    }

    @Test
    void lavaLampGettersAndReset() {
        var lamp = new LavaLamp(new TerminalSize(20, 10));
        assertEquals(8, lamp.getTargetFps());
        lamp.setTargetFps(15);
        assertEquals(15, lamp.getTargetFps());
        assertFalse(lamp.isPaused());
        lamp.tick(0);
        assertTrue(lamp.getTotalMs() >= 0);
        lamp.resetFrame();
        assertEquals(0, lamp.getFrame());
    }

    @Test
    void spirographGetFrame() {
        var spiro = new Spirograph(new TerminalSize(40, 20));
        assertEquals(0, spiro.getFrame());
        spiro.renderFrame(new TextGraphics(new ScreenBuffer(new TerminalSize(40, 20))), new TerminalSize(40, 20));
        assertTrue(spiro.getFrame() > 0);
    }

    @Test
    void circuitBoardIsPaused() {
        var cb = new CircuitBoard(new TerminalSize(40, 20));
        assertFalse(cb.isPaused());
        cb.setPaused(true);
        assertTrue(cb.isPaused());
    }

    @Test
    void animationManagerSetTargetFpsAndIsRunning() {
        var term = new io.jterm.core.MockTerminal(new TerminalSize(40, 20));
        var screen = new io.jterm.screen.DefaultScreen(term);
        var gui = new io.jterm.window.DefaultTextGUI(screen);
        var manager = new AnimationManager(gui, 30);
        assertTrue(manager.isRunning());
        manager.setTargetFps(60);
        assertEquals(60, manager.getTargetFps());
        manager.stop();
        assertFalse(manager.isRunning());
    }

    @Test
    void plasmaWashGetters() {
        var plasma = new PlasmaWash(new TerminalSize(20, 10));
        assertFalse(plasma.isPaused());
        assertNull(plasma.getRenderer());
        assertEquals(15, plasma.getTargetFps());
    }

    @Test
    void moirePatternsGetFrame() {
        var moire = new MoirePatterns(new TerminalSize(40, 20));
        assertEquals(0, moire.getFrame());
    }
}
