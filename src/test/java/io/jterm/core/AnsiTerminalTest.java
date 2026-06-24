package io.jterm.core;

import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.style.AnsiColor;
import io.jterm.style.AnsiCodes;
import io.jterm.style.IndexedColor;
import io.jterm.style.RgbColor;
import io.jterm.style.SGR;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link AnsiTerminal} using a {@link ByteArrayOutputStream}
 * to capture the raw bytes the terminal emits. AnsiTerminal wraps the OutputStream
 * in a BufferedOutputStream, so we call flush() before reading captured output.
 */
class AnsiTerminalTest {

    /**
     * Creates a terminal backed by a ByteArrayOutputStream and returns a holder
     * so we can read the captured output after flushing.
     */
    private static final class TestTerminal {
        final AnsiTerminal terminal;
        final ByteArrayOutputStream captured;

        TestTerminal(byte[] inputBytes) {
            this.captured = new ByteArrayOutputStream();
            var in = inputBytes == null ? new ByteArrayInputStream(new byte[0]) : new ByteArrayInputStream(inputBytes);
            this.terminal = new AnsiTerminal(captured, in, new TerminalSize(80, 24));
        }

        String output() {
            try {
                terminal.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return captured.toString(StandardCharsets.UTF_8);
        }

        byte[] outputBytes() {
            try {
                terminal.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return captured.toByteArray();
        }
    }

    private TestTerminal newTerminal(byte[] inputBytes) {
        return new TestTerminal(inputBytes);
    }

    // ---- writeRaw ----

    @Test
    void writeRawByte() throws Exception {
        var t = newTerminal(null);
        t.terminal.writeRaw(new byte[]{(byte) 'X'});
        assertEquals("X", t.output());
    }

    @Test
    void writeRawMultipleBytes() throws Exception {
        var t = newTerminal(null);
        t.terminal.writeRaw(new byte[]{(byte) 'H', (byte) 'i', (byte) '!'});
        assertEquals("Hi!", t.output());
    }

    @Test
    void writeRawEmptyArray() throws Exception {
        var t = newTerminal(null);
        t.terminal.writeRaw(new byte[0]);
        assertEquals("", t.output());
    }

    @Test
    void writeRawLargeDataFlushesThroughBuffer() throws Exception {
        // 4096 is the buffer size; write enough to force a flush
        var t = newTerminal(null);
        var sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) sb.append('a');
        var bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        t.terminal.writeRaw(bytes);
        t.terminal.flush();
        assertArrayEquals(bytes, t.outputBytes());
    }

    @Test
    void writeRawNullArrayThrowsNpe() {
        var t = newTerminal(null);
        assertThrows(NullPointerException.class, () -> t.terminal.writeRaw(null));
    }

    @Test
    void writeRawUtf8String() throws Exception {
        var t = newTerminal(null);
        t.terminal.writeRaw("héllo".getBytes(StandardCharsets.UTF_8));
        assertEquals("héllo", t.output());
    }

    // ---- putCharacter ----

    @Test
    void putCharacterAscii() throws Exception {
        var t = newTerminal(null);
        t.terminal.putCharacter('A');
        assertEquals("A", t.output());
    }

    @Test
    void putCharacterMultiByte() throws Exception {
        // '€' is 3 bytes in UTF-8
        var t = newTerminal(null);
        t.terminal.putCharacter('€');
        byte[] expected = "€".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, t.outputBytes());
    }

    @Test
    void putCharacterSequence() throws Exception {
        var t = newTerminal(null);
        for (char c : "hello".toCharArray()) t.terminal.putCharacter(c);
        assertEquals("hello", t.output());
    }

    @Test
    void putCharacterNullChar() throws Exception {
        var t = newTerminal(null);
        t.terminal.putCharacter('\0');
        assertArrayEquals(new byte[]{0}, t.outputBytes());
    }

