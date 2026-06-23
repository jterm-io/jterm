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
 * Tests for {@link LightningStorm}: AnimatedBackground contract, non-blank
 * output, lightning bolt rendering, flash behavior, and small terminal sizes.
 */
class LightningStormTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var storm = new LightningStorm(new TerminalSize(80, 24));
        assertTrue(storm instanceof AnimatedBackground);
        assertEquals(8, storm.targetFps());
        storm.start();
        assertTrue(storm.isRunning());
        storm.stop();
        assertFalse(storm.isRunning());
    }

    @Test
    @DisplayName("renderFrame clears background to black")
    void clearsBackgroundToBlack() {
        var storm = new LightningStorm(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        buffer.setCell(10, 2, new TextCell('X', AnsiColor.BRIGHT_RED, AnsiColor.WHITE));

        storm.renderAtTime(new TextGraphics(buffer), size, 0.0);

        assertEquals(AnsiColor.BLACK, buffer.getCell(0, 0).bg());
        assertEquals(AnsiColor.BLACK, buffer.getCell(10, 2).bg());
    }

    @Test
    @DisplayName("renderFrame produces non-empty output")
    void rendersNonBlankOutput() {
        var storm = new LightningStorm(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        storm.renderAtTime(new TextGraphics(buffer), size, 0.0);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "expected non-blank rendered output");
    }

    @Test
    @DisplayName("lightning bolt eventually appears when forced")
    void lightningEventuallyAppears() {
        var storm = new LightningStorm(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        // Force a bolt by rendering many frames; real-time frames decrement the
        // strike counter. We keep rendering until a bolt is generated.
        boolean sawBolt = false;
        for (int i = 0; i < 200 && !sawBolt; i++) {
            var buffer = new ScreenBuffer(size);
            storm.renderFrame(new TextGraphics(buffer), size);
            if (storm.getCurrentBolt() != null && !storm.getCurrentBolt().isEmpty()) {
                sawBolt = true;
            }
        }
        assertTrue(sawBolt, "expected a lightning bolt to appear within 200 frames");
    }

    @Test
    @DisplayName("flash state changes over frames")
    void flashStateChanges() {
        var storm = new LightningStorm(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        int flashesSeen = 0;
        for (int i = 0; i < 200; i++) {
            var buffer = new ScreenBuffer(size);
            storm.renderFrame(new TextGraphics(buffer), size);
            if (storm.getFlashFrames() > 0) {
                flashesSeen++;
            }
        }
        assertTrue(flashesSeen > 0, "expected flash state to activate over frames");
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var storm = new LightningStorm(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> storm.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("small terminal sizes render without throwing")
    void smallTerminalSizes() {
        var storm = new LightningStorm(new TerminalSize(1, 1));
        var size = new TerminalSize(1, 1);
        var buffer = new ScreenBuffer(size);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                storm.renderFrame(new TextGraphics(buffer), size);
            }
        });
    }

    @Test
    @DisplayName("forced bolt renders zig-zag slash characters")
    void forcedBoltUsesSlashCharacters() {
        var storm = new LightningStorm(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        // Force bolt generation indirectly through many real-time frames.
        Set<Character> found = new HashSet<>();
        for (int i = 0; i < 100 && found.isEmpty(); i++) {
            var buffer = new ScreenBuffer(size);
            storm.renderFrame(new TextGraphics(buffer), size);
            if (storm.getCurrentBolt() != null) {
                for (int r = 0; r < size.rows(); r++) {
                    for (int c = 0; c < size.columns(); c++) {
                        char ch = buffer.getCell(c, r).character().charAt(0);
                        if (ch == '/' || ch == '\\') {
                            found.add(ch);
                        }
                    }
                }
            }
        }
        assertTrue(found.contains('/') || found.contains('\\'),
                "expected lightning bolt to use / or \\\\ glyphs");
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var storm = new LightningStorm(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        storm.onResize(size);
        assertEquals(size, storm.lastSize());
    }

    @Test
    @DisplayName("time advances on each renderFrame call")
    void timeAdvances() {
        var storm = new LightningStorm(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        double before = storm.getTime();
        storm.renderFrame(new TextGraphics(buffer), size);
        assertTrue(storm.getTime() > before, "time should advance each frame");
    }
}
