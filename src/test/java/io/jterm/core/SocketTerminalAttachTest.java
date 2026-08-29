package io.jterm.core;

import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for Phase 1 Task 1.3 (Connection Re-attachment) of the
 * session-persistence plan: after a socket disconnects, a new connection's
 * streams must be attachable to the existing terminal so the user's screen
 * state can be re-rendered onto the new socket.
 */
class SocketTerminalAttachTest {

    @Test
    void attachReplacesOutputStream() throws Exception {
        var firstOut = new ByteArrayOutputStream();
        var terminal = new SocketTerminal(
                new java.io.ByteArrayInputStream(new byte[0]),
                firstOut, new TerminalSize(80, 24));

        var secondOut = new ByteArrayOutputStream();
        terminal.attach(new java.io.ByteArrayInputStream(new byte[0]), secondOut);

        terminal.writeRaw("HELLO".getBytes(StandardCharsets.UTF_8));
        terminal.flush();

        // Output after attach MUST go to the new stream...
        assertEquals("HELLO", secondOut.toString(StandardCharsets.UTF_8));
        // ...and not to the old (disconnected) one.
        assertEquals(0, firstOut.size());
    }

    @Test
    void attachRestoresInputPiping() throws Exception {
        var terminal = new SocketTerminal(
                new java.io.ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream(), new TerminalSize(80, 24));

        // Feed keystrokes through the NEW stream after attach; the terminal's
        // reader thread must be reading from the new input.
        var pipeIn = new java.io.PipedInputStream();
        var pipeOut = new java.io.PipedOutputStream(pipeIn);
        terminal.attach(pipeIn, new ByteArrayOutputStream());

        pipeOut.write('a');
        pipeOut.flush();
        pipeOut.close();

        // The keystroke must surface from the NEW stream (the old stream was empty).
        KeyStroke ks = null;
        for (int i = 0; i < 200 && ks == null; i++) {
            var optional = terminal.pollInput(10);
            if (optional.isPresent()) ks = optional.get();
        }
        assertNotNull(ks, "attach must switch input; keystrokes from the new stream must surface");
        assertEquals(KeyType.CHARACTER, ks.type());
        assertEquals('a', ks.character());
    }

    @Test
    void attachThenCloseShutsDownNewStreamsNotOld() throws Exception {
        var firstOut = new ByteArrayOutputStream();
        var terminal = new SocketTerminal(
                new java.io.ByteArrayInputStream(new byte[0]),
                firstOut, new TerminalSize(80, 24));

        var secondOut = new ByteArrayOutputStream();
        terminal.attach(new java.io.ByteArrayInputStream(new byte[0]), secondOut);

        terminal.close();

        // close() after attach must release the reader thread (no hang) —
        // implicitly verified by the test completing. Old stream stays untouched.
        assertEquals(0, firstOut.size());
        assertDoesNotThrow(terminal::flush);
    }

    @Test
    void attachWithNullStreamsThrows() throws Exception {
        var terminal = new SocketTerminal(
                new java.io.ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream(), new TerminalSize(80, 24));
        assertThrows(NullPointerException.class, () -> terminal.attach(null, new ByteArrayOutputStream()));
        assertThrows(NullPointerException.class, () -> terminal.attach(new java.io.ByteArrayInputStream(new byte[0]), null));
    }
}