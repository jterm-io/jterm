package io.jterm.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers public core methods with zero line coverage.
 */
class CoreCoverageGapsTest {

    @Test
    void mockTerminalOutputHelpers() throws Exception {
        var out = new ByteArrayOutputStream();
        var term = new MockTerminal(new TerminalSize(40, 20), out, new ByteArrayInputStream(new byte[0]));
        term.putCharacter('X');
        term.flush();
        term.clearOutput();
        assertTrue(term.getOutput().isEmpty());
        assertFalse(term.isClosed());
        term.close();
        assertTrue(term.isClosed());
    }

    @Test
    void mockTerminalPollInputWithTimeout() throws Exception {
        var term = new MockTerminal(new TerminalSize(40, 20));
        var ks = term.pollInput(5);
        assertTrue(ks.isEmpty());
    }

    @Test
    void mockTerminalWriteRaw() throws Exception {
        var out = new ByteArrayOutputStream();
        var term = new MockTerminal(new TerminalSize(40, 20), out, new ByteArrayInputStream(new byte[0]));
        term.writeRaw("RAW".getBytes(StandardCharsets.UTF_8));
        term.flush();
        assertEquals("RAW", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void terminalPositionWithers() {
        var p = new TerminalPosition(5, 10);
        assertEquals(7, p.withColumn(7).column());
        assertEquals(12, p.withRow(12).row());
    }

    @Test
    void terminalSizeMath() {
        var s = new TerminalSize(20, 10);
        assertEquals(new TerminalSize(10, 5), s.withColumns(10).withRows(5));
        assertEquals(new TerminalSize(20, 10), s);
    }
}
