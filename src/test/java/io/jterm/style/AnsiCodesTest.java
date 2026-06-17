package io.jterm.style;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnsiCodesTest {
    @Test
    void cursorToIsOneIndexed() {
        assertArrayEquals("\033[1;1H".getBytes(), AnsiCodes.cursorTo(0, 0));
        assertArrayEquals("\033[10;20H".getBytes(), AnsiCodes.cursorTo(9, 19));
    }

    @Test
    void enableBold() {
        assertArrayEquals("\033[1m".getBytes(), AnsiCodes.enable(SGR.BOLD));
    }

    @Test
    void disableBold() {
        assertArrayEquals("\033[22m".getBytes(), AnsiCodes.disable(SGR.BOLD));
    }

    @Test
    void resetIs0m() {
        assertArrayEquals("\033[0m".getBytes(), AnsiCodes.reset());
    }
}
