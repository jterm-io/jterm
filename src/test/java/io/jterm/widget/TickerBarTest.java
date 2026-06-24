package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TickerBarTest {

    @Test
    @DisplayName("renders entries with correct colors")
    void rendersEntriesWithColors() {
        var bar = new TickerBar(List.of(
                new TickerBar.TickerEntry("AAPL", 189.52, 1.35),
                new TickerBar.TickerEntry("TSLA", 242.10, -2.57)
        ));
        bar.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(80, 1));
        var buffer = new ScreenBuffer(new TerminalSize(80, 1));
        bar.draw(new TextGraphics(buffer));

        // AAPL symbol should be at column 0 in bright white bold.
        assertEquals('A', buffer.getCell(0, 0).character().charAt(0));
        assertEquals(AnsiColor.BRIGHT_WHITE, buffer.getCell(0, 0).fg());

        // Should contain green cells (AAPL up) and red cells (TSLA down).
        boolean hasGreen = false, hasRed = false;
        for (int c = 0; c < 80; c++) {
            AnsiColor fg = (AnsiColor) buffer.getCell(c, 0).fg();
            if (fg == AnsiColor.BRIGHT_GREEN) hasGreen = true;
            if (fg == AnsiColor.BRIGHT_RED) hasRed = true;
        }
        assertTrue(hasGreen, "expected green for positive change");
        assertTrue(hasRed, "expected red for negative change");
    }

    @Test
    @DisplayName("separator is dim between entries")
    void separatorIsDim() {
        var bar = new TickerBar(List.of(
                new TickerBar.TickerEntry("AAPL", 189.52, 1.35),
                new TickerBar.TickerEntry("MSFT", 378.91, 0.99)
        ));
        bar.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(80, 1));
        var buffer = new ScreenBuffer(new TerminalSize(80, 1));
        bar.draw(new TextGraphics(buffer));

        // Find separator characters (bright black / dim).
        boolean hasDim = false;
        for (int c = 0; c < 80; c++) {
            if (buffer.getCell(c, 0).fg() == AnsiColor.BRIGHT_BLACK) {
                hasDim = true;
                break;
            }
        }
        assertTrue(hasDim, "expected dim separator between entries");
    }

    @Test
    @DisplayName("tick advances scroll offset")
    void tickAdvancesScroll() {
        var bar = new TickerBar(List.of(
                new TickerBar.TickerEntry("AAPL", 189.52, 1.35)
        ));
        int before = bar.getScrollOffset();
        bar.tick();
        assertEquals(before + 1, bar.getScrollOffset());
    }

    @Test
    @DisplayName("scroll wraps around seamlessly")
    void scrollWrapsAround() {
        var bar = new TickerBar(List.of(
                new TickerBar.TickerEntry("A", 1.0, 1.0)
        ));
        int totalLen = bar.buildScrollTextLen();
        // Scroll past the end — should wrap to 0.
        bar.setScrollOffset(totalLen - 1);
        bar.tick();
        assertEquals(0, bar.getScrollOffset(), "scroll offset should wrap to 0");
    }

    @Test
    @DisplayName("two consecutive ticks produce different frames")
    void consecutiveTicksDiffer() {
        var bar = new TickerBar(List.of(
                new TickerBar.TickerEntry("AAPL", 189.52, 1.35),
                new TickerBar.TickerEntry("MSFT", 378.91, 0.99)
        ));
        bar.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(80, 1));
        var buffer1 = new ScreenBuffer(new TerminalSize(80, 1));
        var buffer2 = new ScreenBuffer(new TerminalSize(80, 1));
        bar.draw(new TextGraphics(buffer1));
        bar.tick();
        bar.draw(new TextGraphics(buffer2));

        boolean anyDifferent = false;
        for (int c = 0; c < 80; c++) {
            if (!buffer1.getCell(c, 0).equals(buffer2.getCell(c, 0))) {
                anyDifferent = true;
                break;
            }
        }
        assertTrue(anyDifferent, "expected frames to differ after tick");
    }

    @Test
    @DisplayName("startAnimation begins scrolling, stopAnimation stops")
    void startStopAnimation() {
        var bar = new TickerBar(List.of(
                new TickerBar.TickerEntry("AAPL", 189.52, 1.35)
        ));
        assertFalse(bar.isAnimating());
        bar.startAnimation();
        assertTrue(bar.isAnimating());
        bar.stopAnimation();
        assertFalse(bar.isAnimating());
    }

    @Test
    @DisplayName("empty entries renders blank bar")
    void emptyEntriesRenderBlank() {
        var bar = new TickerBar(List.of());
        bar.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 1));
        var buffer = new ScreenBuffer(new TerminalSize(40, 1));
        bar.draw(new TextGraphics(buffer));

        for (int c = 0; c < 40; c++) {
            assertEquals(' ', buffer.getCell(c, 0).character().charAt(0));
            assertEquals(AnsiColor.BLACK, buffer.getCell(c, 0).bg());
        }
    }

    @Test
    @DisplayName("is not focusable")
    void notFocusable() {
        var bar = new TickerBar();
        assertFalse(bar.isFocusable());
    }

    @Test
    @DisplayName("preferred size is single row")
    void preferredSizeIsSingleRow() {
        var bar = new TickerBar(List.of(
                new TickerBar.TickerEntry("AAPL", 189.52, 1.35)
        ));
        assertEquals(1, bar.getPreferredSize().rows());
    }

    @Test
    @DisplayName("setEntries resets scroll offset")
    void setEntriesResetsScroll() {
        var bar = new TickerBar(List.of(
                new TickerBar.TickerEntry("AAPL", 189.52, 1.35)
        ));
        bar.tick();
        bar.tick();
        assertTrue(bar.getScrollOffset() > 0);
        bar.setEntries(List.of(new TickerBar.TickerEntry("MSFT", 378.91, 0.99)));
        assertEquals(0, bar.getScrollOffset());
    }

    @Test
    @DisplayName("zero-size does not throw")
    void zeroSizeDoesNotThrow() {
        var bar = new TickerBar(List.of(
                new TickerBar.TickerEntry("AAPL", 189.52, 1.35)
        ));
        bar.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> bar.draw(new TextGraphics(buffer)));
    }
}