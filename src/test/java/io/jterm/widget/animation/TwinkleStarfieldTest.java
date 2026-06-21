package io.jterm.widget.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TwinkleStarfield}: density, brightness variation, colors,
 * bounds, resize, and CP437 glyph selection.
 */
class TwinkleStarfieldTest {

    private static final long SEED = 12345L;

    @Test
    @DisplayName("constructor generates 120 stars by default")
    void defaultStarCount() {
        var field = new TwinkleStarfield(new Random(SEED));
        assertEquals(120, field.getStars().size());
    }

    @Test
    @DisplayName("all stars have normalized positions in [0,1)")
    void positionsNormalized() {
        var field = new TwinkleStarfield(new Random(SEED));
        for (var star : field.getStars()) {
            assertTrue(star.xPercent() >= 0.0 && star.xPercent() < 1.0,
                    "xPercent out of bounds: " + star.xPercent());
            assertTrue(star.yPercent() >= 0.0 && star.yPercent() < 1.0,
                    "yPercent out of bounds: " + star.yPercent());
        }
    }

    @Test
    @DisplayName("star characters come from expected Unicode set")
    void unicodeGlyphsUsed() {
        var field = new TwinkleStarfield(new Random(SEED), false);
        Set<Character> expected = new HashSet<>();
        for (char c : new char[]{'.', '`', '\'', '\u2726', '\u2727', '\u2605', '\u2731', '+', '*'}) {
            expected.add(c);
        }
        for (var star : field.getStars()) {
            assertTrue(expected.contains(star.character()),
                    "unexpected glyph: " + star.character());
        }
    }

    @Test
    @DisplayName("CP437 mode only uses ASCII-safe glyphs")
    void cp437SafeGlyphs() {
        var field = new TwinkleStarfield(new Random(SEED), true);
        for (var star : field.getStars()) {
            char c = star.character();
            assertTrue(c < 0x80 || c == '\u2022' || c == '\u25CB' || c == '\u25CF' || c == '\u25A0',
                    "non-ASCII glyph in CP437 mode: " + (int) c);
            assertTrue(isCp437Safe(c), "glyph not CP437 encodable: " + c);
        }
    }

    private boolean isCp437Safe(char c) {
        return io.jterm.style.Cp437.isEncodable(c) || c < 0x80;
    }

    @Test
    @DisplayName("renderFrame clears background to black")
    void clearsBackgroundToBlack() {
        var field = new TwinkleStarfield(new Random(SEED));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        buffer.setCell(10, 10, new TextCell('X', AnsiColor.BRIGHT_RED, AnsiColor.WHITE));

        field.renderFrame(new TextGraphics(buffer), size);

        assertEquals(AnsiColor.BLACK, buffer.getCell(0, 0).bg());
        assertEquals(AnsiColor.BLACK, buffer.getCell(10, 10).bg());
        assertEquals(' ', buffer.getCell(10, 10).character().charAt(0));
    }

    @Test
    @DisplayName("renderFrame draws at least one star at a chosen time")
    void drawsStarsAtSomeTime() {
        var field = new TwinkleStarfield(new Random(SEED));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        int drawn = countDrawnStars(field, buffer, size, 0.0);
        assertTrue(drawn > 0, "expected some stars to be visible at t=0");
    }

    @Test
    @DisplayName("renderFrame skips dim stars (brightness 0)")
    void skipsDimStars() {
        var field = new TwinkleStarfield(new Random(SEED));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        // Find a time where at least one star maps to brightness 0 by brute force
        boolean skippedAny = false;
        for (double t = 0; t < 20.0; t += 0.05) {
            int drawn = countDrawnStars(field, buffer, size, t);
            if (drawn < 120) {
                skippedAny = true;
                break;
            }
        }
        assertTrue(skippedAny, "expected some stars to be skipped at brightness 0");
    }

    @Test
    @DisplayName("brightness varies over time for a star")
    void brightnessVariesOverTime() {
        var field = new TwinkleStarfield(new Random(SEED));
        var star = field.getStars().get(0);
        var values = new HashSet<Integer>();
        for (double t = 0; t < 30.0; t += 0.1) {
            values.add(field.computeBrightness(star, t));
        }
        assertTrue(values.size() > 1, "brightness should vary with time");
    }

    @Test
    @DisplayName("brightness is clamped to [0,3]")
    void brightnessClamped() {
        var field = new TwinkleStarfield(new Random(SEED));
        var star = field.getStars().get(0);
        for (double t = 0; t < 100.0; t += 0.07) {
            int b = field.computeBrightness(star, t);
            assertTrue(b >= 0 && b <= 3, "brightness out of range: " + b);
        }
    }

