package io.jterm.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SGRTest {
    @Test
    void allValuesExist() {
        assertTrue(SGR.values().length >= 8);
    }

    @Test
    void codesMatchSgrNumbers() {
        assertEquals(1, SGR.BOLD.code);
        assertEquals(2, SGR.DIM.code);
        assertEquals(3, SGR.ITALIC.code);
        assertEquals(4, SGR.UNDERLINE.code);
        assertEquals(5, SGR.BLINK.code);
        assertEquals(7, SGR.REVERSE.code);
        assertEquals(8, SGR.HIDDEN.code);
        assertEquals(9, SGR.STRIKETHROUGH.code);
    }

    @Test
    void valueOfRoundTrip() {
        for (var sgr : SGR.values()) {
            assertEquals(sgr, SGR.valueOf(sgr.name()));
        }
    }
}
