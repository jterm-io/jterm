package io.jterm.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeometryTest {
    @Test
    void positionOffset() {
        var p = new TerminalPosition(5, 10);
        assertEquals(new TerminalPosition(7, 13), p.offset(2, 3));
    }

    @Test
    void positionIsValid() {
        var p = new TerminalPosition(3, 4);
        assertTrue(p.isValid(new TerminalSize(5, 5)));
        assertFalse(p.isValid(new TerminalSize(3, 5)));  // column out of bounds
        assertFalse(new TerminalPosition(-1, 0).isValid(new TerminalSize(5, 5)));
    }

    @Test
    void sizeArea() {
        assertEquals(1920, new TerminalSize(80, 24).area());
    }

    @Test
    void sizeContains() {
        var size = new TerminalSize(10, 10);
        assertTrue(size.contains(new TerminalPosition(0, 0)));
        assertTrue(size.contains(new TerminalPosition(9, 9)));
        assertFalse(size.contains(new TerminalPosition(10, 0)));
    }
}
