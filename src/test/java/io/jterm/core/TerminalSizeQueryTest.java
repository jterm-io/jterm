package io.jterm.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TerminalSizeQuery} — the ANSI ESC[18t terminal size
 * discovery utility.
 */
class TerminalSizeQueryTest {

    /**
     * A pass-through OutputStream that also captures what was written,
     * so we can verify ESC[18t was actually sent.
     */
    private static final class CapturingOutputStream extends OutputStream {
        final ByteArrayOutputStream captured = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            captured.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            captured.write(b, off, len);
        }

        @Override
        public void write(byte[] b) throws IOException {
            captured.write(b);
        }

        @Override
        public void flush() {
        }

        String output() {
            return captured.toString(StandardCharsets.UTF_8);
        }
    }

    @Test
    void validResponseReturnsCorrectSize() {
        var response = "\033[8;24;80t";
        var in = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
        var out = new CapturingOutputStream();

        var size = TerminalSizeQuery.query(in, out, 500);

        assertNotNull(size);
        assertEquals(80, size.columns());
        assertEquals(24, size.rows());
        // Verify ESC[18t was sent
        assertEquals("\033[18t", out.output());
    }

    @Test
    void validResponseWithLargerTerminal() {
        var response = "\033[8;50;200t";
        var in = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
        var out = new CapturingOutputStream();

        var size = TerminalSizeQuery.query(in, out, 500);

        assertNotNull(size);
        assertEquals(200, size.columns());
        assertEquals(50, size.rows());
    }

    @Test
    void timeoutWithNoResponseReturnsNull() {
        var in = new ByteArrayInputStream(new byte[0]);
        var out = new CapturingOutputStream();

        var size = TerminalSizeQuery.query(in, out, 200);

        assertNull(size);
    }

    @Test
    void malformedResponseReturnsNull() {
        var garbage = "this is not a valid response";
        var in = new ByteArrayInputStream(garbage.getBytes(StandardCharsets.UTF_8));
        var out = new CapturingOutputStream();

        var size = TerminalSizeQuery.query(in, out, 500);

        assertNull(size);
    }

    @Test
    void responseWithExtraDataStillParses() {
        // Some garbage before the valid response
        var data = "garbage\033[8;30;120tmore stuff";
        var in = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        var out = new CapturingOutputStream();

        var size = TerminalSizeQuery.query(in, out, 500);

        assertNotNull(size);
        assertEquals(120, size.columns());
        assertEquals(30, size.rows());
    }

    @Test
    void querySendsEsc18tSequence() {
        var in = new ByteArrayInputStream(new byte[0]);
        var out = new CapturingOutputStream();

        TerminalSizeQuery.query(in, out, 200);

        assertEquals("\033[18t", out.output());
    }

    @Test
    void zeroOrNegativeDimensionsReturnsNull() {
        // rows=0 — invalid
        var response = "\033[8;0;80t";
        var in = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
        var out = new CapturingOutputStream();

        var size = TerminalSizeQuery.query(in, out, 500);

        assertNull(size);
    }

    @Test
    void negativeColsReturnsNull() {
        // cols=0 — invalid
        var response = "\033[8;24;0t";
        var in = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
        var out = new CapturingOutputStream();

        var size = TerminalSizeQuery.query(in, out, 500);

        assertNull(size);
    }
}