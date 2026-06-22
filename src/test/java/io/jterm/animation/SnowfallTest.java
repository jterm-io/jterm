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
 * Tests for {@link Snowfall}: AnimatedBackground contract, flake rendering,
 * downward motion, small-terminal resilience, and wind gust behavior.
 */
class SnowfallTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var snow = new Snowfall(new TerminalSize(80, 24));
        assertTrue(snow instanceof AnimatedBackground);
        assertEquals(12, snow.targetFps());
        snow.start();
        assertTrue(snow.isRunning());
        snow.stop();
        assertFalse(snow.isRunning());
    }

    @Test
    @DisplayName("flake count scales with terminal size")
    void flakeCountScales() {
        var small = new Snowfall(new TerminalSize(10, 10));
        assertEquals(50, small.getFlakeCount(), "minimum flake count is 50");

        var large = new Snowfall(new TerminalSize(200, 60));
        assertEquals(100, large.getFlakeCount(), "flake count capped at 100");
    }

    @Test
    @DisplayName("renderFrame produces non-empty output")
    void rendersNonBlankOutput() {
        var snow = new Snowfall(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        snow.renderFrame(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "expected non-blank rendered output");
    }

    @Test
    @DisplayName("renderFrame uses snow glyphs")
    void usesSnowGlyphs() {
        var snow = new Snowfall(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        snow.renderFrame(new TextGraphics(buffer), size);

        Set<Character> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                found.add(buffer.getCell(c, r).character().charAt(0));
            }
        }
        assertTrue(found.contains('*') || found.contains('.') || found.contains(',') || found.contains(';'),
                "expected snowflake glyphs");
    }

    @Test
    @DisplayName("flakes move downward over frames")
    void flakesMoveDownward() {
        var snow = new Snowfall(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);

        snow.renderFrame(new TextGraphics(new ScreenBuffer(size)), size);
        double[] yBefore = new double[snow.getFlakeCount()];
        for (int i = 0; i < snow.getFlakeCount(); i++) {
            yBefore[i] = snow.getFlakes()[i].y();
        }

        snow.renderFrame(new TextGraphics(new ScreenBuffer(size)), size);
        int movedDown = 0;
        for (int i = 0; i < snow.getFlakeCount(); i++) {
            double yAfter = snow.getFlakes()[i].y();
            if (yAfter > yBefore[i]) movedDown++;
        }
        assertTrue(movedDown > 0, "at least one flake should have moved downward");
    }

    @Test
    @DisplayName("renderFrame clears background to black")
    void clearsBackgroundToBlack() {
        var snow = new Snowfall(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        buffer.setCell(10, 2, new TextCell('X', AnsiColor.BRIGHT_RED, AnsiColor.WHITE));

        snow.renderFrame(new TextGraphics(buffer), size);

        assertEquals(AnsiColor.BLACK, buffer.getCell(0, 0).bg());
        assertEquals(AnsiColor.BLACK, buffer.getCell(10, 2).bg());
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var snow = new Snowfall(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> snow.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("small terminal sizes render without throwing")
    void smallTerminalSizes() {
        var snow = new Snowfall(new TerminalSize(1, 1));
        var size = new TerminalSize(1, 1);
        var buffer = new ScreenBuffer(size);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                snow.renderFrame(new TextGraphics(buffer), size);
            }
        });
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var snow = new Snowfall(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        snow.onResize(size);
        assertEquals(size, snow.lastSize());
    }

    @Test
    @DisplayName("time advances on each renderFrame call")
    void timeAdvances() {
        var snow = new Snowfall(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        double before = snow.getTime();
        snow.renderFrame(new TextGraphics(buffer), size);
        assertTrue(snow.getTime() > before, "time should advance each frame");
    }

    @Test
    @DisplayName("flakes use white foreground colors")
    void flakesUseWhiteColors() {
        var snow = new Snowfall(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        snow.renderFrame(new TextGraphics(buffer), size);

        boolean foundWhite = false;
        for (int r = 0; r < size.rows() && !foundWhite; r++) {
            for (int c = 0; c < size.columns(); c++) {
                var cell = buffer.getCell(c, r);
                if (cell.character().charAt(0) != ' ') {
                    if (cell.fg() == AnsiColor.WHITE || cell.fg() == AnsiColor.BRIGHT_WHITE) {
                        foundWhite = true;
                        break;
                    }
                }
            }
        }
        assertTrue(foundWhite, "snowflakes should be white or bright white");
    }

    @Test
    @DisplayName("wind offset accumulates over frames")
    void windOffsetAccumulates() {
        var snow = new Snowfall(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        double before = snow.getWindOffset();
        for (int i = 0; i < 20; i++) {
            snow.renderFrame(new TextGraphics(buffer), size);
        }
        assertTrue(snow.getWindOffset() != before, "wind offset should change over frames");
    }
}