    @Test
    void putCharacterNewline() throws Exception {
        var t = newTerminal(null);
        t.terminal.putCharacter('\n');
        assertEquals("\n", t.output());
    }

    // ---- flush ----

    @Test
    void flushWritesBufferedBytesToUnderlyingStream() throws Exception {
        var t = newTerminal(null);
        t.terminal.putCharacter('a');
        // Before flush, ByteArrayOutputStream may be empty (buffered).
        // After flush it must contain the byte.
        t.terminal.flush();
        assertEquals("a", t.output());
    }

    @Test
    void flushIsIdempotent() throws Exception {
        var t = newTerminal(null);
        t.terminal.putCharacter('a');
        t.terminal.flush();
        t.terminal.flush();
        t.terminal.flush();
        assertEquals("a", t.output());
    }

    @Test
    void flushOnEmptyBufferDoesNotThrow() {
        var t = newTerminal(null);
        assertDoesNotThrow(t.terminal::flush);
    }

    @Test
    void flushAfterNoWritesProducesEmptyOutput() throws Exception {
        var t = newTerminal(null);
        t.terminal.flush();
        assertEquals("", t.output());
    }

    // ---- enterPrivateMode / exitPrivateMode ----

    @Test
    void enterPrivateModeEmitsAltScreenHideCursorClear() throws Exception {
        var t = newTerminal(null);
        t.terminal.enterPrivateMode();
        String out = t.output();
        // order: enter alt screen, hide cursor, clear screen
        String expected = AnsiCodes.ENTER_ALT_SCREEN
            + AnsiCodes.HIDE_CURSOR
            + AnsiCodes.CLEAR_SCREEN;
        assertEquals(expected, out);
    }

    @Test
    void exitPrivateModeEmitsShowCursorAndExitAltScreen() throws Exception {
        var t = newTerminal(null);
        t.terminal.enterPrivateMode();
        t.captured.reset();
        t.terminal.exitPrivateMode();
        String out = t.output();
        assertEquals(AnsiCodes.SHOW_CURSOR + AnsiCodes.EXIT_ALT_SCREEN, out);
    }

    @Test
    void enterThenExitPrivateMode() throws Exception {
        var t = newTerminal(null);
        t.terminal.enterPrivateMode();
        t.terminal.exitPrivateMode();
        String out = t.output();
        assertTrue(out.contains(AnsiCodes.ENTER_ALT_SCREEN));
        assertTrue(out.contains(AnsiCodes.EXIT_ALT_SCREEN));
        assertTrue(out.contains(AnsiCodes.SHOW_CURSOR));
        assertTrue(out.contains(AnsiCodes.HIDE_CURSOR));
        assertTrue(out.contains(AnsiCodes.CLEAR_SCREEN));
    }

    @Test
    void enterPrivateModeFlushesImmediately() throws Exception {
        var t = newTerminal(null);
        t.terminal.enterPrivateMode();
        // enterPrivateMode calls flush() internally, so bytes should be in the captured stream
        assertFalse(t.captured.toByteArray().length == 0);
    }

    // ---- clearScreen ----

    @Test
    void clearScreenEmitsClearSequence() throws Exception {
        var t = newTerminal(null);
        t.terminal.clearScreen();
        assertEquals(AnsiCodes.CLEAR_SCREEN, t.output());
    }

    @Test
    void clearScreenEmits2J() throws Exception {
        var t = newTerminal(null);
        t.terminal.clearScreen();
        assertEquals("\033[2J", t.output());
    }

    @Test
    void clearScreenDoesNotFlush() throws Exception {
        var t = newTerminal(null);
        t.terminal.clearScreen();
        // clearScreen writes raw but doesn't flush — bytes may be buffered
        // After explicit flush, content should be there
        t.terminal.flush();
        assertEquals("\033[2J", t.output());
    }

    // ---- setCursorPosition ----

