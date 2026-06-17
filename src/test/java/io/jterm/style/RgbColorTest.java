package io.jterm.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RgbColorTest {
    @Test
    void validConstructorStoresValues() {
        var color = new RgbColor(255, 128, 0);
        assertEquals(255, color.r());
        assertEquals(128, color.g());
        assertEquals(0, color.b());
    }

    @Test
    void negativeRedRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RgbColor(-1, 0, 0));
    }

    @Test
    void negativeGreenRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RgbColor(0, -1, 0));
    }

    @Test
    void negativeBlueRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RgbColor(0, 0, -1));
    }

    @Test
    void valueOver255Rejected() {
        assertThrows(IllegalArgumentException.class, () -> new RgbColor(256, 0, 0));
    }

    @Test
    void boundaryValuesAccepted() {
        assertDoesNotThrow(() -> new RgbColor(0, 0, 0));
        assertDoesNotThrow(() -> new RgbColor(255, 255, 255));
    }

    @Test
    void foregroundSequenceFormat() {
        assertArrayEquals("38;2;255;128;0".getBytes(), new RgbColor(255, 128, 0).fgSequence());
    }

    @Test
    void backgroundSequenceFormat() {
        assertArrayEquals("48;2;255;128;0".getBytes(), new RgbColor(255, 128, 0).bgSequence());
    }
}
