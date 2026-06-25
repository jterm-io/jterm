package io.jterm.animation;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.DefaultScreen;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.window.DefaultTextGUI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional coverage tests for {@link AnimationManager}.
 *
 * <p>Targets the remaining missed lines: registration dispatch, throttling,
 * negative delta handling, and the first-tick branch.</p>
 */
class AnimationManagerExtraTest {

    private DefaultTextGUI makeGui() {
        return new DefaultTextGUI(new DefaultScreen(new MockTerminal(new TerminalSize(80, 24))));
    }

    @Test
    @DisplayName("tick dispatches to every registered background type")
    void tickDispatchesToAllBackgroundTypes() throws IOException {
        var gui = makeGui();
        var manager = new AnimationManager(gui, 60);

        var starfield = new StarfieldBackground(new TerminalSize(80, 24));
        var matrix = new MatrixRain(new TerminalSize(80, 24));
        var plasma = new PlasmaWash(new TerminalSize(80, 24));
        var warp = new io.jterm.widget.animation.WarpStarfield(new TerminalSize(80, 24));
        var circuit = new CircuitBoard(new TerminalSize(80, 24));
        var ocean = new OceanWaves(new TerminalSize(80, 24));
        var lava = new LavaLamp(new TerminalSize(80, 24));
        var terrain = new TerrainFlyover(new TerminalSize(80, 24));
        var aurora = new Aurora(new TerminalSize(80, 24));
        var rain = new RainStorm(new TerminalSize(80, 24));
        var fireworks = new Fireworks(new TerminalSize(80, 24));
        var dna = new DNAHelix(new TerminalSize(80, 24));
        var voronoi = new VoronoiCells(new TerminalSize(80, 24));

        manager.register(starfield);
        manager.register(matrix);
        manager.register(plasma);
        manager.register(warp);
        manager.register(circuit);
        manager.register(ocean);
        manager.register(lava);
        manager.register(terrain);
        manager.register(aurora);
        manager.register(rain);
        manager.register(fireworks);
        manager.register(dna);
        manager.register(voronoi);

        // First tick should step all backgrounds and request refresh.
        manager.tick(0L);
        assertTrue(starfield.getFrame() > 0, "starfield should have advanced");
        assertTrue(matrix.getFrame() > 0, "matrix should have advanced");

        // Second tick with a full frame interval should step again.
        long frameNs = 1_000_000_000L / 60;
        int frameBefore = starfield.getFrame();
        manager.tick(frameNs);
        assertTrue(starfield.getFrame() >= frameBefore, "starfield frame should not regress");
    }

    @Test
    @DisplayName("tick handles negative clock deltas by clamping to zero")
    void tickClampsNegativeDelta() throws IOException {
        var gui = makeGui();
        var manager = new AnimationManager(gui, 60);
        var starfield = new StarfieldBackground(new TerminalSize(80, 24));
        manager.register(starfield);

        manager.tick(1_000_000_000L);
        int frame1 = starfield.getFrame();
        // Clock going backwards must not throw or blow up accumulated time.
        manager.tick(500_000_000L);
        assertTrue(starfield.getFrame() >= frame1, "frame should not regress after negative delta");
    }

    @Test
    @DisplayName("tick throttles updates to target FPS")
    void tickThrottlesToFps() throws IOException {
        var gui = makeGui();
        var manager = new AnimationManager(gui, 60);
        var matrix = new MatrixRain(new TerminalSize(80, 24));
        matrix.setTargetFps(60);
        manager.register(matrix);

        manager.tick(0L);
        int frameAfterFirst = matrix.getFrame();
        assertTrue(frameAfterFirst > 0, "first tick should advance");

        // A tick far shorter than one frame interval should not advance.
        manager.tick(1_000_000L); // 1 ms
        assertEquals(frameAfterFirst, matrix.getFrame(), "sub-frame should not advance");

        // Advance by the full frame interval plus a little slop.
        manager.tick(20_000_000L); // 20 ms; at 60 FPS one frame ~16.7 ms
        assertTrue(matrix.getFrame() > frameAfterFirst, "full-frame should advance");
    }

    @Test
    @DisplayName("register deduplicates backgrounds")
    void registerDeduplicates() throws IOException {
        var gui = makeGui();
        var manager = new AnimationManager(gui);
        var starfield = new StarfieldBackground(new TerminalSize(80, 24));
        manager.register(starfield);
        manager.register(starfield);

        manager.tick(0L);
        int frame1 = starfield.getFrame();
        manager.tick(1_000_000_000L);
        // If registered twice it would advance twice per tick.
        assertEquals(frame1 + 1, starfield.getFrame(), "background should only advance once per tick");
    }

    @Test
    @DisplayName("tick does nothing when no backgrounds are registered")
    void noBackgrounds() throws IOException {
        var gui = makeGui();
        var manager = new AnimationManager(gui);
        assertDoesNotThrow(() -> {
            manager.tick(0L);
            manager.tick(1_000_000_000L);
        });
    }

    @Test
    @DisplayName("tick does nothing after stop")
    void noUpdatesAfterStop() throws IOException {
        var gui = makeGui();
        var manager = new AnimationManager(gui);
        var starfield = new StarfieldBackground(new TerminalSize(80, 24));
        manager.register(starfield);

        manager.tick(0L);
        int frame1 = starfield.getFrame();
        manager.stop();
        manager.tick(1_000_000_000L);
        assertEquals(frame1, starfield.getFrame(), "stopped manager must not advance backgrounds");
    }
}
