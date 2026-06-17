package io.jterm.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerminalPositionTest {
    @Test
    void recordFieldsAccessible() {
        var pos = new TerminalPosition(3, 4);
        assertEquals(3, pos.column());
        assertEquals(4, pos.row());
    }

    @Test
    void topLeftConstantZeroZero() {
        assertEquals(0, TerminalPosition.TOP_LEFT.column());
        assertEquals(0, TerminalPosition.TOP_LEFT.row());
    }

    @Test
    void withColumnReplacesColumn() {
        var pos = new TerminalPosition(1, 2).withColumn(5);
        assertEquals(new TerminalPosition(5, 2), pos);
    }

    @Test
    void withRowReplacesRow() {
        var pos = new TerminalPosition(1, 2).withRow(6);
        assertEquals(new TerminalPosition(1, 6), pos);
    }

    @Test
    void offsetAddsDeltas() {
        var pos = new TerminalPosition(2, 3).offset(4, -1);
        assertEquals(new TerminalPosition(6, 2), pos);
    }

    @Test
    void isValidInsideBounds() {
        var size = new TerminalSize(10, 5);
        assertTrue(new TerminalPosition(0, 0).isValid(size));
        assertTrue(new TerminalPosition(9, 4).isValid(size));
    }

    @Test
    void isValidOutsideBounds() {
        var size = new TerminalSize(10, 5);
        assertFalse(new TerminalPosition(10, 0).isValid(size));
        assertFalse(new TerminalPosition(0, 5).isValid(size));
        assertFalse(new TerminalPosition(-1, 0).isValid(size));
    }
}
