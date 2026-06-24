package io.jterm.core;

import io.jterm.core.input.KeyType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge-case tests for {@link AnsiTerminal} that previous suites did not cover,
 * particularly around timeout handling, the closed-input path, and EOF.
 */
class AnsiTerminalEdgeCasesTest {

    private record Holder(AnsiTerminal terminal, ByteArrayOutputStream captured) {
        String output() throws Exception {
            terminal.flush();
            return captured.toString(StandardCharsets.UTF_8);
        }
    }

    private Holder newHolder(byte[] input) {
        var captured = new ByteArrayOutputStream();
        var in = input == null ? new ByteArrayInputStream(new byte[0]) : new ByteArrayInputStream(input);
        var t = new AnsiTerminal(captured, in, new TerminalSize(80, 24));
        return new Holder(t, captured);
    }

    @Test
    void pollInputTimeoutZeroIsNonBlocking() throws Exception {
        var h = newHolder("x".getBytes(StandardCharsets.UTF_8));
        Thread.sleep(20);
        var ks = h.terminal.pollInput(0);
        assertTrue(ks.isPresent());
        assertEquals(KeyType.CHARACTER, ks.get().type());
        assertEquals('x', ks.get().character());
    }

    @Test
    void pollInputReturnsEmptyAfterQueueDrainedAndInputClosed() throws Exception {
        var h = newHolder("a".getBytes(StandardCharsets.UTF_8));
        var first = h.terminal.pollInput(200);
        assertTrue(first.isPresent());
        Thread.sleep(30);
        var second = h.terminal.pollInput();
        assertTrue(second.isEmpty());
    }

    @Test
    void readInputInterruptedReturnsEof() throws Exception {
        var h = newHolder(null);
        Thread reader = Thread.ofVirtual().start(() -> {
            try {
                var ks = h.terminal.readInput();
                assertEquals(KeyType.EOF, ks.type());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(10);
        reader.interrupt();
        reader.join(500);
        assertFalse(reader.isAlive());
    }

    @Test
    void getTerminalSizeHonorsFixedSize() throws Exception {
        var captured = new ByteArrayOutputStream();
        var in = new ByteArrayInputStream(new byte[0]);
        var t = new AnsiTerminal(captured, in, new TerminalSize(132, 60));
        assertEquals(new TerminalSize(132, 60), t.getTerminalSize());
    }

    @Test
    void closeOnTestTerminalDoesNotInvokeStty() throws Exception {
        var h = newHolder(null);
        h.terminal.enterPrivateMode();
        h.captured.reset();
        h.terminal.close();
        String out = h.output();
        assertTrue(out.contains("\033[?25h"));
        assertTrue(out.contains("\033[?1049l"));
    }
}
