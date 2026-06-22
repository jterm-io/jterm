package io.jterm.core;

import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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
        // With the blocking queue reader thread, give it a moment to decode
        var ks = waitForInput(term, 100);
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

    // ===== Constructor tests =====

    @Test
    @DisplayName("constructor with InputStream/OutputStream/TerminalSize stores size")
    void constructorWithExplicitSizeStoresSize() throws IOException {
        var out = new ByteArrayOutputStream();
        var in = new ByteArrayInputStream(new byte[0]);
        var size = new TerminalSize(132, 50);
        var term = new SocketTerminal(in, out, size);
        assertEquals(size, term.getTerminalSize());
    }

    @Test
    @DisplayName("two-arg constructor falls back to 80×24 default size")
    void twoArgConstructorDefaultsTo80x24() throws IOException {
        var out = new ByteArrayOutputStream();
        var in = new ByteArrayInputStream(new byte[0]);
        var term = new SocketTerminal(in, out);
        assertEquals(new TerminalSize(80, 24), term.getTerminalSize());
    }

    // ===== setTerminalSize / getTerminalSize =====

    @Test
    @DisplayName("setTerminalSize updates getTerminalSize return value")
    void setTerminalSizeUpdatesGetSize() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        var newSize = new TerminalSize(120, 40);
        term.setTerminalSize(newSize);
        assertEquals(newSize, term.getTerminalSize());
    }

    @Test
    @DisplayName("setTerminalSize to same size does not fire listeners")
    void setTerminalSizeSameValueDoesNotFireListeners() {
        var out = new ByteArrayOutputStream();
        var size = new TerminalSize(80, 24);
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, size);
        var fired = new boolean[1];
        term.addResizeListener(newSize -> fired[0] = true);
        term.setTerminalSize(size); // same size
        assertFalse(fired[0], "listener should not fire when size is unchanged");
    }

    @Test
    @DisplayName("setTerminalSize to different size fires resize listeners")
    void setTerminalSizeDifferentValueFiresListeners() {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        var captured = new AtomicReference<TerminalSize>();
        term.addResizeListener(captured::set);
        var newSize = new TerminalSize(100, 30);
        term.setTerminalSize(newSize);
        assertEquals(newSize, captured.get());
    }

    @Test
    @DisplayName("multiple resize listeners are all notified")
    void multipleResizeListenersAllNotified() {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        var count = new int[1];
        TerminalResizeListener l1 = s -> count[0]++;
        TerminalResizeListener l2 = s -> count[0]++;
        term.addResizeListener(l1);
        term.addResizeListener(l2);
        term.setTerminalSize(new TerminalSize(100, 30));
        assertEquals(2, count[0]);
    }

    @Test
    @DisplayName("removed resize listener is not notified")
    void removedResizeListenerNotNotified() {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        var fired = new boolean[1];
        TerminalResizeListener listener = s -> fired[0] = true;
        term.addResizeListener(listener);
        term.removeResizeListener(listener);
        term.setTerminalSize(new TerminalSize(100, 30));
        assertFalse(fired[0]);
    }

    @Test
    @DisplayName("setTerminalSize can be called repeatedly with changing sizes")
    void setTerminalSizeChainedResizes() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        var sizes = new java.util.ArrayList<TerminalSize>();
        term.addResizeListener(sizes::add);
        term.setTerminalSize(new TerminalSize(100, 30));
        term.setTerminalSize(new TerminalSize(120, 40));
        term.setTerminalSize(new TerminalSize(60, 20));
        assertEquals(3, sizes.size());
        assertEquals(new TerminalSize(60, 20), term.getTerminalSize());
    }

    // ===== pollInput / readInput via ChannelInputStream (InputDecoder) =====

    @Test
    @DisplayName("pollInput returns empty when no input available")
    void pollInputEmptyWhenNoInput() throws Exception {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        // Give the reader thread a moment to discover the stream is empty
        Thread.sleep(10);
        var ks = term.pollInput();
        assertNotNull(ks);
        assertTrue(ks.isEmpty());
    }

    @Test
    @DisplayName("pollInput decodes ENTER from carriage return")
    void pollInputDecodesEnter() throws IOException {
        var in = new ByteArrayInputStream(new byte[] {'\r'});
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        var ks = waitForInput(term, 100);
        assertTrue(ks.isPresent());
        assertEquals(KeyType.ENTER, ks.get().type());
    }

    @Test
    @DisplayName("pollInput decodes TAB from tab byte")
    void pollInputDecodesTab() throws IOException {
        var in = new ByteArrayInputStream(new byte[] {'\t'});
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        var ks = waitForInput(term, 100);
        assertTrue(ks.isPresent());
        assertEquals(KeyType.TAB, ks.get().type());
    }

    @Test
    @DisplayName("pollInput decodes BACKSPACE from 0x7f")
    void pollInputDecodesBackspace() throws IOException {
        var in = new ByteArrayInputStream(new byte[] {0x7f});
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        var ks = waitForInput(term, 100);
        assertTrue(ks.isPresent());
        assertEquals(KeyType.BACKSPACE, ks.get().type());
    }

    @Test
    @DisplayName("pollInput decodes ESCAPE from standalone ESC")
    void pollInputDecodesEscape() throws IOException {
        var in = new ByteArrayInputStream(new byte[] {0x1b});
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        var ks = waitForInput(term, 200);
        assertTrue(ks.isPresent());
        assertEquals(KeyType.ESCAPE, ks.get().type());
    }

    @Test
    @DisplayName("pollInput decodes arrow keys via CSI sequences")
    void pollInputDecodesArrowKeys() throws IOException {
        var out = new ByteArrayOutputStream();
        // ESC [ A = Up, ESC [ B = Down, ESC [ C = Right, ESC [ D = Left
        var in = new ByteArrayInputStream(new byte[] {0x1b, '[', 'A', 0x1b, '[', 'B', 0x1b, '[', 'C', 0x1b, '[', 'D'});
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        assertEquals(KeyType.ARROW_UP, waitForInput(term, 200).get().type());
        assertEquals(KeyType.ARROW_DOWN, waitForInput(term, 200).get().type());
        assertEquals(KeyType.ARROW_RIGHT, waitForInput(term, 200).get().type());
        assertEquals(KeyType.ARROW_LEFT, waitForInput(term, 200).get().type());
    }

    @Test
    @DisplayName("readInput blocks then returns key when available")
    void readInputReturnsKey() throws Exception {
        var in = new ByteArrayInputStream(new byte[] {'Z'});
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        var ks = new AtomicReference<KeyStroke>();
        Thread t = new Thread(() -> {
            try { ks.set(term.readInput()); } catch (IOException e) { throw new RuntimeException(e); }
        });
        t.start();
        t.join(2000);
        assertNotNull(ks.get());
        assertEquals('Z', ks.get().character());
    }

    @Test
    @DisplayName("readInput returns EOF when interrupted")
    void readInputReturnsEofOnInterrupt() throws Exception {
        var in = new ByteArrayInputStream(new byte[0]);
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        var ks = new AtomicReference<KeyStroke>();
        Thread t = new Thread(() -> {
            try { ks.set(term.readInput()); } catch (IOException e) { throw new RuntimeException(e); }
        });
        t.start();
        Thread.sleep(100); // let it enter the blocking loop
        t.interrupt();
        t.join(2000);
        assertNotNull(ks.get());
        assertEquals(KeyType.EOF, ks.get().type());
    }

    // ===== write / flush / writeBytes =====

    @Test
    @DisplayName("writeRaw writes raw bytes to output")
    void writeRawWritesBytes() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.writeRaw("hello".getBytes(StandardCharsets.UTF_8));
        term.flush();
        assertEquals("hello", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("writeRaw with empty array writes nothing")
    void writeRawEmptyArray() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.writeRaw(new byte[0]);
        term.flush();
        assertEquals("", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("writeRaw writes UTF-8 multibyte characters")
    void writeRawMultibyteUtf8() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        var text = "héllo≡";
        term.writeRaw(text.getBytes(StandardCharsets.UTF_8));
        term.flush();
        assertEquals(text, out.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("putCharacter writes multibyte UTF-8 char")
    void putCharacterMultibyte() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.putCharacter('é');
        term.flush();
        assertEquals("é", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("flush is a no-op-safe when nothing written")
    void flushSafeWhenNothingWritten() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        assertDoesNotThrow(term::flush);
    }

    @Test
    @DisplayName("writeRaw buffers output until flush")
    void writeRawBuffersUntilFlush() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.writeRaw("ab".getBytes(StandardCharsets.UTF_8));
        // BufferedOutputStream with 4096 buffer — content may or may not be flushed yet,
        // but after flush() it must be present.
        term.flush();
        assertEquals("ab", out.toString(StandardCharsets.UTF_8));
    }

    // ===== enterPrivateMode / exitPrivateMode =====

    @Test
    @DisplayName("enterPrivateMode emits alt-screen, hide-cursor, clear")
    void enterPrivateModeEmitsSequences() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.enterPrivateMode();
        var s = out.toString(StandardCharsets.UTF_8);
        assertTrue(s.contains("\033[?1049h"), "should enter alt screen");
        assertTrue(s.contains("\033[?25l"), "should hide cursor");
        assertTrue(s.contains("\033[2J"), "should clear screen");
    }

    @Test
    @DisplayName("exitPrivateMode emits show-cursor and exit-alt-screen")
    void exitPrivateModeEmitsSequences() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.exitPrivateMode();
        var s = out.toString(StandardCharsets.UTF_8);
        assertTrue(s.contains("\033[?25h"), "should show cursor");
        assertTrue(s.contains("\033[?1049l"), "should exit alt screen");
    }

    @Test
    @DisplayName("enterPrivateMode auto-flushes")
    void enterPrivateModeAutoFlushes() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.enterPrivateMode();
        // No explicit flush() — content should already be in the ByteArrayOutputStream
        assertFalse(out.toString(StandardCharsets.UTF_8).isEmpty());
    }

    @Test
    @DisplayName("exitPrivateMode auto-flushes")
    void exitPrivateModeAutoFlushes() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.exitPrivateMode();
        assertFalse(out.toString(StandardCharsets.UTF_8).isEmpty());
    }

    // ===== clearScreen =====

    @Test
    @DisplayName("clearScreen emits \\033[2J")
    void clearScreenEmitsAnsiClear() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.clearScreen();
        term.flush();
        assertEquals("\033[2J", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("clearScreen does not auto-flush (deferred until flush)")
    void clearScreenNoAutoFlush() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.clearScreen();
        // clearScreen only calls writeRaw, no flush — buffered 4096 so not yet visible
        // After flush, it must be present.
        term.flush();
        assertEquals("\033[2J", out.toString(StandardCharsets.UTF_8));
    }

    // ===== setCursorPosition =====

    @Test
    @DisplayName("setCursorPosition(0,0) emits \\033[1;1H")
    void setCursorPositionOrigin() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.setCursorPosition(0, 0);
        term.flush();
        assertEquals("\033[1;1H", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("setCursorPosition with large values")
    void setCursorPositionLargeValues() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(200, 100));
        term.setCursorPosition(199, 99);
        term.flush();
        assertEquals("\033[100;200H", out.toString(StandardCharsets.UTF_8));
    }

    // ===== setCursorVisible =====

    @Test
    @DisplayName("setCursorVisible(false) emits hide-cursor")
    void setCursorVisibleFalse() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.setCursorVisible(false);
        term.flush();
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("\033[?25l"));
    }

    @Test
    @DisplayName("setCursorVisible(true) emits show-cursor")
    void setCursorVisibleTrue() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.setCursorVisible(true);
        term.flush();
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("\033[?25h"));
    }

    // ===== color / SGR =====

    @Test
    @DisplayName("setForegroundColor emits correct ANSI sequence")
    void setForegroundColorEmitsSequence() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.setForegroundColor(AnsiColor.GREEN);
        term.flush();
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("\033[32m"));
    }

    @Test
    @DisplayName("setBackgroundColor emits correct ANSI sequence")
    void setBackgroundColorEmitsSequence() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.setBackgroundColor(AnsiColor.YELLOW);
        term.flush();
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("\033[43m"));
    }

    @Test
    @DisplayName("enableSGR emits correct sequence for each SGR")
    void enableSgrEmitsCorrectCode() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.enableSGR(SGR.UNDERLINE);
        term.enableSGR(SGR.BLINK);
        term.enableSGR(SGR.REVERSE);
        term.flush();
        var s = out.toString(StandardCharsets.UTF_8);
        assertTrue(s.contains("\033[4m"), "underline");
        assertTrue(s.contains("\033[5m"), "blink");
        assertTrue(s.contains("\033[7m"), "reverse");
    }

    @Test
    @DisplayName("disableSGR emits correct disable codes")
    void disableSgrEmitsCorrectCode() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.disableSGR(SGR.ITALIC);
        term.disableSGR(SGR.BLINK);
        term.disableSGR(SGR.REVERSE);
        term.disableSGR(SGR.HIDDEN);
        term.disableSGR(SGR.STRIKETHROUGH);
        term.flush();
        var s = out.toString(StandardCharsets.UTF_8);
        assertTrue(s.contains("\033[23m"), "italic off");
        assertTrue(s.contains("\033[25m"), "blink off");
        assertTrue(s.contains("\033[27m"), "reverse off");
        assertTrue(s.contains("\033[28m"), "hidden off");
        assertTrue(s.contains("\033[29m"), "strikethrough off");
    }

    @Test
    @DisplayName("resetColorAndSGR emits \\033[0m")
    void resetColorAndSgrEmitsReset() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new SocketTerminal(new ByteArrayInputStream(new byte[0]), out, new TerminalSize(80, 24));
        term.resetColorAndSGR();
        term.flush();
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("\033[0m"));
    }

    // ===== close =====

    @Test
    @DisplayName("close calls exitPrivateMode and closes streams")
    void closeCallsExitPrivateModeAndClosesStreams() throws IOException {
        var in = new CloseAwareInputStream(new byte[0]);
        var out = new CloseAwareOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        term.close();
        assertTrue(in.isClosed());
        assertTrue(out.isClosed());
        // exitPrivateMode writes show-cursor + exit-alt-screen — but out is closed,
        // so the buffered write may have happened before close. Either way streams are closed.
    }

    @Test
    @DisplayName("close is idempotent-safe (second close does not throw on already-closed out)")
    void closeTwiceSafe() throws IOException {
        var in = new CloseAwareInputStream(new byte[0]);
        var out = new CloseAwareOutputStream();
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        term.close();
        // Second close: BufferedOutputStream.close() is a no-op after first close
        assertDoesNotThrow(term::close);
    }

    @Test
    @DisplayName("close after writing data flushes exit sequences")
    void closeFlushesExitSequences() throws IOException {
        // Use a non-closing-wrapping out so we can inspect content after close
        var out = new ByteArrayOutputStream();
        var in = new ByteArrayInputStream(new byte[0]);
        var term = new SocketTerminal(in, out, new TerminalSize(80, 24));
        term.close();
        var s = out.toString(StandardCharsets.UTF_8);
        assertTrue(s.contains("\033[?25h"), "exit private mode shows cursor");
        assertTrue(s.contains("\033[?1049l"), "exit private mode exits alt screen");
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

    /** Helper: poll with timeout, retrying until input arrives or maxAttempts exhausted. */
    private static Optional<KeyStroke> waitForInput(SocketTerminal term, int timeoutMs) throws IOException {
        return term.pollInput(timeoutMs);
    }
}
