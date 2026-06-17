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
}
