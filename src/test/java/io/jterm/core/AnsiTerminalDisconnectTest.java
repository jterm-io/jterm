package io.jterm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that AnsiTerminal.close() does not re-emit EXIT_ALT_SCREEN
 * after clearScreen has already run (same bug as SocketTerminal).
 */
class AnsiTerminalDisconnectTest {

    private String esc(String s) {
        return s.replace("\033", "\\033");
    }

    @Test
    @DisplayName("close() after exitPrivateMode+clearScreen does not re-emit EXIT_ALT_SCREEN")
    void closeAfterExitAndClearDoesNotReEmitExitAltScreen() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new AnsiTerminal(out, new ByteArrayInputStream(new byte[0]), new TerminalSize(80, 24));

        term.enterPrivateMode();
        out.reset();
        term.exitPrivateMode();
        term.clearScreen();
        term.flush();
        term.close();

        var s = out.toString(StandardCharsets.UTF_8);
        int clearIdx = s.lastIndexOf("\033[2J");
        int exitAltIdx = s.lastIndexOf("\033[?1049l");
        assertTrue(clearIdx >= 0, "should contain clear-screen; got: " + esc(s));
        assertTrue(exitAltIdx < clearIdx,
                "EXIT_ALT_SCREEN must not appear after clear-screen; "
                        + "exitAltIdx=" + exitAltIdx + " clearIdx=" + clearIdx + " output: " + esc(s));
    }

    @Test
    @DisplayName("exitPrivateMode is idempotent — second call writes nothing")
    void exitPrivateModeIsIdempotent() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new AnsiTerminal(out, new ByteArrayInputStream(new byte[0]), new TerminalSize(80, 24));

        term.enterPrivateMode();
        out.reset();
        term.exitPrivateMode();
        int lenAfterFirstExit = out.size();
        term.exitPrivateMode();
        int lenAfterSecondExit = out.size();

        assertEquals(lenAfterFirstExit, lenAfterSecondExit,
                "second exitPrivateMode should write nothing; output: "
                        + esc(out.toString(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("close() without prior exitPrivateMode still exits private mode")
    void closeWithoutExitStillExitsPrivateMode() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new AnsiTerminal(out, new ByteArrayInputStream(new byte[0]), new TerminalSize(80, 24));

        term.enterPrivateMode();
        out.reset();
        term.close();

        var s = out.toString(StandardCharsets.UTF_8);
        assertTrue(s.contains("\033[?1049l"),
                "close() should exit private mode if still active; output: " + esc(s));
    }
}