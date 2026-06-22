package io.jterm.core;

import io.jterm.core.input.KeyStroke;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import java.io.IOException;
import java.util.Optional;

/** Lowest-level terminal interface. Controls cursor, colors, SGR, and raw output. */
public interface Terminal extends AutoCloseable {
    void enterPrivateMode() throws IOException;
    void exitPrivateMode() throws IOException;
    void clearScreen() throws IOException;

    void setCursorPosition(int column, int row) throws IOException;
    void setCursorVisible(boolean visible) throws IOException;

    void putCharacter(char c) throws IOException;
    void flush() throws IOException;

    void setForegroundColor(Color color) throws IOException;
    void setBackgroundColor(Color color) throws IOException;
    void enableSGR(SGR sgr) throws IOException;
    void disableSGR(SGR sgr) throws IOException;
    void resetColorAndSGR() throws IOException;

    TerminalSize getTerminalSize() throws IOException;

    Optional<KeyStroke> pollInput() throws IOException;
    KeyStroke readInput() throws IOException;

    /**
     * Polls for input with a timeout. Returns the next keystroke if available
     * within {@code timeoutMillis}, or an empty Optional if the timeout elapses.
     * A timeout of 0 behaves like {@link #pollInput()} (non-blocking).
     */
    default Optional<KeyStroke> pollInput(long timeoutMillis) throws IOException {
        // Default implementation: poll once, then sleep in small increments
        // Real implementations should override with a BlockingQueue for true blocking.
        var ks = pollInput();
        if (ks.isPresent()) return ks;
        if (timeoutMillis <= 0) return Optional.empty();
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (ks.isEmpty() && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(1); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
            ks = pollInput();
        }
        return ks;
    }

    void addResizeListener(TerminalResizeListener listener);
    void removeResizeListener(TerminalResizeListener listener);

    @Override
    void close() throws IOException;

    /** Write raw bytes directly to the terminal output. */
    default void writeRaw(byte[] data) throws IOException {
        for (byte b : data) {
            putCharacter((char) b);
        }
    }
}
