package io.jterm.core;

import io.jterm.core.input.KeyType;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests default {@link Terminal} methods and edge cases of {@link MockTerminal}
 * that previous suites did not exercise.
 */
class MockTerminalDefaultsTest {

    @Test
    void terminalDefaultWriteRawConvertsBytesToCharacters() {
        var out = new ByteArrayOutputStream();
        var in = new ByteArrayInputStream(new byte[0]);
        Terminal t = new MockTerminal(new TerminalSize(80, 24), out, in);
        assertDoesNotThrow(() -> t.writeRaw(new byte[]{(byte) 'A', (byte) 'B'}));
        assertEquals("AB", ((MockTerminal) t).getOutput());
    }

    @Test
    void pollInputTimeoutUsesDefaultImplementation() throws Exception {
        var t = new MockTerminal(new TerminalSize(80, 24));
        long start = System.currentTimeMillis();
        var ks = t.pollInput(20);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(ks.isEmpty());
        assertTrue(elapsed >= 15, "default pollInput(timeout) should sleep ~timeout");
    }

    @Test
    void pollInputZeroTimeoutIsNonBlocking() throws Exception {
        var in = new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8));
        var t = new MockTerminal(new TerminalSize(80, 24), new ByteArrayOutputStream(), in);
        var ks = t.pollInput(0);
        assertTrue(ks.isPresent());
        assertEquals(KeyType.CHARACTER, ks.get().type());
    }

    @Test
    void mockTerminalCapturesSgrAndColor() throws Exception {
        var t = new MockTerminal(new TerminalSize(80, 24));
        t.setForegroundColor(AnsiColor.RED);
        t.enableSGR(SGR.BOLD);
        t.putCharacter('!');
        t.resetColorAndSGR();
        assertTrue(t.getOutput().contains("\033[31m"));
        assertTrue(t.getOutput().contains("\033[1m"));
        assertTrue(t.getOutput().contains("!"));
        assertTrue(t.getOutput().contains("\033[0m"));
    }

    @Test
    void mockTerminalClearOutputResetsBuffer() throws Exception {
        var t = new MockTerminal(new TerminalSize(80, 24));
        t.putCharacter('a');
        t.clearOutput();
        assertEquals("", t.getOutput());
    }

    @Test
    void resizeListenerCanBeAddedAndRemoved() {
        var t = new MockTerminal(new TerminalSize(80, 24));
        TerminalResizeListener l = size -> {};
        assertDoesNotThrow(() -> t.addResizeListener(l));
        assertDoesNotThrow(() -> t.removeResizeListener(l));
        assertDoesNotThrow(() -> t.removeResizeListener(l));
    }
}
