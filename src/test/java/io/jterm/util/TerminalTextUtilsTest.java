package io.jterm.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerminalTextUtilsTest {
    @Test
    void asciiWidth() {
        assertEquals(5, TerminalTextUtils.getTrueWidth("Hello"));
    }

    @Test
    void cjkDoubleWidth() {
        assertEquals(6, TerminalTextUtils.getTrueWidth("漢字字")); // 3 chars × 2 cols
    }

    @Test
    void mixedWidth() {
        assertEquals(6, TerminalTextUtils.getTrueWidth("A漢B字")); // 1+2+1+2
    }

    @Test
    void truncateAscii() {
        assertEquals("Hel", TerminalTextUtils.truncate("Hello", 3));
    }

    @Test
    void truncateDoesNotSplitDoubleWidth() {
        String result = TerminalTextUtils.truncate("漢字", 3);
        assertEquals("漢", result); // Can only fit 1 double-width char in 3 cols
    }

    @Test
    void controlCharCheck() {
        assertTrue(TerminalTextUtils.isControlChar('\n'));
        assertTrue(TerminalTextUtils.isControlChar((char) 127));
        assertFalse(TerminalTextUtils.isControlChar('A'));
    }
}