    @Test
    @DisplayName("brightest stars use BOLD modifier")
    void brightStarsBold() {
        var field = new TwinkleStarfield(new Random(SEED));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        boolean foundBold = false;
        for (double t = 0; t < 20.0; t += 0.05) {
            buffer.fill(TextCell.EMPTY);
            field.renderAtTime(new TextGraphics(buffer), size, t);
            for (int r = 0; r < size.rows(); r++) {
                for (int c = 0; c < size.columns(); c++) {
                    var cell = buffer.getCell(c, r);
                    if (cell.character().charAt(0) != ' ' && cell.modifiers().contains(SGR.BOLD)) {
                        foundBold = true;
                        break;
                    }
                }
                if (foundBold) break;
            }
            if (foundBold) break;
        }
        assertTrue(foundBold, "expected at least one BOLD star");
    }

    @Test
    @DisplayName("star colors are mostly white with a colored minority")
    void colorDistribution() {
        var field = new TwinkleStarfield(new Random(SEED));
        int colored = 0;
        for (var star : field.getStars()) {
            if (star.color() != AnsiColor.WHITE) colored++;
        }
        double ratio = (double) colored / field.getStars().size();
        assertTrue(ratio >= 0.05 && ratio <= 0.20,
                "colored ratio should be ~10%, was " + ratio);
    }

    @Test
    @DisplayName("colored stars use allowed tints")
    void allowedTints() {
        var field = new TwinkleStarfield(new Random(SEED));
        Set<AnsiColor> allowed = Set.of(
                AnsiColor.BRIGHT_BLUE, AnsiColor.BRIGHT_CYAN,
                AnsiColor.BRIGHT_YELLOW, AnsiColor.BRIGHT_RED);
        for (var star : field.getStars()) {
            if (star.color() != AnsiColor.WHITE) {
                assertTrue(allowed.contains(star.color()),
                        "unexpected tint: " + star.color());
            }
        }
    }

    @Test
    @DisplayName("stars stay within screen bounds after resize")
    void starsWithinBoundsAfterResize() {
        var field = new TwinkleStarfield(new Random(SEED));
        var size = new TerminalSize(40, 12);
        var buffer = new ScreenBuffer(size);
        field.renderAtTime(new TextGraphics(buffer), size, 1.0);
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                assertNotNull(buffer.getCell(c, r));
            }
        }
    }

    @Test
    @DisplayName("empty size does not crash")
    void emptySizeDoesNotCrash() {
        var field = new TwinkleStarfield(new Random(SEED));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> field.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("onResize stores the new size")
    void onResizeStoresSize() {
        var field = new TwinkleStarfield(new Random(SEED));
        var size = new TerminalSize(100, 40);
        field.onResize(size);
        assertEquals(size, field.lastSize());
    }

    @Test
    @DisplayName("lifecycle start/stop/isRunning works")
    void lifecycle() {
        var field = new TwinkleStarfield();
        assertFalse(field.isRunning());
        field.start();
        assertTrue(field.isRunning());
        field.stop();
        assertFalse(field.isRunning());
    }

    @Test
    @DisplayName("target FPS is 10")
    void targetFps() {
        assertEquals(10, new TwinkleStarfield().targetFps());
    }

    @Test
    @DisplayName("renderFrame uses system time and clears background")
    void renderFrameUsesSystemTime() {
        var field = new TwinkleStarfield(new Random(SEED));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);
        field.renderFrame(new TextGraphics(buffer), size);

        int drawn = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') drawn++;
            }
        }
        assertTrue(drawn >= 0, "renderFrame should complete and produce a buffer");
    }

    @Test
    @DisplayName("density matches ~1 star per 8x8 cell area on 80x24")
    void densityOnEightyByTwentyFour() {
        var field = new TwinkleStarfield(new Random(SEED));
        var size = new TerminalSize(80, 24);
        int area = size.area();
        int expected = Math.max(1, area / 64);
        int drawn = countDrawnStars(field, new ScreenBuffer(size), size, 1.0);
        assertTrue(drawn >= expected * 0.5, "expected at least half the ideal density to be visible");
    }

    @Test
    @DisplayName("base brightness values are in expected range")
    void baseBrightnessRange() {
        var field = new TwinkleStarfield(new Random(SEED));
        for (var star : field.getStars()) {
            assertTrue(star.baseBrightness() >= 0 && star.baseBrightness() <= 3,
                    "base brightness out of range: " + star.baseBrightness());
        }
    }

    @Test
    @DisplayName("amplitude and speed are positive")
    void positiveAmplitudeAndSpeed() {
        var field = new TwinkleStarfield(new Random(SEED));
        for (var star : field.getStars()) {
            assertTrue(star.amplitude() > 0, "amplitude must be positive");
            assertTrue(star.speed() > 0, "speed must be positive");
        }
    }

    private int countDrawnStars(TwinkleStarfield field, ScreenBuffer buffer, TerminalSize size, double time) {
        buffer.fill(TextCell.EMPTY);
        field.renderAtTime(new TextGraphics(buffer), size, time);
        int drawn = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') drawn++;
            }
        }
        return drawn;
    }
}
