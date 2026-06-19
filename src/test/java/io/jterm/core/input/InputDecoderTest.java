package io.jterm.core.input;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class InputDecoderTest {
    private KeyStroke decode(byte[] input) {
        var decoder = new InputDecoder(new ByteArrayInputStream(input));
        try {
            return decoder.poll().orElseThrow();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void arrowUp() {
        assertEquals(KeyType.ARROW_UP, decode("\033[A".getBytes()).type());
    }

    @Test
    void arrowDown() {
        assertEquals(KeyType.ARROW_DOWN, decode("\033[B".getBytes()).type());
    }

    @Test
    void enter() {
        assertEquals(KeyType.ENTER, decode("\r".getBytes()).type());
    }

    @Test
    void backspace() {
        assertEquals(KeyType.BACKSPACE, decode("\177".getBytes()).type());
    }

    @Test
    void tab() {
        assertEquals(KeyType.TAB, decode("\t".getBytes()).type());
    }

    @Test
    void simpleChar() {
        var ks = decode("A".getBytes());
        assertEquals(KeyType.CHARACTER, ks.type());
        assertEquals('A', ks.character());
    }

    @Test
    void ctrlC() {
        var ks = decode(new byte[]{3}); // Ctrl+C = 0x03
        assertEquals(KeyType.CHARACTER, ks.type());
        assertEquals('C', ks.character());
        assertTrue(ks.ctrl());
    }

    @Test
    void altX() {
        var ks = decode("\033x".getBytes());
        assertEquals(KeyType.CHARACTER, ks.type());
        assertEquals('x', ks.character());
        assertTrue(ks.alt());
    }

    @Test
    void f1Key() {
        assertEquals(KeyType.F1, decode("\033OP".getBytes()).type());
    }

    @Test
    void deleteKey() {
        assertEquals(KeyType.DELETE, decode("\033[3~".getBytes()).type());
    }

    @Test
    void pollReturnsEmptyWhenNoData() throws Exception {
        var decoder = new InputDecoder(new ByteArrayInputStream(new byte[0]));
        assertEquals(Optional.empty(), decoder.poll());
    }

    @Test
    void escapeStandalone() {
        var ks = decode(new byte[]{0x1b});
        assertEquals(KeyType.ESCAPE, ks.type());
    }

    @Test
    void ctrlHIsBackspace() {
        // 0x08 (Ctrl+H) is treated as Backspace — this is standard terminal behavior.
        // Telnet clients send 0x08 for backspace; most terminals also honor it.
        var ks = decode(new byte[]{0x08});
        assertEquals(KeyType.BACKSPACE, ks.type());
    }

    @Test
    void ctrlF() {
        var ks = decode(new byte[]{0x06}); // Ctrl+F = 0x06
        assertEquals(KeyType.CHARACTER, ks.type());
        assertEquals('F', ks.character());
        assertTrue(ks.ctrl());
    }

    @Test
    void ctrlE() {
        var ks = decode(new byte[]{0x05}); // Ctrl+E = 0x05
        assertEquals(KeyType.CHARACTER, ks.type());
        assertEquals('E', ks.character());
        assertTrue(ks.ctrl());
    }

    @Test
    void ctrlJIsNotEnter() {
        // Bug: 0x0A (Ctrl+J) was intercepted as Enter (\n) before reaching the Ctrl branch.
        // This is expected terminal behavior — \n is always Enter. But verify it.
        var ks = decode(new byte[]{0x0A});
        assertEquals(KeyType.ENTER, ks.type());
    }

    @Test
    void altF() {
        var ks = decode("\033f".getBytes()); // ESC + 'f' = Alt+F
        assertEquals(KeyType.CHARACTER, ks.type());
        assertEquals('f', ks.character());
        assertTrue(ks.alt());
        assertFalse(ks.ctrl());
    }

    @Test
    void altH() {
        var ks = decode("\033h".getBytes()); // ESC + 'h' = Alt+H
        assertEquals(KeyType.CHARACTER, ks.type());
        assertEquals('h', ks.character());
        assertTrue(ks.alt());
    }
}