    @Test
    void setCursorPositionByColRow() throws Exception {
        var t = newTerminal(null);
        // setCursorPosition(column, row)
        t.terminal.setCursorPosition(3, 5);
        // AnsiCodes.cursorTo(row, col) -> CSI + (row+1) + ";" + (col+1) + "H"
        assertEquals("\033[6;4H", t.output());
    }

    @Test
    void setCursorPositionOrigin() throws Exception {
        var t = newTerminal(null);
        t.terminal.setCursorPosition(0, 0);
        assertEquals("\033[1;1H", t.output());
    }

    @Test
    void setCursorPositionLargeValues() throws Exception {
        var t = newTerminal(null);
        t.terminal.setCursorPosition(99, 199);
        assertEquals("\033[200;100H", t.output());
    }

    @Test
    void setCursorPositionNegativeValues() throws Exception {
        // The contract does not clamp negative; verify the math.
        var t = newTerminal(null);
        t.terminal.setCursorPosition(-1, -1);
        assertEquals("\033[0;0H", t.output());
    }

    @Test
    void setCursorPositionMultipleCalls() throws Exception {
        var t = newTerminal(null);
        t.terminal.setCursorPosition(0, 0);
        t.terminal.setCursorPosition(10, 20);
        assertEquals("\033[1;1H\033[21;11H", t.output());
    }

    // ---- setCursorVisible ----

    @Test
    void setCursorVisibleTrueEmitsShowCursor() throws Exception {
        var t = newTerminal(null);
        t.terminal.setCursorVisible(true);
        assertEquals(AnsiCodes.SHOW_CURSOR, t.output());
    }

    @Test
    void setCursorVisibleFalseEmitsHideCursor() throws Exception {
        var t = newTerminal(null);
        t.terminal.setCursorVisible(false);
        assertEquals(AnsiCodes.HIDE_CURSOR, t.output());
    }

    // ---- color / SGR methods ----

    @Test
    void setForegroundColorIndexed() throws Exception {
        var t = newTerminal(null);
        t.terminal.setForegroundColor(new IndexedColor(7));
        assertEquals("\033[38;5;7m", t.output());
    }

    @Test
    void setForegroundColorRgb() throws Exception {
        var t = newTerminal(null);
        t.terminal.setForegroundColor(new RgbColor(255, 128, 0));
        assertEquals("\033[38;2;255;128;0m", t.output());
    }

    @Test
    void setBackgroundColorRgb() throws Exception {
        var t = newTerminal(null);
        t.terminal.setBackgroundColor(new RgbColor(10, 20, 30));
        assertEquals("\033[48;2;10;20;30m", t.output());
    }

    @Test
    void setForegroundColorAnsi() throws Exception {
        var t = newTerminal(null);
        t.terminal.setForegroundColor(AnsiColor.RED);
        assertEquals("\033[31m", t.output());
    }

    @Test
    void setBackgroundColorAnsi() throws Exception {
        var t = newTerminal(null);
        t.terminal.setBackgroundColor(AnsiColor.BRIGHT_BLUE);
        assertEquals("\033[104m", t.output());
    }

    @Test
    void setForegroundColorDefault() throws Exception {
        var t = newTerminal(null);
        t.terminal.setForegroundColor(AnsiColor.DEFAULT);
        assertEquals("\033[39m", t.output());
    }

    @Test
    void setBackgroundColorDefault() throws Exception {
        var t = newTerminal(null);
        t.terminal.setBackgroundColor(AnsiColor.DEFAULT);
        assertEquals("\033[49m", t.output());
    }

    @Test
    void enableSGRBold() throws Exception {
        var t = newTerminal(null);
        t.terminal.enableSGR(SGR.BOLD);
        assertEquals("\033[1m", t.output());
    }

    @Test
    void enableSGRUnderline() throws Exception {
        var t = newTerminal(null);
        t.terminal.enableSGR(SGR.UNDERLINE);
        assertEquals("\033[4m", t.output());
    }

