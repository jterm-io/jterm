package io.jterm.core;

import io.jterm.core.input.KeyType;
import io.jterm.style.AnsiCodes;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional coverage for {@link SocketTerminal}: CP437 mode, terminal resize
 * events, and the dynamic-size constructor path.
 */
class SocketTerminalExtraTest {

    private record Holder(SocketTerminal terminal, ByteArrayOutputStream captured) {
        String output() throws Exception {
            terminal.flush();
            return captured.toString(StandardCharsets.UTF_8);
        }
    }

    private Holder newHolder(byte[] input) {
        var captured = new ByteArrayOutputStream();
        var in = input == null ? new ByteArrayInputStream(new byte[0]) : new ByteArrayInputStream(input);
        var t = new SocketTerminal(in, captured, new TerminalSize(80, 24));
        return new Holder(t, captured);
    }

    @Test
    void cp437ModeTranslatesUnicodeToCp437() throws Exception {
        var h = newHolder(null);
        h.terminal.setCp437Mode(true);
        assertTrue(h.terminal.isCp437Mode());
        h.terminal.putCharacter('♦');
        h.terminal.flush();
        assertArrayEquals(new byte[]{(byte) 0x04}, h.captured.toByteArray());
    }

    @Test
    void cp437ModeUnknownCharWritesSpace() throws Exception {
        var h = newHolder(null);
        h.terminal.setCp437Mode(true);
        h.terminal.putCharacter("😀".charAt(0));
        h.terminal.flush();
        assertArrayEquals(new byte[]{0x20}, h.captured.toByteArray());
    }

    @Test
    void cp437ModeDisabledWritesUtf8() throws Exception {
        var h = newHolder(null);
        h.terminal.putCharacter('♦');
        h.terminal.flush();
        byte[] expected = "♦".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, h.captured.toByteArray());
    }

    @Test
    void setTerminalSizeNotifiesListeners() throws Exception {
        var h = newHolder(null);
        AtomicReference<TerminalSize> received = new AtomicReference<>();
        h.terminal.addResizeListener(received::set);
        var size = new TerminalSize(100, 40);
        h.terminal.setTerminalSize(size);
        assertEquals(size, received.get());
        assertEquals(size, h.terminal.getTerminalSize());
    }

    @Test
    void setTerminalSizeToSameValueDoesNotNotify() {
        var h = newHolder(null);
        AtomicReference<TerminalSize> received = new AtomicReference<>();
        h.terminal.addResizeListener(received::set);
        h.terminal.setTerminalSize(new TerminalSize(80, 24));
        assertNull(received.get());
    }

    @Test
    void dynamicSizeConstructorFallsBackTo80x24() throws Exception {
        var captured = new ByteArrayOutputStream();
        var in = new ByteArrayInputStream(new byte[0]);
        var t = new SocketTerminal(in, captured);
        var size = t.getTerminalSize();
        assertEquals(80, size.columns());
        assertEquals(24, size.rows());
    }

    @Test
    void dynamicSizeConstructorWithEsc18tResponse() throws Exception {
        // Pre-load input with ESC[8;rows;cols t response
        var response = "\033[8;40;150t";
        var captured = new ByteArrayOutputStream();
        var in = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
        var t = new SocketTerminal(in, captured);
        var size = t.getTerminalSize();
        assertEquals(150, size.columns());
        assertEquals(40, size.rows());
        // Verify ESC[18t was sent
        var outStr = captured.toString(StandardCharsets.UTF_8);
        assertTrue(outStr.contains("\033[18t"), "should send ESC[18t query");
    }

    @Test
    void dynamicSizeConstructorWithEsc18tResponseSmallTerminal() throws Exception {
        var response = "\033[8;25;90t";
        var captured = new ByteArrayOutputStream();
        var in = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
        var t = new SocketTerminal(in, captured);
        var size = t.getTerminalSize();
        assertEquals(90, size.columns());
        assertEquals(25, size.rows());
    }

    @Test
    void pollInputTimeoutZeroReturnsQueuedInput() throws Exception {
        var h = newHolder("k".getBytes(StandardCharsets.UTF_8));
        Thread.sleep(20);
        var ks = h.terminal.pollInput(0);
        assertTrue(ks.isPresent());
        assertEquals(KeyType.CHARACTER, ks.get().type());
        assertEquals('k', ks.get().character());
    }

    @Test
    void closeFlushesExitPrivateModeAndClosesStreams() throws Exception {
        var captured = new ByteArrayOutputStream();
        var in = new ByteArrayInputStream(new byte[0]);
        var t = new SocketTerminal(in, captured, new TerminalSize(80, 24));
        t.enterPrivateMode();
        captured.reset();
        t.close();
        String out = captured.toString(StandardCharsets.UTF_8);
        assertTrue(out.contains(AnsiCodes.SHOW_CURSOR));
        assertTrue(out.contains(AnsiCodes.EXIT_ALT_SCREEN));
    }
}
