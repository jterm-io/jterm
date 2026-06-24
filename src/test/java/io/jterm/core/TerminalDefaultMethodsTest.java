package io.jterm.core;

import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TerminalDefaultMethodsTest {
    @Test
    void pollInputTimeoutReturnsPresentImmediately() throws IOException {
        byte[] raw = new byte[]{(byte) 'x'};
        var in = new java.io.ByteArrayInputStream(raw);
        var t = new MockTerminal(new TerminalSize(80, 24), new java.io.ByteArrayOutputStream(), in);
        Optional<KeyStroke> ks = t.pollInput(100);
        assertTrue(ks.isPresent());
        assertEquals('x', ks.get().character());
    }

    @Test
    void pollInputTimeoutZeroReturnsEmptyWhenNoInput() throws IOException {
        var t = new MockTerminal(new TerminalSize(80, 24));
        Optional<KeyStroke> ks = t.pollInput(0);
        assertTrue(ks.isEmpty());
    }

    @Test
    void defaultWriteRawDelegatesToPutCharacter() throws IOException {
        var out = new ByteArrayOutputStream();
        Terminal t = new Terminal() {
            public void enterPrivateMode() {}
            public void exitPrivateMode() {}
            public void clearScreen() {}
            public void setCursorPosition(int c, int r) {}
            public void setCursorVisible(boolean v) {}
            public void putCharacter(char c) { out.write(c); }
            public void flush() { try { out.flush(); } catch (java.io.IOException e) { throw new RuntimeException(e); } }
            public void setForegroundColor(io.jterm.style.Color c) {}
            public void setBackgroundColor(io.jterm.style.Color c) {}
            public void enableSGR(io.jterm.style.SGR s) {}
            public void disableSGR(io.jterm.style.SGR s) {}
            public void resetColorAndSGR() {}
            public TerminalSize getTerminalSize() { return new TerminalSize(80, 24); }
            public Optional<KeyStroke> pollInput() { return Optional.empty(); }
            public KeyStroke readInput() { return null; }
            public void addResizeListener(TerminalResizeListener l) {}
            public void removeResizeListener(TerminalResizeListener l) {}
            public void close() {}
        };
        t.writeRaw("Hi".getBytes(StandardCharsets.UTF_8));
        assertEquals("Hi", out.toString(StandardCharsets.UTF_8));
    }
}
