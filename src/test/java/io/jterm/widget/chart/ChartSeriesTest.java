package io.jterm.widget.chart;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChartSeriesTest {

    @Test
    void basicConstruction() {
        var s = new ChartSeries("AAPL", List.of(150.0, 152.0, 149.0), AnsiColor.GREEN);
        assertEquals("AAPL", s.name());
        assertEquals(3, s.size());
        assertEquals(ChartType.LINE, s.type());
        assertEquals(AnsiColor.GREEN, s.color());
    }

    @Test
    void defaultsToLineType() {
        var s = new ChartSeries("X", List.of(1.0), AnsiColor.RED);
        assertEquals(ChartType.LINE, s.type());
    }

    @Test
    void barType() {
        var s = new ChartSeries("Bars", List.of(1.0, 2.0, 3.0), ChartType.BAR, AnsiColor.CYAN);
        assertEquals(ChartType.BAR, s.type());
    }

    @Test
    void scatterType() {
        var s = new ChartSeries("Dots", List.of(1.0, 2.0), ChartType.SCATTER, AnsiColor.YELLOW);
        assertEquals(ChartType.SCATTER, s.type());
    }

    @Test
    void minMax() {
        var s = new ChartSeries("test", List.of(10.0, 50.0, 30.0, 20.0), AnsiColor.WHITE);
        assertEquals(10.0, s.min());
        assertEquals(50.0, s.max());
    }

    @Test
    void emptySeriesMinMax() {
        var s = new ChartSeries("empty", List.of(), AnsiColor.WHITE);
        assertTrue(s.isEmpty());
        assertTrue(Double.isNaN(s.min()));
        assertTrue(Double.isNaN(s.max()));
    }

    @Test
    void valuesAreImmutable() {
        var original = new java.util.ArrayList<>(List.of(1.0, 2.0));
        var s = new ChartSeries("test", original, AnsiColor.WHITE);
        original.add(999.0);
        assertEquals(2, s.size()); // internal copy, not affected
    }

    @Test
    void nullNameThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ChartSeries(null, List.of(1.0), AnsiColor.WHITE));
    }

    @Test
    void nullColorThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ChartSeries("test", List.of(1.0), (Color) null));
    }

    @Test
    void nullValuesTreatedAsEmpty() {
        var s = new ChartSeries("test", null, AnsiColor.WHITE);
        assertTrue(s.isEmpty());
    }

    @Test
    void singlePointMinMax() {
        var s = new ChartSeries("one", List.of(42.0), AnsiColor.GREEN);
        assertEquals(42.0, s.min());
        assertEquals(42.0, s.max());
    }
}