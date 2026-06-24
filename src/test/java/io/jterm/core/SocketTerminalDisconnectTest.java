package io.jterm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that verify the disconnect/clear-screen sequence is not undone by
 * close() re-emitting EXIT_ALT_SCREEN after clearScreen has already run.
 *
 * <p>Bug: SocketTerminal.close() calls exitPrivateMode() unconditionally,
 * which writes ESC[?1049l AFTER a clearScreen() call. When the BufferedOutputStream
 * flushes, those bytes overwrite/undo the clear on the user's terminal.</p>
 */
class SocketTerminalDisconnectTest {

    private String esc(String s) {
        return s.replace("\033", "\\033");
    }

    /**
     * Simulates the BBS disconnect sequence: exitPrivateMode, clearScreen, flush, close.
     * Verifies that close() does NOT re-emit EXIT_ALT_SCREEN after clearScreen.
     */
    @Test
    @DisplayName("close() after exitPrivateMode+clearScreen does not re-emit EXIT_ALT_SCREEN")
    void closeAfterExitAndClearDoesNotReEmitExitAltScreen() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));

        term.enterPrivateMode();   // enter alt screen (as BBS does on connect)
        out.reset();               // clear the enter-mode output so we only see disconnect
        term.exitPrivateMode();    // exit alt screen, show cursor, flush
        term.clearScreen();        // clear the restored main screen
        term.flush();              // push bytes to the socket
        term.close();              // should NOT call exitPrivateMode again

        var s = out.toString(StandardCharsets.UTF_8);

        // ESC[2J (clear screen) must be present
        assertTrue(s.contains("\033[2J"),
                "output should contain clear-screen sequence; got: " + esc(s));

        // The LAST ESC[2J must come AFTER the LAST ESC[?1049l
        // (i.e., no exit-alt-screen after the clear)
        int clearIdx = s.lastIndexOf("\033[2J");
        int exitAltIdx = s.lastIndexOf("\033[?1049l");
        assertTrue(exitAltIdx < clearIdx,
                "EXIT_ALT_SCREEN (ESC[?1049l) must not appear after clear-screen (ESC[2J); "
                        + "exitAltIdx=" + exitAltIdx + " clearIdx=" + clearIdx + " output: " + esc(s));
    }

    @Test
    @DisplayName("exitPrivateMode is idempotent — second call writes nothing")
    void exitPrivateModeIsIdempotent() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));

        term.enterPrivateMode();
        out.reset();
        term.exitPrivateMode();
        int lenAfterFirstExit = out.size();
        term.exitPrivateMode();  // should be no-op
        int lenAfterSecondExit = out.size();

        assertEquals(lenAfterFirstExit, lenAfterSecondExit,
                "second exitPrivateMode should write nothing; output: "
                        + esc(out.toString(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("close() without prior exitPrivateMode still exits private mode")
    void closeWithoutExitStillExitsPrivateMode() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));

        term.enterPrivateMode();
        out.reset();
        term.close();  // should call exitPrivateMode internally since we're still in private mode

        var s = out.toString(StandardCharsets.UTF_8);
        assertTrue(s.contains("\033[?1049l"),
                "close() should exit private mode if still active; output: " + esc(s));
    }

    @Test
    @DisplayName("exitPrivateMode without enterPrivateMode writes nothing")
    void exitWithoutEnterWritesNothing() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));

        term.exitPrivateMode();  // never entered — should be no-op

        assertEquals(0, out.size(),
                "exitPrivateMode without enterPrivateMode should write nothing; output: "
                        + esc(out.toString(StandardCharsets.UTF_8)));
    }
}