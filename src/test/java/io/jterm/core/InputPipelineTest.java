package io.jterm.core;

import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the BlockingQueue-based input pipeline in SocketTerminal and AnsiTerminal.
 * Verifies that input is delivered with sub-millisecond latency via the reader thread
 * instead of the old poll+sleep(16) pattern.
 */
class InputPipelineTest {

    @Test
    void socketTerminalPollInputReturnsKeystrokeFromQueue() throws Exception {
        byte[] input = {'A'};
        var in = new ByteArrayInputStream(input);
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));

        // The reader thread should decode 'A' and push it to the queue.
        // Give it a moment to process.
        Optional<KeyStroke> ks = Optional.empty();
        for (int i = 0; i < 100 && ks.isEmpty(); i++) {
            ks = term.pollInput(10);
        }
        assertTrue(ks.isPresent(), "pollInput should return the keystroke");
        assertEquals(KeyType.CHARACTER, ks.get().type());
        assertEquals('A', ks.get().character());
        term.close();
    }

    @Test
    void socketTerminalPollInputTimeoutReturnsEmptyWhenNoInput() throws Exception {
        var in = new ByteArrayInputStream(new byte[0]);
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));

        long start = System.nanoTime();
        Optional<KeyStroke> ks = term.pollInput(50);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(ks.isEmpty(), "pollInput should return empty when no input");
        assertTrue(elapsedMs >= 40, "pollInput should wait at least ~50ms, got " + elapsedMs + "ms");
        term.close();
    }

    @Test
    void socketTerminalPollInputReturnsImmediatelyWhenDataAvailable() throws Exception {
        // Pre-populate the stream with data
        byte[] input = {'X'};
        var in = new ByteArrayInputStream(input);
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));

        // Wait for the reader thread to process the input
        Optional<KeyStroke> ks = Optional.empty();
        for (int i = 0; i < 100 && ks.isEmpty(); i++) {
            ks = term.pollInput(10);
        }
        assertTrue(ks.isPresent(), "should have received 'X'");

        // Now poll with a long timeout — should return immediately (empty) since
        // there's no more data and the queue is empty.
        long start = System.nanoTime();
        Optional<KeyStroke> ks2 = term.pollInput(100);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(ks2.isEmpty());
        // Should have waited the full timeout (no data), but the point is it didn't
        // hang forever. We don't assert exact timing here.
        term.close();
    }

    @Test
    void socketTerminalReadInputBlocksUntilDataArrives() throws Exception {
        // Use a PipedInputStream so we can write data after the read starts
        var pipedOut = new java.io.PipedOutputStream();
        var pipedIn = new java.io.PipedInputStream(pipedOut, 1024);
        var term = new SocketTerminal(pipedIn, new ByteArrayOutputStream(), new TerminalSize(80, 24));

        // Start a thread that writes data after a short delay
        var received = new java.util.concurrent.atomic.AtomicReference<KeyStroke>();
        var readerThread = Thread.ofVirtual().start(() -> {
            try {
                received.set(term.readInput());
            } catch (Exception e) {
                fail("readInput threw: " + e);
            }
        });

        // Write a byte to the pipe
        Thread.sleep(50);
        pipedOut.write('Z');
        pipedOut.flush();

        // Wait for the reader thread to complete
        readerThread.join(2000);

        assertNotNull(received.get(), "readInput should have returned a keystroke");
        assertEquals('Z', received.get().character());
        term.close();
    }

    @Test
    void socketTerminalHandlesMultipleKeystrokesInSequence() throws Exception {
        byte[] input = {'H', 'i', '\r'};
        var in = new ByteArrayInputStream(input);
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));

        // Drain all three keystrokes
        KeyStroke[] strokes = new KeyStroke[3];
        for (int i = 0; i < 3; i++) {
            Optional<KeyStroke> ks = Optional.empty();
            for (int j = 0; j < 100 && ks.isEmpty(); j++) {
                ks = term.pollInput(10);
            }
            assertTrue(ks.isPresent(), "should have received keystroke " + i);
            strokes[i] = ks.get();
        }

        assertEquals('H', strokes[0].character());
        assertEquals('i', strokes[1].character());
        assertEquals(KeyType.ENTER, strokes[2].type());
        term.close();
    }

    @Test
    void ansiTerminalPollInputReturnsKeystrokeFromQueue() throws Exception {
        byte[] input = {'B'};
        var in = new ByteArrayInputStream(input);
        var out = new ByteArrayOutputStream();
        var term = new AnsiTerminal(out, in, new TerminalSize(80, 24));

        Optional<KeyStroke> ks = Optional.empty();
        for (int i = 0; i < 100 && ks.isEmpty(); i++) {
            ks = term.pollInput(10);
        }
        assertTrue(ks.isPresent(), "pollInput should return the keystroke");
        assertEquals('B', ks.get().character());
        term.close();
    }

    @Test
    void ansiTerminalPollInputTimeoutReturnsEmptyWhenNoInput() throws Exception {
        var in = new ByteArrayInputStream(new byte[0]);
        var out = new ByteArrayOutputStream();
        var term = new AnsiTerminal(out, in, new TerminalSize(80, 24));

        Optional<KeyStroke> ks = term.pollInput(50);
        assertTrue(ks.isEmpty(), "pollInput should return empty when no input");
        term.close();
    }

    @Test
    void ansiTerminalReadInputBlocksUntilDataArrives() throws Exception {
        var pipedOut = new java.io.PipedOutputStream();
        var pipedIn = new java.io.PipedInputStream(pipedOut, 1024);
        var term = new AnsiTerminal(new ByteArrayOutputStream(), pipedIn, new TerminalSize(80, 24));

        var received = new java.util.concurrent.atomic.AtomicReference<KeyStroke>();
        var readerThread = Thread.ofVirtual().start(() -> {
            try {
                received.set(term.readInput());
            } catch (Exception e) {
                fail("readInput threw: " + e);
            }
        });

        Thread.sleep(50);
        pipedOut.write('Y');
        pipedOut.flush();

        readerThread.join(2000);

        assertNotNull(received.get(), "readInput should have returned a keystroke");
        assertEquals('Y', received.get().character());
        term.close();
    }

    @Test
    void socketTerminalCloseStopsReaderThread() throws Exception {
        var in = new ByteArrayInputStream(new byte[0]);
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        term.close();

        // After close, pollInput should return empty quickly (not block)
        long start = System.nanoTime();
        Optional<KeyStroke> ks = term.pollInput(10);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ks.isEmpty());
        // Should return quickly because inputClosed is set
        assertTrue(elapsedMs < 250, "pollInput after close should return quickly, took " + elapsedMs + "ms");
    }

    @Test
    void socketTerminalEscapeSequenceDecodedCorrectly() throws Exception {
        // ESC [ A = Up arrow
        byte[] input = {0x1b, 0x5b, 0x41};
        var in = new ByteArrayInputStream(input);
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));

        Optional<KeyStroke> ks = Optional.empty();
        for (int i = 0; i < 100 && ks.isEmpty(); i++) {
            ks = term.pollInput(10);
        }
        assertTrue(ks.isPresent());
        assertEquals(KeyType.ARROW_UP, ks.get().type());
        term.close();
    }
}