    @Test
    void enableSGRAll() throws Exception {
        var t = newTerminal(null);
        for (var sgr : SGR.values()) {
            t.terminal.enableSGR(sgr);
        }
        String out = t.output();
        assertTrue(out.contains("\033[1m"));  // BOLD
        assertTrue(out.contains("\033[2m"));  // DIM
        assertTrue(out.contains("\033[3m"));  // ITALIC
        assertTrue(out.contains("\033[4m"));  // UNDERLINE
        assertTrue(out.contains("\033[5m"));  // BLINK
        assertTrue(out.contains("\033[7m"));  // REVERSE
        assertTrue(out.contains("\033[8m"));  // HIDDEN
        assertTrue(out.contains("\033[9m"));  // STRIKETHROUGH
    }

    @Test
    void disableSGRBold() throws Exception {
        var t = newTerminal(null);
        t.terminal.disableSGR(SGR.BOLD);
        assertEquals("\033[22m", t.output());
    }

    @Test
    void disableSGRItalic() throws Exception {
        var t = newTerminal(null);
        t.terminal.disableSGR(SGR.ITALIC);
        assertEquals("\033[23m", t.output());
    }

    @Test
    void disableSGRAll() throws Exception {
        var t = newTerminal(null);
        for (var sgr : SGR.values()) {
            t.terminal.disableSGR(sgr);
        }
        String out = t.output();
        assertTrue(out.contains("\033[22m"));  // BOLD/DIM
        assertTrue(out.contains("\033[23m"));  // ITALIC
        assertTrue(out.contains("\033[24m"));  // UNDERLINE
        assertTrue(out.contains("\033[25m"));  // BLINK
        assertTrue(out.contains("\033[27m"));  // REVERSE
        assertTrue(out.contains("\033[28m"));  // HIDDEN
        assertTrue(out.contains("\033[29m"));  // STRIKETHROUGH
    }

    @Test
    void resetColorAndSGR() throws Exception {
        var t = newTerminal(null);
        t.terminal.resetColorAndSGR();
        assertEquals("\033[0m", t.output());
    }

    // ---- combined sequences ----

    @Test
    void multipleWritesAccumulate() throws Exception {
        var t = newTerminal(null);
        t.terminal.setForegroundColor(AnsiColor.GREEN);
        t.terminal.putCharacter('H');
        t.terminal.putCharacter('i');
        t.terminal.resetColorAndSGR();
        assertEquals("\033[32mHi\033[0m", t.output());
    }

    @Test
    void writeRawThenFlushThenWriteMore() throws Exception {
        var t = newTerminal(null);
        t.terminal.writeRaw("abc".getBytes(StandardCharsets.UTF_8));
        t.terminal.flush();
        t.terminal.writeRaw("def".getBytes(StandardCharsets.UTF_8));
        assertEquals("abcdef", t.output());
    }

    @Test
    void fullSequenceColorTextReset() throws Exception {
        var t = newTerminal(null);
        t.terminal.setForegroundColor(AnsiColor.RED);
        t.terminal.enableSGR(SGR.BOLD);
        t.terminal.putCharacter('E');
        t.terminal.putCharacter('r');
        t.terminal.putCharacter('r');
        t.terminal.resetColorAndSGR();
        assertEquals("\033[31m\033[1mErr\033[0m", t.output());
    }

    // ---- getTerminalSize ----

    @Test
    void getTerminalSizeReturnsFixedSize() throws Exception {
        var t = newTerminal(null);
        var size = t.terminal.getTerminalSize();
        assertEquals(80, size.columns());
        assertEquals(24, size.rows());
    }

    @Test
    void getTerminalSizeReturnsCustomFixedSize() throws Exception {
        var captured = new ByteArrayOutputStream();
        var in = new ByteArrayInputStream(new byte[0]);
        var term = new AnsiTerminal(captured, in, new TerminalSize(120, 40));
        var size = term.getTerminalSize();
        assertEquals(120, size.columns());
        assertEquals(40, size.rows());
    }

