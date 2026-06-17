package io.jterm.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerminalSizeTest {
    @Test
    void recordFieldsAccessible() {
        var size = new TerminalSize(80, 24);
        assertEquals(80, size.columns());
        assertEquals(24, size.rows());
    }

    @Test
    void zeroConstantZeroZero() {
        assertEquals(0, TerminalSize.ZERO.columns());
        assertEquals(0, TerminalSize.ZERO.rows());
    }

    @Test
    void withColumnsReplacesColumns() {
        var size = new TerminalSize(10, 5).withColumns(20);
        assertEquals(new TerminalSize(20, 5), size);
    }

    @Test
    void withRowsReplacesRows() {
        var size = new TerminalSize(10, 5).withRows(30);
        assertEquals(new TerminalSize(10, 30), size);
    }

    @Test
    void areaMultipliesColumnsRows() {
        assertEquals(200, new TerminalSize(10, 20).area());
    }

    @Test
    void containsInsideBounds() {
        var size = new TerminalSize(5, 3);
        assertTrue(size.contains(new TerminalPosition(0, 0)));
        assertTrue(size.contains(new TerminalPosition(4, 2)));
    }

    @Test
    void containsOutsideBounds() {
        var size = new TerminalSize(5, 3);
        assertFalse(size.contains(new TerminalPosition(5, 0)));
        assertFalse(size.contains(new TerminalPosition(0, 3)));
        assertFalse(size.contains(new TerminalPosition(-1, 0)));
    }
}
