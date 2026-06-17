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
