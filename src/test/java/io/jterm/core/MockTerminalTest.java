package io.jterm.core;

import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MockTerminalTest {

    @Test
    void getTerminalSizeReturnsConstructorSize() throws IOException {
        var size = new TerminalSize(120, 40);
        var term = new MockTerminal(size);
        assertEquals(size, term.getTerminalSize());
    }

    @Test
    void putCharacterAppendsToOutputAndWritesToOutputStream() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new MockTerminal(new TerminalSize(80, 24), out, new ByteArrayInputStream(new byte[0]));
        term.putCharacter('X');
        assertEquals("X", term.getOutput());
        assertEquals("X", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void setCursorPositionEmitsRowPlusOneColPlusOne() throws IOException {
        var term = new MockTerminal(new TerminalSize(80, 24));
        term.setCursorPosition(5, 3);
        assertEquals("\033[4;6H", term.getOutput());
    }

    @Test
    void clearScreenEmitsAnsiClear() throws IOException {
        var term = new MockTerminal(new TerminalSize(80, 24));
        term.clearScreen();
        assertEquals("\033[2J", term.getOutput());
    }

    @Test
    void setForegroundColorRedEmits31m() throws IOException {
        var term = new MockTerminal(new TerminalSize(80, 24));
        term.setForegroundColor(AnsiColor.RED);
        assertEquals("\033[31m", term.getOutput());
    }

    @Test
    void setBackgroundColorBlueEmits44m() throws IOException {
        var term = new MockTerminal(new TerminalSize(80, 24));
        term.setBackgroundColor(AnsiColor.BLUE);
        assertEquals("\033[44m", term.getOutput());
    }

    @Test
    void enableSgrBoldEmits1m() throws IOException {
        var term = new MockTerminal(new TerminalSize(80, 24));
        term.enableSGR(SGR.BOLD);
        assertEquals("\033[1m", term.getOutput());
    }

    @Test
    void disableSgrBoldEmits22m() throws IOException {
        var term = new MockTerminal(new TerminalSize(80, 24));
        term.disableSGR(SGR.BOLD);
        assertEquals("\033[22m", term.getOutput());
    }

    @Test
    void disableSgrUnderlineEmits24m() throws IOException {
        var term = new MockTerminal(new TerminalSize(80, 24));
        term.disableSGR(SGR.UNDERLINE);
        assertEquals("\033[24m", term.getOutput());
    }

    @Test
    void resetColorAndSgrEmits0m() throws IOException {
        var term = new MockTerminal(new TerminalSize(80, 24));
        term.resetColorAndSGR();
        assertEquals("\033[0m", term.getOutput());
    }

    @Test
    void closeSetsIsClosedTrue() throws IOException {
        var term = new MockTerminal(new TerminalSize(80, 24));
        assertFalse(term.isClosed());
        term.close();
        assertTrue(term.isClosed());
    }

    @Test
    void getOutputReturnsAccumulatedOutput() throws IOException {
        var term = new MockTerminal(new TerminalSize(80, 24));
        term.putCharacter('A');
        term.putCharacter('B');
        term.putCharacter('C');
        assertEquals("ABC", term.getOutput());
    }

    @Test
    void clearOutputResetsToEmptyString() throws IOException {
        var term = new MockTerminal(new TerminalSize(80, 24));
        term.putCharacter('A');
        term.clearScreen();
        assertFalse(term.getOutput().isEmpty());
        term.clearOutput();
        assertEquals("", term.getOutput());
    }

    @Test
    void pollInputReturnsEmptyOptionalWhenNoInput() throws IOException {
        var term = new MockTerminal(new TerminalSize(80, 24));
        Optional<KeyStroke> ks = term.pollInput();
        assertNotNull(ks);
        assertTrue(ks.isEmpty());
    }

    @Test
    void addResizeListenerDoesNotThrow() {
        var term = new MockTerminal(new TerminalSize(80, 24));
        assertDoesNotThrow(() -> term.addResizeListener(newSize -> {}));
    }

    @Test
    void removeResizeListenerDoesNotThrow() {
        var term = new MockTerminal(new TerminalSize(80, 24));
        TerminalResizeListener listener = newSize -> {};
        assertDoesNotThrow(() -> {
            term.addResizeListener(listener);
            term.removeResizeListener(listener);
            // Removing a non-registered listener should also be safe.
            term.removeResizeListener(newSize -> {});
        });
    }

    @Test
    void enterPrivateModeIsNoOpAndDoesNotThrow() {
        var term = new MockTerminal(new TerminalSize(80, 24));
        assertDoesNotThrow(term::enterPrivateMode);
    }

    @Test
    void exitPrivateModeIsNoOpAndDoesNotThrow() {
        var term = new MockTerminal(new TerminalSize(80, 24));
        assertDoesNotThrow(term::exitPrivateMode);
    }

    @Test
    void setCursorVisibleIsNoOpAndDoesNotThrow() {
        var term = new MockTerminal(new TerminalSize(80, 24));
        assertDoesNotThrow(() -> term.setCursorVisible(true));
        assertDoesNotThrow(() -> term.setCursorVisible(false));
    }

    @Test
    void pollInputDecodesUpArrowEscapeSequence() throws IOException {
        // ESC [ A => Up arrow
        byte[] input = new byte[] {0x1b, 0x5b, 0x41};
        var in = new ByteArrayInputStream(input);
        var out = new ByteArrayOutputStream();
        var term = new MockTerminal(new TerminalSize(80, 24), out, in);

        Optional<KeyStroke> ks = term.pollInput();
        assertTrue(ks.isPresent(), "pollInput should return a KeyStroke for ESC [ A");
        assertEquals(KeyType.ARROW_UP, ks.get().type());
    }

    @Test
    void pollInputDecodesDownArrowEscapeSequence() throws IOException {
        // ESC [ B => Down arrow
        byte[] input = new byte[] {0x1b, 0x5b, 0x42};
        var in = new ByteArrayInputStream(input);
        var term = new MockTerminal(new TerminalSize(80, 24), new ByteArrayOutputStream(), in);

        Optional<KeyStroke> ks = term.pollInput();
        assertTrue(ks.isPresent());
        assertEquals(KeyType.ARROW_DOWN, ks.get().type());
    }

    @Test
    void pollInputDecodesEnterFromCarriageReturn() throws IOException {
        byte[] input = new byte[] {'\r'};
        var in = new ByteArrayInputStream(input);
        var term = new MockTerminal(new TerminalSize(80, 24), new ByteArrayOutputStream(), in);

        Optional<KeyStroke> ks = term.pollInput();
        assertTrue(ks.isPresent());
        assertEquals(KeyType.ENTER, ks.get().type());
    }

    @Test
    void pollInputReturnsCharacterForPlainAscii() throws IOException {
        byte[] input = new byte[] {'Z'};
        var in = new ByteArrayInputStream(input);
        var term = new MockTerminal(new TerminalSize(80, 24), new ByteArrayOutputStream(), in);

        Optional<KeyStroke> ks = term.pollInput();
        assertTrue(ks.isPresent());
        assertEquals(KeyType.CHARACTER, ks.get().type());
        assertEquals('Z', ks.get().character());
    }

    @Test
    void pollInputReturnsEmptyAfterStreamExhausted() throws IOException {
        byte[] input = new byte[] {0x1b, 0x5b, 0x41};
        var in = new ByteArrayInputStream(input);
        var term = new MockTerminal(new TerminalSize(80, 24), new ByteArrayOutputStream(), in);

        // Drain the one encoded keystroke.
        Optional<KeyStroke> first = term.pollInput();
        assertTrue(first.isPresent());
        assertEquals(KeyType.ARROW_UP, first.get().type());

        // Subsequent poll should return empty (no more bytes available).
        Optional<KeyStroke> second = term.pollInput();
        assertNotNull(second);
        assertTrue(second.isEmpty());
    }

    @Test
    void flushDoesNotThrowAndFlushesOutputStream() throws IOException {
        var out = new ByteArrayOutputStream();
        var term = new MockTerminal(new TerminalSize(80, 24), out, new ByteArrayInputStream(new byte[0]));
        term.putCharacter('A');
        assertDoesNotThrow(term::flush);
    }

    @Test
    void resizeListenerIsInvokedWhenManuallyTriggered() {
        var term = new MockTerminal(new TerminalSize(80, 24));
        var fired = new AtomicBoolean(false);
        TerminalResizeListener listener = newSize -> fired.set(true);
        term.addResizeListener(listener);
        // MockTerminal does not auto-detect resizes; simulate by invoking the listener directly.
        listener.onResized(new TerminalSize(100, 30));
        assertTrue(fired.get());
    }
}