    // ---- pollInput / readInput ----

    @Test
    void pollInputReturnsEmptyWhenNoData() throws Exception {
        var t = newTerminal(null);
        // Give the reader thread a moment to discover the stream is empty
        Thread.sleep(10);
        assertEquals(Optional.empty(), t.terminal.pollInput());
    }

    @Test
    void pollInputReturnsCharacter() throws Exception {
        var t = newTerminal("a".getBytes(StandardCharsets.UTF_8));
        var ks = t.terminal.pollInput(200);
        assertTrue(ks.isPresent());
        assertEquals(KeyType.CHARACTER, ks.get().type());
        assertEquals('a', ks.get().character());
    }

    @Test
    void pollInputReturnsEscape() throws Exception {
        var t = newTerminal(new byte[]{0x1b});
        var ks = t.terminal.pollInput(200);
        assertTrue(ks.isPresent());
        assertEquals(KeyType.ESCAPE, ks.get().type());
    }

    @Test
    void pollInputReturnsArrowUp() throws Exception {
        var t = newTerminal("\033[A".getBytes(StandardCharsets.UTF_8));
        var ks = t.terminal.pollInput(200);
        assertTrue(ks.isPresent());
        assertEquals(KeyType.ARROW_UP, ks.get().type());
    }

    @Test
    void readInputBlocksUntilData() throws Exception {
        var t = newTerminal("z".getBytes(StandardCharsets.UTF_8));
        var ks = t.terminal.readInput();
        assertEquals(KeyType.CHARACTER, ks.type());
        assertEquals('z', ks.character());
    }

    @Test
    void pollInputAfterConsumingAllReturnsEmpty() throws Exception {
        var t = newTerminal("a".getBytes(StandardCharsets.UTF_8));
        t.terminal.pollInput(200);
        Thread.sleep(10); // give reader thread time to finish
        assertEquals(Optional.empty(), t.terminal.pollInput());
    }

    // ---- resize listeners ----

    @Test
    void addRemoveResizeListenerDoesNotThrow() {
        var t = newTerminal(null);
        TerminalResizeListener listener = size -> {};
        assertDoesNotThrow(() -> t.terminal.addResizeListener(listener));
        assertDoesNotThrow(() -> t.terminal.removeResizeListener(listener));
    }

    @Test
    void removeUnregisteredListenerDoesNotThrow() {
        var t = newTerminal(null);
        TerminalResizeListener listener = size -> {};
        assertDoesNotThrow(() -> t.terminal.removeResizeListener(listener));
    }

    @Test
    void addMultipleListeners() {
        var t = newTerminal(null);
        TerminalResizeListener l1 = size -> {};
        TerminalResizeListener l2 = size -> {};
        t.terminal.addResizeListener(l1);
        t.terminal.addResizeListener(l2);
        t.terminal.removeResizeListener(l1);
        t.terminal.removeResizeListener(l2);
    }

    // ---- close ----

    @Test
    void closeEmitsExitPrivateModeSequence() throws Exception {
        var t = newTerminal(null);
        t.terminal.enterPrivateMode();
        t.captured.reset();
        t.terminal.close();
        String out = t.output();
        assertEquals(AnsiCodes.SHOW_CURSOR + AnsiCodes.EXIT_ALT_SCREEN, out);
    }

    @Test
    void closeCallsExitPrivateMode() throws Exception {
        var t = newTerminal(null);
        t.terminal.enterPrivateMode();
        t.captured.reset();
        t.terminal.close();
        String out = t.output();
        assertTrue(out.contains(AnsiCodes.SHOW_CURSOR));
        assertTrue(out.contains(AnsiCodes.EXIT_ALT_SCREEN));
    }

    // ---- getCapturedOutput ----

    @Test
    void getCapturedOutputReturnsEmptyString() {
        var t = newTerminal(null);
        // The current implementation returns "" — document the contract.
        assertEquals("", t.terminal.getCapturedOutput());
    }
}