package io.jterm.core;

import io.jterm.core.input.KeyStroke;
import io.jterm.style.AnsiColor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SocketTerminalTest {

    @Test
    void putCharacterWritesBytesToOutputStream() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.putCharacter('X');
        term.flush();
        assertEquals("X", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void setCursorPositionEmitsCorrectAnsiSequence() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.setCursorPosition(5, 10);
        term.flush();
        assertEquals("\033[11;6H", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void pollInputReadsFromInputStreamViaInputDecoder() throws IOException {
        var in = new ByteArrayInputStream(new byte[] {'A'});
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        var ks = term.pollInput();
        assertTrue(ks.isPresent());
        assertEquals('A', ks.get().character());
        assertTrue(ks.get().isCharacter());
    }

    @Test
    void closeClosesBothStreams() throws IOException {
        var in = new CloseAwareInputStream(new byte[0]);
        var out = new CloseAwareOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        term.close();
        assertTrue(in.isClosed());
        assertTrue(out.isClosed());
    }

    @Test
    void concurrentReadAndWriteOnDifferentThreads() throws Exception {
        var in = new ByteArrayInputStream(new byte[] {'h', 'i'});
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));

        var readLatch = new CountDownLatch(1);
        var writeLatch = new CountDownLatch(1);
        var error = new AtomicReference<Throwable>();

        Thread reader = new Thread(() -> {
            try {
                term.writeRaw("before".getBytes(StandardCharsets.UTF_8));
                var first = term.pollInput();
                if (first.isEmpty()) {
                    throw new AssertionError("expected input");
                }
                var second = term.pollInput();
                if (second.isEmpty()) {
                    throw new AssertionError("expected second input");
                }
                readLatch.countDown();
                writeLatch.await(2, TimeUnit.SECONDS);
                term.putCharacter((char) (first.get().character() + first.get().character()));
            } catch (Throwable t) {
                error.set(t);
            }
        });

        Thread writer = new Thread(() -> {
            try {
                term.setCursorPosition(0, 0);
                writeLatch.countDown();
                readLatch.await(2, TimeUnit.SECONDS);
                term.flush();
            } catch (Throwable t) {
                error.set(t);
            }
        });

        reader.start();
        writer.start();
        reader.join(3000);
        writer.join(3000);

        assertNull(error.get(), () -> error.get().toString());
        assertTrue(reader.isAlive() == false);
        assertTrue(writer.isAlive() == false);
    }

    @Test
    void terminalSizeFromConstructorWhenDiscoveryNotAvailable() throws IOException {
        var out = new ByteArrayOutputStream();
        var size = new TerminalSize(132, 50);
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, size);
        assertEquals(size, term.getTerminalSize());
    }

    @Test
    void enterAndExitPrivateModeEmitExpectedSequences() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.enterPrivateMode();
        term.flush();
        var enter = out.toString(StandardCharsets.UTF_8);
        assertTrue(enter.contains("\033[?1049h"));
        assertTrue(enter.contains("\033[?25l"));
        assertTrue(enter.contains("\033[2J"));

        out.reset();
        term.exitPrivateMode();
        term.flush();
        var exit = out.toString(StandardCharsets.UTF_8);
        assertTrue(exit.contains("\033[?25h"));
        assertTrue(exit.contains("\033[?1049l"));
    }

    @Test
    void setCursorVisibleEmitsCorrectSequence() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.setCursorVisible(false);
        term.flush();
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("\033[?25l"));
    }

    @Test
    void colorAndSgrEmissions() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.setForegroundColor(AnsiColor.RED);
        term.setBackgroundColor(AnsiColor.BLUE);
        term.enableSGR(io.jterm.style.SGR.BOLD);
        term.disableSGR(io.jterm.style.SGR.BOLD);
        term.resetColorAndSGR();
        term.flush();
        var s = out.toString(StandardCharsets.UTF_8);
        assertTrue(s.contains("\033[31m"));
        assertTrue(s.contains("\033[44m"));
        assertTrue(s.contains("\033[1m"));
        assertTrue(s.contains("\033[22m"));
        assertTrue(s.contains("\033[0m"));
    }

    @Test
    void readInputBlocksUntilKeyAvailable() throws Exception {
        var in = new ByteArrayInputStream(new byte[] {'x'});
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        var ks = new AtomicReference<KeyStroke>();
        Thread t = new Thread(() -> {
            try {
                ks.set(term.readInput());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        t.start();
        t.join(300);
        assertNotNull(ks.get());
        assertEquals('x', ks.get().character());
    }

    @Test
    void resizeListenersCanBeAddedAndRemoved() {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        var listener = (TerminalResizeListener) (TerminalSize newSize) -> { };
        assertDoesNotThrow(() -> term.addResizeListener(listener));
        assertDoesNotThrow(() -> term.removeResizeListener(listener));
    }

    private static class CloseAwareInputStream extends java.io.ByteArrayInputStream {
        private boolean closed = false;

        CloseAwareInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }

    private static class CloseAwareOutputStream extends java.io.ByteArrayOutputStream {
        private boolean closed = false;

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }

        @Override
        public void write(int b) {
            if (closed) {
                throw new RuntimeException("stream closed");
            }
            super.write(b);
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
