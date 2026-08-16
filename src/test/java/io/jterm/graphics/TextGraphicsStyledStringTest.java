package io.jterm.graphics;

import io.jterm.core.TerminalSize;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.StyledSegment;
import io.jterm.style.TextCell;
import io.jterm.style.TextStyleResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TextGraphics#drawStyledString} — per-character color support.
 */
class TextGraphicsStyledStringTest {

    @Test
    void resolverColorsFirstThreeCharsRed() {
        var buf = new ScreenBuffer(new TerminalSize(20, 1));
        var g = new TextGraphics(buf);
        var defaults = new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLACK);
        TextStyleResolver resolver = (charIndex, c, defaultStyle) ->
                charIndex < 3 ? defaultStyle.withForeground(AnsiColor.RED) : null;
        g.drawStyledString(0, 0, "Hello", defaults, resolver);
        assertEquals('H', buf.getCell(0, 0).character().charAt(0));
        assertEquals(AnsiColor.RED, buf.getCell(0, 0).fg());
        assertEquals('e', buf.getCell(1, 0).character().charAt(0));
        assertEquals(AnsiColor.RED, buf.getCell(1, 0).fg());
        assertEquals('l', buf.getCell(2, 0).character().charAt(0));
        assertEquals(AnsiColor.RED, buf.getCell(2, 0).fg());
        // Index 3+ should have default fg
        assertEquals('l', buf.getCell(3, 0).character().charAt(0));
        assertEquals(AnsiColor.WHITE, buf.getCell(3, 0).fg());
        assertEquals('o', buf.getCell(4, 0).character().charAt(0));
        assertEquals(AnsiColor.WHITE, buf.getCell(4, 0).fg());
    }

    @Test
    void resolverReturningNullUsesDefaultStyle() {
        var buf = new ScreenBuffer(new TerminalSize(20, 1));
        var g = new TextGraphics(buf);
        var defaults = new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLACK, SGR.BOLD);
        TextStyleResolver resolver = (charIndex, c, defaultStyle) -> null;
        g.drawStyledString(0, 0, "AB", defaults, resolver);
        assertEquals('A', buf.getCell(0, 0).character().charAt(0));
        assertEquals(AnsiColor.WHITE, buf.getCell(0, 0).fg());
        assertEquals(AnsiColor.BLACK, buf.getCell(0, 0).bg());
        assertTrue(buf.getCell(0, 0).modifiers().contains(SGR.BOLD));
        assertEquals('B', buf.getCell(1, 0).character().charAt(0));
        assertEquals(AnsiColor.WHITE, buf.getCell(1, 0).fg());
    }

    @Test
    void emptySegmentsSameAsRegularDrawString() {
        var buf1 = new ScreenBuffer(new TerminalSize(20, 1));
        var g1 = new TextGraphics(buf1);
        var buf2 = new ScreenBuffer(new TerminalSize(20, 1));
        var g2 = new TextGraphics(buf2);
        var defaults = new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLACK);
        g1.drawString(0, 0, "Hello", defaults);
        g2.drawStyledString(0, 0, "Hello", defaults, List.of());
        for (int i = 0; i < 5; i++) {
            assertEquals(buf1.getCell(i, 0).character(), buf2.getCell(i, 0).character(),
                    "char at " + i + " should match");
            assertEquals(buf1.getCell(i, 0).fg(), buf2.getCell(i, 0).fg(),
                    "fg at " + i + " should match");
            assertEquals(buf1.getCell(i, 0).bg(), buf2.getCell(i, 0).bg(),
                    "bg at " + i + " should match");
        }
    }

    @Test
    void segmentsApplyCorrectColors() {
        var buf = new ScreenBuffer(new TerminalSize(20, 1));
        var g = new TextGraphics(buf);
        var defaults = new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLACK);
        var segments = List.of(
                new StyledSegment(0, 3, AnsiColor.RED),
                new StyledSegment(3, 5, AnsiColor.GREEN)
        );
        g.drawStyledString(0, 0, "Hello", defaults, segments);
        assertEquals(AnsiColor.RED, buf.getCell(0, 0).fg());
        assertEquals(AnsiColor.RED, buf.getCell(1, 0).fg());
        assertEquals(AnsiColor.RED, buf.getCell(2, 0).fg());
        assertEquals(AnsiColor.GREEN, buf.getCell(3, 0).fg());
        assertEquals(AnsiColor.GREEN, buf.getCell(4, 0).fg());
    }

    @Test
    void overlappingSegmentsLastOneWins() {
        // Documented behavior: when segments overlap, the LAST segment in the
        // list that contains the index wins. This allows later, more specific
        // segments to override earlier, broader ones.
        var buf = new ScreenBuffer(new TerminalSize(20, 1));
        var g = new TextGraphics(buf);
        var defaults = new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLACK);
        var segments = List.of(
                new StyledSegment(0, 5, AnsiColor.RED),    // covers all 5 chars
                new StyledSegment(2, 4, AnsiColor.YELLOW)  // overrides chars 2-3
        );
        g.drawStyledString(0, 0, "Hello", defaults, segments);
        assertEquals(AnsiColor.RED, buf.getCell(0, 0).fg());
        assertEquals(AnsiColor.RED, buf.getCell(1, 0).fg());
        assertEquals(AnsiColor.YELLOW, buf.getCell(2, 0).fg());
        assertEquals(AnsiColor.YELLOW, buf.getCell(3, 0).fg());
        assertEquals(AnsiColor.RED, buf.getCell(4, 0).fg());
    }

    @Test
    void noSegmentsCoveringIndexUsesDefault() {
        var buf = new ScreenBuffer(new TerminalSize(20, 1));
        var g = new TextGraphics(buf);
        var defaults = new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLACK);
        var segments = List.of(new StyledSegment(0, 2, AnsiColor.RED));
        g.drawStyledString(0, 0, "Hello", defaults, segments);
        assertEquals(AnsiColor.RED, buf.getCell(0, 0).fg());
        assertEquals(AnsiColor.RED, buf.getCell(1, 0).fg());
        assertEquals(AnsiColor.WHITE, buf.getCell(2, 0).fg()); // default
        assertEquals(AnsiColor.WHITE, buf.getCell(3, 0).fg());
        assertEquals(AnsiColor.WHITE, buf.getCell(4, 0).fg());
    }

    @Test
    void styledStringPreservesCharacters() {
        var buf = new ScreenBuffer(new TerminalSize(20, 1));
        var g = new TextGraphics(buf);
        var defaults = new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLACK);
        TextStyleResolver resolver = (charIndex, c, defaultStyle) ->
                defaultStyle.withForeground(AnsiColor.CYAN);
        g.drawStyledString(0, 0, "Hello", defaults, resolver);
        assertEquals('H', buf.getCell(0, 0).character().charAt(0));
        assertEquals('e', buf.getCell(1, 0).character().charAt(0));
        assertEquals('l', buf.getCell(2, 0).character().charAt(0));
        assertEquals('l', buf.getCell(3, 0).character().charAt(0));
        assertEquals('o', buf.getCell(4, 0).character().charAt(0));
    }
}