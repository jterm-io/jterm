package io.jterm.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndexedColorTest {
    @Test
    void validIndexAccepted() {
        var color = new IndexedColor(128);
        assertEquals(128, color.index());
    }

    @Test
    void negativeIndexRejected() {
        assertThrows(IllegalArgumentException.class, () -> new IndexedColor(-1));
    }

    @Test
    void tooLargeIndexRejected() {
        assertThrows(IllegalArgumentException.class, () -> new IndexedColor(256));
    }

    @Test
    void boundaryValuesAccepted() {
        assertDoesNotThrow(() -> new IndexedColor(0));
        assertDoesNotThrow(() -> new IndexedColor(255));
    }

    @Test
    void foregroundSequenceFormat() {
        assertArrayEquals("38;5;128".getBytes(), new IndexedColor(128).fgSequence());
    }

    @Test
    void backgroundSequenceFormat() {
        assertArrayEquals("48;5;128".getBytes(), new IndexedColor(128).bgSequence());
    }

    @Test
    void fromRgbReturnsIndexedColor() {
        var color = IndexedColor.fromRgb(255, 0, 0);
        assertTrue(color.index() >= 0 && color.index() <= 255);
    }
}
