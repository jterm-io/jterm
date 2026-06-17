package io.jterm.core.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeyStrokeTest {
    @Test
    void characterKey() {
        var ks = KeyStroke.character('A', false, false, false);
        assertEquals(KeyType.CHARACTER, ks.type());
        assertEquals('A', ks.character());
        assertTrue(ks.isCharacter());
    }

    @Test
    void arrowKey() {
        var ks = new KeyStroke(KeyType.ARROW_UP);
        assertEquals(KeyType.ARROW_UP, ks.type());
        assertFalse(ks.isCharacter());
    }

    @Test
    void ctrlModifier() {
        var ks = KeyStroke.character('C', true, false, false);
        assertTrue(ks.ctrl());
    }
}
