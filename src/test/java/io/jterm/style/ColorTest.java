package io.jterm.style;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorTest {
    @Test
    void ansiFgSequence() {
        assertArrayEquals("30".getBytes(), AnsiColor.BLACK.fgSequence());
        assertArrayEquals("31".getBytes(), AnsiColor.RED.fgSequence());
        assertArrayEquals("90".getBytes(), AnsiColor.BRIGHT_BLACK.fgSequence());
        assertArrayEquals("39".getBytes(), AnsiColor.DEFAULT.fgSequence());
    }

    @Test
    void ansiBgSequence() {
        assertArrayEquals("40".getBytes(), AnsiColor.BLACK.bgSequence());
        assertArrayEquals("100".getBytes(), AnsiColor.BRIGHT_BLACK.bgSequence());
        assertArrayEquals("49".getBytes(), AnsiColor.DEFAULT.bgSequence());
    }

    @Test
    void indexedFgSequence() {
        assertArrayEquals("38;5;196".getBytes(), new IndexedColor(196).fgSequence());
    }

    @Test
    void rgbFgSequence() {
        assertArrayEquals("38;2;255;128;0".getBytes(), new RgbColor(255, 128, 0).fgSequence());
    }

    @Test
    void indexedFromRgbFindsCubeColor() {
        IndexedColor c = IndexedColor.fromRgb(255, 255, 255);
        assertEquals(231, c.index()); // white in 6×6×6 cube
    }

    @Test
    void indexedFromRgbFindsGrayscale() {
        IndexedColor c = IndexedColor.fromRgb(64, 64, 64);
        assertTrue(c.index() >= 232 && c.index() <= 255);
    }

    @Test
    void rgbRejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new RgbColor(256, 0, 0));
    }
}
