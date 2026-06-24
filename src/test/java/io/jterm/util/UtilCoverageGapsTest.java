package io.jterm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers public methods in TerminalTextUtils with zero line coverage.
 */
class UtilCoverageGapsTest {

    @Test
    void isPrintableForPrintableChars() {
        assertTrue(TerminalTextUtils.isPrintable('A'));
        assertTrue(TerminalTextUtils.isPrintable(' '));
        assertTrue(TerminalTextUtils.isPrintable('é'));
    }

    @Test
    void isPrintableForControlChars() {
        assertFalse(TerminalTextUtils.isPrintable('\n'));
        assertFalse(TerminalTextUtils.isPrintable('\t'));
        assertFalse(TerminalTextUtils.isPrintable('\u007f'));
        assertFalse(TerminalTextUtils.isPrintable((char) 0));
    }
}
