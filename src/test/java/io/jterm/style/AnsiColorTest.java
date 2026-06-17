package io.jterm.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnsiColorTest {
    @Test
    void allSeventeenValuesExist() {
        assertEquals(17, AnsiColor.values().length);
    }

    @Test
    void foregroundSequences() {
        assertArrayEquals("30".getBytes(), AnsiColor.BLACK.fgSequence());
        assertArrayEquals("31".getBytes(), AnsiColor.RED.fgSequence());
        assertArrayEquals("32".getBytes(), AnsiColor.GREEN.fgSequence());
        assertArrayEquals("37".getBytes(), AnsiColor.WHITE.fgSequence());
        assertArrayEquals("39".getBytes(), AnsiColor.DEFAULT.fgSequence());
        assertArrayEquals("97".getBytes(), AnsiColor.BRIGHT_WHITE.fgSequence());
    }

    @Test
    void backgroundSequences() {
        assertArrayEquals("40".getBytes(), AnsiColor.BLACK.bgSequence());
        assertArrayEquals("41".getBytes(), AnsiColor.RED.bgSequence());
        assertArrayEquals("49".getBytes(), AnsiColor.DEFAULT.bgSequence());
        assertArrayEquals("107".getBytes(), AnsiColor.BRIGHT_WHITE.bgSequence());
    }

    @Test
    void brightColorsHaveIndex8OrHigher() {
        assertTrue(AnsiColor.BRIGHT_BLACK.ordinal() >= AnsiColor.WHITE.ordinal());
    }
}
